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
package org.sonatype.nexus.repository.tools;

import java.util.function.Consumer;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.repository.Repository;

/**
 * Detects orphaned blobs (i.e. non-deleted blobs that exist in the blobstore but not the asset table)
 *
 * @since 3.13
 */
public interface OrphanedBlobFinder
{
  /**
   * Delete orphaned blobs for all repositories
   */
  void delete();

  /**
   * Delete orphaned blobs associated with a given repository
   *
   * @param repository - where to look for orphaned blobs
   */
  void delete(final Repository repository);

  /**
   * Look for orphaned blobs in a given repository and callback for each blobId found
   *
   * @param repository - where to look for orphaned blobs
   * @param handler    - callback to handle an orphaned blob
   */
  void detect(final Repository repository, final Consumer<BlobId> handler);

}
