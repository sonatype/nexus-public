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
package org.sonatype.nexus.repository.apt.rest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.rest.ApiDocConstants.AUTHENTICATION_REQUIRED;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;
import static org.sonatype.nexus.rest.ApiDocConstants.RESOURCE_CREATED;
import static org.sonatype.nexus.rest.ApiDocConstants.RESOURCE_NOT_FOUND;
import static org.sonatype.nexus.rest.ApiDocConstants.RESOURCE_UPDATED;

/**
 * @since 3.next
 */
@Api(value = "Repository Management")
public interface AptProxyRepositoriesApiResourceDoc
{
  @ApiOperation("Create APT proxy repository")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = RESOURCE_CREATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  Response createProxyRepository(@Valid @NotNull final AptProxyRepositoryApiRequest request);

  @ApiOperation("Update APT proxy repository")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = RESOURCE_UPDATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(code = 404, message = RESOURCE_NOT_FOUND)
  })
  Response updateProxyRepository(
      @Valid @NotNull final AptProxyRepositoryApiRequest request,
      @ApiParam(value = "Name of the repository to update") final String repositoryName);
}
