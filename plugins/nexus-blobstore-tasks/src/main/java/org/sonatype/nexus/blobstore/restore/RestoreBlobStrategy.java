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
package org.sonatype.nexus.blobstore.restore;

import java.util.Properties;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.Repository;

/**
 * @since 3.4
 */
public interface RestoreBlobStrategy
{
  /**
   * @deprecated since 3.6
   */
  @Deprecated
  default void restore(Properties properties, Blob blob, BlobStore blobStore) {
    restore(properties, blob, blobStore, false);
  }

  /**
   * @since 3.6
   *
   * @param properties associated with the blob being restore
   * @param blob being restored
   * @param blobStore the blob store where the blob will be stored
   * @param isDryRun if {@code true}, no lasting changes will be made, only logged
   */
  void restore(Properties properties, Blob blob, BlobStore blobStore, boolean isDryRun);

  /**
   * Runs after all blobs have been restored to the database.
   * 
   * @since 3.15
   * @param updateAssets whether updating assets is expected or not
   * @param repository repository to update
   */
  void after(boolean updateAssets, final Repository repository);
}
