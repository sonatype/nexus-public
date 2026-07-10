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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyConfiguration;
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.KeyValueEvent;
import jakarta.validation.ValidationException;

import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AntiSsrfServiceImplTest
{
  @Mock
  private GlobalKeyValueStore kvStore;

  @Mock
  private HttpClientManager httpClientManager;

  private AntiSsrfServiceImpl underTest;

  @BeforeEach
  void setup() throws Exception {
    lenient().when(kvStore.get(eq(AntiSsrfServiceImpl.CONFIG_KEY), eq(SsrfProtectionConfigData.class)))
        .thenReturn(Optional.empty());
    lenient().when(httpClientManager.getConfiguration()).thenReturn(null);

    underTest = createService(false, new String[]{}, new String[]{});
    underTest.start();
  }

  @Test
  void testDoStart_seedsFromPropertiesWhenNoDbConfig() throws Exception {
    underTest = createService(false, new String[]{"10.0.0.1"}, new String[]{"example.com"});
    underTest.start();

    SsrfProtectionConfiguration config = underTest.getConfiguration();
    assertThat(config.enabled(), is(true));
    assertThat(config.allowedIPs(), hasItem("10.0.0.1"));
    assertThat(config.allowedDomains(), hasItem("example.com"));

    verify(kvStore, atLeastOnce()).setString(eq(AntiSsrfServiceImpl.CONFIG_KEY), any(SsrfProtectionConfigData.class));
  }

  @Test
  void testDoStart_loadsFromDbWhenPresent() throws Exception {
    SsrfProtectionConfigData storedData = new SsrfProtectionConfigData(
        true, Set.of("192.168.1.1"), Set.of("internal.corp"));
    when(kvStore.get(eq(AntiSsrfServiceImpl.CONFIG_KEY), eq(SsrfProtectionConfigData.class)))
        .thenReturn(Optional.of(storedData));

    underTest = createService(false, new String[]{}, new String[]{});
    underTest.start();

    SsrfProtectionConfiguration config = underTest.getConfiguration();
    assertThat(config.enabled(), is(true));
    assertThat(config.allowedIPs(), hasItem("192.168.1.1"));
    assertThat(config.allowedDomains(), hasItem("internal.corp"));
  }

  @Test
  void testDoStart_stripsCloudMetadataFromPropsOnSeed() throws Exception {
    underTest = createService(false, new String[]{"10.0.0.1", "169.254.169.254"}, new String[]{"example.com"});
    underTest.start();

    SsrfProtectionConfiguration config = underTest.getConfiguration();
    assertThat(config.enabled(), is(true));
    assertThat(config.allowedIPs(), is(Set.of("10.0.0.1")));
    assertThat(config.allowedDomains(), hasItem("example.com"));
  }

  @Test
  void testDoStart_allowPrivateNetworksMeansDisabled() throws Exception {
    underTest = createService(true, new String[]{}, new String[]{});
    underTest.start();

    SsrfProtectionConfiguration config = underTest.getConfiguration();
    assertThat(config.enabled(), is(false));
  }

  @Test
  void testValidateHost_allowsPublicIp() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("8.8.8.8"));
  }

  @Test
  void testValidateHost_blocksLocalhostWhenEnabled() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("localhost"));
    assertThat(ex.getMessage(), containsString("loopback"));
  }

  @Test
  void testValidateHost_allowsLocalhostWhenDisabled() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("localhost"));
  }

  @Test
  void testValidateHost_alwaysBlocksCloudMetadata() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("169.254.169.254"));
    assertThat(ex.getMessage(), containsString("restricted"));
  }

  @Test
  void testValidateHost_alwaysBlocksCloudMetadataIPv6() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("fd00:ec2::254"));
    assertThat(ex.getMessage(), containsString("restricted"));
  }

  @Test
  void testValidateHost_blocksZeroAddressWhenEnabled() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("0.0.0.0"));
    assertThat(ex.getMessage(), containsString("wildcard"));
  }

  @Test
  void testValidateHost_allowsZeroAddressWhenDisabled() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("0.0.0.0"));
  }

  @Test
  void testValidateHost_blocksIPv6ULAWhenEnabled() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("fd12:3456:789a::1"));
    assertThat(ex.getMessage(), containsString("private network"));
  }

  @Test
  void testValidateHost_allowsIPv6ULAWhenDisabled() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("fd12:3456:789a::1"));
  }

  @Test
  void testValidateHost_blocksIPv6ULA_fc00Prefix() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("fc00::1"));
    assertThat(ex.getMessage(), containsString("private network"));
  }

  @Test
  void testValidateHost_allowsIpInAllowList() {
    underTest.updateConfiguration(
        new SsrfProtectionConfiguration(true, Set.of("127.0.0.1"), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("localhost"));
  }

  @Test
  void testValidateHost_allowsIpInAllowList_ipv6WorksIndependently() {
    // Adding only IPv6 loopback should also allow IPv4 loopback for localhost
    underTest.updateConfiguration(
        new SsrfProtectionConfiguration(true, Set.of("::1"), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("localhost"));
  }

  /**
   * Verifies that any loopback representation present in the allowlist permits localhost
   * regardless of which loopback form {@code InetAddress.getAllByName("localhost")} returns.
   * This covers the canonical forms as well as an IPv6-mapped IPv4 loopback (::ffff:127.0.0.1)
   * and a non-canonical IPv4 loopback (127.0.0.2).
   */
  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "127.0.0.2", "::1", "0:0:0:0:0:0:0:1", "::ffff:127.0.0.1"})
  void testValidateHost_anyLoopbackFormInAllowListAllowsLocalhost(final String loopbackForm) {
    underTest.updateConfiguration(
        new SsrfProtectionConfiguration(true, Set.of(loopbackForm), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("localhost"));
  }

  @Test
  void testValidateHost_allowsDomainInAllowList() {
    underTest.updateConfiguration(
        new SsrfProtectionConfiguration(true, Set.of(), Set.of("localhost")));

    assertDoesNotThrow(() -> underTest.validateHost("localhost"));
  }

  @Test
  void testValidateHost_rejectsNullHost() {
    assertThrows(ValidationException.class, () -> underTest.validateHost(null));
  }

  @Test
  void testValidateHost_rejectsEmptyHost() {
    assertThrows(ValidationException.class, () -> underTest.validateHost(""));
  }

  @Test
  void testUpdateConfiguration_persistsToKvStore() {
    SsrfProtectionConfiguration newConfig =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1"), Set.of("my-host"));
    underTest.updateConfiguration(newConfig);

    verify(kvStore, atLeastOnce()).setString(eq(AntiSsrfServiceImpl.CONFIG_KEY), any(SsrfProtectionConfigData.class));
    assertThat(underTest.getConfiguration().enabled(), is(true));
    assertThat(underTest.getConfiguration().allowedIPs(), hasItem("10.0.0.1"));
    assertThat(underTest.getConfiguration().allowedDomains(), hasItem("my-host"));
  }

  @Test
  void testUpdateConfiguration_rejectsCloudMetadataInAllowedIPs() {
    SsrfProtectionConfiguration newConfig =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1", "169.254.169.254"), Set.of());

    ValidationErrorsException ex = assertThrows(ValidationErrorsException.class,
        () -> underTest.updateConfiguration(newConfig));
    assertThat(ex.getMessage(), containsString("169.254.169.254"));
  }

  @Test
  void testUpdateConfiguration_rejectsIpv6MappedCloudMetadataInAllowedIPs() {
    SsrfProtectionConfiguration newConfig =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1", "::ffff:169.254.169.254"), Set.of());

    ValidationErrorsException ex = assertThrows(ValidationErrorsException.class,
        () -> underTest.updateConfiguration(newConfig));
    assertThat(ex.getMessage(), containsString("::ffff:169.254.169.254"));
  }

  @Test
  void testUpdateConfiguration_rejectsIpv6CloudMetadataInAllowedIPs() {
    SsrfProtectionConfiguration newConfig =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1", "fd00:ec2::254"), Set.of());

    ValidationErrorsException ex = assertThrows(ValidationErrorsException.class,
        () -> underTest.updateConfiguration(newConfig));
    assertThat(ex.getMessage(), containsString("fd00:ec2::254"));
  }

  @Test
  void testGetConfiguration_returnsCurrentConfig() {
    SsrfProtectionConfiguration newConfig =
        new SsrfProtectionConfiguration(false, Set.of("192.168.1.1"), Set.of("internal.corp"));
    underTest.updateConfiguration(newConfig);

    SsrfProtectionConfiguration current = underTest.getConfiguration();
    assertThat(current.enabled(), is(false));
    assertThat(current.allowedIPs(), is(Set.of("192.168.1.1")));
    assertThat(current.allowedDomains(), is(Set.of("internal.corp")));
  }

  @Test
  void testOnKeyValueEvent_updatesConfigOnReplication() {
    Map<String, Object> eventData = Map.of(
        "enabled", true,
        "allowedIPs", List.of("10.0.0.1"),
        "allowedDomains", List.of("internal.corp"));

    KeyValueEvent event = new KeyValueEvent(AntiSsrfServiceImpl.CONFIG_KEY, eventData);
    EventHelper.asReplicating(() -> underTest.on(event));

    SsrfProtectionConfiguration config = underTest.getConfiguration();
    assertThat(config.enabled(), is(true));
    assertThat(config.allowedIPs(), hasItem("10.0.0.1"));
    assertThat(config.allowedDomains(), hasItem("internal.corp"));
  }

  @Test
  void testOnKeyValueEvent_invalidatesCacheOnReplication() {
    // Cache a blocked result for localhost (enabled=true, no allow list)
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    assertThrows(ValidationException.class, () -> underTest.validateHost("localhost"));

    // Replicate a config that allows localhost via domain list
    Map<String, Object> eventData = Map.of(
        "enabled", true,
        "allowedIPs", List.of(),
        "allowedDomains", List.of("localhost"));
    KeyValueEvent event = new KeyValueEvent(AntiSsrfServiceImpl.CONFIG_KEY, eventData);
    EventHelper.asReplicating(() -> underTest.on(event));

    // Cache should be invalidated — localhost now allowed
    assertDoesNotThrow(() -> underTest.validateHost("localhost"));
  }

  @Test
  void testOnKeyValueEvent_ignoresNonConfigKey() {
    Map<String, Object> eventData = Map.of("enabled", false);
    KeyValueEvent event = new KeyValueEvent("some.other.key", eventData);
    EventHelper.asReplicating(() -> underTest.on(event));

    assertThat(underTest.getConfiguration().enabled(), is(true));
  }

  @Test
  void testOnKeyValueEvent_ignoresNonReplicatingEvent() {
    Map<String, Object> eventData = Map.of("enabled", false);
    KeyValueEvent event = new KeyValueEvent(AntiSsrfServiceImpl.CONFIG_KEY, eventData);
    // Not wrapped in asReplicating - should be ignored
    underTest.on(event);

    assertThat(underTest.getConfiguration().enabled(), is(true));
  }

  @Test
  void testOnKeyValueEvent_handlesMalformedValueGracefully() {
    KeyValueEvent event = new KeyValueEvent(AntiSsrfServiceImpl.CONFIG_KEY, "not-a-map");
    EventHelper.asReplicating(() -> underTest.on(event));

    // Config should remain unchanged
    assertThat(underTest.getConfiguration().enabled(), is(true));
  }

  @Test
  void testUpdateConfiguration_rejectsCloudMetadataInAllowedDomains() {
    SsrfProtectionConfiguration newConfig =
        new SsrfProtectionConfiguration(true, Set.of(), Set.of("internal.corp", "169.254.169.254"));

    ValidationErrorsException ex = assertThrows(ValidationErrorsException.class,
        () -> underTest.updateConfiguration(newConfig));
    assertThat(ex.getMessage(), containsString("169.254.169.254"));
  }

  @Test
  void testValidateHostForConfiguration_blocksPrivateHosts() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHostWithoutCache("localhost"));
    assertThat(ex.getMessage(), containsString("loopback"));
  }

  @Test
  void testValidateHostForConfiguration_allowsPublicHosts() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHostWithoutCache("8.8.8.8"));
  }

  @Test
  void testValidateHost_dnsFails_noProxy_throws() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    when(httpClientManager.getConfiguration()).thenReturn(null);

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("does-not-resolve.invalid"));
    assertThat(ex.getMessage(), containsString("Failed to resolve host"));
  }

  @Test
  void testValidateHost_dnsFails_httpProxyEnabled_passes() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = httpClientConfigWithProxy(true, false, null);
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    assertDoesNotThrow(() -> underTest.validateHost("does-not-resolve.invalid"));
  }

  @Test
  void testValidateHost_dnsFails_httpsProxyEnabled_passes() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = httpClientConfigWithProxy(false, true, null);
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    assertDoesNotThrow(() -> underTest.validateHost("does-not-resolve.invalid"));
  }

  @Test
  void testValidateHost_dnsFails_bothProxiesEnabled_passes() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = httpClientConfigWithProxy(true, true, null);
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    assertDoesNotThrow(() -> underTest.validateHost("does-not-resolve.invalid"));
  }

  @Test
  void testValidateHost_dnsFails_proxyEnabled_proxyConfigNull_throws() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = mock(HttpClientConfiguration.class);
    when(cfg.getProxy()).thenReturn(null);
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("does-not-resolve.invalid"));
    assertThat(ex.getMessage(), containsString("Failed to resolve host"));
  }

  @Test
  void testValidateHost_dnsFails_proxyEnabled_neitherProxyEnabled_throws() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = httpClientConfigWithProxy(false, false, null);
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("does-not-resolve.invalid"));
    assertThat(ex.getMessage(), containsString("Failed to resolve host"));
  }

  @Test
  void testValidateHost_dnsFails_proxyEnabled_hostInNonProxyHosts_throws() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = httpClientConfigWithProxy(true, false, new String[]{"*.invalid"});
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("does-not-resolve.invalid"));
    assertThat(ex.getMessage(), containsString("Failed to resolve host"));
  }

  @Test
  void testValidateHost_dnsFails_proxyEnabled_hostNotInNonProxyHosts_passes() {
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    HttpClientConfiguration cfg = httpClientConfigWithProxy(true, false, new String[]{"*.internal", "localhost"});
    when(httpClientManager.getConfiguration()).thenReturn(cfg);

    assertDoesNotThrow(() -> underTest.validateHost("does-not-resolve.invalid"));
  }

  @Test
  void testValidateHost_dnsFails_ssrfDisabled_passes_evenWithoutProxy() {
    // existing fast-path: when SSRF is disabled, DNS failures are allowed (no proxy lookup needed)
    underTest.updateConfiguration(new SsrfProtectionConfiguration(false, Set.of(), Set.of()));

    assertDoesNotThrow(() -> underTest.validateHost("does-not-resolve.invalid"));
  }

  @Test
  void testValidateHost_dnsFails_hostInAllowedDomains_passes_evenWithoutProxy() {
    // existing fast-path: when host is in allowedDomains, DNS failures are allowed (no proxy lookup needed)
    underTest.updateConfiguration(
        new SsrfProtectionConfiguration(true, Set.of(), Set.of("does-not-resolve.invalid")));

    assertDoesNotThrow(() -> underTest.validateHost("does-not-resolve.invalid"));
  }

  @Test
  void testValidateHost_dnsResolves_proxyConfigured_stillValidatesIp() {
    // when DNS works, proxy presence does NOT bypass IP-based checks
    underTest.updateConfiguration(new SsrfProtectionConfiguration(true, Set.of(), Set.of()));
    // Note: no httpClientManager stubbing needed — when DNS resolves we never reach isProxyConfiguredFor

    // 127.0.0.1 resolves locally and is loopback → must still be blocked
    ValidationException ex = assertThrows(ValidationException.class,
        () -> underTest.validateHost("127.0.0.1"));
    assertThat(ex.getMessage(), containsString("loopback"));
  }

  private HttpClientConfiguration httpClientConfigWithProxy(
      final boolean http,
      final boolean https,
      final String[] nonProxyHosts)
  {
    HttpClientConfiguration cfg = mock(HttpClientConfiguration.class);
    ProxyConfiguration proxy = new ProxyConfiguration();

    if (http) {
      ProxyServerConfiguration s = new ProxyServerConfiguration();
      s.setEnabled(true);
      s.setHost("proxy.example.com");
      s.setPort(8080);
      proxy.setHttp(s);
    }
    if (https) {
      ProxyServerConfiguration s = new ProxyServerConfiguration();
      s.setEnabled(true);
      s.setHost("proxy.example.com");
      s.setPort(8443);
      proxy.setHttps(s);
    }
    proxy.setNonProxyHosts(nonProxyHosts);

    when(cfg.getProxy()).thenReturn(proxy);
    return cfg;
  }

  private AntiSsrfServiceImpl createService(
      final boolean allowPrivateNetworks,
      final String[] allowedIPs,
      final String[] allowedDomains)
  {
    return new AntiSsrfServiceImpl(
        kvStore,
        httpClientManager,
        allowPrivateNetworks,
        allowedIPs,
        allowedDomains,
        1000,
        Duration.ofMinutes(10));
  }
}
