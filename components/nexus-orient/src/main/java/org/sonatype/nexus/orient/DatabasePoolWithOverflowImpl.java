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

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.storage.OStorage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link DatabasePool} implementation with separate overflow pool.
 *
 * Use this when the main pool is configured with a maximum partition size rather than a maximum pool size.
 *
 * In that scenario a thread may receive an {@link IllegalStateException} when its partition reaches its limit,
 * at which point we redirect the request to the overflow pool. The overflow should set a maximum pool size so
 * it will block when no connections are currently available.
 *
 * This lets us use a partitioned pool while keeping the overall blocking nature of a non-partitioned pool.
 *
 * @since 3.14
 */
public class DatabasePoolWithOverflowImpl
  extends DatabasePoolSupport
{
  private final OPartitionedDatabasePool delegate;

  private final OPartitionedDatabasePool overflow;

  public DatabasePoolWithOverflowImpl(final OPartitionedDatabasePool delegate,
                                      final OPartitionedDatabasePool overflow,
                                      final String name)
  {
    super(name);
    this.delegate = checkNotNull(delegate);
    this.overflow = checkNotNull(overflow);
  }

  @Override
  protected void doStop() throws Exception {
    try {
      delegate.close();
    }
    finally {
      overflow.close();
    }
  }

  @Override
  public ODatabaseDocumentTx acquire() {
    ensureStarted();
    try {
      return delegate.acquire();
    }
    catch (IllegalStateException e) {
      log.debug("Unable to acquire connection from main pool, trying overflow", e);
      return overflow.acquire();
    }
  }

  @Override
  public int getAvailableCount() {
    return delegate.getAvailableConnections() + overflow.getAvailableConnections();
  }

  @Override
  public int getPoolSize() {
    return delegate.getCreatedInstances() + overflow.getCreatedInstances();
  }

  @Override
  public void replaceStorage(final OStorage storage) {
    try {
      replaceStorage(delegate, storage);
    }
    finally {
      replaceStorage(overflow, storage);
    }
  }
}
