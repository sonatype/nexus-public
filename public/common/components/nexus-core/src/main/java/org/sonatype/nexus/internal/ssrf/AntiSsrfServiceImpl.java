/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.ssrf;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ValidationException;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.KeyValueEvent;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;
import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import com.google.common.net.InetAddresses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Implementation of {@link AntiSsrfService} backed by {@link GlobalKeyValueStore} for DB persistence
 * and cluster replication via {@link KeyValueEvent}.
 *
 * On first startup, seeds the DB from nexus.properties defaults. Subsequent startups load from DB.
 * Cloud metadata endpoint (169.254.169.254) is always blocked regardless of configuration.
 */
@ManagedLifecycle(phase = Phase.SERVICES)
@Component
public class AntiSsrfServiceImpl
    extends StateGuardLifecycleSupport
    implements AntiSsrfService, EventAware
{
  static final String CONFIG_KEY = "ssrf.protection.config";

  static final String CLOUD_METADATA_ADDRESS = "169.254.169.254";

  private static final Set<String> IPV6_LOOPBACK_ADDRESSES = Set.of("::1", "0:0:0:0:0:0:0:1");

  private final GlobalKeyValueStore kvStore;

  private final boolean propsEnabled;

  private final Set<String> propsAllowedIPs;

  private final Set<String> propsAllowedDomains;

  private final LoadingCache<String, Optional<String>> hostValidationCache;

  private volatile SsrfProtectionConfiguration currentConfig;

  @Autowired
  public AntiSsrfServiceImpl(
      final GlobalKeyValueStore kvStore,
      @Value("${nexus.proxy.allowPrivateNetworks:true}") final boolean allowPrivateNetworks,
      @Value("${nexus.proxy.privateNetworks.allowedIPs:}") final String[] allowedIPs,
      @Value("${nexus.proxy.privateNetworks.allowedDomains:}") final String[] allowedDomains,
      @Value("${nexus.proxy.privateNetworks.cacheSize:1000}") final int cacheSize,
      @Value("${nexus.proxy.privateNetworks.cacheExpiration:10m}") final Duration cacheExpiration)
  {
    this.kvStore = checkNotNull(kvStore);
    this.propsEnabled = !allowPrivateNetworks;
    this.propsAllowedIPs = toTrimmedSet(allowedIPs);
    this.propsAllowedDomains = toTrimmedSet(allowedDomains);
    this.hostValidationCache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheExpiration)
        .build(new CacheLoader<>()
        {
          @Override
          public Optional<String> load(final String host) {
            return performValidation(host);
          }
        });
  }

  @Override
  protected void doStart() {
    try {
      Optional<SsrfProtectionConfigData> stored = kvStore.get(CONFIG_KEY, SsrfProtectionConfigData.class);
      if (stored.isPresent()) {
        currentConfig = stored.get().toConfiguration();
        log.info("SSRF protection loaded from database: enabled={}, allowedIPs={}, allowedDomains={}",
            currentConfig.enabled(), currentConfig.allowedIPs().size(), currentConfig.allowedDomains().size());
      }
      else {
        currentConfig = new SsrfProtectionConfiguration(
            propsEnabled,
            filterCloudMetadataFromProps(propsAllowedIPs),
            filterCloudMetadataFromProps(propsAllowedDomains));
        persistConfig(currentConfig);
        log.info("SSRF protection seeded from nexus.properties: enabled={}, allowedIPs={}, allowedDomains={}",
            currentConfig.enabled(), currentConfig.allowedIPs().size(), currentConfig.allowedDomains().size());
      }
    }
    catch (Exception e) {
      log.error("Failed to load or persist SSRF protection config, using defaults", e);
      currentConfig = new SsrfProtectionConfiguration(
          propsEnabled,
          filterCloudMetadataFromProps(propsAllowedIPs),
          filterCloudMetadataFromProps(propsAllowedDomains));
    }
  }

  @Subscribe
  public void on(final KeyValueEvent event) {
    if (EventHelper.isReplicating() && CONFIG_KEY.equals(event.getKey())) {
      Object value = event.getValue();
      if (value instanceof Map) {
        try {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) value;
          currentConfig = SsrfProtectionConfigData.fromMap(map).toConfiguration();
          hostValidationCache.invalidateAll();
          log.info("SSRF protection config updated via cluster replication");
        }
        catch (Exception e) {
          log.warn("Failed to process SSRF protection config from cluster event", e);
        }
      }
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void validateHost(final String host) {
    doValidateHost(host, true);
  }

  @Override
  @Guarded(by = STARTED)
  public void validateHostWithoutCache(final String host) {
    doValidateHost(host, false);
  }

  @Override
  @Guarded(by = STARTED)
  public SsrfProtectionConfiguration getConfiguration() {
    return currentConfig;
  }

  @Override
  @Guarded(by = STARTED)
  public void updateConfiguration(final SsrfProtectionConfiguration configuration) {
    checkNotNull(configuration);

    SsrfProtectionConfiguration sanitized = sanitizeConfiguration(configuration);
    persistConfig(sanitized);
    currentConfig = sanitized;
    hostValidationCache.invalidateAll();
    log.info("SSRF protection config updated: enabled={}, allowedIPs={}, allowedDomains={}",
        sanitized.enabled(), sanitized.allowedIPs().size(), sanitized.allowedDomains().size());
  }

  private void doValidateHost(final String host, final boolean useCache) {
    if (host == null || host.isBlank()) {
      throw new ValidationException("Host cannot be null or empty");
    }

    if (!useCache) {
      hostValidationCache.invalidate(host);
    }

    hostValidationCache.getUnchecked(host).ifPresent(message -> {
      throw new ValidationException(message);
    });
  }

  private Optional<String> performValidation(final String host) {
    SsrfProtectionConfiguration config = currentConfig;

    InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(host);
    }
    catch (UnknownHostException e) {
      log.debug("Failed to resolve host: {}", host, e);
      if (!config.enabled() || config.allowedDomains().contains(host)) {
        return Optional.empty();
      }
      return Optional.of("Failed to resolve host: " + host);
    }

    if (isAllowed(host, addresses, config)) {
      log.debug("Host {} is in allow list, bypassing validation", host);
      return Optional.empty();
    }

    List<String> rejectedAddresses = new ArrayList<>();
    for (InetAddress address : addresses) {
      String rejectionReason = validateIpAddress(address, config);
      if (rejectionReason != null) {
        rejectedAddresses.add(address.getHostAddress() + " (" + rejectionReason + ")");
      }
    }

    if (!rejectedAddresses.isEmpty()) {
      String errorMessage = "Host resolves to private/local IP address(es): " + String.join(", ", rejectedAddresses)
          + ". To allow connections, update the SSRF protection settings via the API at /v1/security/ssrf-protection.";
      log.debug("Blocked access to private/local network host: {} - {}", host, errorMessage);
      return Optional.of(errorMessage);
    }

    return Optional.empty();
  }

  private String validateIpAddress(final InetAddress address, final SsrfProtectionConfiguration config) {
    // Check for IPv6-mapped IPv4 addresses (e.g., ::ffff:127.0.0.1) which bypass
    // Java's isLoopbackAddress/isSiteLocalAddress/isLinkLocalAddress checks
    if (address instanceof Inet6Address inet6 && isIPv4Mapped(inet6)) {
      try {
        byte[] raw = inet6.getAddress();
        InetAddress embedded = InetAddress.getByAddress(Arrays.copyOfRange(raw, 12, 16));
        String embeddedResult = validateIpAddress(embedded, config);
        if (embeddedResult != null) {
          return embeddedResult;
        }
      }
      catch (UnknownHostException e) {
        log.debug("Failed to extract embedded IPv4 from IPv6-mapped address", e);
      }
    }

    String hostAddress = address.getHostAddress();

    if (hostAddress.equals(CLOUD_METADATA_ADDRESS)) {
      return "restricted address";
    }

    if (config.enabled()) {
      if (address.isLoopbackAddress() || IPV6_LOOPBACK_ADDRESSES.contains(hostAddress)) {
        return "loopback address";
      }

      if (address.isLinkLocalAddress()) {
        return "link-local address";
      }

      if (address.isSiteLocalAddress()) {
        return "private network address";
      }
    }

    return null;
  }

  private boolean isAllowed(
      final String host,
      final InetAddress[] addresses,
      final SsrfProtectionConfiguration config)
  {
    boolean hasCloudMetadata = Stream.of(addresses)
        .anyMatch(this::isCloudMetadataAddress);

    if (hasCloudMetadata) {
      log.warn("Host {} resolves to cloud metadata address - blocking", host);
      return false;
    }

    return config.allowedDomains().contains(host)
        || Stream.of(addresses)
            .map(InetAddress::getHostAddress)
            .allMatch(ip -> isAllowedIP(ip, config.allowedIPs()));
  }

  /**
   * Checks if an IP is in the allowlist, with special handling for loopback addresses.
   * Because a single logical loopback address has many textual representations
   * (e.g. {@code 127.0.0.1}, {@code 127.0.0.2}, {@code ::1}, {@code 0:0:0:0:0:0:0:1},
   * {@code ::ffff:127.0.0.1}), any loopback entry in the allowlist is treated as permitting
   * every other loopback form. This mirrors the intent of "allow loopback" while
   * accommodating the different strings that {@link InetAddress#getHostAddress()} may return.
   */
  private boolean isAllowedIP(final String ip, final Set<String> allowedIPs) {
    if (allowedIPs.contains(ip)) {
      return true;
    }

    if (isLoopbackLiteral(ip)) {
      return allowedIPs.stream().anyMatch(AntiSsrfServiceImpl::isLoopbackLiteral);
    }

    return false;
  }

  /**
   * Returns {@code true} if the given string is a valid IP literal that represents a loopback
   * address. Returns {@code false} for hostnames, invalid input, or non-loopback IPs.
   * No DNS resolution is performed.
   */
  private static boolean isLoopbackLiteral(final String ip) {
    if (ip == null || !InetAddresses.isInetAddress(ip)) {
      return false;
    }
    return InetAddresses.forString(ip).isLoopbackAddress();
  }

  private boolean isCloudMetadataAddress(final InetAddress address) {
    if (CLOUD_METADATA_ADDRESS.equals(address.getHostAddress())) {
      return true;
    }
    // Check IPv6-mapped form (::ffff:169.254.169.254)
    if (address instanceof Inet6Address inet6 && isIPv4Mapped(inet6)) {
      try {
        byte[] raw = inet6.getAddress();
        InetAddress embedded = InetAddress.getByAddress(Arrays.copyOfRange(raw, 12, 16));
        return CLOUD_METADATA_ADDRESS.equals(embedded.getHostAddress());
      }
      catch (UnknownHostException e) {
        log.debug("Failed to extract embedded IPv4 from IPv6-mapped address", e);
      }
    }
    return false;
  }

  private SsrfProtectionConfiguration sanitizeConfiguration(final SsrfProtectionConfiguration config) {
    Consumer<String> rejectCloudMetadata =
        e -> {
          throw new ValidationErrorsException("Cloud metadata address " + e + " cannot be added to allow list");
        };
    return new SsrfProtectionConfiguration(
        config.enabled(),
        processEntries(config.allowedIPs(), rejectCloudMetadata),
        processEntries(config.allowedDomains(), rejectCloudMetadata));
  }

  private Set<String> filterCloudMetadataFromProps(final Set<String> entries) {
    List<String> removed = new ArrayList<>();
    Set<String> filtered = processEntries(entries, removed::add);
    if (!removed.isEmpty()) {
      log.warn("Removed cloud metadata address(es) from nexus.properties allow list: {}", removed);
    }
    return filtered;
  }

  private Set<String> processEntries(final Set<String> entries, final Consumer<String> onCloudMetadata) {
    return entries.stream()
        .map(String::trim)
        .filter(Strings2::notBlank)
        .filter(e -> {
          if (isCloudMetadataIp(e)) {
            onCloudMetadata.accept(e);
            return false;
          }
          return true;
        })
        .collect(Collectors.toSet());
  }

  private void persistConfig(final SsrfProtectionConfiguration config) {
    SsrfProtectionConfigData data = SsrfProtectionConfigData.from(config);
    kvStore.setString(CONFIG_KEY, data);
  }

  private Set<String> toTrimmedSet(final String[] values) {
    if (values == null || values.length == 0) {
      return Set.of();
    }
    return Stream.of(values)
        .map(String::trim)
        .filter(Strings2::notBlank)
        .collect(Collectors.toSet());
  }

  private boolean isCloudMetadataIp(final String ip) {
    if (CLOUD_METADATA_ADDRESS.equals(ip)) {
      return true;
    }
    try {
      InetAddress addr = InetAddress.getByName(ip);
      return isCloudMetadataAddress(addr);
    }
    catch (UnknownHostException e) {
      return false;
    }
  }

  /**
   * Checks if an IPv6 address is an IPv4-mapped address (::ffff:x.x.x.x).
   * These have bytes 10-11 set to 0xff and bytes 0-9 set to 0x00.
   */
  private static boolean isIPv4Mapped(final Inet6Address address) {
    byte[] bytes = address.getAddress();
    if (bytes.length != 16) {
      return false;
    }
    for (int i = 0; i < 10; i++) {
      if (bytes[i] != 0) {
        return false;
      }
    }
    return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
  }
}
