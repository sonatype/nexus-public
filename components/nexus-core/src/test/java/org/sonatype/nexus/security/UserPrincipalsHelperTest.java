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
package org.sonatype.nexus.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.usermanagement.AbstractReadOnlyUserManager;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserSearchCriteria;
import org.sonatype.security.usermanagement.UserStatus;

import com.google.common.collect.ObjectArrays;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import junit.framework.Assert;
import net.sf.ehcache.CacheManager;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;

public class UserPrincipalsHelperTest
    extends NexusAppTestSupport
{
  private SecuritySystem securitySystem = null;

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Override
  protected Module[] getTestCustomModules() {
    Module[] modules = super.getTestCustomModules();
    modules = ObjectArrays.concat(modules, new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(Realm.class).annotatedWith(Names.named("TestPrincipalsRealm")).to(TestRealm.class);
        bind(UserManager.class).annotatedWith(Names.named("TestPrincipalsUserManager")).to(TestUserManager.class);
      }
    });
    return modules;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    TestUserManager.status = UserStatus.active;
    TestUserManager.userDeleted = false;

    super.setUp();

    copyResource("UserPrincipalsHelperTest-security.xml", getNexusSecurityConfiguration());
    copyResource("UserPrincipalsHelperTest-security-configuration.xml", getSecurityConfiguration());

    securitySystem = lookup(SecuritySystem.class);
    securitySystem.start();
  }

  @Override
  public void tearDown()
      throws Exception
  {
    try {
      ThreadContext.remove();
      lookup(CacheManager.class).shutdown();
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  public void testFindUserManagerHandlesNull() {
    try {
      helper().findUserManager(null);

      Assert.fail("Expected NoSuchUserManagerException");
    }
    catch (final NoSuchUserManagerException e) {
      // expected...
    }
  }

  @Test
  public void testNoSuchUserManager() {
    try {
      helper().findUserManager(new SimplePrincipalCollection("badUser", "badRealm"));

      Assert.fail("Expected NoSuchUserManagerException");
    }
    catch (final NoSuchUserManagerException e) {
      // expected...
    }
  }

  @Test
  public void testFindUserManager()
      throws NoSuchUserManagerException, AuthenticationException
  {
    final Subject subject = login("test-user", "deployment123");
    try {
      final PrincipalCollection principals = subject.getPrincipals();
      final UserManager userManager = helper().findUserManager(principals);

      assertThat(principals.getPrimaryPrincipal().toString(), isIn(userManager.listUserIds()));
      assertThat(userManager.getAuthenticationRealmName(), isIn(principals.getRealmNames()));
    }
    finally {
      subject.logout();
    }
  }

  @Test
  public void testFindUserManagerNonDefaultRealm()
      throws Exception
  {
    final List<String> realms = securitySystem.getRealms();
    realms.add("TestPrincipalsRealm");
    securitySystem.setRealms(realms);

    final Subject subject = login("tempUser", "tempPass");
    try {
      final PrincipalCollection principals = subject.getPrincipals();
      final UserManager userManager = helper().findUserManager(principals);

      assertThat(principals.getPrimaryPrincipal().toString(), isIn(userManager.listUserIds()));
      assertThat(userManager.getAuthenticationRealmName(), isIn(principals.getRealmNames()));
    }
    finally {
      subject.logout();
    }
  }

  @Test
  public void testGetUserStatusHandlesNull() {
    try {
      helper().getUserStatus(null);

      Assert.fail("Expected UserNotFoundException");
    }
    catch (final UserNotFoundException e) {
      // expected...
    }
  }

  @Test
  public void testUnknownUser() {
    try {
      helper().getUserStatus(new SimplePrincipalCollection("badUser", "badRealm"));

      Assert.fail("Expected UserNotFoundException");
    }
    catch (final UserNotFoundException e) {
      // expected...
    }
  }

  @Test
  public void testGetUserStatus()
      throws UserNotFoundException, AuthenticationException
  {
    final Subject subject = login("test-user", "deployment123");
    try {
      final PrincipalCollection principals = subject.getPrincipals();

      assertThat(helper().getUserStatus(principals), is(UserStatus.active));
    }
    finally {
      subject.logout();
    }
  }

  @Test
  public void testGetUserStatusNonDefaultRealm()
      throws Exception
  {
    final List<String> realms = securitySystem.getRealms();
    realms.add("TestPrincipalsRealm");
    securitySystem.setRealms(realms);

    final Subject subject = login("tempUser", "tempPass");
    try {
      final PrincipalCollection principals = subject.getPrincipals();

      // check status is passed through
      assertThat(helper().getUserStatus(principals), is(UserStatus.active));
      TestUserManager.status = UserStatus.disabled;
      assertThat(helper().getUserStatus(principals), is(UserStatus.disabled));
      TestUserManager.status = UserStatus.locked;
      assertThat(helper().getUserStatus(principals), is(UserStatus.locked));
      TestUserManager.status = UserStatus.active;
      assertThat(helper().getUserStatus(principals), is(UserStatus.active));

      TestUserManager.userDeleted = true;

      try {
        helper().getUserStatus(principals);

        Assert.fail("Expected UserNotFoundException");
      }
      catch (final UserNotFoundException e) {
        // expected...
      }
    }
    finally {
      subject.logout();
    }
  }

  private UserPrincipalsHelper helper() {
    try {
      return lookup(UserPrincipalsHelper.class);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Subject login(String username, String password)
      throws AuthenticationException
  {
    return securitySystem.login(new UsernamePasswordToken(username, password));
  }

  @Singleton
  static final class TestUserManager
      extends AbstractReadOnlyUserManager
  {
    static UserStatus status = UserStatus.active;

    static boolean userDeleted;

    public String getSource() {
      return "TestPrincipalsUserManager";
    }

    public String getAuthenticationRealmName() {
      return "TestPrincipalsRealm";
    }

    @Override
    public Set<User> listUsers() {
      if (!userDeleted) {
        final User user = new DefaultUser();
        user.setUserId("tempUser");
        user.setStatus(status);
        return Collections.singleton(user);
      }
      return Collections.emptySet();
    }

    public User getUser(String userId) {
      for (final User user : listUsers()) {
        if (user.getUserId().equals(userId)) {
          return user;
        }
      }
      return null;
    }

    public Set<String> listUserIds() {
      final Set<String> userIds = new HashSet<String>();
      for (final User user : listUsers()) {
        userIds.add(user.getUserId());
      }
      return userIds;
    }

    public Set<User> searchUsers(UserSearchCriteria criteria) {
      return filterListInMemeory(listUsers(), criteria);
    }
  }

  @Singleton
  static final class TestRealm
      extends AuthorizingRealm
  {
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
      if ("tempUser".equals(((UsernamePasswordToken) token).getUsername())) {
        return new SimpleAuthenticationInfo("tempUser", "tempPass", "TestPrincipalsRealm");
      }
      return null;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
      return null;
    }
  }
}
