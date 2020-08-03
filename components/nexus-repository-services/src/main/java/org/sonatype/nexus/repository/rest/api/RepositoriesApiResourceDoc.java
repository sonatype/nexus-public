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
package org.sonatype.nexus.repository.rest.api;

import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.rest.ApiDocConstants.API_REPOSITORY_MANAGEMENT;
import static org.sonatype.nexus.rest.ApiDocConstants.AUTHENTICATION_REQUIRED;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;
import static org.sonatype.nexus.rest.ApiDocConstants.REPOSITORY_DELETED;
import static org.sonatype.nexus.rest.ApiDocConstants.REPOSITORY_NOT_FOUND;

/**
 * @since 3.20
 */
@Api(value = API_REPOSITORY_MANAGEMENT)
public interface RepositoriesApiResourceDoc
{
  @ApiOperation("Delete repository of any format")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = REPOSITORY_DELETED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(code = 404, message = REPOSITORY_NOT_FOUND)
  })
  Response deleteRepository(@ApiParam(value = "Name of the repository to delete") final String repositoryName)
      throws Exception;

  @ApiOperation("Schedule a 'Repair - Rebuild repository search' Task. Hosted or proxy repositories only.")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Repository search index rebuild has been scheduled"),
      @ApiResponse(code = 400, message = "Repository is not of hosted or proxy type"),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(code = 404, message = REPOSITORY_NOT_FOUND)
  })
  void rebuildIndex(@ApiParam(value = "Name of the repository to rebuild index") final String repositoryName);

  @ApiOperation("Invalidate repository cache. Proxy or group repositories only.")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Repository cache invalidated"),
      @ApiResponse(code = 400, message = "Repository is not of proxy or group type"),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(code = 404, message = REPOSITORY_NOT_FOUND)
  })
  void invalidateCache(@ApiParam(value = "Name of the repository to invalidate cache") final String repositoryName);
}
