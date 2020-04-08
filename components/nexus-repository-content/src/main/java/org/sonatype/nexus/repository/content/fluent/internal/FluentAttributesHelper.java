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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.fluent.AttributeChange;
import org.sonatype.nexus.repository.content.fluent.FluentAttributes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

/**
 * {@link FluentAttributes} helper.
 *
 * @since 3.next
 */
public class FluentAttributesHelper
{
  private FluentAttributesHelper() {
    // static utility methods
  }

  /**
   * Apply a change request to the {@link RepositoryContent}'s attributes.
   */
  public static void apply(final RepositoryContent content,
                           final AttributeChange change,
                           final String key,
                           @Nullable final Object value)
  {
    apply(content.attributes(), change, key, value);
  }

  /**
   * Apply a change request to the given {@link AttributesMap}.
   */
  public static void apply(final AttributesMap attributes,
                           final AttributeChange change,
                           final String key,
                           @Nullable final Object value)
  {
    switch (change) {
      case SET:
        attributes.set(key, checkNotNull(value));
        break;
      case REMOVE:
        attributes.remove(key); // value is ignored
        break;
      case APPEND:
        attributes.compute(key, v -> append(v, checkNotNull(value)));
        break;
      case PREPEND:
        attributes.compute(key, v -> prepend(v, checkNotNull(value)));
        break;
      case MERGE:
        attributes.compute(key, v -> merge(v, checkNotNull(value)));
        break;
      default:
        throw new IllegalArgumentException("Unknown request");
    }
  }

  /**
   * Attempts to append a value to an attribute list.
   *
   * @throws IllegalArgumentException if the attribute is not a list
   */
  @SuppressWarnings("unchecked")
  private static Object append(final Object list, final Object value) {
    if (list == null) {
      return newArrayList(value);
    }
    checkArgument(list instanceof List<?>, "Cannot append to non-list attribute");
    ((List<Object>) list).add(value);
    return list;
  }

  /**
   * Attempts to prepend a value to an attribute list.
   *
   * @throws IllegalArgumentException if the attribute is not a list
   */
  @SuppressWarnings("unchecked")
  private static Object prepend(final Object list, final Object value) {
    if (list == null) {
      return newArrayList(value);
    }
    checkArgument(list instanceof List<?>, "Cannot prepend to non-list attribute");
    ((List<Object>) list).add(0, value);
    return list;
  }

  /**
   * Attempts to merge a map value with an attribute map.
   *
   * @throws IllegalArgumentException if either the value or attribute is not a map
   */
  private static Object merge(final Object map, final Object value) {
    checkArgument(value instanceof Map<?, ?>, "Conflict: cannot merge '%s' with '%s'", value, map);
    if (map == null) {
      return value;
    }
    checkArgument(map instanceof Map<?, ?>, "Conflict: cannot merge '%s' with '%s'", value, map);
    @SuppressWarnings("unchecked")
    Map<Object, Object> mergeInto = (Map<Object, Object>) map; // perform in-place merge
    for (Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
      Object oldValue = mergeInto.get(entry.getKey());
      Object newValue = entry.getValue();
      if (oldValue == null) {
        mergeInto.put(entry.getKey(), newValue);
      }
      else if (!oldValue.equals(newValue)) {
        merge(oldValue, newValue);
      }
    }
    return map;
  }
}
