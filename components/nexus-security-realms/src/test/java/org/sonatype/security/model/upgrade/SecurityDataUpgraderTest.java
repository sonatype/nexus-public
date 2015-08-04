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
package org.sonatype.security.model.upgrade;

import java.io.File;
import java.io.StringWriter;

import org.sonatype.security.model.AbstractSecurityConfigTest;
import org.sonatype.security.model.Configuration;
import org.sonatype.security.model.io.xpp3.SecurityConfigurationXpp3Writer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class SecurityDataUpgraderTest
    extends AbstractSecurityConfigTest
{

  protected SecurityConfigurationUpgrader configurationUpgrader;

  public void setUp()
      throws Exception
  {
    super.setUp();

    FileUtils.cleanDirectory(new File(getSecurityConfiguration()).getParentFile());

    this.configurationUpgrader = (SecurityConfigurationUpgrader) lookup(SecurityConfigurationUpgrader.class);
  }

  protected void resultIsFine(String path, Configuration configuration)
      throws Exception
  {
    SecurityConfigurationXpp3Writer w = new SecurityConfigurationXpp3Writer();

    StringWriter sw = new StringWriter();

    w.write(sw, configuration);

    String shouldBe = IOUtils.toString(getClass().getResourceAsStream(path + ".result"));
    shouldBe = shouldBe.replace("\r\n", "\n");

    assertEquals(shouldBe, sw.toString());
  }

  public void testFrom100()
      throws Exception
  {
    copyFromClasspathToFile("/org/sonatype/security/model/upgrade/data-upgrade/security.xml",
        getSecurityConfiguration());

    Configuration configuration =
        configurationUpgrader.loadOldConfiguration(new File(getSecurityConfiguration()));

    assertEquals(Configuration.MODEL_VERSION, configuration.getVersion());

    resultIsFine("/org/sonatype/security/model/upgrade/data-upgrade/security.xml", configuration);
  }

}
