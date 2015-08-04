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
package org.sonatype.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.realms.XmlAuthenticatingRealm;
import org.sonatype.security.realms.XmlAuthorizingRealm;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeMethodPropertyDescriptor;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegePermissionPropertyDescriptor;
import org.sonatype.security.realms.tools.DefaultConfigurationManager;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserSearchCriteria;
import org.sonatype.security.usermanagement.UserStatus;

import junit.framework.Assert;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

/**
 * Tests adding, updating, searching, authc, and authz a user that has an empty role (a role that does not contain any
 * other role or permission).
 */
public class EmptyRoleTest
    extends AbstractSecurityTestCase
{
  public void testCreateEmptyRole()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    // create an empty role
    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    // now create a user and add it to the user
    DefaultUser user = this.buildTestUser();
    user.setRoles(Collections.singleton(new RoleIdentifier(emptyRole.getSource(), emptyRole.getRoleId())));

    // create the user, this user only has an empty role
    securitySystem.addUser(user);

    Set<RoleIdentifier> emptyRoleSet = Collections.emptySet();
    user.setRoles(emptyRoleSet);
    securitySystem.updateUser(user);

    // delete the empty role
    authManager.deleteRole(emptyRole.getRoleId());
  }

  /**
   * Note: this test is kinda useless, as Security system (as underlying Shiro) is not "reloadable": once created,
   * you
   * need to toss it away and ask another instance from Guice, we cannot reload security currently.
   */
  public void testReloadSecurityWithEmptyRole()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    // make sure the role is still there
    Assert.assertNotNull(authManager.getRole(emptyRole.getRoleId()));
  }

  public void testAuthorizeUserWithEmptyRole()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    securitySystem.setRealms(Arrays.asList(XmlAuthenticatingRealm.ROLE, XmlAuthorizingRealm.ROLE));
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    // create an empty role
    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    Role normalRole =
        new Role("normalRole-" + Math.random(), "NormalRole", "Normal Role", "default", false,
            new HashSet<String>(), new HashSet<String>());
    normalRole.addPrivilege(this.createTestPriv());
    authManager.addRole(normalRole);

    // now create a user and add it to the user
    DefaultUser user = this.buildTestUser();
    user.addRole(new RoleIdentifier(emptyRole.getSource(), emptyRole.getRoleId()));
    user.addRole(new RoleIdentifier(normalRole.getSource(), normalRole.getRoleId()));

    // create the user, this user only has an empty role
    securitySystem.addUser(user, "password");

    // now authorize the user
    Subject subject = securitySystem.login(new UsernamePasswordToken(user.getUserId(), "password"));
    // check if the user is able to be authenticated if he has an empty role
    subject.checkPermission("app:config:read");
  }

  public void testSearchForUserWithEmptyRole()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    // create an empty role
    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    // now create a user and add it to the user
    DefaultUser user = this.buildTestUser();
    user.setRoles(Collections.singleton(new RoleIdentifier(emptyRole.getSource(), emptyRole.getRoleId())));

    // create the user, this user only has an empty role
    securitySystem.addUser(user);

    Set<User> userSearchResult =
        securitySystem.searchUsers(new UserSearchCriteria(null, Collections.singleton(emptyRole.getRoleId()),
            null));
    // this should contain a single result
    Assert.assertEquals(1, userSearchResult.size());
    Assert.assertEquals(user.getUserId(), userSearchResult.iterator().next().getUserId());

  }

  private DefaultUser buildTestUser() {
    DefaultUser user = new DefaultUser();
    user.setUserId("test-user-" + Math.random());
    user.setEmailAddress("test@foo.com");
    user.setFirstName("test");
    user.setLastName("user");
    user.setSource("default");
    user.setStatus(UserStatus.active);

    return user;
  }

  private String createTestPriv()
      throws InvalidConfigurationException
  {
    CProperty permissionProp = new CProperty();
    permissionProp.setKey(ApplicationPrivilegePermissionPropertyDescriptor.ID);
    permissionProp.setValue("app:config");

    CProperty methodProp = new CProperty();
    methodProp.setKey(ApplicationPrivilegeMethodPropertyDescriptor.ID);
    methodProp.setValue("read");

    CPrivilege priv = new CPrivilege();
    priv.setId("priv-" + Math.random());
    priv.setName("somepriv");
    priv.setType(ApplicationPrivilegeDescriptor.TYPE);
    priv.setDescription("somedescription");
    priv.addProperty(permissionProp);
    priv.addProperty(methodProp);

    this.lookup(DefaultConfigurationManager.class).createPrivilege(priv);

    return priv.getId();
  }

  private Role buildEmptyRole() {
    Role emptyRole = new Role();
    emptyRole.setName("Empty Role");
    emptyRole.setDescription("Empty Role");
    emptyRole.setRoleId("emptyRole-" + Math.random());
    // no contained roles or privileges

    return emptyRole;
  }

}
