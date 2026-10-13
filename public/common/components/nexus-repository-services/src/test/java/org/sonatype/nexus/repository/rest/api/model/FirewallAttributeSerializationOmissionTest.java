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
package org.sonatype.nexus.repository.rest.api.model;

import org.sonatype.nexus.repository.firewall.FirewallMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pins the {@code firewall} serialization-omission contract on both
 * {@link SimpleApiProxyRepository} (the GET response type) and {@link ProxyRepositoryApiRequest}
 * (its request-side sibling): {@code "firewall": null} must not be emitted for proxy repositories
 * that have no firewall configuration, matching the sibling {@code replication} property's
 * {@code @JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)} precedent on the same
 * classes.
 * <p>
 * The {@link SimpleApiProxyRepository} half guards a real, observed customer-facing symptom: it is
 * the type actually returned from proxy repository GET/PUT responses, so an unguarded
 * {@code @JsonProperty} getter there would put a new {@code "firewall": null} key on every proxy
 * repository response of every format. The {@link ProxyRepositoryApiRequest} half is defensive /
 * for consistency with its response sibling — {@code ProxyRepositoryApiRequest} is a request-only
 * DTO that is not serialised back to a client on any production code path, so its tests here guard
 * against future misuse rather than a currently observed regression.
 */
public class FirewallAttributeSerializationOmissionTest
{
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void simpleApiProxyRepository_omitsFirewallKey_whenNotConfigured() throws Exception {
    SimpleApiProxyRepository response = new SimpleApiProxyRepository(
        "maven-central", "maven2", "http://localhost:8081/repository/maven-central/", true,
        null, null, null, null, null, null, null);
    // firewall left unset (null) — simulates a plain proxy repository with no firewall config.

    String json = objectMapper.writeValueAsString(response);

    assertFalse(
        "A proxy repository GET response with no firewall configuration must omit the "
            + "\"firewall\" key entirely (matching the existing \"replication\" precedent on the "
            + "same class), not emit \"firewall\": null. Actual JSON: " + json,
        json.contains("firewall"));
  }

  @Test
  public void simpleApiProxyRepository_includesFirewallKey_whenConfigured() throws Exception {
    SimpleApiProxyRepository response = new SimpleApiProxyRepository(
        "npm-proxy-bc", "npm", "http://localhost:8081/repository/npm-proxy-bc/", true,
        null, null, null, null, null, null, null);
    response.setFirewall(new FirewallAttributes(FirewallMode.QUARANTINE));

    String json = objectMapper.writeValueAsString(response);

    assertTrue(
        "A proxy repository GET response WITH firewall configured must still serialise the "
            + "\"firewall\" key — otherwise the NON_EMPTY/NON_NULL omission test above could pass "
            + "for the wrong reason (Jackson dropping the property entirely). Actual JSON: " + json,
        json.contains("\"firewall\""));
  }

  /**
   * Defensive / consistency guard: {@link ProxyRepositoryApiRequest} is not currently serialised
   * back to a client on any production code path, so this does not pin an observed customer
   * symptom — it keeps the request-side DTO's inclusion contract aligned with its
   * {@link SimpleApiProxyRepository} response sibling above.
   */
  @Test
  public void proxyRepositoryApiRequest_omitsFirewallKey_whenNotConfigured() throws Exception {
    ProxyRepositoryApiRequest request = new ProxyRepositoryApiRequest(
        "maven-central", "maven2", true, null, null, null, null, null, null, null);
    // firewall left unset (null).

    String json = objectMapper.writeValueAsString(request);

    assertFalse(
        "A proxy repository request/response body with no firewall configuration must omit the "
            + "\"firewall\" key entirely, matching the existing \"replication\" precedent on the "
            + "same class. Actual JSON: " + json,
        json.contains("firewall"));
  }

  /**
   * Belt-and-braces counterpart to {@link #proxyRepositoryApiRequest_omitsFirewallKey_whenNotConfigured()};
   * see that method's javadoc for why this half is defensive rather than a regression guard for an
   * observed symptom.
   */
  @Test
  public void proxyRepositoryApiRequest_includesFirewallKey_whenConfigured() throws Exception {
    ProxyRepositoryApiRequest request = new ProxyRepositoryApiRequest(
        "npm-proxy-bc", "npm", true, null, null, null, null, null, null, null);
    request.setFirewall(new FirewallAttributes(FirewallMode.PCCS));

    String json = objectMapper.writeValueAsString(request);

    assertTrue(
        "A proxy repository request/response body WITH firewall configured must still serialise "
            + "the \"firewall\" key. Actual JSON: " + json,
        json.contains("\"firewall\""));
  }
}
