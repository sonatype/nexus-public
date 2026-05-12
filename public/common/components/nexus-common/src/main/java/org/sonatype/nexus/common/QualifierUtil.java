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
package org.sonatype.nexus.common;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

public final class QualifierUtil
{
  private static final Logger LOG = LoggerFactory.getLogger(QualifierUtil.class);

  /**
   * Get the value from a class annotated with {@link @Qualifier}
   */
  public static Optional<String> value(final Object obj) {
    Optional<String> result = Optional.ofNullable(obj)
        .map(Object::getClass)
        .map(clazz -> clazz.getDeclaredAnnotation(Qualifier.class))
        .map(Qualifier::value);

    if (obj != null && result.isEmpty()) {
      // if no @Qualifier annotation, check if we are dealing with a mocked object
      result = getMockedQualifier(obj);
    }

    return result;
  }

  private static Optional<String> getMockedQualifier(final Object obj) {
    try {
      // Instead of getting mockito libs onto the runtime classpath,
      // I opted to use reflection to get the necessary String
      Optional<String> mockName = getFieldByReflection(obj, "mockitoInterceptor")
          .flatMap(mockitoInterceptor -> getFieldByReflection(mockitoInterceptor, "mockCreationSettings"))
          .flatMap(mockCreationSettings -> getFieldByReflection(mockCreationSettings, "name", String.class));
      LOG.debug("Found mocked object with name: {} will be returned as the Qualifier value", mockName);
      return mockName;
    }
    catch (Exception e) {
      LOG.trace("Failed to lookup `mockitoInterceptor` field on class: {}. Likely meaning this is not a mocked object.",
          obj, e);
    }

    return Optional.empty();
  }

  private static Optional<Object> getFieldByReflection(final Object obj, final String fieldName) {
    return getFieldByReflection(obj, fieldName, Object.class);
  }

  private static <T> Optional<T> getFieldByReflection(
      final Object obj,
      final String fieldName,
      final Class<T> fieldType)
  {
    try {
      Field field = obj.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return Optional.of(fieldType.cast(field.get(obj)));
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      LOG.trace("Failed to get field {} from object {}.", fieldName, obj, e);
    }

    return Optional.empty();
  }

  public static String description(final Object obj) {
    return Optional.ofNullable(obj)
        .map(Object::getClass)
        .map(clazz -> clazz.getDeclaredAnnotation(Description.class))
        .map(Description::value)
        .orElseGet(() -> value(obj).orElse(null));
  }

  public static int compareByOrder(final Object o1, final Object o2) {
    int priority1 = getOrder(o1.getClass());
    int priority2 = getOrder(o2.getClass());
    return Integer.compare(priority1, priority2);
  }

  private static int getOrder(final Class<?> clazz) {
    Order priorityAnnotation = AnnotationUtils.getAnnotation(clazz, Order.class);
    // 0 is lowest priority, default value
    return priorityAnnotation != null ? priorityAnnotation.value() : 0;
  }

  /**
   * Builds map with keys as the @Qualifier annotation value.
   * The map preserves order based on @Order annotation (lower values first).
   *
   * @param listOfDeps The list of dependencies provided by DI.
   * @param <T> The type of the values in the map.
   * @return A new LinkedHashMap with keys as the @Qualifier value if present, ordered by @Order.
   */
  public static <T> Map<String, T> buildQualifierBeanMap(final List<T> listOfDeps) {
    if (listOfDeps == null) {
      return null;
    }
    return listOfDeps
        .stream()
        .sorted(QualifierUtil::compareByOrder)
        .collect(Collectors.toMap(
            dep -> value(dep).orElse(dep.getClass().toString()),
            Function.identity(),
            (existing, replacement) -> existing,
            LinkedHashMap::new));
  }

  private QualifierUtil() {
    // private
  }
}
