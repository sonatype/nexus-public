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

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserListResponse;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserResponseDTO;
import org.sonatype.nexus.test.utils.ITUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.model.CUserRoleMapping;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapUsersMessageUtil
    extends ITUtil
{
  private static final String SERVICE_PART = RequestFacade.SERVICE_LOCAL + "user_to_roles";

  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(LdapUsersMessageUtil.class);

  public LdapUsersMessageUtil(AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType) {
    super(test);
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public List<LdapUserResponseDTO> getLdapUsers()
      throws IOException
  {
    Response response = this.sendMessage(Method.GET, null, null);

    String responseString = response.getEntity().getText();
    LOG.debug(" getResourceFromResponse: " + responseString);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);
    LdapUserListResponse resourceResponse = (LdapUserListResponse) representation
        .getPayload(new LdapUserListResponse());

    return resourceResponse.getLdapUserRoleMappings();

  }

  public Response updateLdapUser(UserToRoleResource resource, String source)
      throws Exception
  {
    return this.sendMessage(Method.PUT, resource, source);
  }

  public Response sendMessage(Method method, UserToRoleResource resource, String source)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = SERVICE_PART + "/" + source + "/" + resource.getUserId();

    UserToRoleResourceRequest repoResponseRequest = new UserToRoleResourceRequest();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  public void validateLdapConfig(UserToRoleResource userToRole)
      throws Exception
  {
    List<CUserRoleMapping> mapping = getTest().getSecurityConfigUtil().getSecurityConfig().getUserRoleMappings();
    for (CUserRoleMapping userRoleMapping : mapping) {
      if (userToRole.getUserId().equals(userRoleMapping.getUserId())) {
        Assert.assertEquals(userToRole.getRoles(), userRoleMapping.getRoles());
        return;
      }
    }
    Assert.fail("User:  " + userToRole.getUserId() + " not found in config file.");

  }

  public void validateResourceResponse(UserToRoleResource expected, CUserRoleMapping actual)
      throws Exception
  {
    Assert.assertEquals(actual.getUserId(), expected.getUserId());
    Assert.assertEquals(expected.getRoles(), actual.getRoles());

    // also validate the file config
    this.validateLdapConfig(expected);
  }

}
