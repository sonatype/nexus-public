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

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationResponse;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequest;
import org.sonatype.nexus.security.ldap.realms.test.api.dto.LdapUserAndGroupConfigTestRequestDTO;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.ldap.realms.persist.model.CUserAndGroupAuthConfiguration;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapUserGroupMessageUtil
{

  private static final String SERVICE_PART = RequestFacade.SERVICE_LOCAL + "ldap/user_group_conf";

  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(GroupMessageUtil.class);

  public LdapUserGroupMessageUtil(XStream xstream, MediaType mediaType) {
    super();
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public LdapUserAndGroupConfigurationDTO getUserGroupConfig()
      throws IOException
  {
    Response response = this.sendMessage(Method.GET, null);
    return this.getResourceFromResponse(response);
  }

  public LdapUserAndGroupConfigurationDTO updateUserGroupConfig(LdapUserAndGroupConfigurationDTO userGroupConfig)
      throws Exception
  {
    Response response = this.sendMessage(Method.PUT, userGroupConfig);

    if (!response.getStatus().isSuccess()) {
      String responseText = response.getEntity().getText();
      Assert.fail("Could not create Repository: " + response.getStatus() + ":\n" + responseText);
    }
    LdapUserAndGroupConfigurationDTO responseResource = this.getResourceFromResponse(response);

    this.validateResourceResponse(userGroupConfig, responseResource);

    return responseResource;
  }

  public Response sendMessage(Method method, LdapUserAndGroupConfigurationDTO resource)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = SERVICE_PART;

    LdapUserAndGroupConfigurationResponse repoResponseRequest = new LdapUserAndGroupConfigurationResponse();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  public Response sendTestMessage(LdapUserAndGroupConfigTestRequestDTO resource)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = RequestFacade.SERVICE_LOCAL + "ldap/test_user_conf";

    LdapUserAndGroupConfigTestRequest repoResponseRequest = new LdapUserAndGroupConfigTestRequest();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return RequestFacade.sendMessage(serviceURI, Method.PUT, representation);
  }

  public LdapUserAndGroupConfigurationDTO getResourceFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();
    LOG.debug(" getResourceFromResponse: " + responseString);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);
    LdapUserAndGroupConfigurationResponse resourceResponse = (LdapUserAndGroupConfigurationResponse) representation
        .getPayload(new LdapUserAndGroupConfigurationResponse());

    return resourceResponse.getData();
  }

  @SuppressWarnings("unchecked")
  public void validateLdapConfig(LdapUserAndGroupConfigurationDTO connInfo)
      throws Exception
  {
    CUserAndGroupAuthConfiguration fileConfig = LdapConfigurationUtil.getConfiguration().getUserAndGroupConfig();
    Assert.assertEquals(fileConfig.getGroupBaseDn(), connInfo.getGroupBaseDn());
    Assert.assertEquals(fileConfig.getGroupIdAttribute(), connInfo.getGroupIdAttribute());
    Assert.assertEquals(fileConfig.getGroupMemberAttribute(), connInfo.getGroupMemberAttribute());
    Assert.assertEquals(fileConfig.getGroupMemberFormat(), connInfo.getGroupMemberFormat());
    Assert.assertEquals(fileConfig.getGroupObjectClass(), connInfo.getGroupObjectClass());
    Assert.assertEquals(fileConfig.getUserBaseDn(), connInfo.getUserBaseDn());
    Assert.assertEquals(fileConfig.getUserIdAttribute(), connInfo.getUserIdAttribute());
    Assert.assertEquals(fileConfig.getUserObjectClass(), connInfo.getUserObjectClass());
    Assert.assertEquals(fileConfig.getUserPasswordAttribute(), connInfo.getUserPasswordAttribute());
    Assert.assertEquals(fileConfig.getUserRealNameAttribute(), connInfo.getUserRealNameAttribute());
    Assert.assertEquals(fileConfig.getEmailAddressAttribute(), connInfo.getEmailAddressAttribute());
    Assert.assertEquals(fileConfig.isLdapGroupsAsRoles(), connInfo.isLdapGroupsAsRoles());
    Assert.assertEquals(fileConfig.getUserMemberOfAttribute(), connInfo.getUserMemberOfAttribute());
    Assert.assertEquals(fileConfig.isGroupSubtree(), connInfo.isGroupSubtree());
    Assert.assertEquals(fileConfig.isUserSubtree(), connInfo.isUserSubtree());
  }

  public void validateResourceResponse(LdapUserAndGroupConfigurationDTO expected,
                                       LdapUserAndGroupConfigurationDTO actual)
      throws Exception
  {

    // this object has an equals method, but it makes for not so easy debuging, so call it after each field compare to make
    // sure we didn't forget anything
    Assert.assertEquals(actual.getGroupBaseDn(), expected.getGroupBaseDn());
    Assert.assertEquals(actual.getGroupIdAttribute(), expected.getGroupIdAttribute());
    Assert.assertEquals(actual.getGroupMemberAttribute(), expected.getGroupMemberAttribute());
    Assert.assertEquals(actual.getGroupMemberFormat(), expected.getGroupMemberFormat());
    Assert.assertEquals(actual.getGroupObjectClass(), expected.getGroupObjectClass());
    Assert.assertEquals(actual.getUserBaseDn(), expected.getUserBaseDn());
    Assert.assertEquals(actual.getUserIdAttribute(), expected.getUserIdAttribute());
    Assert.assertEquals(actual.getUserObjectClass(), expected.getUserObjectClass());
    Assert.assertEquals(actual.getUserPasswordAttribute(), expected.getUserPasswordAttribute());
    Assert.assertEquals(actual.getUserRealNameAttribute(), expected.getUserRealNameAttribute());
    Assert.assertEquals(actual.getEmailAddressAttribute(), expected.getEmailAddressAttribute());
    Assert.assertEquals(actual.isLdapGroupsAsRoles(), expected.isLdapGroupsAsRoles());
    Assert.assertEquals(actual.getUserMemberOfAttribute(), expected.getUserMemberOfAttribute());
    Assert.assertEquals(actual.isGroupSubtree(), expected.isGroupSubtree());
    Assert.assertEquals(actual.isUserSubtree(), expected.isUserSubtree());
    Assert.assertEquals(actual, expected);

    // also validate the file config
    this.validateLdapConfig(expected);
  }

}
