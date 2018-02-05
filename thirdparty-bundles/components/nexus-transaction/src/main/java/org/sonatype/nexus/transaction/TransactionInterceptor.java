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
import java.lang.reflect.Method;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Supplier;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Opens a transaction when entering a transactional method and closes it on exit.
 * Nested transactional methods proceed as normal inside the current transaction.
 *
 * @since 3.0
 */
final class TransactionInterceptor
    extends ComponentSupport
    implements MethodInterceptor
{
  @Override
  public Object invoke(final MethodInvocation mi) throws Throwable {

    final Method method = mi.getMethod();
    final Transactional spec = findSpec(method);

    log.trace("Invoking: {} -> {}", spec, method);

    final UnitOfWork work = UnitOfWork.createWork();

    if (work.isActive()) {
      return mi.proceed(); // nested transaction, no need to wrap
    }

    Supplier<? extends Transaction> txSupplier = null;
    if (mi.getThis() instanceof TransactionalAware) {
      txSupplier = ((TransactionalAware) mi.getThis()).txSupplier();
    }

    try (final Transaction tx = work.acquireTransaction(txSupplier)) {
      return new TransactionalWrapper(spec, mi).proceedWithTransaction(tx);
    }
    finally {
      work.releaseTransaction();
    }
  }

  private static final Transactional findSpec(final Method method) {
    Transactional spec = method.getAnnotation(Transactional.class);
    if (spec != null) {
      return spec;
    }
    // look for stereotypes; annotations marked with @Transactional
    for (final Annotation ann : method.getDeclaredAnnotations()) {
      spec = ann.annotationType().getAnnotation(Transactional.class);
      if (spec != null) {
        return spec;
      }
    }
    throw new IllegalStateException("Missing @Transactional on: " + method);
  }
}
