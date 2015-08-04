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
package org.sonatype.nexus.testsuite.ldap.nexus3244;

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;
import org.sonatype.nexus.testsuite.ldap.AbstractLdapIntegrationIT;
import org.sonatype.nexus.testsuite.ldap.LdapUserGroupMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResource;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResourceRequest;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus3244ClearConfigWhenChangeIT
    extends AbstractLdapIntegrationIT
{

  private XStream xstream;

  private MediaType mediaType;

  public Nexus3244ClearConfigWhenChangeIT() {
    super();
  }

  @Before
  public void init() {
    this.xstream = this.getJsonXStream();
    this.mediaType = MediaType.APPLICATION_JSON;
  }

  @Test
  public void testConfigIsUpdatedWhenChanged()
      throws Exception
  {
    // start with good configuration
    Assert.assertEquals(4, this.doSearch("", false, "LDAP").size());

    // mess up ldap configuration
    LdapUserGroupMessageUtil userGroupUtil = new LdapUserGroupMessageUtil(xstream, mediaType);
    LdapUserAndGroupConfigurationDTO userGroupConfig = userGroupUtil.getUserGroupConfig();
    String originalIdAttribute = userGroupConfig.getUserIdAttribute();
    userGroupConfig.setUserIdAttribute("JUNKINVALIDJUNK");
    userGroupUtil.updateUserGroupConfig(userGroupConfig);

    Assert.assertEquals(0, this.doSearch("", false, "LDAP").size());

    // change config back to correct state
    userGroupConfig.setUserIdAttribute(originalIdAttribute);
    userGroupUtil.updateUserGroupConfig(userGroupConfig);
    Assert.assertEquals(4, this.doSearch("", false, "LDAP").size());
  }


  private List<PlexusUserResource> doSearch(String userId, boolean effective, String source)
      throws IOException
  {
    PlexusUserSearchCriteriaResourceRequest resourceRequest = new PlexusUserSearchCriteriaResourceRequest();
    PlexusUserSearchCriteriaResource criteria = new PlexusUserSearchCriteriaResource();
    criteria.setUserId(userId);
    criteria.setEffectiveUsers(effective);
    resourceRequest.setData(criteria);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = RequestFacade.SERVICE_LOCAL + "user_search/" + source;

    // now set the payload
    representation.setPayload(resourceRequest);

    log.debug("sendMessage: " + representation.getText());

    Response response = RequestFacade.sendMessage(serviceURI, Method.PUT, representation);

    Assert.assertTrue("Status: " + response.getStatus(), response.getStatus().isSuccess());

    PlexusUserListResourceResponse userList = (PlexusUserListResourceResponse) this.parseResponse(
        response,
        new PlexusUserListResourceResponse());

    return userList.getData();
  }

  private Object parseResponse(Response response, Object expectedObject)
      throws IOException
  {

    String responseString = response.getEntity().getText();
    log.debug(" getResourceFromResponse: " + responseString);

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);
    return representation.getPayload(expectedObject);
  }

}
