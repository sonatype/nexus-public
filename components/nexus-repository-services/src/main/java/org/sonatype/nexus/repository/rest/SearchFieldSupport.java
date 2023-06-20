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
package org.sonatype.nexus.repository.rest;

import java.util.Objects;

import org.sonatype.nexus.repository.rest.sql.TextualQueryType;
import org.sonatype.nexus.repository.search.SortDirection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A reference to a table and column.
 *
 * @since 3.38
 */
public abstract class SearchFieldSupport
{
  private final String table;

  private final String columnName;

  private final String sortColumnName;

  private final SortDirection sortDirection;

  private final TextualQueryType textualQueryType;

  protected SearchFieldSupport(
      final String table,
      final String columnName,
      final String sortColumnName,
      final SortDirection sortDirection,
      final TextualQueryType textualQueryType)
  {
    this.table = checkNotNull(table);
    this.columnName = checkNotNull(columnName);
    this.sortColumnName = checkNotNull(sortColumnName);
    this.sortDirection = checkNotNull(sortDirection);
    this.textualQueryType = checkNotNull(textualQueryType);
  }

  /**
   * @return the name of the column
   */
  public String getColumnName() {
    return columnName;
  }

  /**
   * @return the table to which the field can be found
   */
  public String getTable() {
    return table;
  }

  public String getSortColumnName() {
    return sortColumnName;
  }

  public SortDirection getSortDirection() {
    return sortDirection;
  }

  /**
   * Indicates whether this column in a full text search column or a basic column such as string, int etc
   */
  public TextualQueryType getTextualQueryType() {
    return textualQueryType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, sortColumnName, table, textualQueryType);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    SearchFieldSupport other = (SearchFieldSupport) obj;

    return Objects.equals(columnName, other.columnName) && Objects.equals(sortColumnName, other.sortColumnName) &&
        Objects.equals(table, other.table) && Objects.equals(textualQueryType, other.textualQueryType);
  }

  @Override
  public String toString() {
    return table + '.' + textualQueryType + '.' + columnName + '.' + sortColumnName;
  }
}
