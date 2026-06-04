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
package org.sonatype.nexus.repository.search.query;

import java.util.Optional;

import jakarta.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a search filter with optional explicit operator.
 *
 * When operator is null, the filter uses the global conjunction flag from SearchRequest.
 * When operator is specified (AND or OR), it overrides the global conjunction flag.
 *
 * @since 3.15
 */
public record SearchFilter(String property, String value, @Nullable FilterOperator operator)
{
  /**
   * Logical operator for combining this filter with others.
   */
  public enum FilterOperator
  {
    AND,
    OR
  }

  /**
   * Creates a SearchFilter using the global conjunction flag from SearchRequest.
   */
  public SearchFilter(final String property, final String value) {
    this(property, value, null);
  }

  /**
   * Creates a SearchFilter with an explicit operator that overrides the global conjunction flag.
   *
   * @param operator the logical operator (AND/OR), or null to use global conjunction flag
   */
  public SearchFilter {
    checkNotNull(property);
    checkNotNull(value);
  }

  public String getProperty() {
    return property;
  }

  public String getValue() {
    return value;
  }

  /**
   * Returns the explicit operator for this filter, if specified.
   * Empty if the filter should use the global conjunction flag.
   */
  public Optional<FilterOperator> getOperator() {
    return Optional.ofNullable(operator);
  }
}
