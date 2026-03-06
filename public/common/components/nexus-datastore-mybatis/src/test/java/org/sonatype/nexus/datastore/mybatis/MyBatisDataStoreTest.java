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
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.datastore.TransactionalStoreSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.security.PasswordHelper;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisDataStoreTest
    extends Test5Support
{
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
  ApplicationContext context;

  final DataStoreConfiguration configuration = new DataStoreConfiguration();

  MyBatisDataStore underTest;

  @BeforeEach
  void setup() throws Exception {
    when(directories.getWorkDirectory(any(), anyBoolean())).thenReturn(temporaryDirectory);
    when(directories.getConfigDirectory(any())).thenReturn(new File("target/test-classes").getAbsoluteFile());
    when(declaredAccessType.getDaoClass()).thenReturn(TestDAO.class);
    underTest = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true);

    configuration.setName("nexus");
    configuration.setAttributes(Map.of("jdbcUrl", "jdbc:h2:mem:${storeName}"));
    underTest.setConfiguration(configuration);
    underTest.start();
  }

  @AfterEach
  void teardown() throws Exception {
    underTest.stop();
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
        List.of(declaredAccessType), List.of(), true);
    h2Store.setConfiguration(h2Config);

    assertThat(h2Store.isH2Database(), is(true));
  }

  @Test
  void testNonH2DatabaseSkipsShutdown() throws Exception {
    // Test non-H2 database
    DataStoreConfiguration postgresConfig = new DataStoreConfiguration();
    postgresConfig.setName("test-postgres");
    postgresConfig.setAttributes(Map.of("jdbcUrl", "jdbc:postgresql://localhost/test"));

    MyBatisDataStore postgresStore = new MyBatisDataStore(databaseCipher, passwordHelper, directories, logManager,
        List.of(declaredAccessType), List.of(), true);
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
        List.of(declaredAccessType), List.of(), true);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = postgresStore.configureHikari("test-postgres", attributes);

    assertNotNull(hikariConfig);
    assertThat("PostgreSQL should have initializationFailTimeout configured",
        hikariConfig.getInitializationFailTimeout(), equalTo(10000L));
    assertThat("PostgreSQL should have maximumPoolSize configured",
        hikariConfig.getMaximumPoolSize(), equalTo(100));
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
        List.of(declaredAccessType), List.of(), true);
    postgresStore.setConfiguration(postgresConfig);

    HikariConfig hikariConfig = postgresStore.configureHikari("test-postgres", attributes);

    assertNotNull(hikariConfig);
    assertThat("Advanced config should override default initializationFailTimeout",
        hikariConfig.getInitializationFailTimeout(), equalTo(120000L));
  }
}
