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
package org.sonatype.nexus.datastore.mybatis;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.datastore.TransactionalStoreSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.security.PasswordHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.sonatype.nexus.common.metrics.MetricsConstants;

@ExtendWith(MockitoExtension.class)
class MyBatisDataStoreTest
{
  MockedStatic<SharedMetricRegistries> sharedMetricRegistries = mockStatic(SharedMetricRegistries.class);

  @TempDir
  File temporaryDirectory;

  @Mock
  MyBatisCipher databaseCipher;

  @Mock
  PasswordHelper passwordHelper;

  @Mock
  ApplicationDirectories directories;

  @Mock
  LogManager logManager;

  @Mock
  TransactionalStoreSupport<TestDAO> declaredAccessType;

  @Mock
  private MetricRegistry metricRegistry;

  @Mock
  private Connection connection;

  @Mock
  private Statement statement;

  @Mock
  private HikariDataSource dataSource;

  @Mock
  ApplicationContext context;

  final DataStoreConfiguration configuration = new DataStoreConfiguration();

  MyBatisDataStore underTest;

  @BeforeEach
  void setup() throws Exception {
    sharedMetricRegistries.when(() -> SharedMetricRegistries.getOrCreate(MetricsConstants.NEXUS_METRICS_REGISTRY_NAME))
        .thenReturn(metricRegistry);
    when(directories.getWorkDirectory(any(), anyBoolean())).thenReturn(temporaryDirectory);
    when(directories.getConfigDirectory(any())).thenReturn(new File("target/test-classes").getAbsoluteFile());
    when(declaredAccessType.getDaoClass()).thenReturn(TestDAO.class);
    underTest = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);

