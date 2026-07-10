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
package org.sonatype.nexus.coreui.internal.roles;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.authz.AuthorizationManager;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * REST resource for role-related UI operations.
 */
@Component
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true", matchIfMissing = true)
@Path("/internal/ui/roles")
public class RolesUIResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final List<AuthorizationManager> authorizationManagers;

  @Autowired
  public RolesUIResource(final List<AuthorizationManager> authorizationManagers) {
    this.authorizationManagers = checkNotNull(authorizationManagers);
  }

  /**
   * Retrieves available role sources (excluding the default source).
   *
   * @return list of role sources
   */
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:read")
  @GET
  @Path("/sources")
  public List<RoleSourceUIResponse> listRoleSources() {
    return authorizationManagers.stream()
        .filter(manager -> !DEFAULT_SOURCE.equals(manager.getSource()))
        .map(manager -> new RoleSourceUIResponse(manager.getSource(), manager.getSource()))
        .collect(toList());
  }
}
