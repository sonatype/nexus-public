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
package org.sonatype.nexus.self.hosted.blobstore.s3.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ScheduleS3CompactTasksMigrationStepTest
{
  @Rule
  public DataSessionRule dataSessionRule = new DataSessionRule();

  @Mock
  private UpgradeTaskScheduler upgradeTaskScheduler;

  @InjectMocks
  private ScheduleS3CompactTasksMigrationStep undertest;

  @Test
  public void testMigrate() throws Exception {
    createTable();
    createBlobstoreRecord(S3BlobStore.TYPE);

    try (Connection connection = dataSessionRule.openConnection("nexus")) {
      undertest.migrate(connection);
    }
    verify(upgradeTaskScheduler).createTaskConfigurationInstance(ScheduleS3CompactTasksTaskDescriptor.TYPE_ID);
    verify(upgradeTaskScheduler).schedule(any());
  }

  @Test
  public void testMigrate_noTable() throws Exception {
    try (Connection connection = dataSessionRule.openConnection("nexus")) {
      undertest.migrate(connection);
    }
    verifyNoInteractions(upgradeTaskScheduler);
  }

  @Test
  public void testMigrate_noS3BlobStores() throws Exception {
    createTable();
    createBlobstoreRecord("File");

    try (Connection connection = dataSessionRule.openConnection("nexus")) {
      undertest.migrate(connection);
    }
    verifyNoInteractions(upgradeTaskScheduler);
  }

  private void createBlobstoreRecord(final String type) {
    try (Connection conn = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement statement = conn.prepareStatement(INSERT)) {
      String name = UUID.randomUUID().toString();
      statement.setString(1, name);
      statement.setString(2, name);
      statement.setString(3, type);
      statement.setString(4, "{}");
      statement.executeUpdate();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void createTable() {
    try (Connection conn = dataSessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        PreparedStatement statement = conn.prepareStatement(CREATE_QUERY)) {
      statement.executeUpdate();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static final String INSERT = """
      INSERT INTO blob_store_configuration (id, name, type, attributes)
             VALUES (?, ?, ?, ?);
      """;

  private static final String CREATE_QUERY = """
      CREATE TABLE IF NOT EXISTS blob_store_configuration (
        id         varchar NOT NULL,
        name       VARCHAR(256) NOT NULL,
        type       VARCHAR(100) NOT NULL,
        attributes varchar NOT NULL,

        CONSTRAINT pk_blob_store_configuration_id PRIMARY KEY (id),
        CONSTRAINT uk_blob_store_configuration_name UNIQUE (name)
      );
        """;

}
