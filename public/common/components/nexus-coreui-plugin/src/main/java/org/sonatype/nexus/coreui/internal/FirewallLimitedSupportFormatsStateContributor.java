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
package org.sonatype.nexus.coreui.internal;

import java.util.Map;

import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.FIREWALL_LIMITED_SUPPORT_FORMATS_ENABLED_NAMED_VALUE;

/**
 * Publishes the FIRE-956 limited-support-format Firewall flag to the UI via {@code NX.State}.
 * <p>
 * Consumed by the ExtJS repository settings recipes to decide whether to mount the Firewall facet on
 * formats Sonatype IQ cannot fully analyze, and by {@code firewallFormats.ts} to decide whether the
 * React surfaces should stop rendering "Not supported" for those formats.
 * <p>
 * Mirrors {@link FirewallPubFormatStateContributor}. Interim; removed alongside the flag when FIRE-895
 * replaces it with a capability.
 */
@Component
public class FirewallLimitedSupportFormatsStateContributor
    implements StateContributor
{
  private final boolean firewallLimitedSupportFormatsEnabled;

  @Autowired
  public FirewallLimitedSupportFormatsStateContributor(
      @Value(FIREWALL_LIMITED_SUPPORT_FORMATS_ENABLED_NAMED_VALUE) final boolean firewallLimitedSupportFormatsEnabled)
  {
    this.firewallLimitedSupportFormatsEnabled = firewallLimitedSupportFormatsEnabled;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of("firewallLimitedSupportFormatsEnabled", firewallLimitedSupportFormatsEnabled);
  }
}
