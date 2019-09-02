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
package org.sonatype.nexus.datastore.mybatis.testsupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.DataAccess;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.api.DataStoreNotFoundException;
import org.sonatype.nexus.datastore.mybatis.MyBatisDataStore;

import org.apache.ibatis.type.TypeHandler;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.mybatis.MyBatisDataStoreDescriptor.JDBC_URL;

/**
 * JUnit rule to supply {@link DataSession}s without needing the full store ceremony.
 *
 * @since 3.next
 */
public class DataSessionRule
    extends ExternalResource
    implements DataSessionSupplier
{
  private static final Logger log = LoggerFactory.getLogger(DataSessionRule.class);

  private final Map<String, String> attributes = new HashMap<>();

  private final List<Class<? extends DataAccess>> configAccessTypes = new ArrayList<>();

  private final List<Class<? extends DataAccess>> contentAccessTypes = new ArrayList<>();

  private final List<TypeHandler<?>> typeHandlers = new ArrayList<>();

  private final Map<String, MyBatisDataStore> stores;

  /**
   * Supplies in-memory config sessions.
   */
  public DataSessionRule() {
    this(CONFIG_DATASTORE_NAME);
  }

  /**
   * Supplies in-memory sessions for the named stores.
   */
  public DataSessionRule(final String storeName, final String... storeNames) {
    stores = concat(of(storeName), stream(storeNames)).collect(toImmutableMap(identity(), this::newStore));

    attribute(JDBC_URL, "jdbc:h2:mem:${storeName}");
  }

  /**
   * Applies the given attribute to all sessions managed by this rule.
   */
  public DataSessionRule attribute(final String key, final String value) {
    attributes.put(key, value);
    return this;
  }

  /**
   * Registers the given DAO with the appropriate sessions managed by this rule.
   */
  public DataSessionRule access(final Class<? extends DataAccess> accessType) {
    if (ContentDataAccess.class.isAssignableFrom(accessType)) {
      contentAccessTypes.add(accessType);
    }
    else {
      configAccessTypes.add(accessType);
    }
    return this;
  }

  /**
   * Registers the given type handler with all sessions managed by this rule.
   */
  public DataSessionRule handle(final TypeHandler<?> typeHandler) {
    typeHandlers.add(typeHandler);
    return this;
  }

  @Override
  protected void before() {
    stores.forEach((storeName, store) -> {
      try {
        store.start();

        if (CONFIG_DATASTORE_NAME.equals(storeName)) {
          configAccessTypes.forEach(store::register);
        }
        else {
          contentAccessTypes.forEach(store::register);
        }

        typeHandlers.forEach(store::register);
      }
      catch (Exception e) {
        log.warn("Problem starting {}", storeName, e);
        throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    });
  }

  private MyBatisDataStore newStore(final String storeName) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(storeName);
    config.setSource("test");
    config.setType("jdbc");
    config.setAttributes(attributes);

    MyBatisDataStore store = new MyBatisDataStore();
    store.setConfiguration(config);
    return store;
  }

  @Override
  public DataSession<?> openSession(final String storeName) {
    return ofNullable(stores.get(storeName)).orElseThrow(() -> new DataStoreNotFoundException(storeName)).openSession();
  }

  @Override
  protected void after() {
    stores.values().forEach(t -> {
      try {
        t.stop();
      }
      catch (Exception e) {
        log.warn("Problem stopping {}", t, e);
      }
    });
  }
}
