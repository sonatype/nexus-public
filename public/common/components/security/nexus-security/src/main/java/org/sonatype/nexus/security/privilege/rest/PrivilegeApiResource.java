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
package org.sonatype.nexus.security.privilege.rest;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.privilege.ApplicationPrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.ReadonlyPrivilegeException;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.19
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class PrivilegeApiResource
    extends PrivilegeApiResourceSupport
    implements Resource, PrivilegeApiResourceDoc
{
  @Autowired
  public PrivilegeApiResource(
      final SecuritySystem securitySystem,
      final SecurityHelper securityHelper,
      final List<PrivilegeDescriptor> privilegeDescriptorsList)
  {
    super(securitySystem, securityHelper, privilegeDescriptorsList);
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:read")
  public List<ApiPrivilege> getPrivileges() {
    return getSecuritySystem().listPrivileges()
        .stream()
        .map(this::toApiPrivilege)
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(ApiPrivilege::getName))
        .collect(Collectors.toList());
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:read")
  @Path("{privilegeName}")
  public ApiPrivilege getPrivilege(@PathParam("privilegeName") final String privilegeName) {
    try {
      return toApiPrivilege(getDefaultAuthorizationManager().getPrivilegeByName(privilegeName));
    }
    catch (NoSuchPrivilegeException e) {
      log.debug("Attempt to retrieve privilege '{}' failed, as it wasn't found in the system.", privilegeName, e);
      throw new WebApplicationMessageException(Status.NOT_FOUND, String.format(PRIV_NOT_FOUND, privilegeName),
          MediaType.APPLICATION_JSON);
    }
  }

  @Override
  @DELETE
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:delete")
  @Path("{privilegeName}")
  public void deletePrivilege(@PathParam("privilegeName") final String privilegeName) {
    try {
      getDefaultAuthorizationManager().deletePrivilegeByName(privilegeName);
    }
    catch (NoSuchPrivilegeException e) {
      log.debug("Attempt to delete privilege '{}' failed, as it wasn't found in the system.", privilegeName, e);
      throw new WebApplicationMessageException(Status.NOT_FOUND, String.format(PRIV_NOT_FOUND, privilegeName),
          MediaType.APPLICATION_JSON);
    }
    catch (ReadonlyPrivilegeException e) {
      log.debug("Attempt to delete privilege '{}' failed, as it is readonly.", privilegeName, e);
      throw new WebApplicationMessageException(Status.BAD_REQUEST, String.format(PRIV_INTERNAL, privilegeName),
          MediaType.APPLICATION_JSON);
    }
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:create")
  @Path("application")
  public Response createPrivilege(final ApiPrivilegeApplicationRequest privilege) {
    return doCreate(ApplicationPrivilegeDescriptor.TYPE, privilege);
  }

  @Override
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:update")
  @Path("application/{privilegeName}")
  public void updatePrivilege(
      @PathParam("privilegeName") final String privilegeName,
      final ApiPrivilegeApplicationRequest privilege)
  {
    doUpdate(privilegeName, ApplicationPrivilegeDescriptor.TYPE, privilege);
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:create")
  @Path("wildcard")
  public Response createPrivilege(final ApiPrivilegeWildcardRequest privilege) {
    return doCreate(WildcardPrivilegeDescriptor.TYPE, privilege);
  }

  @Override
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:update")
  @Path("wildcard/{privilegeName}")
  public void updatePrivilege(
      @PathParam("privilegeName") final String privilegeName,
      final ApiPrivilegeWildcardRequest privilege)
  {
    doUpdate(privilegeName, WildcardPrivilegeDescriptor.TYPE, privilege);
  }
}
