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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.slf4j.LoggerFactory.getLogger;

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
 * &#64;DatabaseTest
 * void testMyDAO() {
 * }
 * </pre>
 */
public class DatabaseExtension
    implements Extension, BeforeAllCallback, AfterAllCallback, TestTemplateInvocationContextProvider
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String JDBC_URL = System.getProperty("test.jdbcUrl");

  private PostgreSQLContainer<?> postgres;

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    if (findDataSessionConfiguration(context).postgresql()) {
      startPostgres();
    }
  }

  @Override
  public void afterAll(final ExtensionContext context) throws Exception {
    stopPostgres();
  }

  @Override
  public boolean supportsTestTemplate(final ExtensionContext context) {
    return context.getTestMethod().isPresent();
  }

  @Override
  public boolean mayReturnZeroTestTemplateInvocationContexts(final ExtensionContext context) {
    return true;
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
      final ExtensionContext extensionContext)
  {
    DataSessionConfiguration session = findDataSessionConfiguration(extensionContext);
    DatabaseTest databaseTest = findDatabaseTest(extensionContext);
    String displayName = extensionContext.getDisplayName();
    Map<String, String> databaseToUrl = new HashMap<>();

    if (session.h2() && databaseTest.h2()) {
      databaseToUrl.put("[h2] " + displayName, "jdbc:h2:mem:${storeName}");
    }
    if (session.postgresql() && databaseTest.postgresql()) {
      String schemaName = displayName.replaceAll("[^a-zA-Z0-9_]", "");
      createSchema(schemaName);
      databaseToUrl.put("[postgres] " + displayName,
          Optional.ofNullable(JDBC_URL).orElse(getPostgresqlUrl(schemaName)));
    }

    return databaseToUrl.entrySet()
        .stream()
        .map(entry -> new DatabaseTestTemplateInvocationContext(entry.getKey(), entry.getValue()));
  }

  static DataSessionConfiguration findDataSessionConfiguration(final ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    while (testClass != null && testClass != Object.class) {
      for (Field field : testClass.getDeclaredFields()) {
        if (field.isAnnotationPresent(DataSessionConfiguration.class)) {
          return field.getAnnotation(DataSessionConfiguration.class);
        }
      }
      testClass = testClass.getSuperclass();
    }
    throw new IllegalStateException("No @DataSessionConfiguration found");
  }

  static DatabaseTest findDatabaseTest(final ExtensionContext context) {
    return AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), DatabaseTest.class)
        .orElseThrow();
  }

  private void startPostgres() {
    // Nexus support policy is we support versions of PostgreSQL which are currently supported
    // https://www.postgresql.org/support/versioning/
    // 14 is the minimum support version as of November 2025
    // switch to alpine images due to policy violations
    postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("sonatype.repo.sonatype.app/docker-all/postgres:15.15-alpine")
            .asCompatibleSubstituteFor("postgres"));

    postgres.withLogConsumer(new Slf4jLogConsumer(getLogger("postgres")))
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

  private static class DatabaseTestTemplateInvocationContext
      implements TestTemplateInvocationContext
  {
    private final String testName;

    private final String jdbcUrl;

    DatabaseTestTemplateInvocationContext(final String testName, final String jdbcUrl) {
      this.testName = testName;
      this.jdbcUrl = jdbcUrl;
    }

    @Override
    public String getDisplayName(final int invocationIndex) {
      return testName;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      return List.of(new DatabaseSpecificExtension(jdbcUrl));
    }
  }
}
