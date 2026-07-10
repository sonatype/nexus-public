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
package org.sonatype.nexus.repository.maven.rest;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.maven.api.MavenAttributes;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.FirewallAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.api.model.FirewallAttributes.FIREWALL_CHILD_ATTRIBUTE_KEY;
import static org.sonatype.nexus.repository.rest.api.model.FirewallAttributes.MODE;

/**
 * Integration tests for maven proxy repository firewall configuration via REST API.
 * Tests the end-to-end flow from API request models through converters to repository configuration.
 * Specifically tests that PCCS mode is rejected for non-npm/pypi formats.
 */
@RunWith(MockitoJUnitRunner.class)
public class MavenProxyRepositoryFirewallApiIT
{
  @Mock
  private RoutingRuleStore routingRuleStore;

  @Mock
  private ConfigurationStore configurationStore;

  private MavenProxyRepositoryApiRequestToConfigurationConverter converter;

  @Before
  public void setup() {
    when(configurationStore.newConfiguration()).thenReturn(new ConfigurationData());
    converter = new MavenProxyRepositoryApiRequestToConfigurationConverter(routingRuleStore);
    converter.setConfigurationStore(configurationStore);
  }

  /**
   * Test Scenario 1: Create maven repository with firewall disabled
   */
  @Test
  public void testCreateRepository_FirewallDisabled() {
    // Arrange
    FirewallAttributes firewall = new FirewallAttributes("DISABLED");
    MavenProxyRepositoryApiRequest request = createRequest("test-maven-disabled", firewall);

    // Act
    Configuration config = converter.convert(request);

    // Assert
    assertNotNull("Configuration should not be null", config);
    NestedAttributesMap firewallConfig = config.attributes(FIREWALL_CHILD_ATTRIBUTE_KEY);
    assertNotNull("Firewall configuration should exist", firewallConfig);
    assertEquals("Firewall mode should be DISABLED", "DISABLED", firewallConfig.get(MODE, String.class));
  }

  /**
   * Test Scenario 2: Create maven repository with firewall in AUDIT mode
   */
  @Test
  public void testCreateRepository_FirewallAuditMode() {
    // Arrange
    FirewallAttributes firewall = new FirewallAttributes("AUDIT");
    MavenProxyRepositoryApiRequest request = createRequest("test-maven-audit", firewall);

    // Act
    Configuration config = converter.convert(request);

    // Assert
    assertNotNull("Configuration should not be null", config);
    NestedAttributesMap firewallConfig = config.attributes(FIREWALL_CHILD_ATTRIBUTE_KEY);
    assertNotNull("Firewall configuration should exist", firewallConfig);
    assertEquals("Firewall mode should be AUDIT", "AUDIT", firewallConfig.get(MODE, String.class));
  }

  /**
   * Test Scenario 3: Create maven repository with firewall in QUARANTINE mode
   */
  @Test
  public void testCreateRepository_FirewallQuarantineMode() {
    // Arrange
    FirewallAttributes firewall = new FirewallAttributes("QUARANTINE");
    MavenProxyRepositoryApiRequest request = createRequest("test-maven-quarantine", firewall);

    // Act
    Configuration config = converter.convert(request);

    // Assert
    assertNotNull("Configuration should not be null", config);
    NestedAttributesMap firewallConfig = config.attributes(FIREWALL_CHILD_ATTRIBUTE_KEY);
    assertNotNull("Firewall configuration should exist", firewallConfig);
    assertEquals("Firewall mode should be QUARANTINE", "QUARANTINE",
        firewallConfig.get(MODE, String.class));
  }

  /**
   * Test Scenario 4: PCCS mode validation - should reject for non-npm/pypi formats
   * This is a critical test to ensure PCCS mode is properly restricted to npm and pypi only.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCreateRepository_FirewallPccsMode_RejectedForMaven() {
    // Arrange
    FirewallAttributes firewall = new FirewallAttributes("PCCS");
    MavenProxyRepositoryApiRequest request = createRequest("test-maven-pccs-invalid", firewall);

    // Act - Should throw IllegalArgumentException
    converter.convert(request);

    // Assert is implicit - expecting exception to be thrown
  }

  /**
   * Test Scenario 5: Verify error message for PCCS mode rejection
   */
  @Test
  public void testCreateRepository_FirewallPccsMode_VerifyErrorMessage() {
    // Arrange
    FirewallAttributes firewall = new FirewallAttributes("PCCS");
    MavenProxyRepositoryApiRequest request = createRequest("test-maven-pccs-msg", firewall);

    // Act & Assert
    try {
      converter.convert(request);
      org.junit.Assert.fail("Expected IllegalArgumentException to be thrown");
    }
    catch (IllegalArgumentException e) {
      // Verify error message contains useful information
      String errorMessage = e.getMessage();
      assertNotNull("Error message should not be null", errorMessage);
      assertTrue("Error message should mention PCCS", errorMessage.contains("PCCS"));
      assertTrue("Error message should mention npm and pypi",
          errorMessage.contains("npm") && errorMessage.contains("pypi"));
    }
  }

