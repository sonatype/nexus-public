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
package org.sonatype.nexus.testsuite.ldap;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.ldaptestsuite.LdapServer;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.security.ldap.realms.api.LdapXStreamConfigurator;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractLdapIntegrationIT
    extends AbstractNexusIntegrationTest
{
  public static final String LDIF_DIR = "../../ldif_dir";

  private LdapServer ldapServer;

  public AbstractLdapIntegrationIT() {

  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Override
  protected void copyConfigFiles()
      throws IOException
  {
    super.copyConfigFiles();

    this.copyConfigFile("test.ldif", LDIF_DIR);

    // copy ldap.xml to work dir
    Map<String, String> interpolationMap = new HashMap<String, String>();
    interpolationMap.put("port", Integer.toString(this.getLdapPort()));

    this.copyConfigFile("ldap.xml", interpolationMap, WORK_CONF_DIR);

  }

  protected boolean deleteLdapConfig() {
    File ldapConfig = new File(WORK_CONF_DIR, "ldap.xml");
    if (ldapConfig.exists()) {
      return ldapConfig.delete();
    }
    return true;
  }

  protected int getLdapPort() {
    if (this.ldapServer == null) {
      try {
        beforeLdapTests();
      }
      catch (Exception e) {
        e.printStackTrace();
        Assert.fail("Failed to initilize ldap server: " + e.getMessage());
      }
    }
    return this.ldapServer.getPort();
  }

  @Before
  public void beforeLdapTests()
      throws Exception
  {
    if (this.ldapServer == null) {
      this.ldapServer = lookupLdapServer();
    }

    if (!this.ldapServer.isStarted()) {
      this.ldapServer.start();
    }
  }

  protected LdapServer lookupLdapServer()
      throws ComponentLookupException
  {
    return lookup(LdapServer.class);
  }

  @After
  public void afterLdapTests()
      throws Exception
  {
    if (this.ldapServer != null) {
      this.ldapServer.stop();
    }
  }

  @Override
  public XStream getXMLXStream() {
    return LdapXStreamConfigurator.configureXStream(super.getXMLXStream());
  }

  @Override
  public XStream getJsonXStream() {
    return LdapXStreamConfigurator.configureXStream(super.getJsonXStream());
  }
}
