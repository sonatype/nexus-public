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
package org.sonatype.nexus.internal.capability.storage.datastore.cleanup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemDAO;
import org.sonatype.nexus.internal.capability.storage.CapabilityStorageItemTestSupport;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

/**
 * Real-database tests for {@link CleanupCapabilityDuplicatesMigrationStep_1_27}: it delegates duplicate
 * cleanup to the service, then creates a unique (type, properties) constraint/index on
 * {@code capability_storage_item}.
 */
@ExtendWith(MockitoExtension.class)
class CleanupCapabilityDuplicatesMigrationStep_1_27Test
{
  @DataSessionConfiguration(daos = {CapabilityStorageItemDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  @Mock
  private CleanupCapabilityDuplicatesService service;

  private CleanupCapabilityDuplicatesMigrationStep_1_27 underTest;

  @BeforeEach
  void setUp() {
    underTest = new CleanupCapabilityDuplicatesMigrationStep_1_27(service);
  }

  @DatabaseTest
  void migrate_runsCleanupThenEnforcesUniqueness() throws Exception {
    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);

      verify(service).doCleanup();

      // First row of (type, properties) is accepted; an identical (type, properties) must now be rejected.
      CapabilityStorageItemTestSupport.insert(conn, "rapture.settings", Map.of("a", "1"));
      assertThrows(SQLException.class,
          () -> CapabilityStorageItemTestSupport.insert(conn, "rapture.settings", Map.of("a", "1")));
    }
  }

  @DatabaseTest
  void migrate_isIdempotent() throws Exception {
    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest.migrate(conn);
      assertDoesNotThrow(() -> underTest.migrate(conn));
    }
  }
}
