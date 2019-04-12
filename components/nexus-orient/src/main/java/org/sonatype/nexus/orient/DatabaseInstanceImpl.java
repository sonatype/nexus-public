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

import org.sonatype.goodies.common.ComponentSupport;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link DatabaseInstance} implementation.
 *
 * This is a thin re-usable wrapper around {@link DatabaseManager} and the latest {@link DatabasePool}.
 *
 * @since 3.0
 */
public class DatabaseInstanceImpl
  extends ComponentSupport
  implements DatabaseInstance
{
  private final DatabaseManager databaseManager;

  private final String name;

  private volatile DatabasePool pool;

  public DatabaseInstanceImpl(final DatabaseManager databaseManager, final String name) {
    this.databaseManager = checkNotNull(databaseManager);
    this.name = checkNotNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ODatabaseDocumentTx connect() {
    return databaseManager.connect(name, false);
  }

  @Override
  public ODatabaseDocumentTx acquire() {
    return pool().acquire();
  }

  @Override
  public DatabaseExternalizer externalizer() {
    return databaseManager.externalizer(name);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        '}';
  }

  @Override
  public void setFrozen(final boolean frozen) {
    try (ODatabaseDocumentTx db = connect()) {
      if (frozen) {
        db.freeze(true);
      }
      else {
        db.release();
      }
    }
  }

  @Override
  public boolean isFrozen() {
    try (ODatabaseDocumentTx db = connect()) {
      return db.isFrozen();
    }
  }

  /**
   * @since 3.16
   */
  void releasePool() {
    pool = null;
  }

  private DatabasePool pool() {
    if (pool == null) {
      synchronized (this) {
        if (pool == null) {
          pool = databaseManager.pool(name);
        }
      }
    }
    return pool;
  }
}
