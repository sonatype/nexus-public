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
package org.sonatype.nexus.testsuite.upgrade.nexus4635nexusVersion;

import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.StatusResource;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * tampering the nexusVersion to 1.9.3<BR>
 * https://issues.sonatype.org/browse/NEXUS-4635
 *
 * <pre>
 * <firstStart>false</firstStart>
 * <instanceUpgraded>true</instanceUpgraded>
 * <configurationUpgraded>false</configurationUpgraded>
 * </pre>
 *
 * cstamas: this test was disabled since it does not quite makes sense. It would test following scenario: nexus.xml
 * does
 * not need upgrade, but the nexusVersion field contains old nexus version. To properly test this, you'd need a Nexus
 * version (CURRENT), and a (CURRENT-1) that has previous constraints: nexus.xml model version did not change, but
 * obviously versions did change. Due how security is molded in this play, this is impossible to test without having
 * those two true. By blindly upgrading and "just setting" the field this will never work, since security NPEs for
 * mysterious reasons.
 */
public class Nexus4635OldNexusVersionIT
    extends AbstractNexusIntegrationTest
{
  @BeforeClass
  public static void disableSecurity() {
    TestContainer.getInstance().getTestContext().setSecureTest(false);
  }

  @Test
  @Ignore
  public void checkState()
      throws Exception
  {
    // initial nexus start upgraded config, so we just bounce it but tamper nexus.xml in between
    stopNexus();

    Configuration config = getNexusConfigUtil().loadNexusConfig();
    config.setNexusVersion("1.9.3");
    getNexusConfigUtil().saveNexusConfig(config);

    startNexus();

    StatusResource status = getNexusStatusUtil().getNexusStatus().getData();
    assertFalse(status.isFirstStart());
    assertTrue(status.isInstanceUpgraded());
    assertFalse(status.isConfigurationUpgraded());
  }
}
