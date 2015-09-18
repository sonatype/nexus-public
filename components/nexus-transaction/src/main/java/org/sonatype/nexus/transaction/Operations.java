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

import com.google.common.base.Throwables;
import org.aopalliance.intercept.Joinpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that lets you wrap arbitrary sections of code as &#064;{@link Transactional}.
 *
 * <pre>
 * Operations.transactional(new Operation&lt;T, SomeException&gt;()
 * {
 *   &#064;Transactional
 *   public T call() throws SomeException {
 *     // ... do transactional work
 *   }
 * });
 * </pre>
 *
 * @since 3.0
 */
public final class Operations
{
  private static final Logger log = LoggerFactory.getLogger(Operations.class);

  /**
   * Applies {@link Transactional} behaviour to the given {@link Operation}.
   */
  @SuppressWarnings("unchecked")
  public static <T, E extends Exception> T transactional(final Operation<T, E> operation) throws E {
    final UnitOfWork work = UnitOfWork.self();

    if (work.isActive()) {
      return operation.call(); // nested transaction, no need to wrap
    }

    Class<?>[] thrownExceptions = null;
    try (final Transaction tx = work.acquireTransaction()) {

      final Method method = operation.getClass().getMethod("call");
      final Transactional spec = method.getAnnotation(Transactional.class);
      thrownExceptions = method.getExceptionTypes();

      log.trace("Invoking: {} -> {}", spec, method);

      return (T) new TransactionalWrapper(spec, new Joinpoint()
      {
        public Object proceed() throws Throwable {
          return operation.call();
        }

        public Object getThis() {
          return operation;
        }

        public AccessibleObject getStaticPart() {
          return method;
        }
      }).proceedWithTransaction(tx);
    }
    catch (final Throwable e) {
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
