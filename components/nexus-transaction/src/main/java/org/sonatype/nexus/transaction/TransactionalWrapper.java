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

import org.sonatype.goodies.common.Loggers;

import org.aopalliance.intercept.Joinpoint;
import org.slf4j.Logger;

/**
 * Wraps an intercepted method with transactional behaviour.
 *
 * @since 3.0
 */
final class TransactionalWrapper
{
  private static final Logger log = Loggers.getLogger(TransactionalWrapper.class);

  private final Transactional spec;

  private final Joinpoint aspect;

  private final boolean tracing;

  public TransactionalWrapper(final Transactional spec, final Joinpoint aspect) {
    this.spec = spec;
    this.aspect = aspect;

    tracing = log.isTraceEnabled();
  }

  /**
   * Applies transactional behaviour around the method call, supports automatic retries.
   */
  public Object proceedWithTransaction(final Transaction tx) throws Throwable {
    while (true) {
      boolean committed = false;
      Throwable throwing = null;
      Object result = null;
      try {
        if (tracing) {
          log.trace("BEGIN {} : {}", tx, aspect.getStaticPart());
        }
        tx.begin();
        try {
          result = aspect.proceed();
          return result;
        }
        catch (final Throwable e) { // make sure we capture VM errors here (will be rethrown later)
          throwing = e;
        }
        finally {
          if (throwing == null || instanceOf(throwing, spec.commitOn())) {
            if (tracing) {
              log.trace("COMMIT {} : {}", tx, aspect.getStaticPart(), throwing);
            }
            tx.commit();
            committed = true;
          }
          if (throwing != null) {
            throw throwing;
          }
        }
      }
      catch (final Exception e) { // ignore VM errors as here as we don't rollback/retry on them
        if (!committed) {
          if (tracing) {
            log.trace("ROLLBACK {} : {}", tx, aspect.getStaticPart(), e);
          }
          tx.rollback();
          if (instanceOf(e, spec.retryOn()) && tx.allowRetry(e)) {
            if (tracing) {
              log.trace("RETRY {} : {}", tx, aspect.getStaticPart(), e);
            }
            continue;
          }
          // only want to swallow commit exceptions distinct from 'throwing'
          if (throwing != e && instanceOf(e, spec.swallow())) {
            if (tracing) {
              log.trace("SWALLOW {} : {}", tx, aspect.getStaticPart(), e);
            }
            if (throwing != null) {
              throw throwing;
            }
            return result;
          }
        }
        if (throwing != null && throwing != e) {
          e.addSuppressed(throwing);
        }
        throw e;
      }
    }
  }

  /**
   * @return {@code true} if the given throwable is an instance of one of the types.
   */
  private static boolean instanceOf(final Throwable throwable, final Class<?>... types) {
    final Throwable cause = throwable.getCause();
    for (final Class<?> t : types) {
      if (t.isInstance(throwable) || t.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }
}
