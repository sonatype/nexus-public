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
package org.sonatype.nexus.repository.content;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Each asset represents a unique path to binary content in a repository.
 *
 * @since 3.20
 * @see Component
 */
public interface Asset
    extends RepositoryContent
{
  /**
   * The path in the repository.
   */
  String path();

  /**
   * The kind of asset.
   *
   * @since 3.24
   */
  String kind();

  /**
   * Assets may be grouped together under a logical coordinate, represented by a {@link Component}.
   */
  Optional<Component> component();

  /**
   * Current blob attached to this asset; proxy repositories may have assets whose blobs have not been fetched yet.
   * If checking for existence please use {@code hasBlob()} which is less expensive.
   */
  Optional<AssetBlob> blob();

  /**
   * Indicates whether the asset has a blob, may be faster than {@code blob()} due to lazy loading.
   */
  boolean hasBlob();

  /**
   * If/when this asset was last downloaded.
   */
  Optional<OffsetDateTime> lastDownloaded();

  /**
   * returns the blob store name if blob_store_name is in the query
   */
  String blobStoreName();

  /**
   * The size of the asset(blob)
   */
  public long assetBlobSize();
}
