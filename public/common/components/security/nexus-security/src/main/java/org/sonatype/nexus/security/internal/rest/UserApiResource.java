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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.internal.RealmToSource;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.user.UserSearchCriteria.FALL_BACK_USER_LIMIT_TO_AVOID_OOM;

/**
 * Resource for REST API to perform operations on the user.
 *
 * @since 3.17
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserApiResource
    implements Resource, UserApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String SAML_SOURCE = "SAML";

  private static final String OAUTH2_SOURCE = "OAuth2";

  private static final Set<String> ALLOWED_REALMS_FOR_DELETION = Set.of(
      UserManager.DEFAULT_SOURCE, SAML_SOURCE, OAUTH2_SOURCE);

  private final SecuritySystem securitySystem;

  @Autowired
  public UserApiResource(
      final SecuritySystem securitySystem)
  {
    this.securitySystem = checkNotNull(securitySystem);
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

    if (UserManager.SAML_SOURCE.equals(source)) {
      // This limit was chosen to prevent OOM while not inconveniencing users
      // https://sonatype.atlassian.net/browse/NEXUS-44226?focusedCommentId=826355
      // Ideally we would implement pagination around this endpoint
      criteria.setLimit(FALL_BACK_USER_LIMIT_TO_AVOID_OOM);
    }
    else if (!UserManager.DEFAULT_SOURCE.equals(source)) {
      // We limit the number of users here to avoid issues with remote sources.
      // Note: The way we currently implement LDAP the limit will be 100 * number of configured LDAP servers
      // https://github.com/sonatype/nexus-internal/blob/3520164540301bc0a9d09c8d880e6287abfebf55/private/plugins/nexus-ldap-plugin/src/main/java/org/sonatype/nexus/ldap/internal/realms/EnterpriseLdapManager.java#L332
      criteria.setLimit(100);
    }

    return securitySystem.searchUsers(criteria)
        .stream()
        .map(this::fromUser)
        .collect(Collectors.toList());
  }

  @Override
  @DELETE
  @Path("{userId}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:users:delete")
  public void deleteUser(
      @PathParam("userId") final String userId,
      @QueryParam("realm") final String realm)
  {
    User user = null;
    try {
      if (realm == null) {
        user = securitySystem.getUser(userId);
        if (!ALLOWED_REALMS_FOR_DELETION.contains(user.getSource())) {
          throw createWebException(Status.BAD_REQUEST, "Non-local user cannot be deleted.");
        }
      }
      else {
        if (!securitySystem.isValidRealm(realm)) {
          throw createWebException(Status.BAD_REQUEST, "Invalid or empty realm name.");
        }
        else {
          user = securitySystem.getUser(userId, RealmToSource.getSource(realm));
        }
      }

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
        user.getRoles().stream().filter(isLocal).map(RoleIdentifier::getRoleId).collect(Collectors.toSet());
    Set<String> externalRoles =
        user.getRoles().stream().filter(isLocal.negate()).map(RoleIdentifier::getRoleId).collect(Collectors.toSet());

    return new ApiUser(user.getUserId(), user.getFirstName(), user.getLastName(), user.getEmailAddress(),
        user.getSource(), ApiUserStatus.convert(user.getStatus()), isReadOnly(user), internalRoles, externalRoles);
  }

  private void validateRoles(final Set<String> roleIds) {
    ValidationErrorsException errors = new ValidationErrorsException();

    Set<String> localRoles;
    try {
      localRoles = securitySystem.listRoles(UserManager.DEFAULT_SOURCE)
          .stream()
          .map(Role::getRoleId)
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
    return new WebApplicationMessageException(status, "\"" + message + "\"", MediaType.APPLICATION_JSON);
  }
}
