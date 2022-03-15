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
package org.sonatype.nexus.testsuite.testsupport.blobstore.restore;

import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.repository.Repository;

/**
 * Helper class containing common functionality needed in ITs testing the restoration of component metadata from blobs.
 * Assumes a unit of work has already been started.
 */
public interface BlobstoreRestoreTestHelper
{
  String TYPE_ID = "blobstore.rebuildComponentDB";

  String BLOB_STORE_NAME_FIELD_ID = "blobstoreName";

  String RESTORE_BLOBS = "restoreBlobs";

  String UNDELETE_BLOBS = "undeleteBlobs";

  String INTEGRITY_CHECK = "integrityCheck";

  String DRY_RUN = "dryRun";

  void simulateComponentAndAssetMetadataLoss();

  void simulateAssetMetadataLoss();

  void simulateComponentMetadataLoss();

  void runRestoreMetadataTaskWithTimeout(long timeout, boolean dryRun);

  void runRestoreMetadataTask();

  void runRestoreMetadataTask(boolean isDryRun);

  void assertAssetMatchesBlob(Repository repository, String... names);

  void assertAssetInRepository(Repository repository, String name);

  void assertAssetNotInRepository(Repository repository, String... names);

  void assertComponentInRepository(Repository repository, String name);

  void assertComponentInRepository(Repository repository, String name, String version);

  void assertComponentInRepository(Repository repository, String group, String name, String version);

  void assertComponentNotInRepository(Repository repository, String name);

  void assertComponentNotInRepository(Repository repository, String name, String version);

  void assertAssetAssociatedWithComponent(Repository repository, String name, String path);

  void assertAssetAssociatedWithComponent(
      Repository repository,
      @Nullable String group,
      String name,
      String version,
      String... paths);

  /**
   * Rewrites all the blob names either adding a leading slash, or removing a leading slash to simulate blobs
   * which were written by the other database.
   */
  void rewriteBlobNames();

  /**
   * Retrieve the map of path->blobId for all assets in the provided repository.
   *
   * @param pathFilter a predicate which returns true if the asset path should be included in the result.
   *
   */
  Map<String, BlobId> getAssetToBlobIds(Repository repo, Predicate<String> pathFilter);

  default Map<String, BlobId> getAssetToBlobIds(final Repository repo) {
    return getAssetToBlobIds(repo, path -> true);
  }
}
