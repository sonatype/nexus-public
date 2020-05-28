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
package org.sonatype.nexus.transaction;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;

import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.union;
import static java.util.stream.Collectors.joining;

/**
 * Filters {@link Exception}s by name.
 *
 * @since 3.24
 */
public class ExceptionFilter
    implements Predicate<Exception>
{
  private ImmutableSet<String> filter;

  public ExceptionFilter(final String exceptionFilter) {
    this.filter = parseFilter(exceptionFilter);
  }

  /**
   * Is the given exception included in this filter?
   */
  @Override
  public boolean test(Exception cause) {
    return filter.contains(cause.getClass().getName());
  }

  /**
   * Gets the comma-separated list of types to be filtered.
   */
  public String getFilterString() {
    return filter.stream().collect(joining(","));
  }

  /**
   * Sets the comma-separated list of types to be filtered.
   */
  public void setFilterString(final String filterString) {
    this.filter = parseFilter(filterString);
  }

  /**
   * Adds the given exception type to the filter.
   */
  public void addException(final Class<? extends Exception> type) {
    this.filter = union(filter, ImmutableSet.of(type.getName())).immutableCopy();
  }

  /**
   * Removes the given exception type from the filter.
   */
  public void removeException(final Class<? extends Exception> type) {
    this.filter = difference(filter, ImmutableSet.of(type.getName())).immutableCopy();
  }

  /**
   * Parses a comma-separated list into a {@link Set}.
   */
  private static ImmutableSet<String> parseFilter(final String filterString) {
    return ImmutableSet.copyOf(filterString.split("\\s*,\\s*"));
  }
}
