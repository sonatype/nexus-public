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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;

/**
 * Strategy for checking the integrity of the assets in a repository against its blobstore
 *
 * @since 3.6.1
 */
public interface IntegrityCheckStrategy
{
  /**
   * Run the integrity check on the given repository and blob store
   *
   * @param repository  repository to check
   * @param blobStore   blob store to check
   * @param isCancelled Supplier to check during processing if the task is cancelled
   * @param integrityCheckFailedHandler will be called with Asset if unable to validate blob integrity
   */
  void check(
      final Repository repository,
      final BlobStore blobStore,
      final Supplier<Boolean> isCancelled,
      final Consumer<Asset> integrityCheckFailedHandler);
}
