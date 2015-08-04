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
package org.sonatype.security.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.eclipse.sisu.space.BeanScanning;

public abstract class AbstractSecurityConfigTest
    extends InjectedTestCase
{

  protected final File PLEXUS_HOME = new File(getBasedir(), "target/plexus-home");

  protected final File CONF_HOME = new File(PLEXUS_HOME, "conf");

  @Override
  public void configure(Properties properties) {
    properties.put("security-xml-file", getSecurityConfiguration());
    super.configure(properties);
  }

  @Override
  public BeanScanning scanning() {
    return BeanScanning.INDEX;
  }

  protected void copyDefaultSecurityConfigToPlace()
      throws IOException
  {
    this.copyResource("/META-INF/security/security.xml", getSecurityConfiguration());
  }

  protected String getSecurityConfiguration() {
    return CONF_HOME + "/security.xml";
  }

  protected void copyResource(String resource, String dest)
      throws IOException
  {
    try (InputStream in = getClass().getResourceAsStream(resource);
         FileOutputStream out = new FileOutputStream(dest)) {
      IOUtils.copy(in, out);
    }
  }

  protected void copyFromClasspathToFile(String path, String outputFilename)
      throws IOException
  {
    copyFromClasspathToFile(path, new File(outputFilename));
  }

  protected void copyFromClasspathToFile(String path, File output)
      throws IOException
  {
    copyFromStreamToFile(getClass().getResourceAsStream(path), output);
  }

  // this one may find its way back to plexus-utils, copied from IOUtil In nexus
  public static void copyFromStreamToFile(InputStream is, File output)
      throws IOException
  {
    try (InputStream in = is;
         FileOutputStream fos = new FileOutputStream(output)) {
      IOUtils.copy(is, fos);
    }
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // delete the config dir
    FileUtils.deleteDirectory(PLEXUS_HOME);

    // create conf dir
    CONF_HOME.mkdirs();
  }

}
