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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A reference to a table and column.
 *
 * @since 3.next
 */
public abstract class SearchFieldSupport
{
  private final String table;

  private final String columnName;

  protected SearchFieldSupport(final String table, final String columnName) {
    this.table = checkNotNull(table);
    this.columnName = checkNotNull(columnName);
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

  @Override
  public int hashCode() {
    return Objects.hash(columnName, table);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    SearchFieldSupport other = (SearchFieldSupport) obj;

    return Objects.equals(columnName, other.columnName) && Objects.equals(table, other.table);
  }

  @Override
  public String toString() {
    return table + '.' + columnName;
  }
}
