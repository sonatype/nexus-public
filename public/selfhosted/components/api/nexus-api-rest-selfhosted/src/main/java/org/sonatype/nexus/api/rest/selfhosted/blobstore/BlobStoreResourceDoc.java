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
package org.sonatype.nexus.api.rest.selfhosted.blobstore;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreConnectionXO;
import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreQuotaResultXO;
import org.sonatype.nexus.api.rest.common.blobstore.model.GenericBlobStoreApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import static org.sonatype.nexus.rest.ApiDocConstants.AUTHENTICATION_REQUIRED;
import static org.sonatype.nexus.rest.ApiDocConstants.INSUFFICIENT_PERMISSIONS;

/**
 * REST facade for {@link BlobStoreResource}
 *
 * @since 3.14
 */
@Tag(name = "Blob store")
public interface BlobStoreResourceDoc
{
  @Operation(summary = "List the blob stores")
  List<GenericBlobStoreApiResponse> listBlobStores();

  @Operation(summary = "Delete a blob store by name")
  void deleteBlobStore(@Parameter(description = "The name of the blob store to delete") String name) throws Exception;

  @Operation(summary = "Get quota status for a given blob store")
  BlobStoreQuotaResultXO quotaStatus(String id);

  @Operation(summary = "Verify connection using supplied Blob Store settings", hidden = true)
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Blob Store connection was successful"),
      @ApiResponse(responseCode = "400", description = "Blob Store connection failed"),
      @ApiResponse(responseCode = "401", description = AUTHENTICATION_REQUIRED),
      @ApiResponse(responseCode = "403", description = INSUFFICIENT_PERMISSIONS)
  })
  void verifyConnection(final @NotNull @Valid BlobStoreConnectionXO blobStoreConnectionXO);
}
