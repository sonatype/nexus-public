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

import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.rest.Resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.sonatype.nexus.common.app.FeatureFlags.PREVIEW_UI_SETTINGS_ENABLED;

/**
 * Serves the merged permission map for the Preview UI API module.
 */
@Component
@Produces(APPLICATION_JSON)
@ConditionalOnProperty(name = PREVIEW_UI_SETTINGS_ENABLED, havingValue = "true", matchIfMissing = true)
@Path(ApiPermissionsResource.RESOURCE_PATH)
@Tag(name = "Internal UI: API permissions")
public class ApiPermissionsResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String RESOURCE_PATH = "internal/ui/api/permissions";

  private final EndpointPermissionRegistry registry;

  @Autowired
  public ApiPermissionsResource(final EndpointPermissionRegistry registry) {
    this.registry = checkNotNull(registry);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  @Operation(summary = "List REST endpoints with permission metadata for the API documentation UI")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Permission map",
          content = @Content(schema = @Schema(implementation = ApiPermissionsResponse.class))),
      @ApiResponse(responseCode = "401", description = "Not authenticated"),
      @ApiResponse(responseCode = "403", description = "Missing nexus:settings:read"),
      @ApiResponse(responseCode = "500", description = "Registry unavailable")
  })
  public Response list(
      @QueryParam("method") final String method,
      @QueryParam("path") final String pathSubstring,
      @QueryParam("permission") final String permissionSubstring,
      @QueryParam("tag") final String tag)
  {
    if (!registry.isReady()) {
      return Response.status(INTERNAL_SERVER_ERROR)
          .type(APPLICATION_JSON)
          .entity(registry.snapshot("Permission registry unavailable"))
          .build();
    }

    List<ApiEndpointPermission> filtered = ApiPermissionsQuery.apply(
        registry.getEndpoints(),
        method,
        pathSubstring,
        permissionSubstring,
        tag);

    ApiPermissionsResponse body = new ApiPermissionsResponse(
        filtered,
        DateTimeFormatter.ISO_INSTANT.format(registry.getGeneratedAt()),
        registry.getEndpoints().size(),
        registry.getUnmappedSwaggerOperations(),
        null);

    return Response.ok(body, APPLICATION_JSON).build();
  }
}
