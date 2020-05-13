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
package org.sonatype.nexus.blobstore.file.rest;

import javax.validation.Valid;

import org.sonatype.nexus.validation.Validate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST facade for {@link FileBlobStoreResource}
 *
 * @since 3.19
 */
@Api("Blob store")
public interface FileBlobStoreResourceDoc
{
  @ApiOperation("Create a file blob store")
  void createFileBlobStore(@Valid final FileBlobStoreApiCreateRequest request) throws Exception;

  @ApiOperation("Update a file blob store configuration by name")
  @Validate
  void updateFileBlobStore(
      @ApiParam("The name of the file blob store to update") final String name,
      @Valid final FileBlobStoreApiUpdateRequest request
  ) throws Exception;

  @ApiOperation("Get a file blob store configuration by name")
  FileBlobStoreApiModel getFileBlobStoreConfiguration(
      @ApiParam("The name of the file blob store to read") final String name
  );
}
