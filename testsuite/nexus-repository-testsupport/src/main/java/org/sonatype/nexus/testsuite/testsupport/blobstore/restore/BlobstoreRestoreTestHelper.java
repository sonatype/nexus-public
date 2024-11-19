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

import java.util.List;
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

  String PLAN_RECONCILE_TYPE_ID = "blobstore.planReconciliation";

  String EXECUTE_RECONCILE_TYPE_ID = "blobstore.executeReconciliationPlan";

  /**
   * Get the blob ids of the assets
   */
  List<BlobId> getAssetBlobId();

  /**
   * Clean tables from previous data
   */
  void truncateTables();

  /**
   * Deletes the file with specified extension
   *
   * @param blobStorageName the name of the blobstore
   * @param extension extension of the file to delete
   */
  void simulateFileLoss(String blobStorageName, String extension);

  /**
   * Asserts that the reconcile plan exists with specific set of parameters
   *
   * @param mapParam map of parameters to check
   */
  boolean assertReconcilePlanExists(String type, String action);

  /**
   * Run the reconcile task with the specified wait for task timeout
   *
   * @param blobstoreName the name of the blobstore
   * @param timeout the timeout to wait for the task to complete
   */
  void runReconcileTaskWithTimeout(final String blobstoreName, final long timeout);

  void simulateComponentAndAssetMetadataLoss();

  void simulateAssetMetadataLoss();

  void simulateComponentMetadataLoss();

  /**
   * Run the restore (reconcile) task with the specified wait for task timeout and the specified dry run flag
   *
   * @param blobstoreName the name of the blobstore
   */
  default void runRestoreMetadataTask(final String blobstoreName) {
    runRestoreMetadataTaskWithTimeout(blobstoreName, 60, false);
  }

  /**
   * Run the restore (reconcile) task with the default wait for task timeout and the specified dry run flag
   *
   * @param blobStoreName the name of the blobstore
   * @param isDryRun when true set dryrun on the task which does not restore assets
   */
  default void runRestoreMetadataTask(final String blobStoreName, final boolean isDryRun) {
    runRestoreMetadataTaskWithTimeout(blobStoreName, 60, isDryRun);
  }

  /**
   * Run the restore (reconcile) task with the specified wait for task timeout and the specified dry run flag
   *
   * @param blobstoreName the name of the blobstore
   * @param timeout the timeout to wait for the task to complete
   * @param dryRun when true set dryrun on the task which does not restore assets
   */
  void runRestoreMetadataTaskWithTimeout(final String blobstoreName, final long timeout, final boolean dryRun);

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
   * Rewrites all the blob names either adding a leading slash, or removing a leading slash to simulate blobs which were
   * written by the other database.
   */
  void rewriteBlobNames();

  /**
   * Retrieve the map of path->blobId for all assets in the provided repository.
   *
   * @param pathFilter a predicate which returns true if the asset path should be included in the result.
   */
  Map<String, BlobId> getAssetToBlobIds(Repository repo, Predicate<String> pathFilter);

  default Map<String, BlobId> getAssetToBlobIds(final Repository repo) {
    return getAssetToBlobIds(repo, path -> true);
  }
}
