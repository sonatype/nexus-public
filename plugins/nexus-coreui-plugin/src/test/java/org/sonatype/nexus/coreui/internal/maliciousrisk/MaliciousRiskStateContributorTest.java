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
package org.sonatype.nexus.coreui.internal.maliciousrisk;

import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.firewall.FirewallCapability;
import org.sonatype.nexus.common.app.ApplicationLicense;
import org.sonatype.nexus.common.firewall.FirewallConfigurationHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MaliciousRiskStateContributorTest
    extends TestSupport
{
  @Mock
  private ApplicationLicense applicationLicense;

  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private Optional<CapabilityContext> capabilityContextOptional;

  @Mock
  private CapabilityContext capabilityContext;

  @Test
  public void featureFlagShouldBeEnabled() {
    MaliciousRiskStateContributor underTest =
        new MaliciousRiskStateContributor(true, applicationLicense, capabilityRegistry);
    assertThat(underTest.isFeatureFlag(), is(true));
  }

  @Test
  public void featureFlagShouldBeDisabled() {
    MaliciousRiskStateContributor underTest =
        new MaliciousRiskStateContributor(false, applicationLicense, capabilityRegistry);
    assertThat(underTest.isFeatureFlag(), is(false));
  }

  @Test
  public void checkMaliciousRiskIsEnabledWhenNoCapabilityIsConfigured() {
    try (MockedStatic<FirewallConfigurationHelper> firewallHelper = Mockito.mockStatic(
        FirewallConfigurationHelper.class)) {
      firewallHelper.when(() -> FirewallConfigurationHelper.firewallLicenseCheck(applicationLicense)).thenReturn(true);
      MaliciousRiskStateContributor underTest =
          new MaliciousRiskStateContributor(true, applicationLicense, capabilityRegistry);
      Map<String, Object> state = underTest.getState();
      assertThat(state.get("MaliciousRiskDashboard"), is(true));
    }
  }

  @Test
  public void checkMaliciousRiskStateWhenCapabilitiesAreConfigured() {
    checkDashBoardVisibilityWithFirewallCapabilities(true, false);
    checkDashBoardVisibilityWithFirewallCapabilities(false, true);
  }

  private void checkDashBoardVisibilityWithFirewallCapabilities(
      final boolean firewallAuditQuarantineCapabilityEnabled,
      final boolean expectedVisibility)
  {
    try (MockedStatic<FirewallConfigurationHelper> firewallHelper = Mockito.mockStatic(
        FirewallConfigurationHelper.class)) {
      firewallHelper.when(() -> FirewallConfigurationHelper.firewallLicenseCheck(applicationLicense)).thenReturn(true);
      MaliciousRiskStateContributor underTest =
          new MaliciousRiskStateContributor(true, applicationLicense, capabilityRegistry);
      try (MockedStatic<FirewallCapability> firewallCapability = Mockito.mockStatic(FirewallCapability.class)) {
        firewallCapability.when(() -> FirewallCapability.findFirewallCapability(capabilityRegistry)).thenReturn(
            capabilityContextOptional);
        firewallCapability.when(() -> FirewallCapability.auditAndQuarantineCapabilityExists(capabilityRegistry))
            .thenReturn(firewallAuditQuarantineCapabilityEnabled);
        when(capabilityContextOptional.isPresent()).thenReturn(true);
        when(capabilityContextOptional.get()).thenReturn(capabilityContext);
        when(capabilityContext.isEnabled()).thenReturn(true);
        Map<String, Object> state = underTest.getState();
        assertThat(state.get("MaliciousRiskDashboard"), is(expectedVisibility));
      }
    }
  }
}
