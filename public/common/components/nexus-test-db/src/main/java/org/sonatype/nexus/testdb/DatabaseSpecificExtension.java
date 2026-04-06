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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.mybatis.MyBatisDataStore;

import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * An internal JUnit5 extension which is used by {@link DatabaseExtension} when synthesizing database-specific tests
 */
class DatabaseSpecificExtension
    extends ComponentSupport
    implements BeforeEachCallback, AfterEachCallback
{
  private static final String REGEX_SCHEMA = "currentSchema=([^&]+)$";

  private static final Pattern SCHEMA = Pattern.compile(".*" + REGEX_SCHEMA);

  private final Map<String, MyBatisDataStore> stores = new HashMap<>();

  private final String jdbcUrl;

  DatabaseSpecificExtension(final String jdbcUrl) {
    this.jdbcUrl = checkNotNull(jdbcUrl);
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    Optional<Object> testObj = context.getTestInstance();
    if (testObj.isEmpty()) {
      return;
    }

    Object test = testObj.get();

    List<TypeHandler<?>> typeHandlers = typeHandlers(test);

    for (Field field : AnnotationSupport.findAnnotatedFields(test.getClass(), DataSessionConfiguration.class)) {
      addSession(test, field, typeHandlers);
    }
  }

  private void addSession(final Object test, final Field field, final List<TypeHandler<?>> typeHandlers) {
    try {
      DataSessionConfiguration dataSession = field.getAnnotation(DataSessionConfiguration.class);
      String storeName = DEFAULT_DATASTORE_NAME;
      MyBatisDataStore store = newStore(storeName, Map.of("jdbcUrl", jdbcUrl));

      store.start();

      for (TypeHandler<?> handler : typeHandlers) {
        store.register(handler);
      }

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

    dropSchema();
  }

  private void dropSchema() {
    if (jdbcUrl.indexOf("postgres") == -1) {
      return;
    }

    Matcher matcher = SCHEMA.matcher(jdbcUrl);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Unable to parse schema");
    }
    String name = matcher.group(1);

    try (Connection conn = DriverManager.getConnection(jdbcUrl.replaceAll("&" + REGEX_SCHEMA, ""));
        PreparedStatement statement = conn.prepareStatement("DROP SCHEMA " + name + " CASCADE")) {
      statement.execute();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<TypeHandler<?>> typeHandlers(final Object test) {
    List<TypeHandler<?>> typeHandlers = new ArrayList<>();

    try {
      for (Field field : AnnotationSupport.findAnnotatedFields(test.getClass(), TestTypeHandler.class)) {
        field.setAccessible(true);
        typeHandlers.add((TypeHandler<?>) field.get(test));
      }
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return typeHandlers;
  }

  private static MyBatisDataStore newStore(final String storeName, final Map<String, String> attributes) {
    Map<String, String> storeAttributes = new HashMap<>(attributes);
    storeAttributes.put("maximumPoolSize", "5");

    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(storeName);
    config.setSource("test");
    config.setType("jdbc");
    config.setAttributes(storeAttributes);

    MyBatisDataStore store = new MyBatisDataStore();
    store.setConfiguration(config);
    return store;
  }
}
