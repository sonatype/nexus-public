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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import javax.annotation.Nullable;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import org.aopalliance.intercept.Joinpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class that lets you wrap arbitrary sections of code as &#064;{@link Transactional}.
 *
 * <pre>
 * Operations.transactional(new Operation&lt;T, SomeException&gt;()
 * {
 *   &#064;Transactional(retryOn = IOException.class)
 *   public T call() throws SomeException {
 *     // ... do transactional work
 *   }
 * });
 * </pre>
 *
 * There's a builder API for situations where you can't annotate the code (such as lambdas):
 *
 * <pre>
 * Operations.transactional().retryOn(IOException.class).call(() -> {
 *   // do transactional work
 * });
 * </pre>
 *
 * or when you want to supply your own transactions instead of relying on {@link UnitOfWork}:
 *
 * <pre>
 * Operations.transactional(myTxSupplier).retryOn(IOException.class).call(() -> {
 *   // do transactional work
 * });
 * </pre>
 *
 * @since 3.0
 */
public final class Operations
{
  private static final Logger log = LoggerFactory.getLogger(Operations.class);

  private Operations() {
    // no instance
  }

  /**
   * Builds a {@link Transactional} specification that can be applied to {@link Operation}s.
   */
  public static TransactionalBuilder transactional() {
    return new TransactionalBuilder(null);
  }

  /**
   * Builds a {@link Transactional} specification that can be applied to {@link Operation}s;
   * uses the given supplier to acquire {@link Transaction}s.
   */
  public static TransactionalBuilder transactional(final Supplier<? extends Transaction> db) {
    return new TransactionalBuilder(checkNotNull(db));
  }

  /**
   * Applies the annotated {@link Transactional} behaviour to the given {@link Operation}.
   */
  public static <T, E extends Exception> T transactional(final Operation<T, E> operation)
      throws E
  {
    return transactional(operation, null, null);
  }

  /**
   * Applies the specified {@link Transactional} behaviour to the given {@link Operation}.
   */
  @SuppressWarnings("unchecked")
  static <T, E extends Exception> T transactional(final Operation<T, E> operation,
                                                  @Nullable final Transactional withSpec,
                                                  @Nullable final Supplier<? extends Transaction> db)
      throws E
  {
    final Method method;
    try {
      method = operation.getClass().getMethod("call");
    }
    catch (final NoSuchMethodException e) {
      throw new LinkageError("Problem intercepting 'call' method", e);
    }

    Transactional spec = withSpec;
    if (spec == null) {
      spec = method.getAnnotation(Transactional.class);
      if (spec == null) {
        spec = TransactionalBuilder.DEFAULT_SPEC;
      }
    }

    log.trace("Invoking: {} -> {}", spec, method);

    final UnitOfWork work = UnitOfWork.createWork();

    if (work.isActive()) {
      return operation.call(); // nested transaction, no need to wrap
    }

    try (final Transaction tx = work.acquireTransaction(db)) {
      return (T) new TransactionalWrapper(spec, new Joinpoint()
      {
        @Override
        public Object proceed() throws Throwable {
          return operation.call();
        }

        @Override
        public Object getThis() {
          return operation;
        }

        @Override
        public AccessibleObject getStaticPart() {
          return method;
        }
      }).proceedWithTransaction(tx);
    }
    catch (final Throwable e) {
      final Class<?>[] thrownExceptions = method.getExceptionTypes();
      if (thrownExceptions != null && thrownExceptions.length > 0) {
        Throwables.propagateIfPossible(e, (Class<E>) thrownExceptions[0]);
      }
      throw Throwables.propagate(e);
    }
    finally {
      work.releaseTransaction();
    }
  }
}