    configuration.setName("nexus");
    configuration.setAttributes(Map.of("jdbcUrl", "jdbc:h2:mem:${storeName}"));
    underTest.setConfiguration(configuration);
    underTest.start();
  }

  @AfterEach
  void teardown() throws Exception {
    underTest.stop();
    sharedMetricRegistries.close();
  }

  @Test
  void testStop_restart() throws Exception {
    // sanity check to ensure we actually registered the DAO
    verify(declaredAccessType).getDaoClass();
    assertDoesNotThrow(underTest::stop);

    assertDoesNotThrow(underTest::start, "Restart should not fail");
  }

  @Test
  void testH2DatabaseDetection() {
    // Test H2 database detection
    DataStoreConfiguration h2Config = new DataStoreConfiguration();
    h2Config.setName("test-h2");
    h2Config.setAttributes(Map.of("jdbcUrl", "jdbc:h2:file:/tmp/test"));

    MyBatisDataStore h2Store = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    h2Store.setConfiguration(h2Config);

    assertThat(h2Store.isH2Database(), is(true));
  }

  @Test
  void testH2HikariConfigHasNoMetricRegistry() throws Exception {
    // Test that H2 databases do NOT have a MetricRegistry configured and NO pool name set
    DataStoreConfiguration h2Config = new DataStoreConfiguration();
    h2Config.setName("test-h2");
    Map<String, String> attributes = Map.of("jdbcUrl", "jdbc:h2:mem:test");
    h2Config.setAttributes(attributes);

    MyBatisDataStore h2Store = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    h2Store.setConfiguration(h2Config);

    HikariConfig hikariConfig = h2Store.configureHikari("test-h2", attributes);

    assertNotNull(hikariConfig);
    assertThat("H2 should NOT have MetricRegistry configured",
        hikariConfig.getMetricRegistry(), nullValue());
    assertThat("HikariConfig should have a pool name configured",
        hikariConfig.getPoolName(), is("test_h2"));
  }

  @Test
  void testNonH2DatabaseSkipsShutdown() throws Exception {
    // Test non-H2 database
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    postgresConfig.setAttributes(Map.of("jdbcUrl", "jdbc:postgresql://localhost/test"));

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore.setConfiguration(postgresConfig);

    assertThat(postgresStore.isH2Database(), is(false));
  }

  @Test
  void testPostgresHikariConfigIncludesInitializationTimeout() throws Exception {
    // Test that PostgreSQL databases get the initialization timeout configured
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    Map<String, String> attributes = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/test");
    postgresConfig.setAttributes(attributes);

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = postgresStore.configureHikari("test-postgres", attributes);

    assertNotNull(hikariConfig);
    assertThat("PostgreSQL should have initializationFailTimeout configured",
        hikariConfig.getInitializationFailTimeout(), equalTo(10000L));
    assertThat("PostgreSQL should have maximumPoolSize configured",
        hikariConfig.getMaximumPoolSize(), equalTo(100));
    assertThat("HikariConfig should have MetricRegistry configured",
        hikariConfig.getMetricRegistry(), notNullValue());
    assertThat("HikariConfig should have a pool name configured",
        hikariConfig.getPoolName(), is("test_postgres"));
  }

  @Test
  void testPostgresHikariConfigAllowsTimeoutOverride() throws Exception {
    // Test that the initialization timeout can be overridden via advanced configuration
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    Map<String, String> attributes = Map.of(
        "jdbcUrl", "jdbc:postgresql://localhost:5432/test",
        "advanced", "initializationFailTimeout=120000");
    postgresConfig.setAttributes(attributes);

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = postgresStore.configureHikari("test-postgres", attributes);

    assertNotNull(hikariConfig);
    assertThat("Advanced config should override default initializationFailTimeout",
        hikariConfig.getInitializationFailTimeout(), equalTo(120000L));
  }

  @Test
  void testPostgresHikariConfigAppliesAdvancedDriverProperty() throws Exception {
    // Test that advanced properties which are NOT HikariConfig pool settings (e.g. JDBC
    // driver-level connection properties like loggerLevel) are routed to the DataSource
    // instead of being rejected by HikariConfig's Properties constructor (NEXUS-53610).
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    Map<String, String> attributes = Map.of(
        "jdbcUrl", "jdbc:postgresql://localhost:5432/test",
        "advanced", "loggerLevel=DEBUG");
    postgresConfig.setAttributes(attributes);

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = assertDoesNotThrow(() -> postgresStore.configureHikari("test-postgres", attributes),
        "Advanced JDBC driver properties should not be rejected by HikariConfig");

    assertThat("loggerLevel should be applied as a DataSource property",
        hikariConfig.getDataSourceProperties().getProperty("loggerLevel"), is("DEBUG"));
  }

  @Test
  void testPostgresHikariConfigAppliesBothPoolAndDriverAdvancedProperties() throws Exception {
    // Test that a mix of a pool-level override and a driver-level property in the same
    // advanced string are both applied, each to the correct destination.
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    Map<String, String> attributes = Map.of(
        "jdbcUrl", "jdbc:postgresql://localhost:5432/test",
        "advanced", "initializationFailTimeout=120000\nsocketTimeout=30000");
    postgresConfig.setAttributes(attributes);

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = postgresStore.configureHikari("test-postgres", attributes);

    assertThat("initializationFailTimeout should still be applied as a pool setting",
        hikariConfig.getInitializationFailTimeout(), equalTo(120000L));
    assertThat("socketTimeout should be applied as a DataSource property",
        hikariConfig.getDataSourceProperties().getProperty("socketTimeout"), is("30000"));
  }

  @Test
  void testMultiplePostgresDatastoresCleanupMetrics() throws Exception {
    // Test that creating multiple PostgreSQL datastores cleans up stale metrics
    DataStoreConfiguration postgresConfig1 = new DataStoreConfiguration();
    postgresConfig1.setName("test-postgres-1");
    Map<String, String> attributes1 = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/test1");
    postgresConfig1.setAttributes(attributes1);

    DataStoreConfiguration postgresConfig2 = new DataStoreConfiguration();
    postgresConfig2.setName("test-postgres-2");
    Map<String, String> attributes2 = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/test2");
    postgresConfig2.setAttributes(attributes2);

    MyBatisDataStore postgresStore1 = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore1.setConfiguration(postgresConfig1);

    MyBatisDataStore postgresStore2 = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    postgresStore2.setConfiguration(postgresConfig2);

    HikariConfig hikariConfig1 = postgresStore1.configureHikari("test-postgres-1", attributes1);
    HikariConfig hikariConfig2 = postgresStore2.configureHikari("test-postgres-2", attributes2);

    // Both should have unique pool names
    assertNotNull(hikariConfig1.getPoolName());
    assertNotNull(hikariConfig2.getPoolName());
    assertThat(hikariConfig1.getPoolName(), is("test_postgres_1"));
    assertThat(hikariConfig2.getPoolName(), is("test_postgres_2"));

    // Both should share the same metric registry
    assertThat(hikariConfig1.getMetricRegistry(), is(hikariConfig2.getMetricRegistry()));

    // Both should have MetricRegistry configured
    assertThat("HikariConfig 1 should have MetricRegistry configured",
        hikariConfig1.getMetricRegistry(), notNullValue());
    assertThat("HikariConfig 2 should have MetricRegistry configured",
        hikariConfig2.getMetricRegistry(), notNullValue());
  }

  @Test
  void testPostgresHikariConfigSkipsMetricsWhenDisabled() throws Exception {
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    Map<String, String> attributes = Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/test");
    postgresConfig.setAttributes(attributes);

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, false);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = postgresStore.configureHikari("test-postgres", attributes);

    assertNotNull(hikariConfig);
    assertThat("PostgreSQL with disabled internal metrics should NOT have MetricRegistry configured",
        hikariConfig.getMetricRegistry(), nullValue());
  }

  @Test
  void testPostgresDatastoreCleanupMetricsOnStop() throws Exception {
    // underTest (holder of configuration) is pre-configured as an H2 datastore

    // Test that stopping a PostgreSQL datastore cleans up its metrics
    configuration.setAttributes(Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/test"));

    underTest.doStop();

    // Verify the metrics prefix "nexus" was cleaned up
    // The actual metrics created by Hikari are named like "nexus.*"
    verify(metricRegistry).removeMatching(argThat(new MetricFilterMatcher("nexus.pool.")));

    // put data store back for the teardown method
    underTest.setDataSource(dataSource);
  }

  @Test
  void testPostgresDatastoreSkipsMetricsCleanupWhenDisabled() throws Exception {
    // Create a store with internalMetricsEnabled = false
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    postgresConfig.setAttributes(Map.of("jdbcUrl", "jdbc:postgresql://localhost:5432/test"));

    MyBatisDataStore disabledStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, false);
    disabledStore.setConfiguration(postgresConfig);
    disabledStore.setDataSource(dataSource);

    disabledStore.doStop();

    // Verify that cleanupPostgresqlMetrics was NOT called (no removeMatching interaction)
    verify(metricRegistry, never()).removeMatching(any());

    // put data store back for the teardown method
    disabledStore.setDataSource(dataSource);
  }

  @Test
  void testH2DatabaseDetection_withJdbcH2Mem() {
    DataStoreConfiguration h2Config = new DataStoreConfiguration();
    h2Config.setName("test");
    h2Config.setAttributes(Map.of("jdbcUrl", "jdbc:h2:mem:testdb"));

    MyBatisDataStore store = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    store.setConfiguration(h2Config);

    assertThat(store.isH2Database(), is(true));
  }

  @Test
  void testH2DatabaseDetection_withJdbcH2File() {
    DataStoreConfiguration h2Config = new DataStoreConfiguration();
    h2Config.setName("test");
    h2Config.setAttributes(Map.of("jdbcUrl", "jdbc:h2:file:/tmp/nexus"));

    MyBatisDataStore store = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    store.setConfiguration(h2Config);

    assertThat(store.isH2Database(), is(true));
  }

  @Test
  void testH2DatabaseDetection_withNullJdbcUrl() {
    DataStoreConfiguration h2Config = new DataStoreConfiguration();
    h2Config.setName("test");
    h2Config.setAttributes(Map.of()); // no jdbcUrl

    MyBatisDataStore store = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    store.setConfiguration(h2Config);

    assertThat(store.isH2Database(), is(false));
  }

  @Test
  void testH2DatabaseDetection_withMixedCaseJdbcUrl() {
    DataStoreConfiguration h2Config = new DataStoreConfiguration();
    h2Config.setName("test");
    h2Config.setAttributes(Map.of("jdbcUrl", "JDBC:H2:mem:testdb"));

    MyBatisDataStore store = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true, true);
    store.setConfiguration(h2Config);

    assertThat(store.isH2Database(), is(true));
  }

  @Test
  void testH2DatastoreExecutesShutdownOnStop() throws Exception {
    // underTest is pre-configured as an H2 datastore
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);

    // must call after start() otherwise it will get overwritten, meaning this can't be used to validate
    // usage in doStart() method
    underTest.setDataSource(dataSource);

    // Test that stopping an H2 datastore executes the SHUTDOWN command
    underTest.doStop();

    // Verify shutdown called
    verify(statement).execute("SHUTDOWN");

    // Verify the mock dataSource was closed
    verify(dataSource).close();

    // put data store back for the teardown method
    underTest.setDataSource(dataSource);
  }

  /**
   * Argument matcher for MetricFilter that checks if the filter matches
   * metric names starting with the expected prefix.
   */
  private static class MetricFilterMatcher
      implements ArgumentMatcher<MetricFilter>
  {
    private final String prefix;

    MetricFilterMatcher(final String prefix) {
      this.prefix = prefix;
    }

    @Override
    public boolean matches(final MetricFilter item) {
      // The MetricFilter.startsWith() returns a filter that matches names starting with the prefix
      // We test by checking a sample metric name
      String testMetricName = prefix + "Wait";
      return item.matches(testMetricName, null);
    }
  }
}
