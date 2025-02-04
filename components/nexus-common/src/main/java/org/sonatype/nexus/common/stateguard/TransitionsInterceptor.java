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
package org.sonatype.nexus.common.stateguard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sonatype.goodies.common.ComponentSupport;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@link Transitions} interceptor.
 *
 * @since 3.0
 */
public class TransitionsInterceptor
    extends ComponentSupport
    implements MethodInterceptor
{
  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    checkNotNull(invocation);

    Object target = invocation.getThis();
    Method method = invocation.getMethod();

    checkState(target instanceof StateGuardAware, "Invocation target (%s) does not implement: %s",
        target.getClass(), StateGuardAware.class);
    StateGuard states = ((StateGuardAware) target).getStateGuard();
    checkState(states != null);

    Transitions config = method.getAnnotation(Transitions.class);
    checkState(config != null);
    Transition transition =
        states.transition(config.to(), config.silent(), config.ignore(), config.requiresWriteLock());
    if (config.from() != null && config.from().length != 0) {
      transition = transition.from(config.from());
    }

    log.trace("Invoking: {} -> {}", transition, method);

    try {
      return transition.run(new MethodInvocationAction(invocation));
    }
    catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }
}
