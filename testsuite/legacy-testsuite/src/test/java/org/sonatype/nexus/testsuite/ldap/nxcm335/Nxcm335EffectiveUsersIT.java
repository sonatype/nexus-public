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
package org.sonatype.nexus.testsuite.ldap.nxcm335;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.testsuite.ldap.AbstractLdapIntegrationIT;
import org.sonatype.nexus.testsuite.ldap.LdapUsersMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResource;
import org.sonatype.security.rest.model.PlexusUserSearchCriteriaResourceRequest;
import org.sonatype.security.rest.model.UserToRoleResource;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Nxcm335EffectiveUsersIT
 * <p>
 * FIXME: @FixMethodOrder(MethodSorters.NAME_ASCENDING) needed as tests relies on execution order.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Nxcm335EffectiveUsersIT
    extends AbstractLdapIntegrationIT
{
  private XStream xstream;

  private MediaType mediaType;

  public Nxcm335EffectiveUsersIT() {
    super();
  }

  @Before
  public void init() {
    this.xstream = this.getJsonXStream();
    this.mediaType = MediaType.APPLICATION_JSON;
  }

  @Test
  public void searchTestWithEffectiveUsers()
      throws Exception
  {
    int defaultUserCount = this.doSearch("", false, "default").size();

    List<PlexusUserResource> users = this.doSearch("", true, "LDAP");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 2);

    // by default we should have 2 effective users ( using the developer role )
    List<PlexusUserResource> allUsers = this.doSearch("", true, "all");
    Assert.assertEquals("Users found: " + this.toUserIds(allUsers), allUsers.size(), 2 + defaultUserCount);

    // map user to nexus role
    LdapUsersMessageUtil userUtil = new LdapUsersMessageUtil(this, this.xstream, this.mediaType);
    UserToRoleResource ldapUser = new UserToRoleResource();
    ldapUser.setUserId("cstamas");
    ldapUser.setSource("LDAP");
    ldapUser.addRole("nx-admin");
    Response response = userUtil.sendMessage(Method.PUT, ldapUser, "LDAP");
    Assert.assertTrue("Status: " + response.getStatus() + "\nresponse: "
        + response.getEntity().getText(), response.getStatus().isSuccess());

    // search effective users should find user
    users = this.doSearch("", true, "all");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 2 + defaultUserCount);

    users = this.doSearch("", true, "LDAP");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 2);

    ldapUser = new UserToRoleResource();
    ldapUser.setUserId("brianf");
    ldapUser.setSource("LDAP");
    ldapUser.addRole("nx-admin");
    response = userUtil.sendMessage(Method.PUT, ldapUser, "LDAP");
    Assert.assertTrue("Status: " + response.getStatus() + "\nresponse: "
        + response.getEntity().getText(), response.getStatus().isSuccess());

    // search effective users should find user
    users = this.doSearch("", true, "LDAP");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 2);

    users = this.doSearch("", true, "all");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 3 + defaultUserCount);

  }

  @Test
  public void searchTestWithEffectiveUsersFalse()
      throws Exception
  {
    int defaultUserCount = this.doSearch("", false, "default").size();
    defaultUserCount += this.doSearch("", false, "Simple").size(); // the OSS ITs have a memory realm too

    // the xmlrealm tests bring in a couple too ( now that classpath scanning is enabled )
    defaultUserCount += this.doSearch("", false, "MockUserManagerA").size();
    defaultUserCount += this.doSearch("", false, "MockUserManagerB").size();

    // by default we should have 2 effective users ( using the developer role )
    List<PlexusUserResource> users = this.doSearch("", false, "LDAP");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 4);

    users = this.doSearch("", false, "all");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), defaultUserCount + 4);

    // map user to nexus role
    LdapUsersMessageUtil userUtil = new LdapUsersMessageUtil(this, this.xstream, this.mediaType);
    UserToRoleResource ldapUser = new UserToRoleResource();
    ldapUser.setUserId("cstamas");
    ldapUser.setSource("LDAP");
    ldapUser.addRole("nx-admin");
    Response response = userUtil.sendMessage(Method.PUT, ldapUser, "LDAP");
    Assert.assertTrue("Status: " + response.getStatus() + "\nresponse: "
        + response.getEntity().getText(), response.getStatus().isSuccess());

    // search effective users should find user
    users = this.doSearch("", false, "LDAP");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 4);

    users = this.doSearch("", false, "all");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), defaultUserCount + 4);

    ldapUser = new UserToRoleResource();
    ldapUser.setUserId("brianf");
    ldapUser.setSource("LDAP");
    ldapUser.addRole("nx-admin");
    response = userUtil.sendMessage(Method.PUT, ldapUser, "LDAP");
    Assert.assertTrue("Status: " + response.getStatus() + "\nresponse: "
        + response.getEntity().getText(), response.getStatus().isSuccess());

    // search effective users should find user
    users = this.doSearch("", false, "LDAP");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), 4);

    users = this.doSearch("", false, "all");
    Assert.assertEquals("Users found: " + this.toUserIds(users), users.size(), defaultUserCount + 4);

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

    PlexusUserListResourceResponse userList =
        (PlexusUserListResourceResponse) this.parseResponse(response, new PlexusUserListResourceResponse());

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

  private List<String> toUserIds(List<PlexusUserResource> users) {
    List<String> ids = new ArrayList<String>();

    for (PlexusUserResource user : users) {
      ids.add(user.getUserId());
    }
    return ids;
  }

}
