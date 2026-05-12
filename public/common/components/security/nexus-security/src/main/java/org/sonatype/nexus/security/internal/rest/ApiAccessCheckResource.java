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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.authz.WildcardPermission2;
import org.sonatype.nexus.security.internal.rest.ApiAccessResultXo.EntityRefXo;
import org.sonatype.nexus.security.internal.rest.ApiAccessResultXo.PermissionChainXo;
import org.sonatype.nexus.security.internal.rest.ApiAccessResultXo.RelatedEndpointXo;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;

/**
 * REST resource for checking API access for users and roles.
 */
@Component
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true", matchIfMissing = true)
@Path(ApiAccessCheckResource.RESOURCE_PATH)
@Api(value = "Security management: API access")
public class ApiAccessCheckResource
    implements Resource, ApiAccessCheckResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String RESOURCE_PATH = "internal/ui/security/access-check";

  private static final String GENERIC_NOT_FOUND_MESSAGE = "\"User or role not found or access denied\"";

  private final SecuritySystem securitySystem;

  private final SecurityHelper securityHelper;

  private final EndpointPermissionRegistry endpointPermissionRegistry;

  private final RolePermissionResolver rolePermissionResolver;

  @Inject
  public ApiAccessCheckResource(
      final SecuritySystem securitySystem,
      final SecurityHelper securityHelper,
      final EndpointPermissionRegistry endpointPermissionRegistry,
      final RolePermissionResolver rolePermissionResolver)
  {
    this.securitySystem = checkNotNull(securitySystem);
    this.securityHelper = checkNotNull(securityHelper);
    this.endpointPermissionRegistry = checkNotNull(endpointPermissionRegistry);
    this.rolePermissionResolver = checkNotNull(rolePermissionResolver);
  }

  /**
   * Check if current user is an admin (has nexus:* permission).
   */
  private boolean isAdmin() {
    return securityHelper.isAllPermitted();
  }

  /**
   * Get the current user's ID, or null if not authenticated.
   */
  private String getCurrentUserId() {
    try {
      User currentUser = securitySystem.currentUser();
      return currentUser != null ? currentUser.getUserId() : null;
    }
    catch (UserNotFoundException e) {
      return null;
    }
  }

  /**
   * Check if the request requires admin privileges.
   * Admin is required when:
   * - Checking another user's permissions (userId specified and different from current user)
   * - Checking any role's permissions (roleId specified)
   */
  private void enforceAuthorizationPolicy(final ApiAccessCheckXo request) {
    String currentUserId = getCurrentUserId();

    // Checking a role requires admin
    if (request.getRoleId() != null) {
      if (!isAdmin()) {
        throw new WebApplicationMessageException(
            Status.FORBIDDEN,
            "\"Administrator privileges required to check role permissions\"",
            APPLICATION_JSON);
      }
      return;
    }

    // Checking another user requires admin
    if (request.getUserId() != null && !request.getUserId().equals(currentUserId)) {
      if (!isAdmin()) {
        throw new WebApplicationMessageException(
            Status.FORBIDDEN,
            "\"Administrator privileges required to check other users' permissions\"",
            APPLICATION_JSON);
      }
    }
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  @ApiOperation("Check if a user or role has access to an API endpoint")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Access check result", response = ApiAccessResultXo.class),
      @ApiResponse(code = 400, message = "Invalid request - userId and roleId are mutually exclusive"),
      @ApiResponse(code = 403, message = "Insufficient permissions"),
      @ApiResponse(code = 404, message = "User or role not found")
  })
  public ApiAccessResultXo checkAccess(
      @ApiParam(value = "Access check request", required = true) @NotNull @Valid final ApiAccessCheckXo request)
  {
    // Guard: if the registry has not yet been populated (e.g. onContextRefreshed failed or
    // has not fired yet), every getPermissionForEndpoint call returns null — which would
    // silently make every endpoint appear public. Fail fast with 503 instead.
    if (!endpointPermissionRegistry.isReady()) {
      throw new WebApplicationMessageException(
          Status.SERVICE_UNAVAILABLE,
          "\"Permission registry is not yet available — try again after startup completes\"",
          APPLICATION_JSON);
    }

    // Validate that userId and roleId are mutually exclusive
    if (request.getUserId() != null && request.getRoleId() != null) {
      throw new WebApplicationMessageException(
          Status.BAD_REQUEST,
          "\"userId and roleId are mutually exclusive - specify only one\"",
          APPLICATION_JSON);
    }

    // CRITICAL #1: Enforce authorization policy - admin required to check others
    enforceAuthorizationPolicy(request);

    ApiEndpointPermission endpointInfo = endpointPermissionRegistry.getEndpointInfo(
        request.getEndpoint(), request.getMethod());

    // Primary permission string (first in list) — used for display, chains, and related endpoints
    String requiredPermission = endpointInfo != null && endpointInfo.getPermissions() != null
        && !endpointInfo.getPermissions().isEmpty()
            ? endpointInfo.getPermissions().get(0).getPermission()
            : null;

    boolean hasAccess;
    List<PermissionChainXo> chains;

    if (request.getRoleId() != null) {
      // Check access for a specific role (admin only - enforced above)
      hasAccess = checkRoleAccessAll(request.getRoleId(), endpointInfo);
      chains = buildRoleChains(request.getRoleId(), requiredPermission);
    }
    else {
      // Check access for a user (current user if not specified)
      String userId = request.getUserId();
      if (userId == null) {
        // Use current user
        userId = getCurrentUserId();
        if (userId == null) {
          throw new WebApplicationMessageException(
              Status.BAD_REQUEST,
              "\"No user specified and no current user session\"",
              APPLICATION_JSON);
        }
      }

      hasAccess = checkUserAccessAll(userId, endpointInfo);
      chains = buildUserChains(userId, requiredPermission);
    }

    // Get related endpoints
    List<RelatedEndpointXo> relatedEndpoints = getRelatedEndpoints(requiredPermission);

    // CRITICAL #2: Filter sensitive data for non-admins
    // Only admins can see the full permission chain and related endpoints
    if (!isAdmin()) {
      chains = null;
      relatedEndpoints = null;
    }

    return new ApiAccessResultXo(hasAccess, requiredPermission, chains, relatedEndpoints);
  }

  private boolean checkUserAccessAll(final String userId, final ApiEndpointPermission endpointInfo) {
    if (endpointInfo == null || endpointInfo.getPermissions() == null || endpointInfo.getPermissions().isEmpty()) {
      // No permission required (public endpoint)
      return true;
    }

    try {
      securitySystem.getUser(userId);
      SimplePrincipalCollection principals = new SimplePrincipalCollection(userId, "NexusAuthorizingRealm");
      List<ApiPermissionRequirement> perms = endpointInfo.getPermissions();
      String logical = perms.get(0).getLogical();
      if ("OR".equals(logical)) {
        return perms.stream().anyMatch(r -> securitySystem.isPermitted(principals, r.getPermission()));
      }
      else {
        return perms.stream().allMatch(r -> securitySystem.isPermitted(principals, r.getPermission()));
      }
    }
    catch (UserNotFoundException e) {
      log.debug("User not found during access check: {}", userId);
      throw new WebApplicationMessageException(
          Status.BAD_REQUEST,
          GENERIC_NOT_FOUND_MESSAGE,
          APPLICATION_JSON);
    }
  }

  private boolean checkRoleAccessAll(final String roleId, final ApiEndpointPermission endpointInfo) {
    if (endpointInfo == null || endpointInfo.getPermissions() == null || endpointInfo.getPermissions().isEmpty()) {
      return true;
    }

    try {
      Collection<Permission> rolePermissions = rolePermissionResolver.resolvePermissionsInRole(roleId);
      List<ApiPermissionRequirement> perms = endpointInfo.getPermissions();
      String logical = perms.get(0).getLogical();
      if ("OR".equals(logical)) {
        return perms.stream().anyMatch(r -> {
          WildcardPermission2 required = new WildcardPermission2(r.getPermission());
          return rolePermissions.stream().anyMatch(p -> p.implies(required));
        });
      }
      else {
        return perms.stream().allMatch(r -> {
          WildcardPermission2 required = new WildcardPermission2(r.getPermission());
          return rolePermissions.stream().anyMatch(p -> p.implies(required));
        });
      }
    }
    catch (Exception e) {
      log.debug("Error checking role access for role {}: {}", roleId, e.getMessage());
      return false;
    }
  }

  private List<PermissionChainXo> buildUserChains(final String userId, final String requiredPermission) {
    List<PermissionChainXo> chains = new ArrayList<>();

    if (requiredPermission == null) {
      return chains;
    }

    try {
      User user = securitySystem.getUser(userId);
      EntityRefXo userRef = new EntityRefXo(userId, user.getName());

      // Check each role the user has
      for (RoleIdentifier roleId : user.getRoles()) {
        try {
          Role role = securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE).getRole(roleId.getRoleId());
          List<PermissionChainXo> roleChains = buildRoleChainsInternal(role, userRef, requiredPermission);
          chains.addAll(roleChains);
        }
        catch (NoSuchAuthorizationManagerException | NoSuchRoleException e) {
          log.trace("Could not find role {} for user {}", roleId.getRoleId(), userId);
        }
      }
    }
    catch (UserNotFoundException e) {
      log.debug("User not found: {}", userId);
    }

    return chains;
  }

  private List<PermissionChainXo> buildRoleChains(final String roleId, final String requiredPermission) {
    if (requiredPermission == null) {
      return List.of();
    }

    try {
      Role role = securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE).getRole(roleId);
      return buildRoleChainsInternal(role, null, requiredPermission);
    }
    catch (NoSuchAuthorizationManagerException | NoSuchRoleException e) {
      // MEDIUM #4: Use generic error to prevent role enumeration
      log.debug("Role not found during access check: {}", roleId);
      throw new WebApplicationMessageException(
          Status.BAD_REQUEST,
          GENERIC_NOT_FOUND_MESSAGE,
          APPLICATION_JSON);
    }
  }

  private List<PermissionChainXo> buildRoleChainsInternal(
      final Role role,
      final EntityRefXo userRef,
      final String requiredPermission)
  {
    List<PermissionChainXo> chains = new ArrayList<>();
    EntityRefXo roleRef = new EntityRefXo(role.getRoleId(), role.getName());

    WildcardPermission2 required = new WildcardPermission2(requiredPermission);

    // Check direct privileges on the role
    Set<Privilege> allPrivileges = securitySystem.listPrivileges();
    for (String privilegeId : role.getPrivileges()) {
      Privilege privilege = allPrivileges.stream()
          .filter(p -> p.getId().equals(privilegeId))
          .findFirst()
          .orElse(null);

      if (privilege != null && privilege.getPermission() != null) {
        if (privilege.getPermission().implies(required)) {
          EntityRefXo privilegeRef = new EntityRefXo(privilege.getId(), privilege.getName());
          chains.add(new PermissionChainXo(userRef, roleRef, privilegeRef, privilege.getPermission().toString()));
        }
      }
    }

    // Check nested roles recursively
    for (String nestedRoleId : role.getRoles()) {
      try {
        Role nestedRole = securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE).getRole(nestedRoleId);
        List<PermissionChainXo> nestedChains = buildRoleChainsInternal(nestedRole, userRef, requiredPermission);
        // Update chains to include the parent role
        for (PermissionChainXo chain : nestedChains) {
          if (chain.getRole() != null) {
            // Create a combined chain showing the path
            EntityRefXo combinedRole = new EntityRefXo(
                roleRef.getId() + " -> " + chain.getRole().getId(),
                roleRef.getName() + " -> " + chain.getRole().getName());
            chains.add(new PermissionChainXo(userRef, combinedRole, chain.getPrivilege(), chain.getPermission()));
          }
        }
      }
      catch (NoSuchAuthorizationManagerException | NoSuchRoleException e) {
        log.trace("Could not find nested role {}", nestedRoleId);
      }
    }

    return chains;
  }

  private List<RelatedEndpointXo> getRelatedEndpoints(final String permission) {
    if (permission == null) {
      return List.of();
    }

    return endpointPermissionRegistry.getEndpointsForPermission(permission)
        .stream()
        // Only the primary permission is surfaced in the related-endpoints list: this view is
        // a human-readable hint for the UI, not a full authorization contract. Endpoints that
        // declare multiple @RequiresPermissions entries still enforce all of them at runtime;
        // we intentionally drop the extras here to keep the display concise.
        .map(ep -> new RelatedEndpointXo(
            ep.getHttpMethod(),
            ep.getPathPattern(),
            ep.getDescription(),
            permission))
        .collect(Collectors.toList());
  }
}
