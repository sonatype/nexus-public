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

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.storage.OStorage;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link DatabasePool} implementation.
 *
 * @since 3.0
 */
public class DatabasePoolImpl
  extends LifecycleSupport
  implements DatabasePool
{
  private final String name;

  private final OPartitionedDatabasePool delegate;

  public DatabasePoolImpl(final OPartitionedDatabasePool pool, final String name) {
    this.delegate = checkNotNull(pool);
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
  protected void doStop() throws Exception {
    delegate.close();
  }

  @Override
  public ODatabaseDocumentTx acquire() {
    ensureStarted();

    return delegate.acquire();
  }

  @Override
  public void close() {
    Lifecycles.stop(this);
  }

  /**
   * Updates local pooled connections to use the given storage.
   *
   * @since 3.8
   *
   * @deprecated temporary workaround for https://www.prjhub.com/#/issues/9594
   */
  @Deprecated
  public void replaceStorage(final OStorage storage) {
    if (partitionsField != null) {
      try {
        // use reflection as workaround until public API is available
        for (Object partition : (Object[]) partitionsField.get(delegate)) {
          for (ODatabaseDocumentTx db : (Iterable<ODatabaseDocumentTx>) partitionQueueField.get(partition)) {
            db.replaceStorage(storage);
            // need to bypass 'open-ness' check in getMetadata()
            Object metadata = databaseMetadataField.get(db);
            if (metadata instanceof OMetadata) {
              ((OMetadata) metadata).reload();
            }
          }
        }
      }
      catch (Exception | LinkageError e) {
        log.warn("Problem replacing storage for {}", storage.getName(), e);
      }
    }
  }

  private static final Field partitionsField;

  private static final Field partitionQueueField;

  private static final Field databaseMetadataField;

  /**
   * Introspect OPartitionedDatabasePool to get access to pooled connections and their metadata.
   */
  static {
    Field _partitionsField;
    Field _partitionQueueField;
    Field _databaseMetadataField;
    try {
      _partitionsField = OPartitionedDatabasePool.class.getDeclaredField("partitions");
      _partitionQueueField = _partitionsField.getType().getComponentType().getDeclaredField("queue");
      _databaseMetadataField = ODatabaseDocumentTx.class.getDeclaredField("metadata");

      _partitionsField.setAccessible(true);
      _partitionQueueField.setAccessible(true);
      _databaseMetadataField.setAccessible(true);
    }
    catch (Exception | LinkageError e) {
      LoggerFactory.getLogger(DatabasePoolImpl.class).warn("Problem introspecting OPartitionedDatabasePool", e);

      _partitionsField = null;
      _partitionQueueField = null;
      _databaseMetadataField = null;
    }
    partitionsField = _partitionsField;
    partitionQueueField = _partitionQueueField;
    databaseMetadataField = _databaseMetadataField;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        '}';
  }
}
