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
package org.sonatype.nexus.common.firewall;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationLicense;

import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FirewallConfigurationHelperTest
    extends TestSupport
{
  @Mock
  private ApplicationLicense applicationLicense;

  @Test
  public void checkFirewallLicense() {
    checkFirewallLicense(Arrays.asList("SonatypeCLM", "Firewall", "Feature01"), true);
    checkFirewallLicense(Arrays.asList("Feature01", "Firewall", "Feature03"), true);
    checkFirewallLicense(Arrays.asList("Feature01", "SonatypeCLM", "Feature03"), true);
    checkFirewallLicense(Arrays.asList("Feature01", "Feature02", "Feature03"), false);
  }

  private void checkFirewallLicense(List<String> features, boolean expected) {
    HashMap<String, Object> attributes = new HashMap<>();
    attributes.put("features", features);
    when(applicationLicense.getAttributes()).thenReturn(attributes);
    assertEquals(expected, FirewallConfigurationHelper.firewallLicenseCheck(applicationLicense));
  }
}
