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
package org.sonatype.security.ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;

import org.sonatype.ldaptestsuite.LdapServer;
import org.sonatype.nexus.test.NexusTestSupport;
import org.sonatype.security.guice.SecurityModule;

import com.google.common.collect.ObjectArrays;
import com.google.inject.Module;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.util.InterpolationFilterReader;

public abstract class LdapTestSupport
    extends NexusTestSupport
{
  private LdapServer ldapServer;

  private File ldapRealmConfig;

  @Override
  protected Module[] getTestCustomModules() {
    Module[] modules = super.getTestCustomModules();
    if (modules == null) {
      modules = new Module[0];
    }
    modules = ObjectArrays.concat(modules, new SecurityModule());
    return modules;
  }

  protected LdapServer getLdapServer() {
    return ldapServer;
  }

  protected File getLdapRealmConfig() {
    return ldapRealmConfig;
  }

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    // check if we have a custom ldap.xml for this test
    String classname = this.getClass().getName();
    File sourceLdapXml =
        new File(getBasedir() + "/target/test-classes/" + classname.replace('.', '/') + "-ldap.xml");

    ldapServer = (LdapServer) lookup(LdapServer.ROLE);
    ldapRealmConfig = new File(getConfHomeDir(), "ldap.xml");
    if (sourceLdapXml.exists()) {
      this.interpolateLdapXml(sourceLdapXml, ldapRealmConfig);
    }
    else {
      this.interpolateLdapXml("/test-conf/conf/ldap.xml", ldapRealmConfig);
    }
  }

  @Override
  public void tearDown()
      throws Exception
  {
    lookup(CacheManager.class).shutdown();

    if (ldapServer != null && ldapServer.isStarted()) {
      ldapServer.stop();
      ldapServer = null;
    }

    super.tearDown();
  }

  @Override
  protected void customizeContainerConfiguration(final ContainerConfiguration configuration) {
    super.customizeContainerConfiguration(configuration);
    configuration.setAutoWiring(true);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
  }

  /**
   * Copies a resource from from the classpath to the given output file and closes streams.
   */
  protected void copyResourceToFile(String resource, File outputFile)
      throws IOException
  {
    try (InputStream in = getClass().getResourceAsStream(resource);
         OutputStream out = new FileOutputStream(outputFile)) {
      IOUtils.copy(in, out);
    }
  }

  protected void copyResourceToFile(String resource, String outputFilePath)
      throws IOException
  {
    copyResourceToFile(resource, new File(outputFilePath));
  }

  /**
   * Interpolates the ldap.xml file and copies it to the outputfile.
   */
  protected void interpolateLdapXml(String resource, File outputFile)
      throws IOException
  {
    try (InputStream in = getClass().getResourceAsStream(resource)) {
      interpolateLdapXml(in, outputFile);
    }
  }

  protected void interpolateLdapXml(File sourceFile, File outputFile)
      throws IOException
  {
    try (FileInputStream fis = new FileInputStream(sourceFile)) {
      interpolateLdapXml(fis, outputFile);
    }
  }

  private void interpolateLdapXml(InputStream inputStream, File outputFile)
      throws IOException
  {
    HashMap<String, String> interpolationMap = new HashMap<String, String>();
    interpolationMap.put("port", Integer.toString(getLdapServer().getPort()));

    try (Reader reader = new InterpolationFilterReader(new InputStreamReader(inputStream), interpolationMap);
         OutputStream out = new FileOutputStream(outputFile)) {
      IOUtils.copy(reader, out);
    }
  }

}
