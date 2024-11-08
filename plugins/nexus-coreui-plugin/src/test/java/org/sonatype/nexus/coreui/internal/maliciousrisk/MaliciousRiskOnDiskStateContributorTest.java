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

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.common.app.FeatureFlags.MALWARE_RISK_ON_DISK_ENABLED;

public class MaliciousRiskOnDiskStateContributorTest
    extends TestSupport
{
  @Test
  public void featureFlagShouldBeEnabled() {
    MaliciousRiskOnDiskStateContributor underTest =
        new MaliciousRiskOnDiskStateContributor(true);
    assertThat(underTest.getState().get(MALWARE_RISK_ON_DISK_ENABLED), is(true));
  }

  @Test
  public void featureFlagShouldBeDisabled() {
    MaliciousRiskOnDiskStateContributor underTest =
        new MaliciousRiskOnDiskStateContributor(false);
    assertThat(underTest.getState().get(MALWARE_RISK_ON_DISK_ENABLED), is(false));
  }
}
