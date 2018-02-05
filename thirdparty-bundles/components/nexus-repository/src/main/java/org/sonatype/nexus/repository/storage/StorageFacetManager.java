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
package org.sonatype.nexus.repository.storage;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Bucket;

/**
 * Contains StorageFacet-related operations that cannot be represented in individual StorageFacet instances.
 *
 * @since 3.2.1
 */
public interface StorageFacetManager
{
  /**
   * Enqueues the contents (bucket) of a particular StorageFacet for deletion. Called by a StorageFacet on delete.
   */
  void enqueueDeletion(Repository repository, BlobStore blobStore, Bucket bucket);

  /**
   * Performs the actual deletions for any deleted storage facets, returning the number of deletions successfully
   * performed. This can potentially be a long-running operation!
   *
   * @return the number of successful cleanups performed during the call
   */
  long performDeletions();
}
