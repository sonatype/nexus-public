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
package org.sonatype.security.rest.users;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.UserToRoleResource;
import org.sonatype.security.rest.model.UserToRoleResourceRequest;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserStatus;
import org.sonatype.security.usermanagement.xml.SecurityXmlUserManager;

import junit.framework.Assert;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class MultipleUsersSameNameTest
    extends AbstractSecurityRestTest
{

  SecuritySystem securitySystem = null;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    securitySystem = this.lookup(SecuritySystem.class);
    securitySystem.start();

    // add a user jcoder to the xml realm
    User jcoder = new DefaultUser();
    jcoder.setEmailAddress("jcoder@java.com");
    jcoder.setName("Joe Coder");
    jcoder.setSource(SecurityXmlUserManager.SOURCE);
    jcoder.setStatus(UserStatus.active);
    jcoder.setUserId("jcoder");
    securitySystem.addUser(jcoder, "jcoder");
  }

  public void testSearchUsers()
      throws Exception
  {
    // now we should have two jcoders, one from the mock and one from the XML realm

    PlexusResource userSearchResource = this.lookup(PlexusResource.class, "UserSearchPlexusResource");

    Request request = this.buildRequest();
    request.getAttributes().put(UserSearchPlexusResource.USER_SOURCE_KEY, "all");
    request.getAttributes().put(UserSearchPlexusResource.USER_ID_KEY, "jcoder");

    List<PlexusUserResource> result =
        ((PlexusUserListResourceResponse) userSearchResource.get(null, request, null, null)).getData();

    // now make sure we have 2 jcoders
    PlexusUserResource jcoderXML = null;
    PlexusUserResource jcoderMock = null;

    for (PlexusUserResource plexusUserResource : result) {
      if (plexusUserResource.getUserId().equals("jcoder")) {
        if (plexusUserResource.getSource().endsWith(SecurityXmlUserManager.SOURCE)) {
          jcoderXML = plexusUserResource;
        }
        else if (plexusUserResource.getSource().endsWith(MockUserManager.SOURCE)) {
          jcoderMock = plexusUserResource;
        }
        else {
          Assert.fail("found a jcoder with an unknown source of: " + plexusUserResource.getSource());
        }
      }
    }

    Assert.assertNotNull("jcoderXML is null", jcoderXML);
    Assert.assertNotNull("jcoderMock is null", jcoderMock);
  }

  public void updateUserRoleTest()
      throws Exception
  {
    // first get the list of the previous users roles so we can make sure they change
    User jcoderXML = securitySystem.getUser("jcoder", SecurityXmlUserManager.SOURCE);
    User jcoderMock = securitySystem.getUser("jcoder", MockUserManager.SOURCE);

    List<String> jcoderXMLOriginalRoles = new ArrayList<String>();
    for (RoleIdentifier role : jcoderXML.getRoles()) {
      jcoderXMLOriginalRoles.add(role.getRoleId());
    }

    List<String> jcoderMockOriginalRoles = new ArrayList<String>();
    for (RoleIdentifier role : jcoderMock.getRoles()) {
      jcoderMockOriginalRoles.add(role.getRoleId());
    }

    // now update one... and check the other

    Request request = this.buildRequest();
    Response response = new Response(request);
    request.getAttributes().put(UserToRolePlexusResource.USER_ID_KEY, "jcoder");
    request.getAttributes().put(UserToRolePlexusResource.SOURCE_ID_KEY, MockUserManager.SOURCE);

    PlexusResource userToRoleResource = this.lookup(PlexusResource.class, "UserSearchPlexusResource");

    UserToRoleResourceRequest payload = new UserToRoleResourceRequest();
    payload.setData(new UserToRoleResource());
    payload.getData().setUserId("jcoder");
    payload.getData().setSource(MockUserManager.SOURCE);
    payload.getData().getRoles().add("admin");

    userToRoleResource.put(null, request, response, null);

    // the xml user should have the original roles the mock users should only have admin.

    jcoderXML = securitySystem.getUser("jcoder", SecurityXmlUserManager.SOURCE);
    jcoderMock = securitySystem.getUser("jcoder", MockUserManager.SOURCE);

    List<String> jcoderXMLNewRoles = new ArrayList<String>();
    for (RoleIdentifier role : jcoderXML.getRoles()) {
      jcoderXMLNewRoles.add(role.getRoleId());
    }

    List<String> jcoderMockNewRoles = new ArrayList<String>();
    for (RoleIdentifier role : jcoderMock.getRoles()) {
      jcoderMockNewRoles.add(role.getRoleId());
    }

    Assert.assertEquals(jcoderXMLOriginalRoles, jcoderXMLNewRoles);

    Assert.assertEquals(1, jcoderMockNewRoles.size());
    Assert.assertTrue(jcoderMockNewRoles.contains("admin"));
  }

  private Request buildRequest() {
    Request request = new Request();

    Reference ref = new Reference("http://localhost:12345/");

    request.setRootRef(ref);
    request.setResourceRef(new Reference(ref, "users"));

    return request;
  }
}
