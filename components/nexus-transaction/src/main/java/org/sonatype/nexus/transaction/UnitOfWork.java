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

import java.util.function.Supplier;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.transaction.UnitOfWork.Scope.LOCAL_STORE;
import static org.sonatype.nexus.transaction.UnitOfWork.Scope.TRANSACTIONAL;
import static org.sonatype.nexus.transaction.UnitOfWork.Scope.UNIT_OF_WORK;

/**
 * Utility class that lets you scope a sequence of work containing transactional methods:
 *
 * <pre>
 * UnitOfWork.begin(sessionSupplier);
 * try {
 *   // ... do transactional work
 * }
 * finally {
 *   UnitOfWork.end();
 * }
 * </pre>
 *
 * If you want the same session to be re-used (i.e. batched) across the unit-of-work:
 *
 * <pre>
 * UnitOfWork.beginBatch(sessionSupplier);
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
    implements TransactionalSession<Transaction>
{
  private static final ThreadLocal<UnitOfWork> CURRENT_WORK = new ThreadLocal<>();

  enum Scope
  {
    TRANSACTIONAL, UNIT_OF_WORK, LOCAL_STORE
  }

  @Nullable
  private final UnitOfWork parent;

  private final TransactionalStore<?> store;

  private final Scope scope;

  @Nullable
  TransactionalSession<?> session;

  private UnitOfWork(final UnitOfWork parent, final TransactionalStore<?> store, final Scope scope) {
    this.parent = parent;
    this.store = checkNotNull(store);
    this.scope = checkNotNull(scope);
  }

  /**
   * Begins a unit-of-work which uses a new session for each {@link Transactional} operation.
   */
  public static void begin(final Supplier<? extends TransactionalSession<?>> db) {
    checkNotNull(db);
    doBegin(db::get, TRANSACTIONAL);
  }

  /**
   * Begins a unit-of-work which lazily uses the same session for each {@link Transactional} operation.
   */
  public static void beginBatch(final Supplier<? extends TransactionalSession<?>> db) {
    checkNotNull(db);
    doBegin(db::get, UNIT_OF_WORK);
  }

  /**
   * Begins a unit-of-work which eagerly uses a given session for each {@link Transactional} operation.
   */
  public static void beginBatch(final TransactionalSession<?> session) {
    checkNotNull(session);
    doBegin(() -> session, UNIT_OF_WORK);
    currentWork().session = session; // make sure session is immediately available
  }

  /**
   * Ends the current unit-of-work.
   */
  public static void end() {
    currentWork().doEnd();
  }

  /**
   * @return current session
   *
   * @throws IllegalStateException if no session exists for the current context
   *
   * @since 3.19
   */
  @SuppressWarnings("unchecked")
  public static <S extends TransactionalSession<?>> S currentSession() {
    TransactionalSession<?> session = currentWork().session;
    checkState(session != null, "No transactional session");
    return (S) session;
  }

  /**
   * @return current transaction
   *
   * @throws IllegalStateException if no transaction exists for the current context
   */
  @SuppressWarnings("unchecked")
  public static <T extends Transaction> T currentTx() {
    Transaction tx = currentWork().getTransaction();
    checkState(tx != null, "No transaction in progress");
    return (T) tx;
  }

  /**
   * Pauses current unit-of-work (if it exists) to avoid leaking context when sending events, etc.
   */
  @Nullable
  public static UnitOfWork pause() {
    UnitOfWork pausedWork = CURRENT_WORK.get();
    CURRENT_WORK.remove();
    return pausedWork;
  }

  /**
   * Resumes the previously paused unit-of-work (if it exists).
   */
  public static void resume(@Nullable final UnitOfWork pausedWork) {
    checkState(CURRENT_WORK.get() == null, "Cannot resume unit-of-work while other work is ongoing");
    if (pausedWork != null) {
      CURRENT_WORK.set(pausedWork);
    }
  }

  // -------------------------------------------------------------------------

  @Override
  public Transaction getTransaction() {
    // shortcut when session and transaction concerns are mixed (works better with mocking)
    if (session instanceof Transaction) {
      return (Transaction) session;
    }
    else if (session != null) {
      return session.getTransaction();
    }
    return null;
  }

  /**
   * Called when our wrapper session from {@link #doOpenSession(TransactionalStore)} is closed.
   */
  @Override
  public void close() {
    if (scope == LOCAL_STORE) {
      popWork(); // automatically pop any local work so it won't leak outside the store
    }

    if (scope != UNIT_OF_WORK) {
      doCloseSession(); // close the original session now our wrapper session is closed
    }

    // sessions with unit-of-work scope are left open and closed when that work ends
  }

  // -------------------------------------------------------------------------

  /**
   * Peeks at the current transaction; returns {@code null} if there isn't one.
   */
  @Nullable
  static Transaction peekTransaction() {
    UnitOfWork currentWork = CURRENT_WORK.get();
    return currentWork != null ? currentWork.getTransaction() : null;
  }

  /**
   * Opens a new session; from the local store if it exists or from the surrounding unit-of-work.
   */
  static TransactionalSession<?> openSession(@Nullable final TransactionalStore<?> localStore) {
    UnitOfWork currentWork = CURRENT_WORK.get();
    // introduce a short-lived unit-of-work when we need to track a locally sourced session
    if (localStore != null && (currentWork == null || currentWork.scope == UNIT_OF_WORK)) {
      currentWork = new UnitOfWork(currentWork, localStore, LOCAL_STORE);
      CURRENT_WORK.set(currentWork);
    }
    else {
      checkState(currentWork != null, "Unit of work has not been set");
    }
    return currentWork.doOpenSession(localStore);
  }

  // -------------------------------------------------------------------------

  private static UnitOfWork currentWork() {
    UnitOfWork currentWork = CURRENT_WORK.get();
    checkState(currentWork != null, "Unit of work has not been set");
    return currentWork;
  }

  private static void doBegin(final TransactionalStore<?> store, final Scope scope) {
    UnitOfWork parent = CURRENT_WORK.get();
    checkState(parent == null || parent.session == null,
        "Transaction in progress, pause current unit-of-work before beginning new work");
    CURRENT_WORK.set(new UnitOfWork(parent, store, scope));
  }

  /**
   * Opens a new session if one doesn't already exist.
   *
   * Returns this work as a wrapper session so {@link #doCloseSession()} is called when the client closes the session.
   */
  private TransactionalSession<?> doOpenSession(@Nullable final TransactionalStore<?> localStore) {
    if (session == null) {
      session = checkNotNull(localStore != null ? localStore.openSession() : store.openSession());
    }
    return this; // implicitly wraps the new session so we can intercept close
  }

  /**
   * Closes the current session if it exists.
   */
  private void doCloseSession() {
    if (session != null) {
      try {
        session.close();
      }
      finally {
        session = null;
      }
    }
  }

  private void doEnd() {
    if (scope != UNIT_OF_WORK) {
      checkState(session == null, "Cannot end unit-of-work while transaction in progress");
    }

    popWork(); // pop the work that was originally pushed when this unit-of-work began

    doCloseSession();
  }

  private void popWork() {
    if (parent != null) {
      CURRENT_WORK.set(parent);
    }
    else {
      CURRENT_WORK.remove();
    }
  }
}
