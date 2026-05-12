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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.validation.ssrf.SsrfProtectionConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

class SsrfProtectionConfigDataTest
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void testRoundTrip() {
    SsrfProtectionConfiguration config =
        new SsrfProtectionConfiguration(true, Set.of("10.0.0.1"), Set.of("example.com"));

    SsrfProtectionConfigData data = SsrfProtectionConfigData.from(config);
    SsrfProtectionConfiguration roundTripped = data.toConfiguration();

    assertThat(roundTripped.enabled(), is(true));
    assertThat(roundTripped.allowedIPs(), is(Set.of("10.0.0.1")));
    assertThat(roundTripped.allowedDomains(), is(Set.of("example.com")));
  }

  @Test
  void testFromMap_handlesMapDeserialization() {
    Map<String, Object> map = Map.of(
        "enabled", true,
        "allowedIPs", List.of("192.168.1.1"),
        "allowedDomains", List.of("host.internal"));

    SsrfProtectionConfigData data = SsrfProtectionConfigData.fromMap(map);
    SsrfProtectionConfiguration config = data.toConfiguration();

    assertThat(config.enabled(), is(true));
    assertThat(config.allowedIPs(), hasItem("192.168.1.1"));
    assertThat(config.allowedDomains(), hasItem("host.internal"));
  }

  @Test
  void testFromMap_handlesMissingFields() {
    Map<String, Object> map = Map.of();

    SsrfProtectionConfigData data = SsrfProtectionConfigData.fromMap(map);
    SsrfProtectionConfiguration config = data.toConfiguration();

    assertThat(config.enabled(), is(false));
    assertThat(config.allowedIPs().isEmpty(), is(true));
    assertThat(config.allowedDomains().isEmpty(), is(true));
  }

  @Test
  void testJsonSerialization() throws Exception {
    SsrfProtectionConfigData data = new SsrfProtectionConfigData(
        true, Set.of("10.0.0.1"), Set.of("example.com"));

    String json = mapper.writeValueAsString(data);
    SsrfProtectionConfigData deserialized = mapper.readValue(json, SsrfProtectionConfigData.class);

    assertThat(deserialized.isEnabled(), is(true));
    assertThat(deserialized.getAllowedIPs(), hasItem("10.0.0.1"));
    assertThat(deserialized.getAllowedDomains(), hasItem("example.com"));
  }

  @Test
  void testNullCollectionsDefaultToEmpty() {
    SsrfProtectionConfigData data = new SsrfProtectionConfigData(false, null, null);

    assertThat(data.getAllowedIPs().isEmpty(), is(true));
    assertThat(data.getAllowedDomains().isEmpty(), is(true));
  }
}
