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

import java.lang.reflect.Field;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.goodies.lifecycle.Lifecycles;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.storage.OStorage;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Scaffolding for {@link DatabasePool} implementations.
 *
 * @since 3.14
 */
public abstract class DatabasePoolSupport
  extends LifecycleSupport
  implements DatabasePool
{
  private final String name;

  public DatabasePoolSupport(final String name) {
    this.name = checkNotNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  // promote to public
  @Override
  public boolean isStarted() {
    return super.isStarted();
  }

  @Override
  public void close() {
    Lifecycles.stop(this);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        '}';
  }

  /**
   * Updates local pooled connections to use the given storage.
   *
   * @since 3.8
   *
   * @deprecated temporary workaround for https://www.prjhub.com/#/issues/9594
   */
  @Deprecated
  public abstract void replaceStorage(final OStorage storage);

  protected void replaceStorage(final OPartitionedDatabasePool pool, final OStorage storage) {
    if (partitionsField != null) {
      ODatabaseDocumentInternal originalDb = ODatabaseRecordThreadLocal.instance().getIfDefined();
      try {
        // use reflection as workaround until public API is available
        for (Object partition : (Object[]) partitionsField.get(pool)) {
          for (ODatabaseDocumentTx db : (Iterable<ODatabaseDocumentTx>) partitionQueueField.get(partition)) {
            replaceStorage(db, storage);
          }
        }
      }
      catch (Exception | LinkageError e) {
        log.warn("Problem replacing storage for {}", storage.getName(), e);
      }
      finally {
        ODatabaseRecordThreadLocal.instance().set(originalDb);
      }
    }
  }

  private void replaceStorage(ODatabaseDocumentTx db, final OStorage storage) {
    db.replaceStorage(storage);
    if (!db.isClosed()) {
      try {
        // reload metadata for active connections if old schema is gone
        if (db.getMetadata().getSchema().countClasses() == 0) {
          log.debug("Reloading metadata for {} as storage has changed", db.getName());
          db.activateOnCurrentThread();
          db.getMetadata().reload();
        }
      }
      catch (Exception e) {
        log.warn("Problem reloading metadata for {}", db.getName(), e);
      }
    }
  }

  private static final Field partitionsField;

  private static final Field partitionQueueField;

  /**
   * Introspect OPartitionedDatabasePool to get access to pooled connections and their metadata.
   */
  static {
    Field _partitionsField;
    Field _partitionQueueField;
    try {
      _partitionsField = OPartitionedDatabasePool.class.getDeclaredField("partitions");
      _partitionQueueField = _partitionsField.getType().getComponentType().getDeclaredField("queue");

      _partitionsField.setAccessible(true);
      _partitionQueueField.setAccessible(true);
    }
    catch (Exception | LinkageError e) {
      LoggerFactory.getLogger(DatabasePoolSupport.class).warn("Problem introspecting OPartitionedDatabasePool", e);

      _partitionsField = null;
      _partitionQueueField = null;
    }
    partitionsField = _partitionsField;
    partitionQueueField = _partitionQueueField;
  }
}
