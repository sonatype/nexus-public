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
package org.sonatype.nexus.jmx.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.management.Descriptor;
import javax.management.DescriptorKey;
import javax.management.ImmutableDescriptor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to generate {@link Descriptor} instances.
 *
 * @since 3.0
 */
public class DescriptorHelper
{
  private static final Logger log = LoggerFactory.getLogger(DescriptorHelper.class);

  /**
   * Build descriptor for given type.
   */
  public static Descriptor build(final Class<?> type) {
    checkNotNull(type);
    log.trace("Building descriptor for type: {}", type);
    return build(type.getAnnotations());
  }

  /**
   * Build descriptor for given method.
   */
  public static Descriptor build(final Method method) {
    checkNotNull(method);
    log.trace("Building descriptor for method: {}", method);
    return build(method.getAnnotations());
  }

  /**
   * Build descriptor for given annotations.
   */
  public static Descriptor build(final Annotation... annotations) {
    checkNotNull(annotations);
    log.trace("Building descriptor for annotations: {}", Arrays.asList(annotations));

    Map<String, Object> fields = Maps.newTreeMap();

    // TODO: consider caching which annotations actually have keys, perhaps even which methods
    // TODO: ... to avoid rescanning the list over and over?

    // find all DescriptorKey annotations
    for (Annotation annotation : findAllAnnotations(annotations)) {
      log.trace("Scanning annotation: {}", annotation);

      for (Method method : annotation.annotationType().getMethods()) {
        log.trace("Scanning method: {}", method);

        DescriptorKey key = method.getAnnotation(DescriptorKey.class);
        if (key == null) {
          continue;
        }
        log.trace("Found key: {}", key);

        // extract name and value for key
        String name = key.value();
        Object value = null;
        try {
          value = method.invoke(annotation);
        }
        catch (Exception e) {
          Throwables.propagate(e);
        }

        // skip if there is no value
        if (value == null) {
          continue;
        }

        // convert types as described by DescriptorKey javadocs
        if (value instanceof Class) {
          // class constant
          value = ((Class) value).getCanonicalName();
        }
        else if (value instanceof Enum) {
          // enum constant
          value = ((Enum) value).name();
        }
        else if (value.getClass().isArray()) {
          Class<?> componentType = value.getClass().getComponentType();
          if (Class.class.equals(componentType)) {
            // array of class constants, convert to string[]
            Class[] classes = (Class[]) value;
            String[] strings = new String[classes.length];
            for (int i = 0; i < classes.length; i++) {
              strings[i] = classes[i].getName();
            }
            value = strings;
          }
          else if (Enum.class.equals(componentType)) {
            // array of enum constants, convert to string[]
            Enum[] enums = (Enum[]) value;
            String[] strings = new String[enums.length];
            for (int i = 0; i < enums.length; i++) {
              strings[i] = enums[i].name();
            }
            value = strings;
          }
          else if (Annotation.class.equals(componentType)) {
            // annotations are forbidden
            throw new InvalidDescriptorKeyException(key, annotation, method);
          }
          // other component-types should be valid
        }
        else if (value instanceof Annotation) {
          // annotations are forbidden
          throw new InvalidDescriptorKeyException(key, annotation, method);
        }
        // other types should be valid

        fields.put(name, value);
      }
    }

    return new ImmutableDescriptor(fields);
  }

  /**
   * Thrown when @DescriptorKey type coercion fails.
   */
  @VisibleForTesting
  static class InvalidDescriptorKeyException
      extends RuntimeException
  {
    public InvalidDescriptorKeyException(final DescriptorKey key, final Annotation annotation, final Method method) {
      super("Invalid @DescriptorKey: " + key + ", annotation=" + annotation + ", method=" + method);
    }
  }

  /**
   * Find all annotations attached to given annotations deeply.
   */
  @VisibleForTesting
  static List<Annotation> findAllAnnotations(final Annotation... annotations) {
    Set<Annotation> visited = Sets.newHashSet();
    List<Annotation> found = Lists.newArrayList();
    visitAnnotations(visited, found, annotations);
    return found;
  }

  /**
   * Visit all annotations, and all annotations attached to annotation type.
   */
  private static void visitAnnotations(final Set<Annotation> visited,
                                       final List<Annotation> found,
                                       final Annotation... annotations)
  {
    for (Annotation annotation : annotations) {
      visited.add(annotation);
      for (Annotation parent : annotation.annotationType().getAnnotations()) {
        if (!visited.contains(parent)) {
          visitAnnotations(visited, found, parent);
        }
      }
      found.add(annotation);
    }
  }

  /**
   * Extract field value as string from descriptor if possible.
   */
  @Nullable
  public static String stringValue(final Descriptor descriptor, final String name) {
    checkNotNull(descriptor);
    checkNotNull(name);
    Object value = descriptor.getFieldValue(name);
    if (value instanceof CharSequence) {
      return value.toString();
    }
    return null;
  }
}
