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
package org.sonatype.nexus.security.ldap.realms.api;

import java.io.File;

import org.sonatype.nexus.NexusLdapTestSupport;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.junit.Assert;
import org.junit.Test;

public class LdapConnNotConfiguredIT
    extends NexusLdapTestSupport
{
  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    // delete the ldap.xml file, if any
    new File(getConfHomeDir(), "ldap.xml").delete();
  }

  private PlexusResource getResource()
      throws Exception
  {
    return this.lookup(PlexusResource.class, "LdapConnectionInfoPlexusResource");
  }

  @Test
  public void testGetNotConfigured()
      throws Exception
  {
    PlexusResource resource = getResource();

    // none of these args are used, but if they start being used, we will need to change this.
    LdapConnectionInfoResponse response = (LdapConnectionInfoResponse) resource.get(null, null, null, null);

    // asssert an empty data is returned
    Assert.assertEquals(new LdapConnectionInfoDTO(), response.getData());
  }

  @Test
  public void testPutNotConfigured()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
    LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
    response.setData(connectionInfo);
    connectionInfo.setHost("localhost");
    connectionInfo.setPort(this.getLdapPort());
    connectionInfo.setSearchBase("o=sonatype");
    connectionInfo.setSystemPassword("secret");
    connectionInfo.setSystemUsername("uid=admin,ou=system");
    connectionInfo.setProtocol("ldap");
    connectionInfo.setAuthScheme("simple");

    LdapConnectionInfoResponse result = (LdapConnectionInfoResponse) resource.put(null, null, null, response);
    this.validateConnectionDTO(connectionInfo, result.getData());

    // now how about that get
    result = (LdapConnectionInfoResponse) resource.get(null, null, null, null);
    this.validateConnectionDTO(connectionInfo, result.getData());
  }

  @Test
  public void testSetPasswordToFake()
      throws Exception
  {

    PlexusResource resource = getResource();

    LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
    LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
    response.setData(connectionInfo);
    connectionInfo.setHost("localhost");
    connectionInfo.setPort(this.getLdapPort());
    connectionInfo.setSearchBase("o=sonatype");
    connectionInfo.setSystemPassword(LdapRealmPlexusResourceConst.FAKE_PASSWORD);
    connectionInfo.setSystemUsername("uid=admin,ou=system");
    connectionInfo.setProtocol("ldap");
    connectionInfo.setAuthScheme("simple");

    //this is the same as not setting the password so it should throw an exception

    try {
      resource.put(null, null, null, response);
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse errorResponse = (ErrorResponse) e.getResultObject();
      Assert.assertEquals(1, errorResponse.getErrors().size());

      Assert.assertTrue(this.getErrorString(errorResponse, 0).toLowerCase().contains("password"));
    }
  }

  @Test
  public void testGetPasswordNullWhenNotSet()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
    LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
    response.setData(connectionInfo);
    connectionInfo.setHost("localhost");
    connectionInfo.setPort(this.getLdapPort());
    connectionInfo.setSearchBase("o=sonatype");
    //        connectionInfo.setSystemPassword( "secret" );
    //        connectionInfo.setSystemUsername( "uid=admin,ou=system" );
    connectionInfo.setProtocol("ldap");
    connectionInfo.setAuthScheme("none");

    LdapConnectionInfoResponse result = (LdapConnectionInfoResponse) resource.put(null, null, null, response);
    this.validateConnectionDTO(connectionInfo, result.getData());

    // now how about that get
    result = (LdapConnectionInfoResponse) resource.get(null, null, null, null);
    this.validateConnectionDTO(connectionInfo, result.getData());
  }
}