  /**
   * Test Scenario 6: Update repository firewall mode transition (AUDIT to QUARANTINE)
   */
  @Test
  public void testUpdateRepository_FirewallModeTransition_AuditToQuarantine() {
    // Arrange - Create with AUDIT mode
    FirewallAttributes initialFirewall = new FirewallAttributes("AUDIT");
    MavenProxyRepositoryApiRequest initialRequest = createRequest("test-maven-transition", initialFirewall);
    converter.convert(initialRequest);

    // Act - Update to QUARANTINE mode
    FirewallAttributes updatedFirewall = new FirewallAttributes("QUARANTINE");
    MavenProxyRepositoryApiRequest updateRequest = createRequest("test-maven-transition", updatedFirewall);
    Configuration updatedConfig = converter.convert(updateRequest);

    // Assert
    NestedAttributesMap updatedFirewallConfig = updatedConfig.attributes(FIREWALL_CHILD_ATTRIBUTE_KEY);
    assertEquals("Firewall mode should now be QUARANTINE", "QUARANTINE",
        updatedFirewallConfig.get(MODE, String.class));
  }

  /**
   * Test Scenario 7: Update attempt to PCCS mode should also be rejected
   */
  @Test(expected = IllegalArgumentException.class)
  public void testUpdateRepository_FirewallModeTransition_AuditToPccs_Rejected() {
    // Arrange - Create with AUDIT mode
    FirewallAttributes initialFirewall = new FirewallAttributes("AUDIT");
    MavenProxyRepositoryApiRequest initialRequest = createRequest("test-maven-to-pccs-rejected", initialFirewall);
    converter.convert(initialRequest);

    // Act - Attempt to update to PCCS mode (should be rejected)
    FirewallAttributes updatedFirewall = new FirewallAttributes("PCCS");
    MavenProxyRepositoryApiRequest updateRequest = createRequest("test-maven-to-pccs-rejected", updatedFirewall);
    converter.convert(updateRequest);

    // Assert is implicit - expecting exception to be thrown
  }

  /**
   * Test Scenario 8: Create repository without firewall attributes (backward compatibility)
   */
  @Test
  public void testCreateRepository_NoFirewallAttributes_BackwardCompatibility() {
    // Arrange - Request without firewall field
    MavenProxyRepositoryApiRequest request = createRequest("test-maven-no-firewall", null);

    // Act
    Configuration config = converter.convert(request);

    // Assert - Should succeed without errors
    assertNotNull("Configuration should not be null", config);
    // Firewall configuration may or may not exist depending on defaults, but no exception should be thrown
  }

  /**
   * Helper method to create a standard maven proxy repository API request
   */
  private MavenProxyRepositoryApiRequest createRequest(String name, FirewallAttributes firewall) {
    StorageAttributes storage = new StorageAttributes("default", true);
    ProxyAttributes proxy = new ProxyAttributes("https://repo1.maven.org/maven2", 1440, 1440, null);
    NegativeCacheAttributes negativeCache = new NegativeCacheAttributes(true, 1440);
    HttpClientAttributesWithPreemptiveAuth httpClient = new HttpClientAttributesWithPreemptiveAuth(
        false, true, null, null);
    CleanupPolicyAttributes cleanup = null;
    MavenAttributes maven = new MavenAttributes("RELEASE", "STRICT", null);

    MavenProxyRepositoryApiRequest request = new MavenProxyRepositoryApiRequest(
        name,
        true,
        storage,
        cleanup,
        proxy,
        negativeCache,
        httpClient,
        null,
        maven,
        null);
    // The base ProxyRepositoryApiRequest now exposes firewall via a setter rather than the
    // constructor; keep the helper signature stable for callers and apply the value here.
    request.setFirewall(firewall);
    return request;
  }
}
