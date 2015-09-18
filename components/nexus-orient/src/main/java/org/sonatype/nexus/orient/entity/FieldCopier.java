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
package org.sonatype.nexus.orient.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Helper to copy {@link ODocument} field values.
 *
 * @since 3.0
 */
public class FieldCopier
{
  private FieldCopier() {}

  private static Object maybeCopy(final Object source) {
    if (source instanceof ODocument) {
      return copy((ODocument)source);
    }
    else if (source instanceof Map) {
      return copy((Map) source);
    }
    else if (source instanceof List) {
      return copy((List) source);
    }
    else if (source instanceof Set) {
      return copy((Set)source);
    }
    return source;
  }

  @SuppressWarnings("unchecked")
  public static Map copy(final ODocument source) {
    String[] names = source.fieldNames();
    Map target = Maps.newHashMapWithExpectedSize(names.length);
    for (String name : names) {
      Object value = maybeCopy(source.field(name));
      target.put(name, value);
    }
    return target;
  }

  @SuppressWarnings("unchecked")
  public static Map copy(final Map<?, ?> source) {
    Map target = Maps.newHashMapWithExpectedSize(source.size());
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      Object value = maybeCopy(entry.getValue());
      target.put(entry.getKey(), value);
    }
    return target;
  }

  /**
   * Copy given map if non-null.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T extends Map<?,?>> T copyIf(final @Nullable T source) {
    if (source != null) {
      return (T) copy(source);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static List copy(final List<?> source) {
    List target = Lists.newArrayListWithExpectedSize(source.size());
    for (Object value : source) {
      value = maybeCopy(value);
      target.add(value);
    }
    return target;
  }

  @SuppressWarnings("unchecked")
  public static Set copy(final Set<?> source) {
    Set target = Sets.newHashSetWithExpectedSize(source.size());
    for (Object value : source) {
      value = maybeCopy(value);
      target.add(value);
    }
    return target;
  }
}
