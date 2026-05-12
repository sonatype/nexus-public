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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.transaction.UnitOfWork.openSession;
import static org.sonatype.nexus.transaction.UnitOfWork.peekTransaction;

@Aspect
public class TransactionAspect
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Around("@annotation(transactional) && execution(* *(..))")
  public Object invoke(final ProceedingJoinPoint mi, final Transactional transactional) throws Throwable {
    log.trace("Invoking: {}", mi);
    TransactionalStore<?> store = null;
    if (mi.getTarget() instanceof TransactionalStore<?>) {
      store = (TransactionalStore<?>) mi.getTarget();
    }

    Transaction tx = peekTransaction();
    if (tx != null) { // nested transactional session
      if (store != null) {
        tx.capture(store);
      }
      if (tx.isActive()) {
        return mi.proceed(); // no need to wrap active transaction
      }
      return proceedWithTransaction(mi, transactional, tx);
    }

    try (TransactionalSession<?> session = openSession(store, transactional.isolation())) {
      return proceedWithTransaction(mi, transactional, session.getTransaction());
    }
  }

  public static Object proceedWithTransaction(
      final ProceedingJoinPoint aspect,
      final Transactional spec,
      final Transaction tx) throws Throwable
  {
    return new TransactionalWrapper(spec, aspect).proceedWithTransaction(tx);
  }
}
