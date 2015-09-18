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
package org.sonatype.nexus.validation.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.executable.ExecutableValidator;

import org.sonatype.nexus.validation.Validate;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.validator.HibernateValidator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link MethodInterceptor} that validates method arguments and return values.
 * 
 * @since 3.0
 */
public class ValidationInterceptor
    implements MethodInterceptor
{
  @Inject
  private ExecutableValidator methodValidator;

  public Object invoke(final MethodInvocation mi) throws Throwable {
    checkNotNull(methodValidator);

    final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(HibernateValidator.class.getClassLoader());
      final Validate validate = mi.getMethod().getAnnotation(Validate.class);

      validateParameters(mi.getThis(), mi.getMethod(), mi.getArguments(), validate.groups());

      final Object result = mi.proceed();

      validateReturnValue(mi.getThis(), mi.getMethod(), result, validate.groups());

      return result;
    }
    finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  private void validateParameters(final Object obj, final Method method, final Object[] args, final Class<?>[] groups) {
    final Set<ConstraintViolation<Object>> violations = methodValidator.validateParameters(obj, method, args, groups);
    if (!violations.isEmpty()) {
      final String message = "Invalid arguments calling '" + method + "' with " + Arrays.deepToString(args);
      throw new ConstraintViolationException(message, violations);
    }
  }

  private void validateReturnValue(final Object obj, final Method method, final Object value, final Class<?>[] groups) {
    final Set<ConstraintViolation<Object>> violations = methodValidator.validateReturnValue(obj, method, value, groups);
    if (!violations.isEmpty()) {
      final String message = "Invalid value returned by '" + method + "' was " + value;
      throw new ConstraintViolationException(message, violations);
    }
  }
}
