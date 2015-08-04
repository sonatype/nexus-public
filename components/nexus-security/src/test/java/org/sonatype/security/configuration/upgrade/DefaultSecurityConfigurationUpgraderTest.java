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
package org.sonatype.security.configuration.upgrade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;

import org.sonatype.security.configuration.model.SecurityConfiguration;
import org.sonatype.security.configuration.model.io.xpp3.SecurityConfigurationXpp3Writer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DefaultSecurityConfigurationUpgraderTest
    extends InjectedTestCase
{
  private final String UPGRADE_HOME = new String("/org/sonatype/security/configuration/upgrade");

  protected final File PLEXUS_HOME = new File(getBasedir(), "target/plexus-home");

  protected final File CONF_HOME = new File(PLEXUS_HOME, "upgrade");

  private SecurityConfigurationUpgrader configurationUpgrader;

  @Override
  public void configure(Properties properties) {
    properties.put("application-conf", CONF_HOME.getAbsolutePath());
    super.configure(properties);
  }

  @Before
  public void setUp()
      throws Exception
  {
    super.setUp();

    FileUtils.deleteDirectory(PLEXUS_HOME);
    CONF_HOME.mkdirs();

    this.configurationUpgrader = (SecurityConfigurationUpgrader) lookup(SecurityConfigurationUpgrader.class);
  }

  @Test
  public void testFrom203()
      throws Exception
  {
    testUpgrade("security-configuration-203.xml");
  }

  private void testUpgrade(String filename)
      throws Exception
  {
    copyFromClasspathToFile(UPGRADE_HOME + "/" + filename, getSecurityConfiguration());

    SecurityConfiguration configuration = configurationUpgrader
        .loadOldConfiguration(new File(getSecurityConfiguration()));

    assertThat(configuration.getVersion(), is(SecurityConfiguration.MODEL_VERSION));

    resultIsFine(UPGRADE_HOME + "/" + filename, configuration);
  }

  private void resultIsFine(String path, SecurityConfiguration configuration)
      throws Exception
  {
    SecurityConfigurationXpp3Writer w = new SecurityConfigurationXpp3Writer();

    StringWriter sw = new StringWriter();

    w.write(sw, configuration);

    String actual = sw.toString();
    actual = actual.replace("\r\n", "\n");

    String shouldBe = IOUtils.toString(getClass().getResourceAsStream(path + ".result"));
    shouldBe = shouldBe.replace("\r\n", "\n");

    assertThat(actual, is(shouldBe));
  }

  private void copyFromClasspathToFile(String path, String outputFilename)
      throws IOException
  {
    copyFromClasspathToFile(path, new File(outputFilename));
  }

  private void copyFromClasspathToFile(String path, File output)
      throws IOException
  {
    copyFromStreamToFile(getClass().getResourceAsStream(path), output);
  }

  private void copyFromStreamToFile(InputStream is, File output)
      throws IOException
  {
    try (InputStream in = is;
         FileOutputStream fos = new FileOutputStream(output);) {
      IOUtils.copy(is, fos);
    }
  }

  protected String getSecurityConfiguration() {
    return CONF_HOME + "/security-configuration.xml";
  }
}
