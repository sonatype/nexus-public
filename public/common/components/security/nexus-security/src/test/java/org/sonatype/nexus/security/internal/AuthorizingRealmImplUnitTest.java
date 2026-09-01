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
package org.sonatype.nexus.security.internal;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.authz.PrincipalPermissionsCache;
import org.sonatype.nexus.security.authz.PrincipalPermissionsCacheFactory;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.RoleMappingUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct (non-Spring) unit tests for {@link AuthorizingRealmImpl#doGetAuthorizationInfo} and its helpers, covering the
 * disabled-realm, role-mapping, default-realm and unmanaged-user branches that the Spring integration test does not.
 */
class AuthorizingRealmImplUnitTest
{
  private static final String USER = "jdoe";

  private final RealmSecurityManager realmSecurityManager = mock(RealmSecurityManager.class);

  private static PrincipalPermissionsCacheFactory disabledCacheFactory() {
    PrincipalPermissionsCacheFactory factory = mock(PrincipalPermissionsCacheFactory.class);
    when(factory.create(anyString())).thenReturn(new PrincipalPermissionsCache(false, -1, Duration.ZERO, false, 1));
    return factory;
  }

  private static Realm realmNamed(final String name) {
    Realm realm = mock(Realm.class);
    lenient().when(realm.getName()).thenReturn(name);
    return realm;
  }

  private AuthorizingRealmImpl realm(final UserManager userManager, final List<UserManager> userManagerList) {
    return new AuthorizingRealmImpl(realmSecurityManager, userManager, userManagerList, disabledCacheFactory());
  }

  @Test
  void doesNotAuthenticate() {
    AuthorizingRealmImpl realm = realm(mock(UserManager.class), emptyList());
    assertFalse(realm.supports(mock(AuthenticationToken.class)));
    assertNull(realm.doGetAuthenticationInfo(mock(AuthenticationToken.class)));
  }

  @Test
  void nullPrincipalsThrows() {
    AuthorizingRealmImpl realm = realm(mock(UserManager.class), emptyList());
    assertThrows(AuthorizationException.class, () -> realm.doGetAuthorizationInfo(null));
  }

  @Test
  void principalFromDisabledRealmThrows() {
    Realm enabled = realmNamed("LDAP");
    when(realmSecurityManager.getRealms()).thenReturn(List.of(enabled));
    AuthorizingRealmImpl realm = realm(mock(UserManager.class), emptyList());

    // Principal's realm ("Ghost") is neither this realm nor an enabled realm.
    SimplePrincipalCollection principals = new SimplePrincipalCollection(USER, "Ghost");
    assertThrows(AuthorizationException.class, () -> realm.doGetAuthorizationInfo(principals));
  }

  @Test
  void roleMappingManagerAggregatesRolesAcrossEnabledRealm() throws Exception {
    Realm enabled = realmNamed("LDAP");
    when(realmSecurityManager.getRealms()).thenReturn(List.of(enabled));
    RoleMappingUserManager userManager = mock(RoleMappingUserManager.class);
    when(userManager.getUsersRoles(USER, "LDAP")).thenReturn(Set.of(new RoleIdentifier("LDAP", "role1")));

    AuthorizationInfo info =
        realm(userManager, emptyList()).doGetAuthorizationInfo(new SimplePrincipalCollection(USER, "LDAP"));

    assertTrue(info.getRoles().contains("role1"));
  }

  @Test
  void roleMappingManagerToleratesUserNotFound() throws Exception {
    Realm enabled = realmNamed("LDAP");
    when(realmSecurityManager.getRealms()).thenReturn(List.of(enabled));
    RoleMappingUserManager userManager = mock(RoleMappingUserManager.class);
    when(userManager.getUsersRoles(USER, "LDAP")).thenThrow(new UserNotFoundException(USER));

    AuthorizationInfo info =
        realm(userManager, emptyList()).doGetAuthorizationInfo(new SimplePrincipalCollection(USER, "LDAP"));

    assertTrue(info.getRoles().isEmpty());
  }

  @Test
  void cleanUpRealmListMapsAuthenticationRealmNameToSource() throws Exception {
    Realm enabled = realmNamed("LDAP");
    when(realmSecurityManager.getRealms()).thenReturn(List.of(enabled));
    RoleMappingUserManager userManager = mock(RoleMappingUserManager.class);
    // Source-keyed lookup proves the auth-realm-name "LDAP" was rewritten to source "ldap".
    when(userManager.getUsersRoles(USER, "ldap")).thenReturn(Set.of(new RoleIdentifier("ldap", "ldapRole")));

    UserManager mapped = mock(UserManager.class);
    when(mapped.getAuthenticationRealmName()).thenReturn("LDAP");
    when(mapped.getSource()).thenReturn("ldap");
    UserManager nullAuthRealm = mock(UserManager.class);
    when(nullAuthRealm.getAuthenticationRealmName()).thenReturn(null);

    AuthorizationInfo info = realm(userManager, List.of(mapped, nullAuthRealm))
        .doGetAuthorizationInfo(new SimplePrincipalCollection(USER, "LDAP"));

    assertTrue(info.getRoles().contains("ldapRole"));
  }

  @Test
  void defaultManagerResolvesRolesForLocalRealm() throws Exception {
    UserManager userManager = mock(UserManager.class);
    User user = mock(User.class);
    when(user.getRoles()).thenReturn(Set.of(new RoleIdentifier("default", "localRole")));
    when(userManager.getUser(USER)).thenReturn(user);

    // Principal is in this realm's own name -> cleanUpRealmList rewrites it to "default".
    AuthorizationInfo info = realm(userManager, emptyList())
        .doGetAuthorizationInfo(new SimplePrincipalCollection(USER, AuthorizingRealmImpl.NAME));

    assertTrue(info.getRoles().contains("localRole"));
  }

  @Test
  void defaultManagerThrowsWhenUserNotFound() throws Exception {
    UserManager userManager = mock(UserManager.class);
    when(userManager.getUser(USER)).thenThrow(new UserNotFoundException(USER));

    AuthorizingRealmImpl realm = realm(userManager, emptyList());
    SimplePrincipalCollection principals = new SimplePrincipalCollection(USER, AuthorizingRealmImpl.NAME);
    assertThrows(AuthorizationException.class, () -> realm.doGetAuthorizationInfo(principals));
  }

  @Test
  void unmanagedUserThrows() {
    Realm enabled = realmNamed("Weird");
    when(realmSecurityManager.getRealms()).thenReturn(List.of(enabled));
    // Plain (non-role-mapping) manager and a realm that is neither "default" nor rewritten -> unmanaged.
    AuthorizingRealmImpl realm = realm(mock(UserManager.class), emptyList());

    SimplePrincipalCollection principals = new SimplePrincipalCollection(USER, "Weird");
    assertThrows(AuthorizationException.class, () -> realm.doGetAuthorizationInfo(principals));
  }
}
