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
package org.sonatype.nexus.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.context.Context;

/**
 * Abstract test case for nexus tests.
 *
 * @author cstamas
 */
public abstract class NexusTestSupport
    extends PlexusTestCaseSupport
{

  public static final String WORK_CONFIGURATION_KEY = "nexus-work";

  public static final String APPS_CONFIGURATION_KEY = "apps";

  public static final String CONF_DIR_KEY = "application-conf";

  public static final String SECURITY_XML_FILE = "security-xml-file";

  public static final String RUNTIME_CONFIGURATION_KEY = "runtime";

  public static final String NEXUS_APP_CONFIGURATION_KEY = "nexus-app";

  private File plexusHomeDir = null;

  private File appsHomeDir = null;

  private File workHomeDir = null;

  private File confHomeDir = null;

  private File runtimeHomeDir = null;

  private File nexusappHomeDir = null;

  private File tempDir = null;

  @Override
  protected void customizeContext(Context ctx) {
    super.customizeContext(ctx);

    plexusHomeDir = new File(
        getBasedir(), "target/plexus-home-" + new Random(System.currentTimeMillis()).nextLong()
    );
    appsHomeDir = new File(plexusHomeDir, "apps");
    workHomeDir = new File(plexusHomeDir, "nexus-work");
    confHomeDir = new File(workHomeDir, "conf");
    runtimeHomeDir = new File(plexusHomeDir, "runtime");
    nexusappHomeDir = new File(plexusHomeDir, "nexus-app");
    tempDir = new File(workHomeDir, "tmp");

    ctx.put(WORK_CONFIGURATION_KEY, workHomeDir.getAbsolutePath());
    ctx.put(APPS_CONFIGURATION_KEY, appsHomeDir.getAbsolutePath());
    ctx.put(CONF_DIR_KEY, confHomeDir.getAbsolutePath());
    ctx.put(SECURITY_XML_FILE, getNexusSecurityConfiguration());
    ctx.put(RUNTIME_CONFIGURATION_KEY, runtimeHomeDir.getAbsolutePath());
    ctx.put(NEXUS_APP_CONFIGURATION_KEY, nexusappHomeDir.getAbsolutePath());
    ctx.put("java.io.tmpdir", tempDir.getAbsolutePath());
  }

  @Override
  protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
    configuration.setAutoWiring(true);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_CACHE);
  }

  @Override
  protected void setUp()
      throws Exception
  {
    // keep since PlexusTestCase is not JUnit4 annotated
    super.setUp();

    // simply to make sure customizeContext is handled before anything else
    getContainer();

    plexusHomeDir.mkdirs();
    appsHomeDir.mkdirs();
    workHomeDir.mkdirs();
    confHomeDir.mkdirs();
    runtimeHomeDir.mkdirs();
    nexusappHomeDir.mkdirs();
    tempDir.mkdirs();
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    // keep since PlexusTestCase is not JUnit4 annotated
    super.tearDown();

    cleanDir(plexusHomeDir);
  }

  protected void cleanDir(File dir) {
    if (dir != null) {
      try {
        FileUtils.deleteDirectory(dir);
      }
      catch (IOException e) {
        // couldn't delete directory, too bad
      }
    }
  }

  public File getPlexusHomeDir() {
    return plexusHomeDir;
  }

  public File getWorkHomeDir() {
    return workHomeDir;
  }

  public File getConfHomeDir() {
    return confHomeDir;
  }

  protected String getNexusConfiguration() {
    return new File(confHomeDir, "nexus.xml").getAbsolutePath();
  }

  protected String getSecurityConfiguration() {
    return new File(confHomeDir, "security-configuration.xml").getAbsolutePath();
  }

  protected String getNexusSecurityConfiguration() {
    return new File(confHomeDir, "security.xml").getAbsolutePath();
  }

  protected void copyDefaultConfigToPlace()
      throws IOException
  {
    this.copyResource("/META-INF/nexus/default-oss-nexus.xml", getNexusConfiguration());
  }

  protected void copyDefaultSecurityConfigToPlace()
      throws IOException
  {
    this.copyResource("/META-INF/security/security.xml", getNexusSecurityConfiguration());
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
    FileUtils.copyInputStreamToFile(getClass().getResourceAsStream(path), output);
  }

}
