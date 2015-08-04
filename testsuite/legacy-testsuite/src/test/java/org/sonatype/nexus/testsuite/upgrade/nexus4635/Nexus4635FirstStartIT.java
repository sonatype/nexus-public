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
package org.sonatype.nexus.testsuite.upgrade.nexus4635;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.StatusResource;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 1st start of "virgin" Nexus<BR>
 * https://issues.sonatype.org/browse/NEXUS-4635
 *
 * <pre>
 * <firstStart>true</firstStart>
 * <instanceUpgraded>false</instanceUpgraded>
 * <configurationUpgraded>false</configurationUpgraded>
 * </pre>
 */
public class Nexus4635FirstStartIT
    extends AbstractNexusIntegrationTest
{
  @BeforeClass
  public static void disableSecurity() {
    TestContainer.getInstance().getTestContext().setSecureTest(false);
  }

  @Override
  protected void copyConfigFiles()
      throws IOException
  {
    super.copyConfigFiles();

    // after we copied, we
    // selectively delete config files to make nexus believe this is 1st start, but leave stuff like logback config
    // in place!
    // FileUtils.forceDelete fits nice, since it will throw IOEx if unable to delete (will not be dumb like
    // File.delete() is)
    final File nexusXml = new File(new File(WORK_CONF_DIR), "nexus.xml");
    final File securityXml = new File(new File(WORK_CONF_DIR), "security.xml");
    final File securityConfigurationXml = new File(new File(WORK_CONF_DIR), "security-configuration.xml");

    FileUtils.forceDelete(nexusXml);
    FileUtils.forceDelete(securityXml);
    FileUtils.forceDelete(securityConfigurationXml);
  }

  @Test
  public void checkState()
      throws Exception
  {
    StatusResource status = getNexusStatusUtil().getNexusStatus().getData();
    assertTrue(status.isFirstStart());
    assertFalse(status.isInstanceUpgraded());
    assertFalse(status.isConfigurationUpgraded());
  }

}
