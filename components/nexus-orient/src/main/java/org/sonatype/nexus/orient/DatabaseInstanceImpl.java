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

import org.sonatype.goodies.lifecycle.LifecycleSupport;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link DatabaseInstance} implementation.
 *
 * @since 3.0
 */
public class DatabaseInstanceImpl
  extends LifecycleSupport
  implements DatabaseInstance
{
  private final DatabaseManager databaseManager;

  private final String name;

  private final DatabasePool pool;

  public DatabaseInstanceImpl(final DatabaseManager databaseManager, final String name) {
    this.databaseManager = checkNotNull(databaseManager);
    this.name = checkNotNull(name);
    this.pool = databaseManager.pool(name);
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
  protected void doStart() throws Exception {
    // ensure the database is created
    databaseManager.connect(name, true).close();
  }

  @Override
  public ODatabaseDocumentTx connect() {
    ensureStarted();

    return databaseManager.connect(name, false);
  }

  @Override
  public ODatabaseDocumentTx acquire() {
    ensureStarted();

    return pool.acquire();
  }

  @Override
  public DatabaseExternalizer externalizer() {
    ensureStarted();

    return databaseManager.externalizer(name);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        '}';
  }
}
