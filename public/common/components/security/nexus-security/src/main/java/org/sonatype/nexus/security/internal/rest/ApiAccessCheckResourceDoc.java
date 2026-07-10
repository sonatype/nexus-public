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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Swagger documentation for {@link ApiAccessCheckResource}
 */
@Tag(name = "Security management: API access")
public interface ApiAccessCheckResourceDoc
{
  @Operation(summary = "Check if a user or role has access to an API endpoint",
      description = "This endpoint allows administrators to verify whether a specific user or role " +
          "has the required permissions to access a given REST API endpoint. " +
          "The response includes the permission chain showing how access is granted, " +
          "from user to role to privilege to permission.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Access check result",
          content = @Content(schema = @Schema(implementation = ApiAccessResultXo.class))),
      @ApiResponse(responseCode = "400",
          description = "Invalid request - userId and roleId are mutually exclusive, or missing required fields"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to perform access check"),
      @ApiResponse(responseCode = "404", description = "User or role not found")
  })
  ApiAccessResultXo checkAccess(
      @Parameter(description = "Access check request containing target user/role and endpoint to check",
          required = true) @NotNull @Valid ApiAccessCheckXo request);
}
