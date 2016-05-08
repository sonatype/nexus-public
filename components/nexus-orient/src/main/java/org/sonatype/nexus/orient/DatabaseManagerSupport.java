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

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.goodies.lifecycle.Lifecycles;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
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
  public static final String SYSTEM_USER = "admin";

  public static final String SYSTEM_PASSWORD = "admin";

  private static final String EXPLAIN_PREFIX = "org.sonatype.nexus.orient.explain.";

  private final Map<String,DatabasePoolImpl> pools = Maps.newHashMap();

  private final Map<String,DatabaseInstanceImpl> instances = Maps.newHashMap();

  @Override
  protected void doStart() throws Exception {
    checkState(pools.isEmpty());
    checkState(instances.isEmpty());
  }

  @Override
  protected void doStop() throws Exception {
    stopAllPools();
    stopAllInstances();
  }

  /**
   * Stop all instances and stop tracking.
   *
   * Usage is protected by lifecycle lock, and use of ensureStarted.
   */
  private void stopAllInstances() {
    if (instances.isEmpty()) {
      return;
    }

    log.info("Stopping {} instances", instances.size());

    Iterator<DatabaseInstanceImpl> iter = instances.values().iterator();
    while (iter.hasNext()) {
      DatabaseInstanceImpl instance = iter.next();

      if (instance.isStarted()) {
        log.info("Stopping instance: {}", instance.getName());
        try {
          instance.stop();
        }
        catch (Exception e) {
          log.warn("Failed to stop instance: {}", instance.getName(), e);
        }
      }
      else {
        log.info("Instance already stopped: {}", instance.getName());
      }

      iter.remove();
    }
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

    Iterator<DatabasePoolImpl> iter = pools.values().iterator();
    while (iter.hasNext()) {
      DatabasePoolImpl pool = iter.next();

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
          throw Throwables.propagate(e);
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
      DatabasePoolImpl pool = pools.get(name);
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

  private DatabasePoolImpl createPool(final String name) {
    // TODO: refine more control over how pool settings are configured per-database or globally

    String uri = connectionUri(name);
    OPartitionedDatabasePool underlying = new OPartitionedDatabasePool(uri, SYSTEM_USER, SYSTEM_PASSWORD, //
        25, // max connections per partition
        25); // max connections in the pool

    // TODO: Do not allow shared pool() to be closed by users, only by ourselves
    DatabasePoolImpl pool = new DatabasePoolImpl(underlying, name);
    Lifecycles.start(pool);
    return pool;
  }

  @Override
  public DatabaseInstance instance(final String name) {
    checkNotNull(name);
    ensureStarted();

    synchronized (instances) {
      DatabaseInstanceImpl instance = instances.get(name);
      if (instance == null) {
        instance = createInstance(name);
        log.debug("Created database instance: {}", instance);
        instances.put(name, instance);
      }
      return instance;
    }
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
    Lifecycles.start(instance);
    return instance;
  }
}
