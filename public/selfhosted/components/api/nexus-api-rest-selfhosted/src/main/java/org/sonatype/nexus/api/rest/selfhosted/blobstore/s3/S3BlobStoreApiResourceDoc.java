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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.s3;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiModel;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Tag(name = API_BLOB_STORE)
public interface S3BlobStoreApiResourceDoc
{
  @Operation(summary = "Create an S3 blob store")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = S3_BLOB_STORE_CREATED),
      @ApiResponse(responseCode = "401", description = AUTHENTICATION_REQUIRED),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS)
  })
  Response createBlobStore(@Valid S3BlobStoreApiModel request) throws Exception;

  @Operation(summary = "Update an S3 blob store configuration by name")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = S3_BLOB_STORE_UPDATED),
      @ApiResponse(responseCode = "400", description = UNKNOWN_S3_BLOB_STORE),
      @ApiResponse(responseCode = "401", description = AUTHENTICATION_REQUIRED),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS)
  })
  void updateBlobStore(
      @Valid final S3BlobStoreApiModel request,
      @Parameter(description = "Name of the blob store to update") String blobStoreName) throws Exception;

  @Operation(summary = "Get a S3 blob store configuration by name")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = SUCCESS,
          content = @Content(schema = @Schema(implementation = S3BlobStoreApiModel.class))),
      @ApiResponse(responseCode = "401", description = AUTHENTICATION_REQUIRED),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(responseCode = "404", description = UNKNOWN_S3_BLOB_STORE)
  })
  S3BlobStoreApiModel getBlobStore(
      @Parameter(description = "Name of the blob store configuration to fetch") String blobStoreName);

  @Operation(summary = "Delete an S3 blob store with an empty name")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Blob store deleted"),
      @ApiResponse(responseCode = "400", description = "Unknown S3 blob store"),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
      @ApiResponse(responseCode = "404", description = "Blob store not found"),
  })
  Response deleteBlobStoreWithEmptyName();
}
