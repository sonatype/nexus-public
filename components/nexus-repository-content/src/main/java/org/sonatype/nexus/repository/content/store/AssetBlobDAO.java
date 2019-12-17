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
package org.sonatype.nexus.repository.content.store;

import java.util.Collection;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.content.AssetBlob;

import org.apache.ibatis.annotations.Param;

/**
 * Asset blob {@link ContentDataAccess}.
 *
 * @since 3.20
 */
@SchemaTemplate("format")
public interface AssetBlobDAO
    extends ContentDataAccess
{
  /**
   * Browse unused blob references in the content data store.
   */
  Collection<BlobRef> browseUnusedBlobs();

  /**
   * Creates the given asset blob in the content data store.
   *
   * @param assetBlob the asset blob to create
   */
  void createAssetBlob(AssetBlobData assetBlob);

  /**
   * Retrieves an asset blob from the content data store.
   *
   * @param blobRef the blob reference
   * @return asset blob if it was found
   */
  Optional<AssetBlob> readAssetBlob(@Param("blobRef") BlobRef blobRef);

  /**
   * Deletes an asset blob from the content data store.
   *
   * @param blobRef the blob reference
   * @return {@code true} if the asset blob was deleted
   */
  boolean deleteAssetBlob(@Param("blobRef") BlobRef blobRef);
}
