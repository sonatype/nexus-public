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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A Junit 5 extension which sets up the validation. Use {@link RegisterValidator} on a field or method to register
 * a custom validator if required
 */
@SuppressWarnings("rawtypes")
@Order(Integer.MAX_VALUE)
public class ValidationExtension
    implements Extension, BeforeEachCallback, AfterEachCallback
{
  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    ValidationConfiguration.EXECUTABLE_VALIDATOR = null;
  }

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    setExecutable(validators(context));
  }

  private static Collection<ConstraintValidator> validators(final ExtensionContext context) {
    return Stream
        .concat(fields(context, RegisterValidator.class, true).map(field -> getConstraintValidator(field, context)),
            methods(context, RegisterValidator.class, true).map(method -> getConstraintValidator(method, context)))
        .toList();
  }

  private static <T extends Annotation> Stream<Field> fields(
      final ExtensionContext context,
      final Class<T> clazz,
      final boolean includeStatic)
  {
    Class<?> testClazz = context.getRequiredTestClass();

    return Stream.of(FieldUtils.getAllFields(testClazz))
        .filter(field -> includeStatic || !Modifier.isStatic(field.getModifiers()))
        .filter(field -> field.getAnnotation(clazz) != null)
        .peek(field -> field.setAccessible(true));
  }

  private static <T extends Annotation> Stream<Method> methods(
      final ExtensionContext context,
      final Class<T> clazz,
      final boolean includeStatic)
  {
    Class<?> testClazz = context.getRequiredTestClass();

    return MethodUtils.getMethodsListWithAnnotation(testClazz, clazz, true, true)
        .stream()
        .filter(method -> includeStatic || !Modifier.isStatic(method.getModifiers()))
        .peek(method -> method.setAccessible(true));
  }

  private static void setExecutable(final Collection<ConstraintValidator> validators) {
    ValidationConfiguration.EXECUTABLE_VALIDATOR = new ValidationConfiguration()
        .validatorFactory(new TestConstraintValidatorFactory(validators))
        .getValidator()
        .forExecutables();
  }

  private static ConstraintValidator getConstraintValidator(final Method method, final ExtensionContext context) {
    try {
      return (ConstraintValidator) method.invoke(context.getTestInstance().orElse(null));
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static ConstraintValidator getConstraintValidator(final Field field, final ExtensionContext context) {
    try {
      return (ConstraintValidator) field.get(context.getTestInstance().orElse(null));
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static class TestConstraintValidatorFactory
      implements ConstraintValidatorFactory
  {
    private Map<Class<?>, ConstraintValidator> validators;

    TestConstraintValidatorFactory(final Collection<ConstraintValidator> validators) {
      this.validators = validators.stream()
          .collect(Collectors.toMap(Object::getClass, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
      ConstraintValidator<?, ?> validator = validators.get(key);
      if (validator != null) {
        return (T) validator;
      }
      return NewInstance.action(key, "ConstraintValidator").run();
    }

    @Override
    public void releaseInstance(final ConstraintValidator<?, ?> instance) {
      // noop
    }
  }

  @Target({ElementType.FIELD, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RegisterValidator
  {
    // nothing required
  }
}
