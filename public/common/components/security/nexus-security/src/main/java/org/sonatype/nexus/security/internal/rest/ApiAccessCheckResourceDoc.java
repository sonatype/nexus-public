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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Swagger documentation for {@link ApiAccessCheckResource}
 */
@Api(value = "Security management: API access")
public interface ApiAccessCheckResourceDoc
{
  @ApiOperation(
      value = "Check if a user or role has access to an API endpoint",
      notes = "This endpoint allows administrators to verify whether a specific user or role " +
          "has the required permissions to access a given REST API endpoint. " +
          "The response includes the permission chain showing how access is granted, " +
          "from user to role to privilege to permission.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Access check result", response = ApiAccessResultXo.class),
      @ApiResponse(code = 400,
          message = "Invalid request - userId and roleId are mutually exclusive, or missing required fields"),
      @ApiResponse(code = 403, message = "Insufficient permissions to perform access check"),
      @ApiResponse(code = 404, message = "User or role not found")
  })
  ApiAccessResultXo checkAccess(
      @ApiParam(value = "Access check request containing target user/role and endpoint to check",
          required = true) @NotNull @Valid ApiAccessCheckXo request);
}
