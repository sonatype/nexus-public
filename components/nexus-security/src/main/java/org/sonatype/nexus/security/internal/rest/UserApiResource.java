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

package org.sonatype.nexus.security.internal.rest;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * Resource for REST API to perform operations on the user.
 *
 * @since 3.next
 */
@Named
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(UserApiResource.RESOURCE_URI)
public class UserApiResource
    extends ComponentSupport
    implements Resource, UserApiResourceDoc
{
  public static final String RESOURCE_URI = SecurityApiResource.RESOURCE_URI + "users/";

  private final SecuritySystem securitySystem;

  @Inject
  public UserApiResource(final SecuritySystem securitySystem) {
    this.securitySystem = securitySystem;
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:users:read")
  public Collection<ApiUser> getUsers(
      @QueryParam("userId") final String userId,
      @QueryParam("source") final String source)
  {
    UserSearchCriteria criteria = new UserSearchCriteria(userId, null, source);

    if (!UserManager.DEFAULT_SOURCE.equals(source)) {
      // we limit the number of users here to avoid issues with remote sources
      criteria.setLimit(100);
    }

    return securitySystem.searchUsers(criteria).stream().map(u -> fromUser(u))
        .collect(Collectors.toList());
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:users:create")
  public ApiUser createUser(@NotNull @Valid final ApiCreateUser createUser) {
    if (Strings2.isBlank(createUser.getPassword())) {
      throw createWebException(Status.BAD_REQUEST, "A non-empty password is required.");
    }
    try {
      User user = securitySystem.addUser(createUser.toUser(), createUser.getPassword());
      return fromUser(user);
    }
    catch (NoSuchUserManagerException e) {
      log.error("Unable to locate default usermanager.", e);
      throw createNoSuchUserManagerException(UserManager.DEFAULT_SOURCE);
    }
  }

  @Override
  @PUT
  @Path("{userId}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:users:update")
  public void updateUser(@PathParam("userId") final String userId, @NotNull @Valid final ApiUser apiUser) {
    if (!userId.equals(apiUser.getUserId())) {
      log.debug("The path userId '{}' does not match the userId supplied in the body '{}'.", userId,
          apiUser.getUserId());
      throw createWebException(Status.BAD_REQUEST, "The path's userId does not match the body");
    }

    try {
      validateRoles(apiUser.getRoles());

      if (UserManager.DEFAULT_SOURCE.equals(apiUser.getSource())) {
        securitySystem.updateUser(apiUser.toUser());
      }
      else {
        // Ensure user exists
        securitySystem.getUser(userId, apiUser.getSource());

        Set<RoleIdentifier> roleIdentifiers = apiUser.getRoles().stream()
            .map(roleId -> new RoleIdentifier(UserManager.DEFAULT_SOURCE, roleId)).collect(Collectors.toSet());
        securitySystem.setUsersRoles(userId, apiUser.getSource(), roleIdentifiers);
      }
    }
    catch (UserNotFoundException e) {
      log.debug("Unable to locate userId: {}", userId, e);
      throw createUnknownUserException(userId);
    }
    catch (NoSuchUserManagerException e) {
      log.debug("Unable to locate source: {}", userId, apiUser.getSource(), e);
      throw createNoSuchUserManagerException(apiUser.getSource());
    }
  }

  @Override
  @DELETE
  @Path("{userId}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:users:delete")
  public void deleteUser(@PathParam("userId") final String userId) {
    User user = null;
    try {
      user = securitySystem.getUser(userId);

      securitySystem.deleteUser(userId, user.getSource());
    }
    catch (NoSuchUserManagerException e) {
      // this should never actually happen
      String source = user.getSource() != null ? user.getSource() : "";
      log.error("Unable to locate source: {} for userId: {}", source, userId, e);
      throw createNoSuchUserManagerException(source);
    }
    catch (UserNotFoundException e) {
      log.debug("Unable to locate userId: {}", userId, e);
      throw createUnknownUserException(userId);
    }
  }

  @Override
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Path("{userId}/change-password")
  @Consumes(MediaType.TEXT_PLAIN)
  public void changePassword(@PathParam("userId") final String userId, @NotNull final String password) {
    if (StringUtils.isBlank(password)) {
      throw createWebException(Status.BAD_REQUEST, "Password must be supplied.");
    }

    try {
      securitySystem.changePassword(userId, password);
    }
    catch (UserNotFoundException e) { // NOSONAR
      log.debug("Request to change password for invalid user '{}'.", userId);
      throw createUnknownUserException(userId);
    }
  }

  private boolean isReadOnly(final User user) {
    try {
      return !securitySystem.getUserManager(user.getSource()).supportsWrite();
    }
    catch (NoSuchUserManagerException e) {
      log.debug("Unable to locate user manager: {}", user.getSource(), e);
      return true;
    }
  }

  @VisibleForTesting
  ApiUser fromUser(final User user) {
    Predicate<RoleIdentifier> isLocal = r -> UserManager.DEFAULT_SOURCE.equals(r.getSource());

    Set<String> internalRoles =
        user.getRoles().stream().filter(isLocal).map(role -> role.getRoleId()).collect(Collectors.toSet());
    Set<String> externalRoles =
        user.getRoles().stream().filter(isLocal.negate()).map(role -> role.getRoleId()).collect(Collectors.toSet());

    return new ApiUser(user.getUserId(), user.getFirstName(), user.getLastName(), user.getEmailAddress(),
        user.getSource(), ApiUserStatus.convert(user.getStatus()), isReadOnly(user), internalRoles, externalRoles);
  }

  private void validateRoles(final Set<String> roleIds) {
    ValidationErrorsException errors = new ValidationErrorsException();

    Set<String> localRoles;
    try {
      localRoles = securitySystem.listRoles(UserManager.DEFAULT_SOURCE).stream().map(r -> r.getRoleId())
          .collect(Collectors.toSet());
      for (String roleId : roleIds) {
        if (!localRoles.contains(roleId)) {
          errors.withError("roles", "Unable to locate roleId: " + roleId);
        }
      }
      if (errors.hasValidationErrors()) {
        throw errors;
      }
    }
    catch (NoSuchAuthorizationManagerException e) {
      log.error("Unable to locate default user manager", e);
      throw createWebException(Status.INTERNAL_SERVER_ERROR, "Unable to locate default user manager");
    }
  }

  private WebApplicationMessageException createNoSuchUserManagerException(final String source) {
    return createWebException(Status.NOT_FOUND, "Unable to locate source: " + source);
  }
  private WebApplicationMessageException createUnknownUserException(final String userId) {
    return createWebException(Status.NOT_FOUND, "User '" + userId + "' not found.");
  }

  private WebApplicationMessageException createWebException(final Status status, final String message) {
    return new WebApplicationMessageException(status, message, MediaType.APPLICATION_JSON);
  }
}
