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

import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.transaction.Operations;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.base.Supplier;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.tx.OTransaction;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Transaction} backed by an OrientDB connection.
 *
 * @since 3.0
 */
public class OrientTransaction
    extends ComponentSupport
    implements Transaction
{
  private static final int MAX_RETRIES = 8;

  private final ODatabaseDocumentTx db;

  private int retries = 0;

  public OrientTransaction(final ODatabaseDocumentTx db) {
    this.db = checkNotNull(db);
  }

  public ODatabaseDocumentTx getDb() {
    return db;
  }

  /**
   * @return {@link Transaction} supplier for the given {@link DatabaseInstance}.
   */
  public static Supplier<? extends Transaction> txSupplier(final DatabaseInstance db) {
    return () -> new OrientTransaction(db.acquire());
  }

  /**
   * @return current OrientDB connection.
   *
   * @throws IllegalArgumentException if no connection exists in the current context
   */
  public static ODatabaseDocumentTx currentDb() {
    final Transaction tx = UnitOfWork.currentTx();
    if (tx instanceof OrientTransaction) {
      return ((OrientTransaction) tx).db;
    }
    try {
      // support alternative formats which just need to provide a 'getDb' method
      return (ODatabaseDocumentTx) tx.getClass().getMethod("getDb").invoke(tx);
    }
    catch (final Exception e) {
      throw new IllegalArgumentException("Transaction " + tx + " has no public 'getDb' method", e);
    }
  }

  /**
   * Executes the operation in the context of an {@link OrientTransaction}.
   *
   * @since 3.1
   */
  public static void inTxNoReturn(final Provider<DatabaseInstance> databaseInstance,
                                  final Consumer<ODatabaseDocumentTx> operation)
  {
    inTx(databaseInstance, db -> {
      operation.accept(db);
      return (Void) null;
    });
  }

  /**
   * Executes the operation in the context of an {@link OrientTransaction} and returns a result.
   *
   * @return the result of the operation
   *
   * @since 3.1
   */
  public static <T> T inTx(final Provider<DatabaseInstance> databaseInstance,
                           final Function<ODatabaseDocumentTx, T> operation)
  {
    return Operations.transactional(txSupplier(databaseInstance.get()))
        .retryOn(ONeedRetryException.class)
        .call(() -> operation.apply(currentDb()));
  }

  @Override
  public void begin() {
    db.begin();
  }

  @Override
  public void commit() {
    db.commit();
    retries = 0;
  }

  @Override
  public void rollback() {
    db.rollback();
  }

  @Override
  public void close() {
    db.close();
  }

  @Override
  public boolean isActive() {
    OTransaction tx = null;
    if (db.isActiveOnCurrentThread()) {
      tx = db.getTransaction();
    }
    return tx != null && tx.isActive();
  }

  @Override
  public boolean allowRetry(final Exception cause) {
    if (retries < MAX_RETRIES) {
      retries++;
      log.debug("Retrying operation: {}/{}", retries, MAX_RETRIES);
      return true;
    }
    log.warn("Reached max retries: {}/{}", retries, MAX_RETRIES);
    return false;
  }
}
