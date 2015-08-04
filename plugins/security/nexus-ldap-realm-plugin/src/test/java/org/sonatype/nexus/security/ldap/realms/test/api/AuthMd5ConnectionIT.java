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
package org.sonatype.nexus.security.ldap.realms.test.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.NexusLdapTestSupport;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapConnectionInfoDTO;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapAuthenticationTestRequest;
import org.sonatype.plexus.rest.resource.PlexusResource;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;


public class AuthMd5ConnectionIT
    extends NexusLdapTestSupport
{

  private PlexusResource getResource()
      throws Exception
  {
    return this
        .lookup(PlexusResource.class, "LdapTestAuthenticationPlexusResource");
  }

  @Override
  protected void copyDefaultLdapConfigToPlace()
      throws IOException
  {
    try (InputStream in = getClass().getResourceAsStream("/test-conf/md5-ldap.xml")) {
      interpolateLdapXml(in, new File(getNexusLdapConfiguration()));
    }
  }

  @Test
  public void testSuccess() throws Exception {
    PlexusResource resource = getResource();
    LdapAuthenticationTestRequest testRequest = new LdapAuthenticationTestRequest();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();
    testRequest.setData(dto);
    dto.setHost("localhost");
    dto.setPort(this.getLdapPort());
    dto.setSearchBase("o=sonatype");
    dto.setSystemPassword("secret");
    dto.setSystemUsername("admin");
    dto.setProtocol("ldap");
    dto.setAuthScheme("DIGEST-MD5");
    dto.setRealm("localhost");

    Request request = new Request();
    Response response = new Response(request);

    Assert.assertNull(resource.put(null, request, response, testRequest));
    Assert.assertEquals(204, response.getStatus().getCode());
  }

  @Test
  public void testSuccessWithPass() throws Exception {
    PlexusResource resource = getResource();
    LdapAuthenticationTestRequest testRequest = new LdapAuthenticationTestRequest();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();
    testRequest.setData(dto);
    dto.setHost("localhost");
    dto.setPort(this.getLdapPort());
    dto.setSearchBase("o=sonatype");
    dto.setSystemPassword("secret");
    dto.setSystemUsername("uid=admin,ou=system");
    dto.setProtocol("ldap");
    dto.setAuthScheme("simple");
    dto.setRealm("localhost");

    Request request = new Request();
    Response response = new Response(request);

    Assert.assertNull(resource.put(null, request, response, testRequest));
    Assert.assertEquals(204, response.getStatus().getCode());
  }

  @Test
  public void testSuccessWithNoPass() throws Exception {
    PlexusResource resource = getResource();
    LdapAuthenticationTestRequest testRequest = new LdapAuthenticationTestRequest();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();
    testRequest.setData(dto);
    dto.setHost("localhost");
    dto.setPort(this.getLdapPort());
    dto.setSearchBase("o=sonatype");
    //        dto.setSystemPassword( "secret" );
    //        dto.setSystemUsername( "uid=admin,ou=system" );
    dto.setProtocol("ldap");
    dto.setAuthScheme("none");
    dto.setRealm("localhost");

    Request request = new Request();
    Response response = new Response(request);

    Assert.assertNull(resource.put(null, request, response, testRequest));
    Assert.assertEquals(204, response.getStatus().getCode());
  }

  @Test
  public void testFailure() throws Exception {

    PlexusResource resource = getResource();
    LdapAuthenticationTestRequest testRequest = new LdapAuthenticationTestRequest();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();
    testRequest.setData(dto);
    dto.setHost("localhost");
    dto.setPort(this.getLdapPort());
    dto.setSearchBase("o=sonatype");
    //        dto.setSystemPassword( "secret" );
    //        dto.setSystemUsername( "admin" );
    dto.setProtocol("ldap");
    dto.setAuthScheme("DIGEST-MD5");
    dto.setRealm("localhost");

    Request request = new Request();
    Response response = new Response(request);

    try {
      resource.put(null, request, response, testRequest);
      Assert.fail("Expected ResourceException");
    }
    catch (ResourceException e) {
      Assert.assertEquals(400, e.getStatus().getCode());
    }
  }

  @Test
  public void testFailureWrongPass() throws Exception {
    PlexusResource resource = getResource();
    LdapAuthenticationTestRequest testRequest = new LdapAuthenticationTestRequest();
    LdapConnectionInfoDTO dto = new LdapConnectionInfoDTO();
    testRequest.setData(dto);
    dto.setHost("localhost");
    dto.setPort(this.getLdapPort());
    dto.setSearchBase("o=sonatype");
    dto.setSystemPassword("JUNK");
    dto.setSystemUsername("admin");
    dto.setProtocol("ldap");
    dto.setAuthScheme("DIGEST-MD5");
    dto.setRealm("localhost");

    Request request = new Request();
    Response response = new Response(request);

    try {
      resource.put(null, request, response, testRequest);
      Assert.fail("Expected ResourceException");
    }
    catch (ResourceException e) {
      Assert.assertEquals(400, e.getStatus().getCode());
    }
  }
}
