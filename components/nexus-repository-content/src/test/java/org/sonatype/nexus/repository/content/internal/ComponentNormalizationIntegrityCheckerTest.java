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
package org.sonatype.nexus.repository.content.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTaskDescriptor;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.UpgradeTaskScheduler;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationUtility;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.String.format;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class ComponentNormalizationIntegrityCheckerTest
    extends TestSupport
{
  private static final String MAVEN2_FORMAT = "maven2";

  private static final String RAW_FORMAT = "raw";

  private static final String MAVEN2_TABLE = "maven2_component";

  private static final String RAW_TABLE = "raw_component";

  private static final String COLUMN_NAME = "normalized_version";

  private static final String MAVEN2_INDEX = "idx_maven2_normalized_version";

  private static final String RAW_INDEX = "idx_raw_normalized_version";

  private final String ADD_COLUMN_TO_MAVEN2 =
      format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s VARCHAR;", MAVEN2_TABLE, COLUMN_NAME);

  private final String ADD_COLUMN_TO_RAW =
      format("ALTER TABLE %s ADD COLUMN IF NOT EXISTS %s VARCHAR;", RAW_TABLE, COLUMN_NAME);

  private final String ADD_INDEX_TO_MAVEN2 =
      format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)", MAVEN2_INDEX, MAVEN2_TABLE.toUpperCase(), COLUMN_NAME);

  private final String ADD_INDEX_TO_RAW =
      format("CREATE INDEX IF NOT EXISTS %s ON %s (%s)", RAW_INDEX, RAW_TABLE.toUpperCase(), COLUMN_NAME);

  private final Continuation<ComponentData> EMPTY_CONTINUATION =
      new ContinuationArrayList<>();

  private final List<Format> formats = Arrays.asList(new MockFormat(MAVEN2_FORMAT), new MockFormat(RAW_FORMAT));

  @Mock
  private FormatStoreManager mavenStoreManager;

  @Mock
  private FormatStoreManager rawStoreManager;

  @Mock
  private ComponentStore mavenComponentStore;

  @Mock
  private ComponentStore rawComponentStore;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private UpgradeTaskScheduler startupScheduler;

  @Mock
  private Map<String, FormatStoreManager> managersByFormat;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private Connection connection;

  @Mock
  private Statement statement;

  @Mock
  private DatabaseMigrationUtility databaseMigrationUtility;

  private ComponentNormalizationIntegrityChecker checker;

  @Before
  public void setUp() throws SQLException {
    checker = new ComponentNormalizationIntegrityChecker(
        formats,
        taskScheduler,
        startupScheduler,
        managersByFormat,
        globalKeyValueStore,
        databaseMigrationUtility);

    when(connection.createStatement()).thenReturn(statement);

    when(managersByFormat.get(MAVEN2_FORMAT)).thenReturn(mavenStoreManager);
    when(mavenStoreManager.componentStore(DEFAULT_DATASTORE_NAME)).thenReturn(mavenComponentStore);
    when(managersByFormat.get(RAW_FORMAT)).thenReturn(rawStoreManager);
    when(rawStoreManager.componentStore(DEFAULT_DATASTORE_NAME)).thenReturn(rawComponentStore);
  }

  @Test
  public void checkAndRepairFailsIfTableIsMissing() throws SQLException {
    mockTableMissing();

    assertThrows(SQLException.class, () -> checker.checkAndRepair(connection));
  }

  @Test
  public void checkAndRepairAddsMissingColumnAndIndex() throws SQLException {
    mockTableExistsButColumnsNeedCreation();
    mockHasNormalizedData(mavenComponentStore);
    mockHasNormalizedData(rawComponentStore);

    checker.checkAndRepair(connection);

    verify(statement).execute(ADD_COLUMN_TO_MAVEN2);
    verify(statement).execute(ADD_INDEX_TO_MAVEN2);

    verify(statement).execute(ADD_COLUMN_TO_RAW);
    verify(statement).execute(ADD_INDEX_TO_RAW);
  }

  @Test
  public void checkAndRepairSchedulesNecessaryTasks() throws SQLException {
    mockTableAndColumnExists();
    mockHasNormalizedData(mavenComponentStore);
    mockHasUnnormalizedData(rawComponentStore);
    TaskConfiguration normalizedVersionTaskConfiguration = normalizedVersionTaskConfiguration();

    checker.checkAndRepair(connection);

    verify(statement, times(0)).execute(ADD_COLUMN_TO_MAVEN2);
    verify(statement, times(0)).execute(ADD_INDEX_TO_MAVEN2);
    verify(statement, times(0)).execute(ADD_COLUMN_TO_RAW);
    verify(statement, times(0)).execute(ADD_INDEX_TO_RAW);
    verify(globalKeyValueStore).setKey(normalizedVersionUnavailable(RAW_FORMAT));
    verify(startupScheduler).schedule(normalizedVersionTaskConfiguration);
  }

  private void mockTableExistsButColumnsNeedCreation() throws SQLException {
    when(databaseMigrationUtility.tableExists(connection, MAVEN2_TABLE.toUpperCase())).thenReturn(true);
    when(databaseMigrationUtility.tableExists(connection, RAW_TABLE.toUpperCase())).thenReturn(true);
    when(databaseMigrationUtility.columnExists(connection, MAVEN2_TABLE, COLUMN_NAME)).thenReturn(false);
    when(databaseMigrationUtility.columnExists(connection, RAW_TABLE, COLUMN_NAME)).thenReturn(false);
    when(databaseMigrationUtility.indexExists(connection, MAVEN2_INDEX)).thenReturn(false);
    when(databaseMigrationUtility.indexExists(connection, RAW_INDEX)).thenReturn(false);
  }

  private void mockTableMissing() throws SQLException {
    when(databaseMigrationUtility.tableExists(connection, MAVEN2_TABLE.toUpperCase())).thenReturn(false);
  }

  private void mockTableAndColumnExists() throws SQLException {
    when(databaseMigrationUtility.tableExists(connection, MAVEN2_TABLE.toUpperCase())).thenReturn(true);
    when(databaseMigrationUtility.tableExists(connection, RAW_TABLE.toUpperCase())).thenReturn(true);
    when(databaseMigrationUtility.columnExists(connection, MAVEN2_TABLE.toUpperCase(), COLUMN_NAME)).thenReturn(true);
    when(databaseMigrationUtility.columnExists(connection, RAW_TABLE.toUpperCase(), COLUMN_NAME)).thenReturn(true);
  }

  private void mockHasNormalizedData(ComponentStore componentStore) {
    when(componentStore.browseUnnormalized(1, null)).thenReturn(EMPTY_CONTINUATION);
  }

  private void mockHasUnnormalizedData(ComponentStore componentStore) {
    Continuation<ComponentData> continuationWithASingleElement = new ContinuationArrayList()
    {{
      add(new ComponentData());
    }};
    when(componentStore.browseUnnormalized(1, null)).thenReturn(continuationWithASingleElement);
  }

  private NexusKeyValue normalizedVersionUnavailable(final String formatName) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(formatName + ".normalized.version.available");
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(false);
    return kv;
  }

  private TaskConfiguration normalizedVersionTaskConfiguration() {
    TaskConfiguration config = new TaskConfiguration();
    when(taskScheduler.createTaskConfigurationInstance(NormalizeComponentVersionTaskDescriptor.TYPE_ID)).thenReturn(
        config);
    return config;
  }

  private static class MockFormat
      extends Format
  {
    public MockFormat(final String formatName) {
      super(formatName);
    }
  }
}