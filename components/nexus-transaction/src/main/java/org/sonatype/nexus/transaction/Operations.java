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

import java.lang.annotation.Annotation;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fluent API for wrapping lambda operations with {@link Transactional} behaviour:
 *
 * <pre>
 * Value value = Transactional.operation.retryOn(IOException.class).call(() -> {
 *   // do transactional work which returns a value
 * });
 *
 * Transactional.operation.retryOn(IOException.class).run(() -> {
 *   // do transactional work which doesn't return a value
 * });
 * </pre>
 *
 * You can choose to supply your own transactions instead of relying on {@link UnitOfWork}:
 *
 * <pre>
 * Transactional.operation.withDb(myTxSupplier).retryOn(IOException.class).call(() -> {
 *   // do transactional work
 * });
 * </pre>
 *
 * If your lambda throws a checked exception then you need to explicitly declare this:
 *
 * <pre>
 * Transactional.operation.throwing(PersistenceException.class).call(() -> {
 *   // do transactional work
 * });
 * </pre>
 *
 * You can also use stereotype annotations meta-annotated with {@link Transactional}:
 *
 * <pre>
 * Transactional.operation.stereotype(TransactionalGet.class).call(() -> {
 *   // do transactional work
 * });
 * </pre>
 *
 * The result of each fluent step (except call and run) can be cached and re-used.
 *
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class Operations<E extends Exception, B extends Operations<E, B>>
{
  private static final Logger log = LoggerFactory.getLogger(Operations.class);

  private static final Class<?>[] NOTHING = {};

  @VisibleForTesting
  static final Transactional DEFAULT_SPEC = new TransactionalImpl(NOTHING, NOTHING, NOTHING);

  @VisibleForTesting
  final Transactional spec;

  @Nullable
  private final Class<E> throwing;

  @Nullable
  private final Supplier<? extends Transaction> db;

  /**
   * @see Transactional#commitOn()
   */
  @SafeVarargs
  public final B commitOn(final Class<? extends Exception>... exceptionTypes) {
    Class<?>[] commitOn = deepCheckNotNull(exceptionTypes).clone();
    return (B) copy(new TransactionalImpl(commitOn, spec.retryOn(), spec.swallow()), throwing, db);
  }

  /**
   * @see Transactional#retryOn()
   */
  @SafeVarargs
  public final B retryOn(final Class<? extends Exception>... exceptionTypes) {
    Class<?>[] retryOn = deepCheckNotNull(exceptionTypes).clone();
    return (B) copy(new TransactionalImpl(spec.commitOn(), retryOn, spec.swallow()), throwing, db);
  }

  /**
   * @see Transactional#swallow()
   */
  @SafeVarargs
  public final B swallow(final Class<? extends Exception>... exceptionTypes) {
    Class<?>[] swallow = deepCheckNotNull(exceptionTypes).clone();
    return (B) copy(new TransactionalImpl(spec.commitOn(), spec.retryOn(), swallow), throwing, db);
  }

  /**
   * Applies the given stereotype annotation (meta-annotated with &#064;{@link Transactional}).
   *
   * @since 3.2
   */
  public final B stereotype(final Class<? extends Annotation> annotationType) {
    Transactional metaSpec = annotationType.getAnnotation(Transactional.class);
    checkArgument(metaSpec != null, "Stereotype annotation is not meta-annotated with @Transactional");
    return (B) copy(metaSpec, throwing, db);
  }

  /**
   * Assumes the lambda may throw the given checked exception.
   */
  public <X extends Exception> Operations<X, ?> throwing(final Class<X> exceptionType) {
    return copy(spec, checkNotNull(exceptionType), db);
  }

  /**
   * Uses the given supplier to acquire {@link Transaction}s.
   *
   * @since 3.2
   */
  public final B withDb(final Supplier<? extends Transaction> txSupplier) {
    return (B) copy(spec, throwing, checkNotNull(txSupplier));
  }

  /**
   * Calls the given operation with {@link Transactional} behaviour.
   */
  public final <T> T call(final Operation<T, E> operation) throws E {
    return transactional(new OperationPoint<>(operation));
  }

  /**
   * Runs the given operation with {@link Transactional} behaviour.
   *
   * @since 3.2
   */
  public final void run(final VoidOperation<E> operation) throws E {
    transactional(new OperationPoint<>(operation));
  }

  /**
   * Default settings.
   */
  protected Operations() {
    this(DEFAULT_SPEC, null, null);
  }

  /**
   * Custom settings.
   */
  protected Operations(final Transactional spec,
                       @Nullable final Class<E> throwing,
                       @Nullable final Supplier<? extends Transaction> db)
  {
    this.spec = checkNotNull(spec);
    this.throwing = throwing;
    this.db = db;
  }

  /**
   * Copies the given settings into a new fluent step.
   */
  protected <X extends Exception> Operations<X, ?> copy(final Transactional spec,
                                                        @Nullable final Class<X> throwing,
                                                        @Nullable final Supplier<? extends Transaction> db)
  {
    return new Operations<>(spec, throwing, db);
  }

  /**
   * Invokes the given {@link OperationPoint} using the current settings.
   */
  private <T> T transactional(final OperationPoint<T, E> point) throws E {
    log.trace("Invoking: {} -> {}", spec, point);

    final UnitOfWork work = UnitOfWork.createWork();

    if (work.isActive()) {
      return point.proceed(); // nested transaction, no need to wrap
    }

    try (final Transaction tx = work.acquireTransaction(db)) {
      return (T) new TransactionalWrapper(spec, point).proceedWithTransaction(tx);
    }
    catch (final Throwable e) {
      if (throwing != null) {
        Throwables.propagateIfPossible(e, throwing);
      }
      throw Throwables.propagate(e);
    }
    finally {
      work.releaseTransaction();
    }
  }

  /**
   * Checks that the given array and its elements are not null.
   */
  private static <T> T[] deepCheckNotNull(final T[] elements) {
    for (T e : elements) {
      checkNotNull(e);
    }
    return elements;
  }
}
