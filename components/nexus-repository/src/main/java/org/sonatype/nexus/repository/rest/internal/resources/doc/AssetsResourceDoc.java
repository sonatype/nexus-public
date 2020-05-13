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
package org.sonatype.nexus.repository.rest.internal.resources.doc;

import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.internal.resources.AssetsResource;
import org.sonatype.nexus.rest.Page;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


/**
 * Swagger documentation for {@link AssetsResource}
 *
 * @since 3.4.0
 */
@Api(value = "Assets", description = "Operations to get and delete assets")
public interface AssetsResourceDoc
{
  @ApiOperation("List assets")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to list assets"),
      @ApiResponse(code = 422, message = "Parameter 'repository' is required")
  })
  Page<AssetXO> getAssets(
      @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
      final String continuationToken,

      @ApiParam(value = "Repository from which you would like to retrieve assets.", required = true)
      final String repository);

  @ApiOperation("Get a single asset")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to get asset"),
      @ApiResponse(code = 404, message = "Asset not found"),
      @ApiResponse(code = 422, message = "Malformed ID")
  })
  AssetXO getAssetById(@ApiParam(value = "Id of the asset to get") final String id);

  @ApiOperation(value = "Delete a single asset")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Asset was successfully deleted"),
      @ApiResponse(code = 403, message = "Insufficient permissions to delete asset"),
      @ApiResponse(code = 404, message = "Asset not found"),
      @ApiResponse(code = 422, message = "Malformed ID")
  })
  void deleteAsset(@ApiParam(value = "Id of the asset to delete") final String id);
}
