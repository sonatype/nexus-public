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

import org.sonatype.nexus.NexusLdapTestSupport;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import org.junit.Assert;
import org.junit.Test;


public class LdapConnValidationIT
    extends NexusLdapTestSupport
{

  private PlexusResource getResource()
      throws Exception
  {
    return this.lookup(PlexusResource.class, "LdapConnectionInfoPlexusResource");
  }

  private LdapConnectionInfoDTO getPopulatedDTO() {
    LdapConnectionInfoDTO connectionInfo = new LdapConnectionInfoDTO();
    connectionInfo.setHost("localhost");
    connectionInfo.setPort(this.getLdapPort());
    connectionInfo.setSearchBase("o=sonatype");
    connectionInfo.setSystemPassword("secret");
    connectionInfo.setSystemUsername("uid=admin,ou=system");
    connectionInfo.setProtocol("ldap");
    connectionInfo.setAuthScheme("simple");
    return connectionInfo;
  }

  @Test
  public void testNoHost()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
    LdapConnectionInfoDTO connectionInfo = this.getPopulatedDTO();
    response.setData(connectionInfo);

    connectionInfo.setHost(null);

    try {
      resource.put(null, null, null, response);
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse result = (ErrorResponse) e.getResultObject();
      Assert.assertEquals(1, result.getErrors().size());
      Assert.assertTrue("Expected error to have the work 'host', was: " + this.getErrorString(result, 0),
          (this.getErrorString(result, 0).toString().toLowerCase().contains("host")));
    }

  }

  @Test
  public void testMultipleErrors()
      throws Exception
  {
    PlexusResource resource = getResource();

    LdapConnectionInfoResponse response = new LdapConnectionInfoResponse();
    LdapConnectionInfoDTO connectionInfo = this.getPopulatedDTO();
    response.setData(connectionInfo);

    connectionInfo.setHost(null);
    connectionInfo.setPort(0);
    try {
      resource.put(null, null, null, response);
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse result = (ErrorResponse) e.getResultObject();
      Assert.assertEquals(2, result.getErrors().size());
    }
  }
}
