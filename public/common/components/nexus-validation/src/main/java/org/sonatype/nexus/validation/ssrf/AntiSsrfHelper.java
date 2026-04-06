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
package org.sonatype.nexus.validation.ssrf;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper to validate hostnames and IP addresses to prevent Server-Side Request Forgery (SSRF) attacks.
 * Performs DNS resolution and checks if the target resolves to restricted addresses. When private network
 * access is disabled, blocks loopback addresses (127.0.0.0/8, ::1), link-local addresses (169.254.0.0/16),
 * and private networks (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16). Cloud metadata endpoint (169.254.169.254)
 * is always blocked regardless of configuration. Supports IP and domain allow lists via configuration properties.
 */
@Component
public class AntiSsrfHelper
    extends ComponentSupport
{
  private static final String CLOUD_METADATA_ADDRESS = "169.254.169.254";

  private static final Set<String> IPV6_LOOPBACK_ADDRESSES = Set.of("::1", "0:0:0:0:0:0:0:1");

  private final boolean allowPrivateNetworks;

  private final Set<String> allowedIPs;

  private final Set<String> allowedDomains;

  private final LoadingCache<String, SsrfValidationResult> hostValidationCache;

  public AntiSsrfHelper(
      @Value("${nexus.proxy.allowPrivateNetworks:true}") final boolean allowPrivateNetworks,
      @Value("${nexus.proxy.privateNetworks.allowedIPs:}") final String[] allowedIPs,
      @Value("${nexus.proxy.privateNetworks.allowedDomains:}") final String[] allowedDomains,
      @Value("${nexus.proxy.privateNetworks.cacheSize:1000}") final int cacheSize,
      @Value("${nexus.proxy.privateNetworks.cacheExpiration:10m}") final Duration cacheExpiration)
  {
    this.allowPrivateNetworks = allowPrivateNetworks;
    this.allowedIPs = toTrimmedSet(allowedIPs);
    this.allowedDomains = toTrimmedSet(allowedDomains);
    this.hostValidationCache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheExpiration)
        .build(new CacheLoader<>()
        {
          @Override
          public SsrfValidationResult load(final String host) {
            return performValidation(host);
          }
        });

    validateAllowListConfiguration();
  }

  /**
   * Validates a hostname or IP address for runtime proxy requests. Results are cached to improve performance.
   *
   * @param host the hostname or IP address to validate
   * @return validation result indicating if the host is allowed
   */
  public SsrfValidationResult validateHost(final String host) {
    return validateHost(host, true);
  }

  /**
   * Validates a hostname or IP address during repository configuration. Does not use caching to ensure
   * configuration changes are validated with current settings.
   *
   * @param host the hostname or IP address to validate
   * @return validation result indicating if the host is allowed
   */
  public SsrfValidationResult validateHostForConfiguration(final String host) {
    return validateHost(host, false);
  }

  private SsrfValidationResult validateHost(final String host, final boolean useCache) {
    if (host == null || Strings.isNullOrEmpty(host) || host.trim().isEmpty()) {
      return SsrfValidationResult.failure("Host cannot be null or empty");
    }

    if (useCache) {
      try {
        return hostValidationCache.get(host);
      }
      catch (ExecutionException e) {
        log.warn("Cache execution error for host: {}, falling back to direct validation", host, e);
        return performValidation(host);
      }
    }

    return performValidation(host);
  }

  private SsrfValidationResult performValidation(final String host) {
    InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(host);
    }
    catch (UnknownHostException e) {
      log.debug("Failed to resolve host: {}", host, e);
      if (allowPrivateNetworks || allowedDomains.contains(host)) {
        return SsrfValidationResult.success();
      }
      return SsrfValidationResult.failure("Failed to resolve host: " + host);
    }

    if (isAllowed(host, addresses)) {
      log.debug("Host {} is in allow list, bypassing validation", host);
      return SsrfValidationResult.success();
    }

    List<String> rejectedAddresses = new ArrayList<>();
    for (InetAddress address : addresses) {
      String rejectionReason = validateIpAddress(address);
      if (rejectionReason != null) {
        rejectedAddresses.add(address.getHostAddress() + " (" + rejectionReason + ")");
      }
    }

    if (!rejectedAddresses.isEmpty()) {
      String errorMessage = "Host resolves to private/local IP address(es): " + String.join(", ", rejectedAddresses);
      log.debug("Blocked access to private/local network host: {} - {}", host, errorMessage);
      return SsrfValidationResult.failure(errorMessage);
    }

    return SsrfValidationResult.success();
  }

  private String validateIpAddress(final InetAddress address) {
    String hostAddress = address.getHostAddress();

    if (hostAddress.equals(CLOUD_METADATA_ADDRESS)) {
      return "restricted address";
    }

    if (!allowPrivateNetworks) {
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

  private boolean isAllowed(final String host, final InetAddress[] addresses) {
    boolean hasCloudMetadata = Arrays.stream(addresses)
        .map(InetAddress::getHostAddress)
        .anyMatch(CLOUD_METADATA_ADDRESS::equals);

    if (hasCloudMetadata) {
      log.warn("Host {} resolves to cloud metadata address - blocking", host);
      return false;
    }

    return allowedDomains.contains(host)
        || Arrays.stream(addresses)
            .map(InetAddress::getHostAddress)
            .anyMatch(allowedIPs::contains);
  }

  private void validateAllowListConfiguration() {
    if (!allowedIPs.isEmpty()) {
      log.info("Allowed IPs: {}", String.join(", ", allowedIPs));
      if (allowPrivateNetworks) {
        log.warn("Allowed IPs will be ignored since allowing private networks");
      }
    }

    if (!allowedDomains.isEmpty()) {
      log.info("Allowed domains: {}", String.join(", ", allowedDomains));
      if (allowPrivateNetworks) {
        log.warn("Allowed domains will be ignored since allowing private networks");
      }
    }

    if (allowedIPs.contains(CLOUD_METADATA_ADDRESS)) {
      log.error("Cloud metadata address {} cannot be added to allow list. This will be blocked",
          CLOUD_METADATA_ADDRESS);
    }
  }

  private Set<String> toTrimmedSet(final String[] values) {
    if (values == null || values.length == 0) {
      return Set.of();
    }
    return Arrays.stream(values)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  public record SsrfValidationResult(boolean valid, String errorMessage)
  {
    public static SsrfValidationResult success() {
      return new SsrfValidationResult(true, null);
    }

    public static SsrfValidationResult failure(final String errorMessage) {
      return new SsrfValidationResult(false, errorMessage);
    }

    public boolean isValid() {
      return valid;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
