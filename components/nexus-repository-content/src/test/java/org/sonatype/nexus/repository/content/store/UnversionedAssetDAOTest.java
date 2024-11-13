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

import org.junit.Before;
import org.junit.Test;

/**
 * Test {@link AssetDAO} without setting the component's entity version
 */
public class UnversionedAssetDAOTest
    extends AssetDAOTestSupport
{
  @Before
  public void setupContent() {
    initialiseContent(false);
  }

  @Test
  public void testCrudOperations() throws InterruptedException {
    super.testCrudOperations();
  }

  @Test
  public void testLastDownloaded() throws InterruptedException {
    super.testLastDownloaded();
  }

  @Test
  public void testAttachingBlobs() throws InterruptedException {
    super.testAttachingBlobs();
  }

  @Test
  public void testBrowseComponentAssets() {
    super.testBrowseComponentAssets();
  }

  @Test
  public void testContinuationBrowsing() {
    super.testContinuationBrowsing();
  }

  @Test
  public void testFlaggedBrowsing() {
    super.testFlaggedBrowsing();
  }

  @Test
  public void testReadPathTest() {
    super.testReadPathTest();
  }

  @Test
  public void testDeleteAllAssets() {
    super.testDeleteAllAssets();
  }

  @Test
  public void testReadPaths() {
    super.testReadPaths();
  }

  @Test
  public void testPurgeOperation() {
    super.testPurgeOperation();
  }

  @Test
  public void testRoundTrip() {
    super.testRoundTrip();
  }

  @Test
  public void testBrowseAssetsInRepositories() {
    super.testBrowseAssetsInRepositories();
  }

  @Test
  public void testBrowseEagerAssetsInRepository() {
    super.testBrowseEagerAssetsInRepository();
  }

  @Test
  public void testSetLastDownloaded() {
    super.testSetLastDownloaded();
  }

  @Test
  public void testLastUpdated() {
    super.testLastUpdated();
  }

  @Test
  public void testFilterClauseIsolation() {
    super.testFilterClauseIsolation();
  }

  @Test
  public void testFindByBlobRef() throws InterruptedException {
    super.testFindByBlobRef();
  }

  @Test
  public void testFindByComponentIds() {
    super.testFindByComponentIds();
  }

  @Test
  public void testFindAddedToRepository() {
    super.testFindAddedToRepository();
  }

  @Test
  public void testFindAddedToRepositoryTruncatesToMilliseconds() {
    super.testFindAddedToRepositoryTruncatesToMilliseconds();
  }

  @Test
  public void testDeleteByPaths() {
    super.testDeleteByPaths();
  }

  @Test
  public void testAssetRecordsExist() {
    super.testAssetRecordsExist();
  }
}
