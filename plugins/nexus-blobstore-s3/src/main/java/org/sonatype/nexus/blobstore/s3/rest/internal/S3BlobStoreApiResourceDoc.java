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
package org.sonatype.nexus.blobstore.s3.rest.internal;

import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static org.sonatype.nexus.rest.ApiDocConstants.API_BLOB_STORE;
import static org.sonatype.nexus.rest.ApiDocConstants.AUTHENTICATION_REQUIRED;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;
import static org.sonatype.nexus.rest.ApiDocConstants.S3_BLOB_STORE_CREATED;
import static org.sonatype.nexus.rest.ApiDocConstants.S3_BLOB_STORE_UPDATED;
import static org.sonatype.nexus.rest.ApiDocConstants.SUCCESS;
import static org.sonatype.nexus.rest.ApiDocConstants.UNKNOWN_S3_BLOB_STORE;

/**
 * API documentation for operations provided by {@link S3BlobStoreApiResource}.
 *
 * @since 3.20
 */
@Api(API_BLOB_STORE)
public interface S3BlobStoreApiResourceDoc
{
  @ApiOperation("Create an S3 blob store")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = S3_BLOB_STORE_CREATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  Response createBlobStore(@Valid S3BlobStoreApiModel request) throws Exception;

  @ApiOperation("Update an S3 blob store configuration by name")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = S3_BLOB_STORE_UPDATED),
      @ApiResponse(code = 400, message = UNKNOWN_S3_BLOB_STORE),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  void updateBlobStore(
      @Valid final S3BlobStoreApiModel request,
      @ApiParam(value = "Name of the blob store to update") String blobStoreName) throws Exception;

  @ApiOperation("Get a S3 blob store configuration by name")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = SUCCESS, response = S3BlobStoreApiModel.class),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(code = 404, message = UNKNOWN_S3_BLOB_STORE)
  })
  S3BlobStoreApiModel getBlobStore(
      @ApiParam(value = "Name of the blob store configuration to fetch") String blobStoreName);


  @ApiOperation("Delete an S3 blob store with an empty name")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Blob store deleted"),
      @ApiResponse(code = 400, message = "Unknown S3 blob store"),
      @ApiResponse(code = 401, message = "Authentication required"),
      @ApiResponse(code = 403, message = "Insufficient permissions"),
      @ApiResponse(code = 404, message = "Blob store not found"),
  })
  Response deleteBlobStoreWithEmptyName();
}

