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
package org.sonatype.nexus.repository.content.search;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Container that holds fields for SQL search
 *
 * @since 3.41
 */
public class SqlSearchRequest
{
  // The maximum number of rows to return
  public final int limit;

  // Number of rows to skip in relation to the first row of the first page
  public final int offset;

  // Optional filter to apply
  @Nullable
  public final String filter;

  // Optional values map for filter (required if filter is not null)
  @Nullable
  public final Map<String, String> filterParams;

  // Optional column name to be used for sorting
  public final String sortColumnName;

  // Indicates sort direction
  public final String sortDirection;

  // Column name to be used for default/secondary sort
  public static final String defaultSortColumnName = SearchViewColumns.FORMAT.name();

  // Column name to be used for second default sort
  public static final String secondDefaultSortColumnName = SearchViewColumns.COMPONENT_ID.name();

  private SqlSearchRequest(final Builder builder) {
    this.limit = builder.limit;
    this.offset = builder.offset;
    this.filter = builder.filter;
    this.filterParams = builder.filterParams;
    this.sortColumnName = builder.sortColumnName;
    this.sortDirection = builder.sortDirection;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private int limit;

    private int offset;

    private String filter;

    private Map<String, String> filterParams;

    private String sortColumnName;

    private String sortDirection;

    public Builder limit(final int limit) {
      this.limit = limit;
      return this;
    }

    public Builder offset(final int offset) {
      this.offset = offset;
      return this;
    }

    public Builder searchFilter(final String filter) {
      this.filter = filter;
      return this;
    }

    public Builder searchFilterValues(final Map<String, String> values) {
      this.filterParams = values;
      return this;
    }

    public Builder sortColumnName(@Nullable final String sortColumnName) {
      this.sortColumnName = sortColumnName;
      return this;
    }

    public Builder sortDirection(final String sortDirection) {
      this.sortDirection = sortDirection;
      return this;
    }

    public SqlSearchRequest build() {
      return new SqlSearchRequest(this);
    }
  }
}
