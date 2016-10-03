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
package org.sonatype.nexus.coreui

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.sonatype.nexus.common.wonderland.AuthTicketService
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.rapture.PasswordPlaceholder
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.anonymous.AnonymousManager
import org.sonatype.nexus.security.role.RoleIdentifier
import org.sonatype.nexus.security.user.User
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.user.UserSearchCriteria
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.google.inject.Key
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import groovy.transform.PackageScope
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.apache.shiro.authz.annotation.RequiresUser
import org.apache.shiro.subject.Subject
import org.eclipse.sisu.inject.BeanLocator
import org.hibernate.validator.constraints.NotEmpty

import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE

/**
 * User {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_User')
class UserComponent
    extends DirectComponentSupport
{
  @Inject
  SecuritySystem securitySystem

  @Inject
  AnonymousManager anonymousManager

  @Inject
  AuthTicketService authTickets

  @Inject
  BeanLocator beanLocator

  /**
   * Retrieve users.
   * @return a list of users
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:users:read')
  List<UserXO> read(@Nullable final StoreLoadParameters parameters) {
    def source = parameters?.getFilter('source')
    if (!source) {
      source = DEFAULT_SOURCE
    }
    def userId = parameters?.getFilter('userId')
    securitySystem.searchUsers(new UserSearchCriteria(source: source, userId: userId)).collect { user ->
      convert(user)
    }
  }

  /**
   * Retrieves available user sources.
   * @return a list of available user sources (user managers)
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:users:read')
  List<ReferenceXO> readSources() {
    beanLocator.locate(Key.get(UserManager.class, Named.class)).collect { entry ->
      new ReferenceXO(
          id: entry.key.value,
          name: entry.description ?: entry.key.value
      )
    }
  }

  /**
   * Retrieves user account (logged in user info).
   * @return current logged in user account.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresUser
  UserAccountXO readAccount() {
    User user = securitySystem.currentUser()
    return new UserAccountXO(
        userId: user.userId,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.emailAddress,
        external: user.source != DEFAULT_SOURCE
    )
  }

  /**
   * Creates a user.
   * @param userXO to be created
   * @return created user
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:users:create')
  @Validate(groups = [Create.class, Default.class])
  UserXO create(@NotNull @Valid final UserXO userXO) {
    def user = new User(
        userId: userXO.userId,
        source: DEFAULT_SOURCE,
        firstName: userXO.firstName,
        lastName: userXO.lastName,
        emailAddress: userXO.email,
        status: userXO.status,
        roles: userXO.roles?.collect {id ->
          new RoleIdentifier(DEFAULT_SOURCE, id)
        }
    )
    convert(securitySystem.addUser(user, userXO.password))
  }

  /**
   * Update a user.
   * @param userXO to be updated
   * @return updated user
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:users:update')
  @Validate(groups = [Update.class, Default.class])
  UserXO update(@NotNull @Valid final UserXO userXO) {
    convert(securitySystem.updateUser(new User(
        userId: userXO.userId,
        version: userXO.version,
        source: DEFAULT_SOURCE,
        firstName: userXO.firstName,
        lastName: userXO.lastName,
        emailAddress: userXO.email,
        status: userXO.status,
        roles: userXO.roles?.collect {id ->
          new RoleIdentifier(DEFAULT_SOURCE, id)
        }
    )))
  }

  /**
   * Update user role mappings.
   * @param userRoleMappingsXO to be updated
   * @return updated user
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:users:update')
  @Validate(groups = [Update.class, Default.class])
  UserXO updateRoleMappings(@NotNull @Valid final UserRoleMappingsXO userRoleMappingsXO) {
    def mappedRoles = userRoleMappingsXO.roles
    if (mappedRoles?.size()) {
      User user = securitySystem.getUser(userRoleMappingsXO.userId, userRoleMappingsXO.realm)
      user.roles.each {role ->
        if (role.source == userRoleMappingsXO.realm) {
          mappedRoles.remove(role.roleId)
        }
      }
    }
    securitySystem.setUsersRoles(
        userRoleMappingsXO.userId,
        userRoleMappingsXO.realm,
        mappedRoles?.size() > 0
            ? mappedRoles?.collect {roleId -> new RoleIdentifier(DEFAULT_SOURCE, roleId)} as Set
            : null
    )
    return convert(securitySystem.getUser(userRoleMappingsXO.userId, userRoleMappingsXO.realm))
  }

  /**
   * Update user account (logged in user info).
   * @param userAccountXO to be updated
   * @return current logged in user account
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresUser
  @RequiresAuthentication
  @Validate
  UserAccountXO updateAccount(@NotNull @Valid final UserAccountXO userAccountXO) {
    User user = securitySystem.currentUser().with {
      firstName = userAccountXO.firstName
      lastName = userAccountXO.lastName
      emailAddress = userAccountXO.email
      return it
    }
    securitySystem.updateUser(user)
    return readAccount()
  }

  /**
   * Change password of a specified user.
   * @param authToken authentication token
   * @param userId id of user to change password for
   * @param password new password
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresUser
  @RequiresAuthentication
  @RequiresPermissions('nexus:userschangepw:create')
  @Validate
  void changePassword(@NotEmpty final String authToken,
                      @NotEmpty final String userId,
                      @NotEmpty final String password)
  {
    if (authTickets.redeemTicket(authToken)) {
      if (isAnonymousUser(userId)) {
        throw new Exception("Password cannot be changed for user ${userId}, since is marked as the Anonymous user")
      }
      securitySystem.changePassword(userId, password)
    }
    else {
      throw new IllegalAccessException('Invalid authentication ticket')
    }
  }

  /**
   * Deletes a user.
   * @param id of user to be deleted
   * @param source of user to be deleted
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:users:delete')
  @Validate
  void remove(@NotEmpty final String id, @NotEmpty final String source) {
    // TODO check if source is required or we always delete from default realm
    if (isAnonymousUser(id)) {
      throw new Exception("User ${id} cannot be deleted, since is marked as the Anonymous user")
    }
    if (isCurrentUser(id)) {
      throw new Exception("User ${id} cannot be deleted, since is the user currently logged into the application")
    }
    securitySystem.deleteUser(id, source)
  }

  /**
   * Convert user to XO.
   */
  @PackageScope
  UserXO convert(final User user) {
    UserXO userXO = new UserXO(
        userId: user.userId,
        version: user.version,
        realm: user.source,
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.emailAddress,
        status: user.status,
        password: PasswordPlaceholder.get(),
        roles: user.roles.collect { role ->
          role.roleId
        },
        external: user.source != DEFAULT_SOURCE
    )
    if (userXO.external) {
      userXO.externalRoles = user.roles
          .findResults { RoleIdentifier role -> return role.source == DEFAULT_SOURCE ? null : role }
          .collect { role -> role.roleId }
    }
    return userXO
  }

  private boolean isAnonymousUser(final String userId) {
    def config = anonymousManager.configuration
    return config.enabled && config.userId == userId
  }

  private boolean isCurrentUser(final String userId) {
    Subject subject = securitySystem.subject
    if (!subject || !subject.principal) {
      return false
    }
    return subject.principal == userId
  }
}
