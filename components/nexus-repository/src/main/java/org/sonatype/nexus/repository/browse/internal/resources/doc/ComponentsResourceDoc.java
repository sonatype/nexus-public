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
package org.sonatype.nexus.repository.browse.internal.resources.doc;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.browse.internal.resources.ComponentsResource;
import org.sonatype.nexus.rest.Page;

/**
 * Swagger documentation for {@link ComponentsResource}
 *
 * @since 3.4.0
 */
@Api(value = "components")
public interface ComponentsResourceDoc
{
  @ApiOperation("List components")
  @ApiResponses(value = {
      @ApiResponse(code = 422, message = "repositoryId is required")
  })
  Page<ComponentXO> getComponents(
      @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
      final String continuationToken,

      @ApiParam(value = "ID of the repository from which you would like to retrieve components", required = true)
      final String repositoryId);

  @ApiOperation("Get a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Component not found"),
      @ApiResponse(code = 422, message = "Malformed ID")
  })
  ComponentXO getComponentById(@ApiParam(value = "ID of the component to retrieve") final String id);

  @ApiOperation(value = "Delete a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to delete component"),
      @ApiResponse(code = 404, message = "Component not found"),
      @ApiResponse(code = 422, message = "Malformed ID")
  })
  void deleteComponent(@ApiParam(value = "ID of the component to delete") final String id);
}
