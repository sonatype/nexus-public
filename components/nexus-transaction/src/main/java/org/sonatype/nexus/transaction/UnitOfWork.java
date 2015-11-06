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
package org.sonatype.nexus.transaction;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Utility class that lets you scope a sequence of work containing transactional methods:
 *
 * <pre>
 * UnitOfWork.begin(transactionSupplier);
 * try {
 *   // ... do transactional work
 * }
 * finally {
 *   UnitOfWork.end();
 * }
 * </pre>
 *
 * If you want the same transaction to be re-used (i.e. batched) across the unit-of-work:
 *
 * <pre>
 * UnitOfWork.beginBatch(transactionSupplier);
 * try {
 *   // ... do transactional work
 * }
 * finally {
 *   UnitOfWork.end();
 * }
 * </pre>
 *
 * When calling out of scope, such as sending events, you should pause the current work:
 *
 * <pre>
 * UnitOfWork work = UnitOfWork.pause();
 * try {
 *   // ... broadcast events, etc.
 * }
 * finally {
 *   UnitOfWork.resume(work);
 * }
 * </pre>
 *
 * @since 3.0
 */
public final class UnitOfWork
{
  private static final ThreadLocal<UnitOfWork> SELF = new ThreadLocal<>();

  private final Deque<Supplier<? extends Transaction>> dbHistory = new ArrayDeque<>();

  private Transaction tx;

  private UnitOfWork() {
    // internal use only
  }

  /**
   * Begins a unit-of-work which acquires a fresh transaction each time one is needed.
   */
  public static void begin(final Supplier<? extends Transaction> db) {
    checkNotNull(db);
    createWork().doBegin(db);
  }

  /**
   * Begins a unit-of-work which only acquires a transaction when needed and then re-uses it (batch-mode).
   */
  public static void beginBatch(final Supplier<? extends Transaction> db) {
    begin(Suppliers.compose(tx -> new BatchTransaction(tx), db));
  }

  /**
   * Begins a unit-of-work which immediately acquires the given transaction and re-uses it (batch-mode).
   */
  public static void beginBatch(final Transaction tx) {
    begin(Suppliers.ofInstance(new BatchTransaction(tx)));
    currentWork().acquireTransaction(null);
  }

  /**
   * @return current transaction
   *
   * @throws IllegalStateException if no transaction has been acquired for the current context
   */
  @SuppressWarnings("unchecked")
  public static <T extends Transaction> T currentTx() {
    Transaction tx = currentWork().tx;
    if (tx instanceof BatchTransaction) {
      tx = ((BatchTransaction) tx).delegate;
    }
    checkState(tx != null, "No transaction for current context");
    return (T) tx;
  }

  /**
   * Pauses current unit-of-work to avoid leaking context when sending events, etc.
   */
  public static UnitOfWork pause() {
    final UnitOfWork self = SELF.get();
    SELF.remove();
    return self;
  }

  /**
   * Resumes the given unit-of-work.
   */
  public static void resume(final UnitOfWork self) {
    checkState(SELF.get() == null, "Unit of work is already set");
    SELF.set(self);
  }

  /**
   * Ends the current unit-of-work.
   */
  public static void end() {
    currentWork().doEnd();
  }

  // -------------------------------------------------------------------------

  static UnitOfWork createWork() {
    UnitOfWork self = SELF.get();
    if (self == null) {
      self = new UnitOfWork();
      SELF.set(self);
    }
    return self;
  }

  static UnitOfWork currentWork() {
    final UnitOfWork self = SELF.get();
    checkState(self != null, "Unit of work has not been set");
    return self;
  }

  boolean isActive() {
    return tx != null && tx.isActive();
  }

  /**
   * Acquires transaction from given supplier; otherwise falls back to the current unit-of-work.
   */
  Transaction acquireTransaction(@Nullable final Supplier<? extends Transaction> customDb) {
    if (tx == null) {
      Supplier<? extends Transaction> db = customDb;
      if (db == null) {
        db = dbHistory.peek();
        checkState(db != null, "Unit of work has not been set");
      }
      tx = checkNotNull(db.get());
    }
    return tx;
  }

  void releaseTransaction() {
    if (!(tx instanceof BatchTransaction)) {
      tx = null;
      if (dbHistory.isEmpty()) {
        SELF.remove(); // avoid dangling thread-local
      }
    }
  }

  // -------------------------------------------------------------------------

  private void doBegin(final Supplier<? extends Transaction> db) {
    checkState(tx == null, "Transaction already in progress");
    dbHistory.push(db);
  }

  private void doEnd() {
    Throwable throwing = null;
    if (tx instanceof BatchTransaction) {
      checkState(!tx.isActive(), "Transaction still in progress");
      try {
        ((BatchTransaction) tx).closeBatch();
      }
      catch (final Throwable e) {
        throwing = e;
      }
      finally {
        tx = null;
      }
    }
    else {
      checkState(tx == null, "Transaction still in progress");
    }
    dbHistory.pop();
    if (dbHistory.isEmpty()) {
      SELF.remove(); // avoid dangling thread-local
    }
    if (throwing != null) {
      Throwables.propagate(throwing);
    }
  }
}
