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
package org.sonatype.nexus.script.plugin.internal.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.script.plugin.internal.security.ScriptPrivilegeDescriptor;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.rest.PrivilegeApiResourceSupport;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.19
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ScriptPrivilegeApiResource
    extends PrivilegeApiResourceSupport
    implements Resource, ScriptPrivilegeApiResourceDoc
{
  @Autowired
  public ScriptPrivilegeApiResource(
      final SecuritySystem securitySystem,
      final SecurityHelper securityHelper,
      final List<PrivilegeDescriptor> privilegeDescriptorsList)
  {
    super(securitySystem, securityHelper, privilegeDescriptorsList);
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:create")
  @Path("script")
  public Response createPrivilege(final ApiPrivilegeScriptRequest privilege) {
    return doCreate(ScriptPrivilegeDescriptor.TYPE, privilege);
  }

  @Override
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:privileges:update")
  @Path("script/{privilegeName}")
  public void updatePrivilege(
      @PathParam("privilegeName") final String privilegeName,
      final ApiPrivilegeScriptRequest privilege)
  {
    doUpdate(privilegeName, ScriptPrivilegeDescriptor.TYPE, privilege);
  }
}
