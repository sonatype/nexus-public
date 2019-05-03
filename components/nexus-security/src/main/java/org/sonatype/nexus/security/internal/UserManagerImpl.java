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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.CUserRoleMapping;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.AbstractUserManager;
import org.sonatype.nexus.security.user.NoSuchRoleMappingException;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.RoleMappingUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserCreatedEvent;
import org.sonatype.nexus.security.user.UserDeletedEvent;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserRoleMappingCreatedEvent;
import org.sonatype.nexus.security.user.UserRoleMappingDeletedEvent;
import org.sonatype.nexus.security.user.UserRoleMappingUpdatedEvent;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.security.user.UserUpdatedEvent;

import com.google.common.collect.Sets;
import org.apache.shiro.authc.credential.PasswordService;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.config.CUser.STATUS_ACTIVE;
import static org.sonatype.nexus.security.config.CUser.STATUS_CHANGE_PASSWORD;

/**
 * Default {@link UserManager}.
 */
@Named("default")
@Singleton
@Description("Local")
public class UserManagerImpl
    extends AbstractUserManager
    implements RoleMappingUserManager
{
  private final EventManager eventManager;

  private final SecurityConfigurationManager configuration;

  private final SecuritySystem securitySystem;

  private final PasswordService passwordService;

  @Inject
  public UserManagerImpl(final EventManager eventManager,
                         final SecurityConfigurationManager configuration,
                         final SecuritySystem securitySystem,
                         final PasswordService passwordService)
  {
    this.eventManager = checkNotNull(eventManager);
    this.configuration = configuration;
    this.securitySystem = securitySystem;
    this.passwordService = passwordService;
  }

  protected CUser toUser(User user) {
    if (user == null) {
      return null;
    }

    CUser secUser = new CUser();

    secUser.setId(user.getUserId());
    secUser.setVersion(user.getVersion());
    secUser.setFirstName(user.getFirstName());
    secUser.setLastName(user.getLastName());
    secUser.setEmail(user.getEmailAddress());
    secUser.setStatus(user.getStatus().name());
    // secUser.setPassword( password )// DO NOT set the users password!

    return secUser;
  }

  protected User toUser(CUser cUser) {
    if (cUser == null) {
      return null;
    }

    User user = new User();

    user.setUserId(cUser.getId());
    user.setVersion(cUser.getVersion());
    user.setFirstName(cUser.getFirstName());
    user.setLastName(cUser.getLastName());
    user.setEmailAddress(cUser.getEmail());
    user.setSource(DEFAULT_SOURCE);
    user.setStatus(UserStatus.valueOf(cUser.getStatus()));

    try {
      user.setRoles(this.getUsersRoles(cUser.getId(), DEFAULT_SOURCE));
    }
    catch (UserNotFoundException e) {
      // We should NEVER get here
      log.warn("Could not find user: '{}' of source: '{}' while looking up the users roles.", cUser.getId(),
          DEFAULT_SOURCE, e);
    }

    return user;
  }

  protected RoleIdentifier toRole(String roleId) {
    if (roleId == null) {
      return null;
    }

    try {
      CRole role = configuration.readRole(roleId);

      return new RoleIdentifier(DEFAULT_SOURCE, role.getId());
    }
    catch (NoSuchRoleException e) {
      return null;
    }
  }

  @Override
  public Set<User> listUsers() {
    Set<User> users = new HashSet<User>();

    for (CUser user : configuration.listUsers()) {
      users.add(toUser(user));
    }

    return users;
  }

  @Override
  public Set<String> listUserIds() {
    Set<String> userIds = new HashSet<String>();

    for (CUser user : configuration.listUsers()) {
      userIds.add(user.getId());
    }

    return userIds;
  }

  @Override
  public User getUser(String userId) throws UserNotFoundException {
    return toUser(configuration.readUser(userId));
  }

  @Override
  public String getSource() {
    return DEFAULT_SOURCE;
  }

  @Override
  public boolean supportsWrite() {
    return true;
  }

  @Override
  public User addUser(final User user, String password) {
    final CUser secUser = this.toUser(user);
    secUser.setPassword(this.hashPassword(password));

    configuration.createUser(secUser, getRoleIdsFromUser(user));

    eventManager.post(new UserCreatedEvent(user));

    // TODO: i am starting to feel we shouldn't return a user.
    return user;
  }

  @Override
  public void changePassword(final String userId, final String newPassword) throws UserNotFoundException {
    final CUser secUser = configuration.readUser(userId);
    if (STATUS_CHANGE_PASSWORD.equals(secUser.getStatus())) {
      secUser.setStatus(STATUS_ACTIVE);
    }
    secUser.setPassword(hashPassword(newPassword));
    configuration.updateUser(secUser);

    User user = getUser(userId);
    eventManager.post(new UserUpdatedEvent(user));
  }

  @Override
  public User updateUser(final User user) throws UserNotFoundException {
    // we need to pull the users password off off the old user object
    CUser oldSecUser = configuration.readUser(user.getUserId());
    CUser newSecUser = toUser(user);
    newSecUser.setPassword(oldSecUser.getPassword());

    configuration.updateUser(newSecUser, getRoleIdsFromUser(user));

    eventManager.post(new UserUpdatedEvent(user));

    return user;
  }

  @Override
  public void deleteUser(final String userId) throws UserNotFoundException {
    User user = getUser(userId);
    configuration.deleteUser(userId);

    eventManager.post(new UserDeletedEvent(user));
  }

  @Override
  public Set<RoleIdentifier> getUsersRoles(final String userId, final String source) throws UserNotFoundException {
    final Set<RoleIdentifier> roles = new HashSet<RoleIdentifier>();

    try {
      CUserRoleMapping roleMapping = configuration.readUserRoleMapping(userId, source);
      if (roleMapping != null) {
        for (String roleId : roleMapping.getRoles()) {
          RoleIdentifier role = toRole(roleId);
          if (role != null) {
            roles.add(role);
          }
        }
      }
    }
    catch (NoSuchRoleMappingException e) {
      log.debug("No user role mapping found for user: {}", userId);
    }

    return roles;
  }

  @Override
  public Set<User> searchUsers(final UserSearchCriteria criteria) {
    final Set<User> users = new HashSet<User>();

    users.addAll(filterListInMemeory(listUsers(), criteria));

    // we also need to search through the user role mappings.

    List<CUserRoleMapping> roleMappings = configuration.listUserRoleMappings();
    for (CUserRoleMapping roleMapping : roleMappings) {
      if (!DEFAULT_SOURCE.equals(roleMapping.getSource())) {
        if (matchesCriteria(roleMapping.getUserId(), roleMapping.getSource(), roleMapping.getRoles(),
            criteria)) {
          try {
            User user = getSecuritySystem().getUser(roleMapping.getUserId(), roleMapping.getSource());
            users.add(user);
          }
          catch (UserNotFoundException e) {
            log.debug("User: '{}' of source: '{}' could not be found.",
                roleMapping.getUserId(), roleMapping.getSource(), e);
          }
          catch (NoSuchUserManagerException e) {
            log.warn("User: '{}' of source: '{}' could not be found.",
                roleMapping.getUserId(), roleMapping.getSource(), e);
          }

        }
      }
    }

    return users;
  }

  private SecuritySystem getSecuritySystem() {
    return this.securitySystem;
  }

  private String hashPassword(String clearPassword) {
    // set the password if its not null
    if (clearPassword != null && clearPassword.trim().length() > 0) {
      return this.passwordService.encryptPassword(clearPassword);
    }

    return clearPassword;
  }

  @Override
  public void setUsersRoles(final String userId, final String userSource, final Set<RoleIdentifier> roleIdentifiers)
      throws UserNotFoundException
  {
    // delete if no roleIdentifiers
    if (roleIdentifiers == null || roleIdentifiers.isEmpty()) {
      try {
        configuration.deleteUserRoleMapping(userId, userSource);

        eventManager.post(new UserRoleMappingDeletedEvent(userId, userSource));
      }
      catch (NoSuchRoleMappingException e) { // NOSONAR
        log.debug("User role mapping for user: {} source: {} could not be deleted because it does not exist.",
            userId, userSource);
      }
    }
    else {
      // try to update first
      try {
        CUserRoleMapping roleMapping = configuration.readUserRoleMapping(userId, userSource).clone();
        roleMapping.setRoles(Sets.newHashSet());

        updateRoles(roleMapping, roleIdentifiers);

        configuration.updateUserRoleMapping(roleMapping);

        eventManager.post(new UserRoleMappingUpdatedEvent(userId, userSource, roleMapping.getRoles()));
      }
      catch (NoSuchRoleMappingException e) { // NOSONAR
        CUserRoleMapping roleMapping = new CUserRoleMapping();
        roleMapping.setUserId(userId);
        roleMapping.setSource(userSource);

        updateRoles(roleMapping, roleIdentifiers);

        // update failed try create
        log.debug("Update of user role mapping for user: {} source: {} did not exist, creating new one.",
            userId, userSource);
        configuration.createUserRoleMapping(roleMapping);

        eventManager.post(new UserRoleMappingCreatedEvent(userId, userSource, roleMapping.getRoles()));
      }
    }
  }

  private void updateRoles(CUserRoleMapping roleMapping, final Set<RoleIdentifier> roleIdentifiers) {
    for (RoleIdentifier roleIdentifier : roleIdentifiers) {
      if (getSource().equals(roleIdentifier.getSource())) {
        roleMapping.addRole(roleIdentifier.getRoleId());
      }
    }
  }

  @Override
  public String getAuthenticationRealmName() {
    return AuthenticatingRealmImpl.NAME;
  }

  private Set<String> getRoleIdsFromUser(User user) {
    Set<String> roles = new HashSet<String>();
    for (RoleIdentifier roleIdentifier : user.getRoles()) {
      // TODO: should we just grab the Default roles?
      // these users are managed by this realm so they should ONLY have roles from it anyway.
      roles.add(roleIdentifier.getRoleId());
    }
    return roles;
  }
}
