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

import org.junit.jupiter.api.BeforeEach;
import org.sonatype.nexus.testdb.DatabaseTest;

/**
 * Test {@link AssetDAO} without setting the component's entity version
 */
class UnversionedAssetDAOTest
    extends AssetDAOTestSupport
{
  @BeforeEach
  void setupContent() {
    initialiseContent(false);
  }

  @Override
  @DatabaseTest
  protected void testCrudOperations() throws InterruptedException {
    super.testCrudOperations();
  }

  @Override
  @DatabaseTest
  protected void testLastDownloaded() throws InterruptedException {
    super.testLastDownloaded();
  }

  @Override
  @DatabaseTest
  protected void testAttachingBlobs() throws InterruptedException {
    super.testAttachingBlobs();
  }

  @Override
  @DatabaseTest
  protected void testBrowseComponentAssets() {
    super.testBrowseComponentAssets();
  }

  @Override
  @DatabaseTest
  protected void testContinuationBrowsing() {
    super.testContinuationBrowsing();
  }

  @Override
  @DatabaseTest
  protected void testFlaggedBrowsing() {
    super.testFlaggedBrowsing();
  }

  @Override
  @DatabaseTest
  protected void testReadPathTest() {
    super.testReadPathTest();
  }

  @Override
  @DatabaseTest
  protected void testDeleteAllAssets() {
    super.testDeleteAllAssets();
  }

  @Override
  @DatabaseTest
  protected void testReadPaths() {
    super.testReadPaths();
  }

  @Override
  @DatabaseTest
  protected void testPurgeOperation() {
    super.testPurgeOperation();
  }

  @Override
  @DatabaseTest
  protected void testRoundTrip() {
    super.testRoundTrip();
  }

  @Override
  @DatabaseTest
  protected void testBrowseAssetsInRepositories() {
    super.testBrowseAssetsInRepositories();
  }

  @Override
  @DatabaseTest
  protected void testBrowseEagerAssetsInRepository() {
    super.testBrowseEagerAssetsInRepository();
  }

  @Override
  @DatabaseTest
  protected void testBrowseEagerAssetsInRepositoryOrderingByBlobCreated() throws InterruptedException {
    super.testBrowseEagerAssetsInRepositoryOrderingByBlobCreated();
  }

  @Override
  @DatabaseTest
  protected void testBrowseEagerAssetsInRepositoryPaginationWithBlobCreated() throws InterruptedException {
    super.testBrowseEagerAssetsInRepositoryPaginationWithBlobCreated();
  }

  @Override
  @DatabaseTest
  protected void testBrowseEagerAssetsInRepositorySameTimestampDifferentAssetId() throws InterruptedException {
    super.testBrowseEagerAssetsInRepositorySameTimestampDifferentAssetId();
  }

  @Override
  @DatabaseTest
  protected void testSetLastDownloaded() {
    super.testSetLastDownloaded();
  }

  @Override
  @DatabaseTest
  protected void testLastUpdated() {
    super.testLastUpdated();
  }

  @Override
  @DatabaseTest
  protected void testFilterClauseIsolation() {
    super.testFilterClauseIsolation();
  }

  @Override
  @DatabaseTest
  protected void testFindByBlobRef() throws InterruptedException {
    super.testFindByBlobRef();
  }

  @Override
  @DatabaseTest
  protected void testFindByComponentIds() {
    super.testFindByComponentIds();
  }

  @Override
  @DatabaseTest
  protected void testFindAddedToRepository() {
    super.testFindAddedToRepository();
  }

  @Override
  @DatabaseTest
  protected void testFindAddedToRepositoryTruncatesToMilliseconds() {
    super.testFindAddedToRepositoryTruncatesToMilliseconds();
  }

  @Override
  @DatabaseTest
  protected void testDeleteByPaths() {
    super.testDeleteByPaths();
  }

  @Override
  @DatabaseTest
  protected void testAssetRecordsExist() {
    super.testAssetRecordsExist();
  }
}
