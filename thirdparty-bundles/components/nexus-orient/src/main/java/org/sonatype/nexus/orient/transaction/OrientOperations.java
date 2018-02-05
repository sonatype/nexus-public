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
package org.sonatype.nexus.orient.transaction;

import javax.annotation.Nullable;
import javax.inject.Provider;

import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.transaction.Operations;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.base.Supplier;

import static org.sonatype.nexus.orient.transaction.OrientTransaction.currentDb;

/**
 * Orient specific fluent API for wrapping lambda operations with {@link Transactional} behaviour
 *
 * @see Operations example usage
 *
 * @since 3.2
 */
public class OrientOperations<E extends Exception, B extends OrientOperations<E, B>>
    extends Operations<E, B>
{
  /**
   * Assumes the lambda may throw the given checked exception.
   */
  @Override
  public <X extends Exception> OrientOperations<X, ?> throwing(final Class<X> exceptionType) {
    return (OrientOperations<X, ?>) super.throwing(exceptionType);
  }

  /**
   * Uses the provided database to acquire {@link Transaction}s.
   */
  public final B withDb(final Provider<DatabaseInstance> db) {
    return super.withDb(() -> new OrientTransaction(db.get().acquire()));
  }

  /**
   * Calls the given function with {@link Transactional} behaviour.
   */
  public final <T> T call(final OrientFunction<T, E> function) throws E {
    return super.call(() -> function.apply(currentDb()));
  }

  /**
   * Calls the given consumer with {@link Transactional} behaviour.
   */
  public final void run(final OrientConsumer<E> consumer) throws E {
    super.run(() -> consumer.accept(currentDb()));
  }

  /**
   * Default settings.
   */
  protected OrientOperations() {
    // use superclass defaults
  }

  /**
   * Custom settings.
   */
  protected OrientOperations(final Transactional spec,
                             @Nullable final Class<E> throwing,
                             @Nullable final Supplier<? extends Transaction> db)
  {
    super(spec, throwing, db);
  }

  /**
   * Copies the given settings into a new fluent step.
   */
  @Override
  protected <X extends Exception> Operations<X, ?> copy(final Transactional spec,
                                                        @Nullable final Class<X> throwing,
                                                        @Nullable final Supplier<? extends Transaction> db)
  {
    return new OrientOperations<>(spec, throwing, db);
  }
}
