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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.file;

import jakarta.validation.Valid;

import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiCreateRequest;
import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiModel;
import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiUpdateRequest;
import org.sonatype.nexus.validation.Validate;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import static org.sonatype.nexus.rest.ApiDocConstants.BLOBSTORE_NOT_FOUND;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;
import static org.sonatype.nexus.rest.ApiDocConstants.SUCCESS;

/**
 * REST facade for {@link FileBlobStoreResource}
 *
 * @since 3.19
 */
@Tag(name = "Blob store")
public interface FileBlobStoreResourceDoc
{
  @Operation(summary = "Create a file blob store")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = SUCCESS),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS)
  })
  void createFileBlobStore(@Valid final FileBlobStoreApiCreateRequest request) throws Exception;

  @Operation(summary = "Update a file blob store configuration by name")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = SUCCESS),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(responseCode = "404", description = BLOBSTORE_NOT_FOUND)
  })
  @Validate
  void updateFileBlobStore(
      @Parameter(description = "The name of the file blob store to update") final String name,
      @Valid final FileBlobStoreApiUpdateRequest request) throws Exception;

  @Operation(summary = "Get a file blob store configuration by name")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = SUCCESS,
          content = @Content(schema = @Schema(implementation = FileBlobStoreApiModel.class))),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS),
      @ApiResponse(responseCode = "404", description = BLOBSTORE_NOT_FOUND)
  })
  FileBlobStoreApiModel getFileBlobStoreConfiguration(
      @Parameter(description = "The name of the file blob store to read", example = "default") final String name);
}
