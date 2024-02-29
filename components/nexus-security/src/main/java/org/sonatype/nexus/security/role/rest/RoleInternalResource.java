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
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.security.role.rest.RoleApiResource.SOURCE_NOT_FOUND;

@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RoleInternalResource.RESOURCE_PATH)
public class RoleInternalResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/roles";

  private final SecuritySystem securitySystem;

  @Inject
  public RoleInternalResource(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:roles:read")
  public List<RoleXOResponse> searchRoles(
      @QueryParam("source") final String source,
      @QueryParam("search") final String search)
  {
    if (StringUtils.isEmpty(source)) {
      return securitySystem.listRoles().stream().map(RoleXOResponse::fromRole)
          .sorted(Comparator.comparing(RoleXOResponse::getId)).collect(toList());
    }
    try {
      return securitySystem.searchRoles(source, search).stream().map(RoleXOResponse::fromRole)
          .sorted(Comparator.comparing(RoleXOResponse::getId)).collect(toList());
    }
    catch (NoSuchAuthorizationManagerException e) {
      throw buildBadSourceException(source);
    }
  }

  private WebApplicationMessageException buildBadSourceException(final String source) {
    log.debug("attempt to use invalid source {}", source);
    return new WebApplicationMessageException(Status.NOT_FOUND, String.format(SOURCE_NOT_FOUND, source),
        APPLICATION_JSON);
  }
}
