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
package org.sonatype.nexus.security.role;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;
import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;
import org.sonatype.nexus.security.internal.SecurityConfigurationManagerImpl;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests adding, updating, searching, authc, and authz a user that has an empty role (a role that does not contain any
 * other role or permission).
 */
public class EmptyRoleTest
    extends AbstractSecurityTest
{
  @Test
  public void testCreateEmptyRole() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    // create an empty role
    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    assertThat(authManager.addRole(emptyRole), notNullValue());

    // now create a user and add it to the user
    User user = this.buildTestUser();
    user.setRoles(Collections.singleton(new RoleIdentifier(emptyRole.getSource(), emptyRole.getRoleId())));

    // create the user, this user only has an empty role
    assertThat(securitySystem.addUser(user, "test123"), notNullValue());

    Set<RoleIdentifier> emptyRoleSet = Collections.emptySet();
    user.setRoles(emptyRoleSet);
    assertThat(securitySystem.updateUser(user), notNullValue());

    // delete the empty role
    authManager.deleteRole(emptyRole.getRoleId());
  }

  /**
   * Note: this test is kinda useless, as Security system (as underlying Shiro) is not "reloadable": once created,
   * you need to toss it away and ask another instance from Guice, we cannot reload security currently.
   */
  @Test
  public void testReloadSecurityWithEmptyRole() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    // make sure the role is still there
    Assert.assertNotNull(authManager.getRole(emptyRole.getRoleId()));
  }

  @Test
  public void testAuthorizeUserWithEmptyRole() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    RealmManager realmManager = lookup(RealmManager.class);
    realmManager.setConfiguredRealmIds(ImmutableList.of(AuthenticatingRealmImpl.NAME, AuthorizingRealmImpl.NAME));

    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    // create an empty role
    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    Role normalRole = new Role("normalRole-" + Math.random(), "NormalRole", "Normal Role", "default", false,
            new HashSet<String>(), new HashSet<String>());

    normalRole.addPrivilege(this.createTestPriv());
    authManager.addRole(normalRole);

    // now create a user and add it to the user
    User user = this.buildTestUser();
    user.addRole(new RoleIdentifier(emptyRole.getSource(), emptyRole.getRoleId()));
    user.addRole(new RoleIdentifier(normalRole.getSource(), normalRole.getRoleId()));

    // create the user, this user only has an empty role
    securitySystem.addUser(user, "password");

    // now authorize the user
    Subject subject = securitySystem.getSubject();
    subject.login(new UsernamePasswordToken(user.getUserId(), "password"));
    // check if the user is able to be authenticated if he has an empty role
    subject.checkPermission("app:config:read");
  }

  @Test
  public void testSearchForUserWithEmptyRole() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    AuthorizationManager authManager = securitySystem.getAuthorizationManager("default");

    // create an empty role
    Role emptyRole = this.buildEmptyRole();

    // this should work fine
    authManager.addRole(emptyRole);

    // now create a user and add it to the user
    User user = this.buildTestUser();
    user.setRoles(Collections.singleton(new RoleIdentifier(emptyRole.getSource(), emptyRole.getRoleId())));

    // create the user, this user only has an empty role
    securitySystem.addUser(user, "test123");

    Set<User> userSearchResult = securitySystem.searchUsers(
        new UserSearchCriteria(null, Collections.singleton(emptyRole.getRoleId()), null));
    // this should contain a single result
    Assert.assertEquals(1, userSearchResult.size());
    Assert.assertEquals(user.getUserId(), userSearchResult.iterator().next().getUserId());

  }

  private User buildTestUser() {
    User user = new User();
    user.setUserId("test-user-" + Math.random());
    user.setEmailAddress("test@foo.com");
    user.setFirstName("test");
    user.setLastName("user");
    user.setSource("default");
    user.setStatus(UserStatus.active);

    return user;
  }

  private String createTestPriv() throws Exception {
    CPrivilege priv = WildcardPrivilegeDescriptor.privilege("app:config:read");
    this.lookup(SecurityConfigurationManagerImpl.class).createPrivilege(priv);

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
