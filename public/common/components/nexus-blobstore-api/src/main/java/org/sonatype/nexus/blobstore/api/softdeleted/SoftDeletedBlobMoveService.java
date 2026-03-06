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
package org.sonatype.nexus.blobstore.api.softdeleted;

import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobId;

/**
 * Service for updating soft_deleted_blobs records after blobs are moved between blob stores.
 * <p>
 * When blobs are moved (e.g., during group member removal or repository move operations),
 * their soft_deleted_blobs records need to be updated with:
 * <ul>
 * <li>The new source_blob_store_name where the blob now resides</li>
 * <li>The new date_path_ref timestamp (assigned during move by BlobStoreSupport.getBlobId())</li>
 * </ul>
 */
public interface SoftDeletedBlobMoveService
{
  /**
   * Updates soft_deleted_blobs records after blobs are moved to new locations.
   * Updates both source_blob_store_name and date_path_ref for each moved blob.
   *
   * @param movedBlobs map of moved blob IDs (containing new datePathRef) to their target blob store names
   * @param oldBlobStoreName the blob store the blobs were moved from
   */
  void updateMovedBlobRecords(Map<BlobId, String> movedBlobs, String oldBlobStoreName);
}
