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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Aspect
public class TransitionsAspect
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Around("@annotation(config) && execution(* *(..))")
  public Object aroundTransitionsMethod(
      final ProceedingJoinPoint joinPoint,
      final Transitions config) throws Throwable
  {
    checkNotNull(joinPoint);

    Object target = joinPoint.getTarget(); // Get the *target* object, not the proxy
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

    checkState(target instanceof StateGuardAware, "Invocation target (%s) does not implement: %s",
        target.getClass(), StateGuardAware.class);
    StateGuard states = ((StateGuardAware) target).getStateGuard();

    Transition transition =
        states.transition(config.to(), config.silent(), config.ignore(), config.requiresWriteLock());
    if (config.from() != null && config.from().length != 0) {
      transition = transition.from(config.from());
    }

    log.trace("Invoking: {} -> {}", transition, method);

    try {
      return transition.run(() -> {
        try {
          return joinPoint.proceed();
        }
        catch (Throwable t) {
          if (t instanceof Exception e) {
            throw e;
          }
          if (t instanceof Error e) {
            throw e;
          }
          throw new RuntimeException(t);
        }
      });
    }
    catch (InvocationTargetException e) {
      throw e.getCause();
    }
    catch (RuntimeException e) {
      if (e.getCause() != null) {
        throw e.getCause();
      }
      throw e;
    }
  }
}
