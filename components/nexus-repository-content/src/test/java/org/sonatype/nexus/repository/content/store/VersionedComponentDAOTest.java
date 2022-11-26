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
 * Test {@link ComponentDAO} with entity versioning enabled
 */
public class VersionedComponentDAOTest
    extends ComponentDAOTestSupport
{
  @Before
  public void setup() {
    setupContent(true);
  }

  @Test
  public void testCrudOperations() throws InterruptedException {
    super.testCrudOperations();
  }

  @Test
  public void testBrowseComponentCoordinates() {
    super.testBrowseComponentCoordinates();
  }

  @Test
  public void testContinuationBrowsing() {
    super.testContinuationBrowsing();
  }

  @Test
  public void testDeleteAllComponents() {
    super.testDeleteAllComponents();
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
  public void testBrowseComponentsInRepositories() {
    super.testBrowseComponentsInRepositories();
  }

  @Test
  public void testFilterClauseIsolation() {
    super.testFilterClauseIsolation();
  }
}
