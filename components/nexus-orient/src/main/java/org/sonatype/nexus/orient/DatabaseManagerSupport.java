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
package org.sonatype.nexus.orient;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.goodies.lifecycle.Lifecycles;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.storage.OStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support for {@link DatabaseManager} implementations.
 *
 * @since 3.0
 */
public abstract class DatabaseManagerSupport
    extends LifecycleSupport
    implements DatabaseManager
{
  private static final int DEFAULT_MAX_CONNECTIONS_PER_CORE = 16;

  private static final int DEFAULT_MAX_CONNECTIONS = -1;

  private static final String MAX_CONNECTIONS_PER_CORE_PROPERTY = "nexus.orient.maxConnectionsPerCore";

  private static final String MAX_CONNECTIONS_PROPERTY = "nexus.orient.maxConnections";

  private static final String MAX_CONNECTIONS_PER_CORE_PLACEHOLDER =
      "${" + MAX_CONNECTIONS_PER_CORE_PROPERTY + ":-" + DEFAULT_MAX_CONNECTIONS_PER_CORE + "}";

  private static final String MAX_CONNECTIONS_PLACEHOLDER =
      "${" + MAX_CONNECTIONS_PROPERTY + ":-" + DEFAULT_MAX_CONNECTIONS + "}";

  public static final String SYSTEM_USER = "admin";

  public static final String SYSTEM_PASSWORD = "admin";

  private static final String EXPLAIN_PREFIX = "org.sonatype.nexus.orient.explain.";

  private final Map<String,DatabasePoolSupport> pools = Maps.newHashMap();

  private final Map<String,DatabaseInstanceImpl> instances = Maps.newConcurrentMap();

  private int maxConnectionsPerCore = DEFAULT_MAX_CONNECTIONS_PER_CORE;

  private int maxConnections = DEFAULT_MAX_CONNECTIONS;

  @Inject
  public void setMaxConnectionsPerCore(@Named(MAX_CONNECTIONS_PER_CORE_PLACEHOLDER) final int maxConnectionsPerCore) {
    this.maxConnectionsPerCore = maxConnectionsPerCore;
  }

  @Inject
  public void setMaxConnections(@Named(MAX_CONNECTIONS_PLACEHOLDER) final int maxConnections) {
    this.maxConnections = maxConnections;
  }

  @Override
  protected void doStart() throws Exception {
    // relax instances check; may not be empty if we are bouncing the lifecycle
    checkState(pools.isEmpty());
  }

  @Override
  protected void doStop() throws Exception {
    // release pools for cleanup, but keep instance wrappers so we can re-use them
    instances.values().forEach(DatabaseInstanceImpl::releasePool);
    stopAllPools();
  }

  /**
   * Stop all pools and stop tracking.
   *
   * Usage is protected by lifecycle lock, and use of ensureStarted.
   */
  private void stopAllPools() {
    if (pools.isEmpty()) {
      return;
    }

    log.info("Stopping {} pools", pools.size());

    Iterator<DatabasePoolSupport> iter = pools.values().iterator();
    while (iter.hasNext()) {
      DatabasePoolSupport pool = iter.next();

      if (pool.isStarted()) {
        log.info("Stopping pool: {}", pool.getName());
        try {
          pool.stop();
        }
        catch (Exception e) {
          log.warn("Failed to stop pool: {}", pool.getName(), e);
        }
      }
      else {
        log.info("Pool already stopped: {}", pool.getName());
      }

      iter.remove();
    }
  }

  /**
   * Returns the OrientDB connection URI string for the given database name.
   */
  protected abstract String connectionUri(final String name);

  @Override
  public ODatabaseDocumentTx connect(final String name, final boolean create) {
    checkNotNull(name);
    ensureStarted();

    String uri = connectionUri(name);
    ODatabaseDocumentTx db = new ODatabaseDocumentTx(uri);

    if (db.exists()) {
      db.open(SYSTEM_USER, SYSTEM_PASSWORD);
      log.debug("Opened database: {} -> {}", name, db);
    }
    else {
      if (create) {
        db.create();
        log.debug("Created database: {} -> {}", name, db);

        // invoke created callback
        try {
          created(db, name);
        }
        catch (Exception e) {
          Throwables.throwIfUnchecked(e);
          throw new RuntimeException(e);
        }
      }
      else {
        log.debug("Database does not exist: {}", name);
      }
    }

    return db;
  }

  /**
   * Callback invoked when database is being created.
   */
  protected void created(final ODatabaseDocumentTx db, final String name) throws Exception {
    // nop
  }

  /**
   * Exposing impl type here for sub-classes.
   */
  @Override
  public DatabaseExternalizerImpl externalizer(final String name) {
    checkNotNull(name);
    ensureStarted();

    return new DatabaseExternalizerImpl(this, name);
  }

  @Override
  public DatabasePool pool(final String name) {
    checkNotNull(name);
    ensureStarted();

    synchronized (pools) {
      DatabasePoolSupport pool = pools.get(name);
      if (pool == null) {
        pool = createPool(name);
        log.debug("Created database pool: {}", pool);
        pools.put(name, pool);
      }
      return pool;
    }
  }

  @Override
  public DatabasePool newPool(final String name) {
    checkNotNull(name);
    ensureStarted();

    // TODO: Track non-shared pools so that we can attempt to shut them down if users didn't properly do this?
    // TODO: ... or at the very least complain if this happens?
    return createPool(name);
  }

  private DatabasePoolSupport createPool(final String name) {
    String uri = connectionUri(name);

    // primary pool for this DB which may have a per-core limit or an overall limit
    OPartitionedDatabasePool underlying = new OPartitionedDatabasePool(
        uri, SYSTEM_USER, SYSTEM_PASSWORD, maxConnectionsPerCore, maxConnections);

    DatabasePoolSupport pool;

    if (maxConnections > 0) {
      // when there's an overall limit Orient will use a single partition
      // and block any pending requests when there's no connections left
      log.info("Configuring OrientDB pool {} with overall limit of {}", name, maxConnections);
      pool = new DatabasePoolImpl(underlying, name);
    }
    else if (maxConnectionsPerCore > 0) {
      // otherwise Orient uses a partition per-core which throws an ISE when
      // there are no connections left in the partition - to fall back to the
      // original blocking behaviour we add an overflow with an overall limit
      // the same as the per-core limit
      OPartitionedDatabasePool overflow = new OPartitionedDatabasePool(
          uri, SYSTEM_USER, SYSTEM_PASSWORD, -1 /* unused */, maxConnectionsPerCore);

      log.info("Configuring OrientDB pool {} with per-core limit of {}", name, maxConnectionsPerCore);
      pool = new DatabasePoolWithOverflowImpl(underlying, overflow, name);
    }
    else {
      throw new IllegalArgumentException(
          "Either " + MAX_CONNECTIONS_PER_CORE_PROPERTY +
              " or " + MAX_CONNECTIONS_PROPERTY + " must be positive");
    }

    Lifecycles.start(pool);

    return pool;
  }

  @Override
  public DatabaseInstance instance(final String name) {
    checkNotNull(name);
    ensureStarted();

    return instances.computeIfAbsent(name, this::createInstance);
  }

  private DatabaseInstanceImpl createInstance(final String name) {
    DatabaseInstanceImpl instance;
    final Logger explainLogger = LoggerFactory.getLogger(EXPLAIN_PREFIX + name);
    if (explainLogger.isDebugEnabled()) {
      instance = new DatabaseInstanceImpl(this, name)
      {
        @Override
        public ODatabaseDocumentTx acquire() {
          String uri = connectionUri(name);
          ODatabaseDocumentTx db = new ExplainODatabaseDocumentTx(uri, explainLogger);
          db.open(SYSTEM_USER, SYSTEM_PASSWORD);
          return db;
        }
      };
    }
    else {
      instance = new DatabaseInstanceImpl(this, name);
    }

    // ensure the database is created
    connect(name, true).close();

    log.debug("Created database instance: {}", instance);
    return instance;
  }

  @Override
  public void replaceStorage(final OStorage storage) {
    DatabasePoolSupport pool;
    synchronized (pools) {
      pool = pools.get(storage.getName());
    }
    if (pool != null) {
      pool.replaceStorage(storage);
    }
  }
}
