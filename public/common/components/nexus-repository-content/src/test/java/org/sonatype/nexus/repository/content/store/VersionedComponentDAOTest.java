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
 * Test {@link ComponentDAO} with entity versioning enabled
 */
class VersionedComponentDAOTest
    extends ComponentDAOTestSupport
{
  @BeforeEach
  void setup() {
    setupContent(true);
  }

  @Override
  @DatabaseTest
  protected void testCrudOperations() throws InterruptedException {
    super.testCrudOperations();
  }

  @Override
  @DatabaseTest
  protected void testBrowseComponentCoordinates() {
    super.testBrowseComponentCoordinates();
  }

  @Override
  @DatabaseTest
  protected void testContinuationBrowsing() {
    super.testContinuationBrowsing();
  }

  @Override
  @DatabaseTest
  protected void testDeleteAllComponents() {
    super.testDeleteAllComponents();
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
  protected void testBrowseComponentsInRepositories() {
    super.testBrowseComponentsInRepositories();
  }

  @Override
  @DatabaseTest
  protected void testFilterClauseIsolation() {
    super.testFilterClauseIsolation();
  }

  @Override
  @DatabaseTest
  protected void testContinuationSetBrowsing() {
    super.testContinuationSetBrowsing();
  }

  @Override
  @DatabaseTest
  protected void testNormalizationMethods() {
    super.testNormalizationMethods();
  }

  @Override
  @DatabaseTest
  protected void testBrowseComponentsEager() {
    super.testBrowseComponentsEager();
  }

  @Override
  @DatabaseTest
  protected void testBrowseVersionsByRepoIds() {
    super.testBrowseVersionsByRepoIds();
  }

  @Override
  @DatabaseTest
  protected void testReadCoordinateInRepoIds() {
    super.testReadCoordinateInRepoIds();
  }

  @Override
  @DatabaseTest(h2 = false, postgresql = true)
  protected void testCreateComponentReturnsNullIdWhenRepositoryDeleted() {
    super.testCreateComponentReturnsNullIdWhenRepositoryDeleted();
  }
}
