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
package org.sonatype.nexus.testdb;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.mybatis.MyBatisDataStore;

import org.apache.ibatis.type.TypeHandler;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getBoolean;

/**
 * <p>
 * JUnit 5 extension to inject an instance of {@code DataSessionSupplier} into tests.
 * </p>
 *
 * <p>
 * Annotate the test class with {@code @ExtendWith(DatabaseExtension.class)}
 * </p>
 *
 * <p>
 * Create a field and annotate it, e.g.
 * </p>
 *
 * <pre>
 * &#64;DataSessionConfiguration(MyDAO.class)
 * TestDataSessionSupplier dataSessionSupplier;
 *
 * &#64;TestTable("my_table")
 * Table table;
 * </pre>
 */
public class DatabaseExtension
    implements Extension, BeforeEachCallback, BeforeAllCallback, AfterAllCallback, AfterEachCallback, ParameterResolver
{
  private static final Logger log = LoggerFactory.getLogger(DatabaseExtension.class);

  private static final String JDBC_URL = System.getProperty("test.jdbcUrl");

  private static final boolean TEST_POSTGRES = getBoolean("test.postgres", false);

  private final Map<String, MyBatisDataStore> stores = new HashMap<>();

  private PostgreSQLContainer<?> postgres;

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    if (JDBC_URL == null && TEST_POSTGRES) {
      startPostgres();
    }
  }

  @Override
  public void afterAll(final ExtensionContext context) throws Exception {
    stopPostgres();
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    Optional<Object> testObj = context.getTestInstance();
    if (testObj.isEmpty()) {
      return;
    }

    Object test = testObj.get();

    for (Field field : AnnotationSupport.findAnnotatedFields(test.getClass(), DataSessionConfiguration.class)) {
      addSession(test, field, context);
    }

    AnnotationSupport.findAnnotatedFields(test.getClass(), TestTable.class).forEach(field -> {
      try {
        TestTable testTable = field.getAnnotation(TestTable.class);
        field.setAccessible(true);
        field.set(test, new Table(store(testTable.storeName()).getDataSource(), testTable.table()));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void addSession(final Object test, final Field field, final ExtensionContext context) {
    try {
      DataSessionConfiguration dataSession = field.getAnnotation(DataSessionConfiguration.class);
      String storeName = dataSession.storeName();
      MyBatisDataStore store =
          newStore(storeName, Map.of("jdbcUrl", discoverJdbcUrl(context.getTestMethod().get().getName())));

      store.start();

      for (Class<? extends TypeHandler<?>> handler : dataSession.typeHandlers()) {
        store.register(ReflectionSupport.newInstance(handler));
      }
      Stream.of(dataSession.daos()).forEach(store::register);
      stores.put(storeName, store);

      field.setAccessible(true);
      field.set(test, new TestDataSessionSupplier(storeName, store));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    for (MyBatisDataStore store : stores.values()) {
      try {
        store.stop();
      }
      catch (Exception e) {
        log.error("Failed to stop store", e);
      }
    }
    stores.clear();
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext,
      final ExtensionContext extensionContext) throws ParameterResolutionException
  {
    // We don't inject DAOs because they won't auto-commit transactions.
    if (match(Table.class, parameterContext)) {
      TestTable testTable = parameterContext.getAnnotatedElement().getAnnotation(TestTable.class);

      return new Table(store(testTable.storeName()).getDataSource(), testTable.table());
    }
    throw new IllegalArgumentException();
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext,
      final ExtensionContext extensionContext) throws ParameterResolutionException
  {
    return match(Table.class, parameterContext) && parameterContext.isAnnotated(TestTable.class);
  }

  private static boolean match(final Class<?> clazz, final ParameterContext parameterContext) {
    return clazz.isAssignableFrom(parameterContext.getParameter().getType());
  }

  /**
   * Discover the JDBC URL to run the tests against.
   */
  private String discoverJdbcUrl(@Nullable final String testname) {
    if (JDBC_URL != null) {
      return JDBC_URL;
    }
    else if (TEST_POSTGRES) {
      createSchema(testname);
      return getPostgresqlUrl(testname);
    }
    return "jdbc:h2:mem:${storeName}";
  }

  private void startPostgres() {
    // Nexus support policy is we support versions of PostgreSQL which are currently supported
    // https://www.postgresql.org/support/versioning/
    // 14 is the minimum support version as of November 2025
    // switch to alpine images due to policy violations
    postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("sonatype.repo.sonatype.app/docker-all/postgres:15.15-alpine")
            .asCompatibleSubstituteFor("postgres"));

    postgres.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("postgres")))
        .withCommand("postgres", "-c",
            "max_connections=110");
    postgres.start();

    postgres.withUrlParam("user", "test").withUrlParam("password", "test");

    log.info("Started postgres {}", postgres.getJdbcUrl());
  }

  private void stopPostgres() {
    if (postgres != null) {
      postgres.close();
      postgres = null;
    }
  }

  private void createSchema(@Nullable final String name) {
    if (name == null) {
      return;
    }

    try (Connection conn = DriverManager.getConnection(getPostgresqlUrl(null));
        PreparedStatement statement = conn.prepareStatement("CREATE SCHEMA " + name)) {
      statement.execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private String getPostgresqlUrl(@Nullable final String schema) {
    if (schema == null) {
      return postgres.getJdbcUrl();
    }
    return postgres.getJdbcUrl() + "&currentSchema=" + schema;
  }

  private MyBatisDataStore store(final String name) {
    return checkNotNull(stores.get(name));
  }

  private static MyBatisDataStore newStore(final String storeName, final Map<String, String> attributes) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(storeName);
    config.setSource("test");
    config.setType("jdbc");
    config.setAttributes(attributes);

    MyBatisDataStore store = new MyBatisDataStore();
    store.setConfiguration(config);
    return store;
  }
}
