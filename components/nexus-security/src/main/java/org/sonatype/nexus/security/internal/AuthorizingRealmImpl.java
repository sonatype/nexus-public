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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.RoleMappingUserManager;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link AuthorizingRealm}.
 *
 * This realm ONLY handles authorization.
 */
@Singleton
@Named(AuthorizingRealmImpl.NAME)
@Description("Nexus Authorizing Realm")
public class AuthorizingRealmImpl
    extends AuthorizingRealm
    implements Realm
{
  private static final Logger logger = LoggerFactory.getLogger(AuthorizingRealmImpl.class);

  public static final String NAME = "NexusAuthorizingRealm";

  private final RealmSecurityManager realmSecurityManager;

  private final UserManager userManager;

  private final Map<String, UserManager> userManagerMap;

  @Inject
  public AuthorizingRealmImpl(final RealmSecurityManager realmSecurityManager,
                              final UserManager userManager,
                              final Map<String, UserManager> userManagerMap)
  {
    this.realmSecurityManager = realmSecurityManager;
    this.userManager = userManager;
    this.userManagerMap = userManagerMap;
    setCredentialsMatcher(new Sha1CredentialsMatcher());
    setName(NAME);
    setAuthenticationCachingEnabled(false); // we authz only, no authc done by this realm
    setAuthorizationCachingEnabled(true);
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return false;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
    return null;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    if (principals == null) {
      throw new AuthorizationException("Cannot authorize with no principals.");
    }

    String username = principals.getPrimaryPrincipal().toString();
    Set<String> roles = new HashSet<String>();

    Set<String> realmNames = new HashSet<String>(principals.getRealmNames());

    // if the user belongs to this realm, we are most likely using this realm stand alone, or for testing
    if (!realmNames.contains(this.getName())) {
      // make sure the realm is enabled
      Collection<Realm> configureadRealms = realmSecurityManager.getRealms();
      boolean foundRealm = false;
      for (Realm realm : configureadRealms) {
        if (realmNames.contains(realm.getName())) {
          foundRealm = true;
          break;
        }
      }
      if (!foundRealm) {
        // user is from a realm that is NOT enabled
        throw new AuthorizationException("User for principals: " + principals.getPrimaryPrincipal()
            + " belongs to a disabled realm(s): " + principals.getRealmNames() + ".");
      }
    }

    // clean up the realm names for processing (replace the Nexus*Realm with default)
    cleanUpRealmList(realmNames);

    if (RoleMappingUserManager.class.isInstance(userManager)) {
      for (String realmName : realmNames) {
        try {
          for (RoleIdentifier roleIdentifier : ((RoleMappingUserManager) userManager).getUsersRoles(username,
              realmName)) {
            roles.add(roleIdentifier.getRoleId());
          }
        }
        catch (UserNotFoundException e) {
          logger.trace("Failed to find role mappings for user: {} realm: {}", username, realmName);
        }
      }
    }
    else if (realmNames.contains("default")) {
      try {
        for (RoleIdentifier roleIdentifier : userManager.getUser(username).getRoles()) {
          roles.add(roleIdentifier.getRoleId());
        }
      }
      catch (UserNotFoundException e) {
        throw new AuthorizationException("User for principals: " + principals.getPrimaryPrincipal()
            + " could not be found.", e);
      }

    }
    else
    // user not managed by this Realm
    {
      throw new AuthorizationException("User for principals: " + principals.getPrimaryPrincipal()
          + " not manged by Nexus realm.");
    }

    return new SimpleAuthorizationInfo(roles);
  }

  private void cleanUpRealmList(Set<String> realmNames) {
    for (UserManager userManager : this.userManagerMap.values()) {
      String authRealmName = userManager.getAuthenticationRealmName();
      if (authRealmName != null && realmNames.contains(authRealmName)) {
        realmNames.remove(authRealmName);
        realmNames.add(userManager.getSource());
      }
    }

    if (realmNames.contains(getName())) {
      realmNames.remove(getName());
      realmNames.add("default");
    }
  }
}
