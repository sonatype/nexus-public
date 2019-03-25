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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.authc.UserPasswordChanged;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.InvalidCredentialsException;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.RoleMappingUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.LifecycleUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SECURITY;

/**
 * This implementation wraps a Shiro SecurityManager, and adds user management.
 */
@Named("default")
@ManagedLifecycle(phase = SECURITY)
@Singleton
public class DefaultSecuritySystem
    extends StateGuardLifecycleSupport
    implements SecuritySystem
{
  private static final String ALL_ROLES_KEY = "all";

  private final EventManager eventManager;

  private final RealmSecurityManager realmSecurityManager;

  private final RealmManager realmManager;

  private final AnonymousManager anonymousManager;

  private final Map<String, AuthorizationManager> authorizationManagers;

  private final Map<String, UserManager> userManagers;

  @Inject
  public DefaultSecuritySystem(final EventManager eventManager,
                               final RealmSecurityManager realmSecurityManager,
                               final RealmManager realmManager,
                               final AnonymousManager anonymousManager,
                               final Map<String, AuthorizationManager> authorizationManagers,
                               final Map<String, UserManager> userManagers)
  {
    this.eventManager = checkNotNull(eventManager);
    this.realmSecurityManager = checkNotNull(realmSecurityManager);
    this.realmManager = checkNotNull(realmManager);
    this.anonymousManager = checkNotNull(anonymousManager);
    this.authorizationManagers = checkNotNull(authorizationManagers);
    this.userManagers = checkNotNull(userManagers);
  }

  // TODO: Sort out better lifecycle management for dependent components

  @Override
  protected void doStart() throws Exception {
    if (Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE) {
      log.info("Unlimited strength JCE policy detected");
    }

    SecurityUtils.setSecurityManager(realmSecurityManager);

    realmManager.start();
  }

  @Override
  protected void doStop() throws Exception {
    realmManager.stop();

    LifecycleUtils.destroy(realmSecurityManager);
  }

  @Override
  public Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  @Override
  public boolean isPermitted(PrincipalCollection principal, String permission) {
    return realmSecurityManager.isPermitted(principal, permission);
  }

  @Override
  public boolean[] isPermitted(PrincipalCollection principal, List<String> permissions) {
    return realmSecurityManager.isPermitted(principal, permissions.toArray(new String[permissions.size()]));
  }

  @Override
  public void checkPermission(PrincipalCollection principal, String permission) {
    realmSecurityManager.checkPermission(principal, permission);
  }

  @Override
  public Set<Role> listRoles() {
    Set<Role> result = new HashSet<>();
    for (AuthorizationManager authzManager : authorizationManagers.values()) {
      Set<Role> roles = authzManager.listRoles();
      if (roles != null) {
        result.addAll(roles);
      }
    }

    return result;
  }

  @Override
  public Set<Role> listRoles(String sourceId) throws NoSuchAuthorizationManagerException {
    if (ALL_ROLES_KEY.equalsIgnoreCase(sourceId)) {
      return listRoles();
    }
    else {
      AuthorizationManager authzManager = getAuthorizationManager(sourceId);
      return authzManager.listRoles();
    }
  }

  @Override
  public Set<Privilege> listPrivileges() {
    Set<Privilege> result = new HashSet<>();
    for (AuthorizationManager authzManager : authorizationManagers.values()) {
      Set<Privilege> privileges = authzManager.listPrivileges();
      if (privileges != null) {
        result.addAll(privileges);
      }
    }

    return result;
  }

  // *********************
  // * user management
  // *********************

  @Override
  public User addUser(User user, String password) throws NoSuchUserManagerException {
    // first save the user
    // this is the UserManager that owns the user
    UserManager userManager = getUserManager(user.getSource());

    if (!userManager.supportsWrite()) {
      throw new RuntimeException("UserManager: " + userManager.getSource() + " does not support writing.");
    }

    userManager.addUser(user, password);

    // then save the users Roles
    for (UserManager tmpUserManager : getUserManagers()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only save roles
      if (!tmpUserManager.getSource().equals(user.getSource()) &&
          RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
          roleMappingUserManager.setUsersRoles(
              user.getUserId(),
              user.getSource(),
              RoleIdentifier.getRoleIdentifiersForSource(user.getSource(), user.getRoles())
          );
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the user-manager: {}", user.getUserId(), tmpUserManager.getSource());
        }
      }
    }

    return user;
  }

  @Override
  public User updateUser(User user) throws UserNotFoundException, NoSuchUserManagerException {
    // first update the user
    // this is the UserManager that owns the user
    UserManager userManager = getUserManager(user.getSource());

    if (!userManager.supportsWrite()) {
      throw new RuntimeException("UserManager: " + userManager.getSource() + " does not support writing.");
    }

    final User oldUser = userManager.getUser(user.getUserId());
    userManager.updateUser(user);
    if (oldUser.getStatus() == UserStatus.active && user.getStatus() != oldUser.getStatus()) {
      // clear the realm authc caches as user got disabled
      eventManager.post(new UserPrincipalsExpired(user.getUserId(), user.getSource()));
    }

    // then save the users Roles
    for (UserManager tmpUserManager : getUserManagers()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only save roles
      if (!tmpUserManager.getSource().equals(user.getSource())
          && RoleMappingUserManager.class.isInstance(tmpUserManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) tmpUserManager;
          roleMappingUserManager.setUsersRoles(
              user.getUserId(),
              user.getSource(),
              RoleIdentifier.getRoleIdentifiersForSource(user.getSource(), user.getRoles())
          );
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the user-manager: {}", user.getUserId(), tmpUserManager.getSource());
        }
      }
    }

    // clear the realm authz caches as user might get roles changed
    eventManager.post(new AuthorizationConfigurationChanged());

    return user;
  }

  @Override
  public void deleteUser(String userId) throws UserNotFoundException {
    User user = getUser(userId);
    try {
      deleteUser(userId, user.getSource());
    }
    catch (NoSuchUserManagerException e) {
      log.error("User manager returned user, but could not be found: {}", e.getMessage(), e);
      throw new IllegalStateException("User manager returned user, but could not be found: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteUser(String userId, String source) throws UserNotFoundException, NoSuchUserManagerException {
    checkNotNull(userId, "User ID may not be null");

    Subject subject = getSubject();
    if (subject.getPrincipal() != null && userId.equals(subject.getPrincipal().toString())) {
      throw new IllegalArgumentException("Can not delete currently signed in user");
    }

    AnonymousConfiguration anonymousConfiguration = anonymousManager.getConfiguration();
    if (anonymousConfiguration.isEnabled() && userId.equals(anonymousConfiguration.getUserId())) {
      throw new IllegalArgumentException("Can not delete anonymous user");
    }

    UserManager userManager = getUserManager(source);
    userManager.deleteUser(userId);

    // flush authc
    eventManager.post(new UserPrincipalsExpired(userId, source));
  }

  @Override
  public void setUsersRoles(String userId, String source, Set<RoleIdentifier> roleIdentifiers)
      throws UserNotFoundException
  {
    // TODO: this is a bit sticky, what we really want to do is just expose the RoleMappingUserManagers this way (i
    // think), maybe this is too generic

    boolean foundUser = false;

    for (UserManager userManager : getUserManagers()) {
      if (RoleMappingUserManager.class.isInstance(userManager)) {
        RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) userManager;
        try {
          foundUser = true;
          roleMappingUserManager.setUsersRoles(
              userId,
              source,
              RoleIdentifier.getRoleIdentifiersForSource(userManager.getSource(), roleIdentifiers)
          );
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the user-manager: {}", userId, userManager.getSource());
        }
      }
    }

    if (!foundUser) {
      throw new UserNotFoundException(userId);
    }
    // clear the authz realm caches
    eventManager.post(new AuthorizationConfigurationChanged());
  }

  private User findUser(String userId, UserManager userManager) throws UserNotFoundException {
    log.trace("Finding user: {} in user-manager: {}", userId, userManager);

    User user = userManager.getUser(userId);
    if (user == null) {
      throw new UserNotFoundException(userId);
    }
    log.trace("Found user: {}", user);

    // add roles from other user managers
    addOtherRolesToUser(user);

    return user;
  }

  @Override
  @Nullable
  public User currentUser() throws UserNotFoundException {
    Subject subject = getSubject();
    if (subject.getPrincipal() == null) {
      return null;
    }

    return getUser(subject.getPrincipal().toString());
  }

  @Override
  public User getUser(String userId) throws UserNotFoundException {
    log.trace("Finding user: {}", userId);

    for (UserManager userManager : orderUserManagers()) {
      try {
        return findUser(userId, userManager);
      }
      catch (UserNotFoundException e) {
        log.trace("User: '{}' was not found in: '{}'", userId, userManager, e);
      }
    }

    log.trace("User not found: {}", userId);
    throw new UserNotFoundException(userId);
  }

  @Override
  public User getUser(String userId, String source) throws UserNotFoundException, NoSuchUserManagerException {
    log.trace("Finding user: {} in source: {}", userId, source);

    UserManager userManager = getUserManager(source);
    return findUser(userId, userManager);
  }

  @Override
  public Set<User> listUsers() {
    Set<User> result = new HashSet<>();

    for (UserManager userManager : getUserManagers()) {
      result.addAll(userManager.listUsers());
    }

    // now add all the roles to the users
    for (User user : result) {
      // add roles from other user managers
      addOtherRolesToUser(user);
    }

    return result;
  }

  @Override
  public Set<User> searchUsers(UserSearchCriteria criteria) {
    Set<User> result = new HashSet<>();

    // if the source is not set search all realms.
    if (Strings2.isBlank(criteria.getSource())) {
      // search all user managers
      for (UserManager userManager : getUserManagers()) {
        Set<User> users = userManager.searchUsers(criteria);
        if (users != null) {
          result.addAll(users);
        }
      }
    }
    else {
      try {
        result.addAll(getUserManager(criteria.getSource()).searchUsers(criteria));
      }
      catch (NoSuchUserManagerException e) {
        log.warn("UserManager: {} was not found.", criteria.getSource(), e);
      }
    }

    // now add all the roles to the users
    for (User user : result) {
      // add roles from other user managers
      addOtherRolesToUser(user);
    }

    return result;
  }

  /**
   * We need to order the UserManagers the same way as the Realms are ordered. We need to be able to find a user
   * based on the ID.
   *
   * This my never go away, but the current reason why we need it is:
   * https://issues.apache.org/jira/browse/KI-77 There is no (clean) way to resolve a realms roles into permissions.
   * take a look at the issue and VOTE!
   *
   * @return the list of UserManagers in the order (as close as possible) to the list of realms.
   */
  private List<UserManager> orderUserManagers() {
    List<UserManager> orderedLocators = new ArrayList<>();

    List<UserManager> unOrderdLocators = new ArrayList<>(getUserManagers());

    Map<String, UserManager> realmToUserManagerMap = new HashMap<>();

    for (UserManager userManager : getUserManagers()) {
      if (userManager.getAuthenticationRealmName() != null) {
        realmToUserManagerMap.put(userManager.getAuthenticationRealmName(), userManager);
      }
    }

    // get the sorted order of realms from the realm locator
    Collection<Realm> realms = realmSecurityManager.getRealms();

    for (Realm realm : realms) {
      // now user the realm.name to find the UserManager
      if (realmToUserManagerMap.containsKey(realm.getName())) {
        UserManager userManager = realmToUserManagerMap.get(realm.getName());
        // remove from unorderd and add to orderd
        unOrderdLocators.remove(userManager);
        orderedLocators.add(userManager);
      }
    }

    // now add all the un-ordered ones to the ordered ones, this way they will be at the end of the ordered list
    orderedLocators.addAll(unOrderdLocators);

    return orderedLocators;
  }

  private void addOtherRolesToUser(final User user) {
    // then save the users Roles
    for (UserManager userManager : getUserManagers()) {
      // skip the user manager that owns the user, we already did that
      // these user managers will only have roles
      if (!userManager.getSource().equals(user.getSource()) && RoleMappingUserManager.class.isInstance(userManager)) {
        try {
          RoleMappingUserManager roleMappingUserManager = (RoleMappingUserManager) userManager;
          Set<RoleIdentifier> roleIdentifiers = roleMappingUserManager
              .getUsersRoles(user.getUserId(), user.getSource());
          if (roleIdentifiers != null) {
            user.addAllRoles(roleIdentifiers);
          }
        }
        catch (UserNotFoundException e) {
          log.debug("User '{}' is not managed by the user-manager: {}", user.getUserId(), userManager.getSource());
        }
      }
    }
  }

  @Override
  public AuthorizationManager getAuthorizationManager(String source) throws NoSuchAuthorizationManagerException {
    if (!authorizationManagers.containsKey(source)) {
      throw new NoSuchAuthorizationManagerException(source);
    }

    return authorizationManagers.get(source);
  }

  @Override
  public void changePassword(String userId, String oldPassword, String newPassword)
      throws UserNotFoundException, InvalidCredentialsException
  {
    // first authenticate the user
    try {
      UsernamePasswordToken authenticationToken = new UsernamePasswordToken(userId, oldPassword);
      if (realmSecurityManager.authenticate(authenticationToken) == null) {
        throw new InvalidCredentialsException();
      }
    }
    catch (AuthenticationException e) {
      log.debug("User failed to change password reason: " + e.getMessage(), e);
      throw new InvalidCredentialsException();
    }

    // if that was good just change the password
    changePassword(userId, newPassword);
  }

  @Override
  public void changePassword(String userId, String newPassword) throws UserNotFoundException {
    User user = getUser(userId);

    try {
      UserManager userManager = getUserManager(user.getSource());
      userManager.changePassword(userId, newPassword);
    }
    catch (NoSuchUserManagerException e) {
      // this should NEVER happen
      log.warn("User '{}' with source: '{}' but could not find the user-manager for that source.",
          userId, user.getSource());
    }

    // Post event containing the userId for which the password has been changed
    eventManager.post(new UserPasswordChanged(userId));
  }

  private Collection<UserManager> getUserManagers() {
    return userManagers.values();
  }

  @Override
  public UserManager getUserManager(final String source) throws NoSuchUserManagerException {
    if (!userManagers.containsKey(source)) {
      throw new NoSuchUserManagerException(source);
    }
    return userManagers.get(source);
  }
}
