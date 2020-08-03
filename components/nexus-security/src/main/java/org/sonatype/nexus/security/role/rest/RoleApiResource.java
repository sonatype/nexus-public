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
package org.sonatype.nexus.security.role.rest;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.NoSuchRoleException;
import org.sonatype.nexus.security.role.ReadonlyRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleContainsItselfException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;

/**
 * @since 3.19
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class RoleApiResource
    extends ComponentSupport
    implements Resource, RoleApiResourceDoc
{
  public static final String SOURCE_NOT_FOUND = "\"Source '%s' not found.\"";

  public static final String ROLE_NOT_FOUND = "\"Role '%s' not found.\"";

  public static final String ROLE_INTERNAL = "\"Role '%s' is internal and cannot be modified or deleted.\"";

  public static final String ROLE_UNIQUE = "\"Role '%s' already exists, use a unique roleId.\"";

  public static final String ROLE_CONFLICT = "\"The Role id '%s' does not match the id used in the path '%s'.\"";

  public static final String CONTAINED_ROLE_NOT_FOUND = "\"Role '%s' contained in role '%s' not found.\"";

  public static final String CONTAINED_PRIV_NOT_FOUND = "\"Privilege '%s' contained in role '%s' not found.\"";

  public static final String ROLE_CONTAINS_ITSELF = "\"Role '%s' cannot contain itself either directly or indirectly through child roles.\"";

  private final SecuritySystem securitySystem;

  @Inject
  public RoleApiResource(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:read")
  public List<RoleXOResponse> getRoles(@QueryParam("source") final String source)
  {
    if (StringUtils.isEmpty(source)) {
      return securitySystem.listRoles().stream().map(RoleXOResponse::fromRole)
          .sorted(Comparator.comparing(RoleXOResponse::getId)).collect(toList());
    }
    else {
      try {
        return securitySystem.listRoles(source).stream().map(RoleXOResponse::fromRole)
            .sorted(Comparator.comparing(RoleXOResponse::getId)).collect(toList());
      }
      catch (NoSuchAuthorizationManagerException e) {
        throw buildBadSourceException(source);
      }
    }
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:create")
  public RoleXOResponse create(@NotNull @Valid final RoleXORequest roleXO) {
    try {
      Role role = getDefaultAuthorizationManager().addRole(fromXO(roleXO));

      return RoleXOResponse.fromRole(role);
    }
    catch (DuplicateRoleException e) {
      throw buildDuplicateRoleException(roleXO.getId());
    }
    catch (NoSuchRoleException e) {
      throw buildContainedRoleNotFoundException(e.getRoleId(), roleXO.getId());
    }
    catch (NoSuchPrivilegeException e) {
      throw buildContainedPrivilegeNotFoundException(e.getPrivilegeId(), roleXO.getId());
    }
  }

  @Override
  @GET
  @Path("/{id}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:read")
  public RoleXOResponse getRole(@DefaultValue(DEFAULT_SOURCE) @QueryParam("source") final String source,
                               @PathParam("id") @NotEmpty final String id)
  {
    try {
      return RoleXOResponse.fromRole(securitySystem.getAuthorizationManager(source).getRole(id));
    }
    catch (NoSuchAuthorizationManagerException e) {
      throw buildBadSourceException(source);
    }
    catch (NoSuchRoleException e) {
      throw buildRoleNotFoundException(id);
    }
  }

  @Override
  @PUT
  @Path("/{id}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:update")
  public void update(@PathParam("id") @NotEmpty final String id, @NotNull @Valid final RoleXORequest roleXO)
  {
    try {
      if (!roleXO.getId().equals(id)) {
        throw buildRoleConflictException(roleXO.getId(), id);
      }

      AuthorizationManager authorizationManager = getDefaultAuthorizationManager();
      int latestVersion = authorizationManager.getRole(id).getVersion();
      Role role = fromXO(roleXO);
      role.setRoleId(id);
      role.setVersion(latestVersion);

      authorizationManager.updateRole(role);
    }
    catch (ReadonlyRoleException e) {
      throw buildReadonlyRoleException(id);
    }
    catch (NoSuchRoleException e) {
      if (e.getRoleId().equals(id)) {
        throw buildRoleNotFoundException(e.getRoleId());
      }
      throw buildContainedRoleNotFoundException(e.getRoleId(), id);
    }
    catch (NoSuchPrivilegeException e) {
      throw buildContainedPrivilegeNotFoundException(e.getPrivilegeId(), id);
    }
    catch (RoleContainsItselfException e) {
      throw buildRoleContainsItselfException(e.getRoleId());
    }
  }

  @Override
  @DELETE
  @Path("/{id}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:delete")
  public void delete(@PathParam("id") @NotEmpty final String id)
  {
    AuthorizationManager authorizationManager = getDefaultAuthorizationManager();
    try {
      authorizationManager.deleteRole(id);
    }
    catch (NoSuchRoleException e) { //NOSONAR
      throw buildRoleNotFoundException(id);
    }
    catch (ReadonlyRoleException e) { //NOSONAR
      throw buildReadonlyRoleException(id);
    }
  }

  private WebApplicationMessageException buildBadSourceException(final String source) {
    log.debug("attempt to use invalid source {}", source);
    return new WebApplicationMessageException(Status.BAD_REQUEST, String.format(SOURCE_NOT_FOUND, source),
        APPLICATION_JSON);
  }

  private WebApplicationMessageException buildDuplicateRoleException(final String id) {
    log.debug("attempt to use duplicate role {}", id);
    return new WebApplicationMessageException(Status.BAD_REQUEST, String.format(ROLE_UNIQUE, id), APPLICATION_JSON);
  }

  private WebApplicationMessageException buildReadonlyRoleException(final String id) {
    log.debug("attempt to modify/delete readonly role {}", id);
    return new WebApplicationMessageException(Status.BAD_REQUEST, String.format(ROLE_INTERNAL, id),
        MediaType.APPLICATION_JSON);
  }

  private WebApplicationMessageException buildRoleNotFoundException(final String id) {
    log.debug("Role {} not found", id);
    return new WebApplicationMessageException(Status.NOT_FOUND, String.format(ROLE_NOT_FOUND, id), APPLICATION_JSON);
  }

  private WebApplicationMessageException buildContainedRoleNotFoundException(final String containedId,
                                                                                  final String roleId)
  {
    log.debug("Role {} in role {} not found", containedId, roleId);
    return new WebApplicationMessageException(Status.BAD_REQUEST,
        String.format(CONTAINED_ROLE_NOT_FOUND, containedId, roleId), APPLICATION_JSON);
  }

  private WebApplicationMessageException buildContainedPrivilegeNotFoundException(final String containedId,
                                                                                  final String roleId)
  {
    log.debug("Privilege {} in role {} not found", containedId, roleId);
    return new WebApplicationMessageException(Status.BAD_REQUEST,
        String.format(CONTAINED_PRIV_NOT_FOUND, containedId, roleId), APPLICATION_JSON);
  }

  private WebApplicationMessageException buildRoleConflictException(final String xoId, final String pathId) {
    log.debug("XO id {} and path id {} do not match", xoId, pathId);
    return new WebApplicationMessageException(Status.CONFLICT, String.format(ROLE_CONFLICT, xoId, pathId),
        MediaType.APPLICATION_JSON);
  }

  private WebApplicationMessageException buildRoleContainsItselfException(final String roleId) {
    log.debug("Role {} cannot contain itself either directly or indirectly.", roleId);
    return new WebApplicationMessageException(Status.BAD_REQUEST, String.format(ROLE_CONTAINS_ITSELF, roleId),
        MediaType.APPLICATION_JSON);
  }

  private Role fromXO(final RoleXORequest roleXO) {
    return new Role(roleXO.getId(), roleXO.getName(), roleXO.getDescription(), DEFAULT_SOURCE, false,
        roleXO.getRoles(), roleXO.getPrivileges());
  }

  private AuthorizationManager getDefaultAuthorizationManager() {
    try {
      return securitySystem.getAuthorizationManager(DEFAULT_SOURCE);
    }
    //this should never happen, the default source is always available
    catch (NoSuchAuthorizationManagerException e) {
      log.error("Unable to retrieve the default authorization manager", e);
      return null;
    }
  }
}
