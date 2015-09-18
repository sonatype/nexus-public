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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Utility methods for managing {@link Guarded} and {@link Transitions} aspects with non-proxied instances.
 * 
 * @since 3.0
 */
public class StateGuardAspect
{
  static final MethodInterceptor GUARD = new GuardedInterceptor();

  static final MethodInterceptor TRANSITION = new TransitionsInterceptor();

  /**
   * Applies the appropriate {@link Guarded} and {@link Transitions} aspects around an existing non-proxied instance.
   */
  @SuppressWarnings("unchecked")
  public static <S, T extends S> S around(final T instance) {
    final Class<?> impl = instance.getClass();
    return (S) Proxy.newProxyInstance(impl.getClassLoader(), impl.getInterfaces(), new InvocationHandler()
    {
      public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Method implMethod = impl.getMethod(method.getName(), method.getParameterTypes());

        final boolean isGuarded = implMethod.isAnnotationPresent(Guarded.class);
        final boolean isTransition = implMethod.isAnnotationPresent(Transitions.class);

        final MethodInvocation invocation = new SimpleMethodInvocation(instance, implMethod, args);

        if (isGuarded && isTransition) {
          return GUARD.invoke(new SimpleMethodInvocation(instance, implMethod, args)
          {
            @Override
            public Object proceed() throws Throwable {
              return TRANSITION.invoke(invocation);
            }
          });
        }
        else if (isGuarded) {
          return GUARD.invoke(invocation);
        }
        else if (isTransition) {
          return TRANSITION.invoke(invocation);
        }

        return invocation.proceed();
      }
    });
  }
}
