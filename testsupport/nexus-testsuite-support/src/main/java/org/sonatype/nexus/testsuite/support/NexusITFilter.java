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
package org.sonatype.nexus.testsuite.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.testsuite.support.filters.CompositeFilter;
import org.sonatype.nexus.testsuite.support.filters.ImplicitVersionFilter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;

/**
 * Nexus Integration Tests specific filter. It uses all available filters and an context pre-configured
 * context.
 *
 * @since 2.2
 */
public class NexusITFilter
    extends CompositeFilter
{

  private final Map<String, String> defaultContext;

  /**
   * Constructor.
   *
   * @param filters        member filters. Cannot be null.
   * @param defaultContext context to be used as default while filtering. Can be null.
   */
  public NexusITFilter(final List<Filter> filters,
                       @Nullable final Map<String, String> defaultContext)
  {
    super(addDefaultFilters(filters));
    this.defaultContext = defaultContext == null ? Maps.<String, String>newHashMap() : defaultContext;
  }

  /**
   * Constructor.
   *
   * @param filters        member filters. Cannot be null.
   * @param defaultContext context to be used as default while filtering. Can be null.
   */
  public NexusITFilter(final List<Filter> filters,
                       final Map.Entry<String, String>... defaultContext)
  {
    this(addDefaultFilters(filters), asMap(defaultContext));
  }

  /**
   * Filters placeholders.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Cannot be null.
   * @return filtered value. If null the filter is not considered in filtering chain.
   */
  public String filter(final Map.Entry<String, String>[] context, String value) {
    final Map<String, String> fullContext = Maps.newHashMap();
    fullContext.putAll(defaultContext);
    fullContext.putAll(asMap(context));

    return filter(fullContext, value);
  }

  /**
   * Filters placeholders using default context.
   *
   * @param value value to be filtered. Cannot be null.
   * @return filtered value.
   */
  public String filter(final String value) {
    return filter(defaultContext, value);
  }

  @Override
  public String filter(final Map<String, String> context, final String value) {
    if (value == null) {
      return null;
    }

    // filter the value until filters does not do any more changes
    String previousValue;
    String filteredValue = value;
    do {
      previousValue = filteredValue;
      filteredValue = super.filter(context, previousValue);
    }
    while (filteredValue != null && !filteredValue.equals(previousValue));

    return filteredValue;
  }

  public static Map.Entry<String, String> contextEntry(final String key, final String value) {
    return new Map.Entry<String, String>()
    {
      @Override
      public String getKey() {
        return key;
      }

      @Override
      public String getValue() {
        return value;
      }

      @Override
      public String setValue(final String value) {
        throw new UnsupportedOperationException();
      }

    };
  }

  private static Map<String, String> asMap(final Map.Entry<String, String>[] contextEntries) {
    final HashMap<String, String> context = Maps.newHashMap();
    if (contextEntries != null && contextEntries.length > 0) {
      for (final Map.Entry<String, String> contextEntry : contextEntries) {
        context.put(contextEntry.getKey(), contextEntry.getValue());
      }
    }
    return context;
  }

  private static List<Filter> addDefaultFilters(final List<Filter> filters) {
    final ArrayList<Filter> members = Lists.newArrayList();
    members.add(new ImplicitVersionFilter());
    if (filters != null) {
      members.addAll(filters);
    }
    return members;
  }

}
