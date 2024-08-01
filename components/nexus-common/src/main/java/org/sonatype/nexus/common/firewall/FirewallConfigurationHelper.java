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

import java.util.Collection;
import java.util.Optional;

import org.sonatype.nexus.common.app.ApplicationLicense;

/**
 * Helper class for firewall configuration.
 */
public class FirewallConfigurationHelper
{
  private static final String APPLICATION_LICENSE_FEATURES = "features";

  private static final String CLM_FEATURE = "SonatypeCLM";

  private static final String FIREWALL_FEATURE = "Firewall";

  private FirewallConfigurationHelper() {
    throw new IllegalStateException("Utility class");
  }

  public static boolean firewallLicenseCheck(final ApplicationLicense applicationLicense) {
    Object features = applicationLicense.getAttributes().get(APPLICATION_LICENSE_FEATURES);
    if (features instanceof Collection) {
      Collection featureCollection = (Collection) features;
      Optional firewallFeature = featureCollection.stream()
          .filter(x -> x.equals(CLM_FEATURE) || x.equals(FIREWALL_FEATURE))
          .findFirst();
      return firewallFeature.isPresent();
    }
    return false;
  }
}
