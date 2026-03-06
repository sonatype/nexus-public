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
package org.sonatype.nexus.repository.search.sql.query;

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
  public final Map<String, Object> filterParams;

  // Optional assetFilter to apply
  @Nullable
  public final String assetFilter;

  // Optional values map for assetFilterParams (required if contentSelectorFilter is not null)
  @Nullable
  public final Map<String, Object> assetFilterParams;

  // Optional column name to be used for sorting
  public final String sortColumnName;

  // Indicates sort direction
  public final String sortDirection;

  // Column name to be used for default/secondary sort
  public static final String defaultSortColumnName = "cs." + SearchViewColumns.FORMAT.name();

  // Column name to be used for second default sort
  public static final String secondDefaultSortColumnName = "cs." + SearchViewColumns.COMPONENT_ID.name();

  public final boolean distinctNameAndNamespace;

  private SqlSearchRequest(final Builder builder) {
    this.limit = builder.limit;
    this.offset = builder.offset;
    this.filter = builder.filter;
    this.filterParams = builder.filterParams;
    this.assetFilter = builder.assetFilter;
    this.assetFilterParams = builder.assetFilterValues;
    this.sortColumnName = builder.sortColumnName;
    this.sortDirection = builder.sortDirection;
    this.distinctNameAndNamespace = builder.distinctNameAndNamespace;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private int limit = 100;

    private int offset;

    private String filter;

    private Map<String, Object> filterParams;

    private String sortColumnName;

    private String sortDirection;

    private String assetFilter;

    private Map<String, Object> assetFilterValues;

    private boolean distinctNameAndNamespace = false;

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

    public Builder searchFilterValues(final Map<String, Object> values) {
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

    public Builder searchAssetFilter(final String assetFilter) {
      this.assetFilter = assetFilter;
      return this;
    }

    public Builder searchAssetFilterValues(final Map<String, Object> assetFilterValues) {
      this.assetFilterValues = assetFilterValues;
      return this;
    }

    public Builder distinctNameAndNamespace(final boolean distinctNameAndNamespace) {
      this.distinctNameAndNamespace = distinctNameAndNamespace;
      return this;
    }

    public SqlSearchRequest build() {
      return new SqlSearchRequest(this);
    }
  }
}
