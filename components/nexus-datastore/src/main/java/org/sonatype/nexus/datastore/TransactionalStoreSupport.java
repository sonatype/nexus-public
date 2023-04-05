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
package org.sonatype.nexus.datastore;

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.TransactionIsolation;
import org.sonatype.nexus.transaction.TransactionalStore;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class TransactionalStoreSupport
    extends StateGuardLifecycleSupport
    implements TransactionalStore<DataSession<?>>
{
  protected final DataSessionSupplier sessionSupplier;

  private final String storeName;

  protected TransactionalStoreSupport(final DataSessionSupplier sessionSupplier, final String storeName) {
    this.sessionSupplier = checkNotNull(sessionSupplier);
    this.storeName = checkNotNull(storeName);
  }

  @Override
  public DataSession<?> openSession() {
    return sessionSupplier.openSession(storeName);
  }

  @Override
  public DataSession<?> openSession(final TransactionIsolation isolationLevel) {
    switch (isolationLevel) {
      case SERIALIZABLE:
        log.debug("Opening session with serializable transaction isolation");
        return sessionSupplier.openSerializableTransactionSession(storeName);
      default:
        return sessionSupplier.openSession(storeName);
    }
  }
}
