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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.user.ConfiguredUsersUserManager;
import org.sonatype.nexus.security.user.UserManager;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @since 3.17
 */
@Named
@Singleton
@RequiresAuthentication
@Path(SecurityApiResource.RESOURCE_URI)
@Produces(MediaType.APPLICATION_JSON)
public class SecurityApiResource
    extends ComponentSupport
    implements Resource, SecurityApiResourceDoc
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/security/";

  private final Map<String, UserManager> userManagers;

  @Inject
  public SecurityApiResource(final Map<String, UserManager> userManagers) {
    this.userManagers = userManagers;
  }

  @Override
  @GET
  @Path("user-sources")
  @RequiresPermissions("nexus:users:read")
  public List<ApiUserSource> getUserSources() {
    return userManagers.values().stream().filter(um -> !ConfiguredUsersUserManager.SOURCE.equals(um.getSource()))
        .map(um -> new ApiUserSource(um)).collect(Collectors.toList());
  }
}
