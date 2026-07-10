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
package org.sonatype.nexus.testcommon.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.mock;

/**
 * A Junit 5 extension which sets up the validation. Use {@link ValidationExecutor} on a field to provide
 * a custom validation if required
 */
@Order(Integer.MAX_VALUE)
public class ValidationExtension
    implements Extension, BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback
{
  @Override
  public void afterAll(final ExtensionContext context) throws Exception {
    ValidationConfiguration.EXECUTABLE_VALIDATOR = null;
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {
    Field field = validationField(context);
    Method method = validationMethod(context);
    if (field != null && Modifier.isStatic(field.getModifiers())) {
      setExecutable(checkNotNull(field.get(null)));
    }
    else if (method != null && Modifier.isStatic(method.getModifiers())) {
      setExecutable(checkNotNull(method.invoke(null)));
    }
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    Field field = validationField(context);
    Method method = validationMethod(context);
    if ((field != null && !Modifier.isStatic(field.getModifiers()))
        || (method != null && !Modifier.isStatic(method.getModifiers()))) {
      ValidationConfiguration.EXECUTABLE_VALIDATOR = null;
    }
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    Field field = validationField(context);
    Method method = validationMethod(context);
    if (field != null && !Modifier.isStatic(field.getModifiers())) {
      setExecutable(checkNotNull(field.get(context.getRequiredTestInstance())));
    }
    else if (method != null && !Modifier.isStatic(method.getModifiers())) {
      setExecutable(checkNotNull(method.invoke(context.getRequiredTestInstance())));
    }
    else if (ValidationConfiguration.EXECUTABLE_VALIDATOR == null) {
      setExecutable(null);
    }
  }

  private static Field validationField(final ExtensionContext context) {
    Class<?> testClazz = context.getRequiredTestClass();

    for (Field field : FieldUtils.getAllFields(testClazz)) {
      ValidationExecutor executor = field.getAnnotation(ValidationExecutor.class);
      if (executor != null) {
        field.setAccessible(true);
        return field;
      }
    }
    return null;
  }

  private static Method validationMethod(final ExtensionContext context) {
    Class<?> testClazz = context.getRequiredTestClass();

    Method method = MethodUtils.getMethodsListWithAnnotation(testClazz, ValidationExecutor.class, true, true)
        .stream()
        .findFirst()
        .orElse(null);

    if (method != null) {
      method.setAccessible(true);
    }
    return method;
  }

  private static void setExecutable(final Object o) {
    if (o == null) {
      // ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      ValidationConfiguration.EXECUTABLE_VALIDATOR = mock(ExecutableValidator.class);
    }
    else if (o instanceof Validator validator) {
      ValidationConfiguration.EXECUTABLE_VALIDATOR = validator.forExecutables();
    }
    else {
      throw new UnsupportedOperationException();
    }
  }

  @Target({ElementType.FIELD, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ValidationExecutor
  {
    // nothing required
  }
}
