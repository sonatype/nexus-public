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
package org.sonatype.nexus.blobstore.rest;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST facade for {@link BlobStoreResource}
 *
 * @since 3.14
 */
@Api(value = "Blob store")
public interface BlobStoreResourceDoc
{
  @ApiOperation("List the blob stores")
  List<GenericBlobStoreApiResponse> listBlobStores();

  @ApiOperation("Delete a blob store by name")
  void deleteBlobStore(@ApiParam("The name of the blob store to delete") String name) throws Exception;

  @ApiOperation("Get quota status for a given blob store")
  BlobStoreQuotaResultXO quotaStatus(String id);
}
