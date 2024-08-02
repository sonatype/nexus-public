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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.firewall.FirewallCapability;
import org.sonatype.nexus.common.app.ApplicationLicense;
import org.sonatype.nexus.common.firewall.FirewallConfigurationHelper;
import org.sonatype.nexus.rapture.StateContributor;

import static org.sonatype.nexus.common.app.FeatureFlags.MALICIOUS_RISK_ENABLED_NAMED;

@Named
@Singleton
public class MaliciousRiskStateContributor
    implements StateContributor
{
  private static final String MALICIOUS_RISK_DASH_BOARD = "MaliciousRiskDashboard";

  private final CapabilityRegistry capabilityRegistry;

  private final boolean featureFlag;

  private final boolean hasFirewall;

  @Inject
  public MaliciousRiskStateContributor(
      @Named(MALICIOUS_RISK_ENABLED_NAMED) final Boolean featureFlag,
      final ApplicationLicense applicationLicense,
      final CapabilityRegistry capabilityRegistry)
  {
    this.capabilityRegistry = capabilityRegistry;
    this.hasFirewall = FirewallConfigurationHelper.firewallLicenseCheck(applicationLicense);
    this.featureFlag = featureFlag;
  }

  @Override
  public Map<String, Object> getState() {
    Map<String, Object> state = null;
    if (featureFlag) {
      boolean firewallCapabilityEnabled = false;
      Optional<CapabilityContext> firewallCapability = FirewallCapability.findFirewallCapability(capabilityRegistry);
      if (firewallCapability.isPresent() && firewallCapability.get().isEnabled()) {
        firewallCapabilityEnabled = true;
      }

      boolean AuditQuarantineCapabilityEnabled =
          FirewallCapability.auditAndQuarantineCapabilityExists(capabilityRegistry);
      boolean showMaliciousRiskDashBoard =
          !hasFirewall || !firewallCapabilityEnabled || !AuditQuarantineCapabilityEnabled;
      state = new HashMap<>();
      state.put(MALICIOUS_RISK_DASH_BOARD, showMaliciousRiskDashBoard);
    }
    return state;
  }

  boolean isFeatureFlag() {
    return featureFlag;
  }
}
