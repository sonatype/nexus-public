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
package org.sonatype.nexus.repository.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.search.query.SearchFilter;

/**
 * This class encompasses all the options for a search request.
 *
 * @since 3.38
 */
public class SearchRequest
{
  private static final int DEFAULT_LIMIT = 10_000;

  private final boolean checkAuthorization;

  private final List<SearchFilter> searchFilters;

  private final List<String> repositories;

  private final String sortField;

  private final String continuationToken;

  private final SortDirection sortDirection;

  private final int limit;

  private final Integer offset;

  private final boolean conjunction;

  private final boolean includeAssets;

  private final boolean distinctNameAndNamespace;

  private SearchRequest(final Builder builder) {
    this.checkAuthorization = builder.checkAuthorization;
    this.searchFilters = builder.searchFilters;
    this.repositories = builder.repositories;
    this.sortField = builder.sortField;
    this.continuationToken = builder.continuationToken;
    this.sortDirection = builder.sortDirection;
    this.limit = builder.limit;
    this.offset = builder.offset;
    this.conjunction = builder.conjunction;
    this.includeAssets = builder.includeAssets;
    this.distinctNameAndNamespace = builder.distinctNameAndNamespace;
  }

  public boolean isCheckAuthorization() {
    return checkAuthorization;
  }

  public List<SearchFilter> getSearchFilters() {
    return searchFilters;
  }

  public List<String> getRepositories() {
    return repositories;
  }

  public String getSortField() {
    return sortField;
  }

  @Nullable
  public String getContinuationToken() {
    return continuationToken;
  }

  public SortDirection getSortDirection() {
    return sortDirection;
  }

  public int getLimit() {
    return limit;
  }

  @Nullable
  public Integer getOffset() {
    return offset;
  }

  public boolean isConjunction() {
    return conjunction;
  }

  public boolean isIncludeAssets() {
    return includeAssets;
  }

  public boolean isDistinctNameAndNamespace() {
    return distinctNameAndNamespace;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("checkAuthorization", checkAuthorization)
        .append("searchFilters", searchFilters)
        .append("repositories", repositories)
        .append("sortField", sortField)
        .append("continuationToken", continuationToken)
        .append("sortDirection", sortDirection)
        .append("limit", limit)
        .append("offset", offset)
        .append("conjunction", conjunction)
        .append("includeAssets", includeAssets)
        .toString();
  }

  public static class Builder
  {
    private boolean checkAuthorization = true;

    private List<SearchFilter> searchFilters = new ArrayList<>();

    private List<String> repositories = new ArrayList<>();

    private String sortField;

    private SortDirection sortDirection;

    private String continuationToken;

    private Integer limit;

    private Integer offset;

    private boolean conjunction = true;

    private boolean includeAssets = false;

    private boolean distinctNameAndNamespace = false;

    public Builder from(final SearchRequest request) {
      this.checkAuthorization = request.checkAuthorization;
      this.searchFilters = new ArrayList<>(request.searchFilters);
      this.repositories = new ArrayList<>(request.repositories);
      this.sortField = request.sortField;
      this.continuationToken = request.continuationToken;
      this.sortDirection = request.sortDirection;
      this.limit = request.limit;
      this.offset = request.offset;
      this.conjunction = request.conjunction;
      this.includeAssets = request.includeAssets;
      this.distinctNameAndNamespace = request.distinctNameAndNamespace;
      return this;
    }

    public Builder disableAuthorization() {
      this.checkAuthorization = false;
      return this;
    }

    public Builder searchFilter(final String field, final String value) {
      this.searchFilters.add(new SearchFilter(field, value));
      return this;
    }

    /**
     * Adds a search filter with an explicit operator (AND or OR).
     * The operator overrides the global conjunction flag for this specific filter.
     *
     * @param field the field to filter on
     * @param value the value to match
     * @param operator the logical operator (AND or OR)
     * @return this builder
     */
    public Builder searchFilter(final String field, final String value, final SearchFilter.FilterOperator operator) {
      this.searchFilters.add(new SearchFilter(field, value, operator));
      return this;
    }

    /**
     * Adds a search filter that is always AND'ed with other conditions,
     * regardless of the global conjunction flag.
     *
     * @param field the field to filter on
     * @param value the value to match
     * @return this builder
     */
    public Builder andFilter(final String field, final String value) {
      return searchFilter(field, value, SearchFilter.FilterOperator.AND);
    }

    /**
     * Adds a search filter that is always OR'ed with other conditions,
     * regardless of the global conjunction flag.
     *
     * @param field the field to filter on
     * @param value the value to match
     * @return this builder
     */
    public Builder orFilter(final String field, final String value) {
      return searchFilter(field, value, SearchFilter.FilterOperator.OR);
    }

    public Builder searchFilters(final Collection<SearchFilter> searchFilters) {
      this.searchFilters.addAll(searchFilters);
      return this;
    }

    /**
     * Replaces the current search filters with the provided collection. Useful when a
     * {@code modifyRequest} hook needs to rewrite filters rather than append to them.
     */
    public Builder replaceSearchFilters(final Collection<SearchFilter> searchFilters) {
      this.searchFilters = new ArrayList<>(searchFilters);
      return this;
    }

    public Builder repository(final String repositoryName) {
      this.repositories.add(repositoryName);
      return this;
    }

    public Builder repositories(final String... repositoryNames) {
      this.repositories.addAll(Arrays.asList(repositoryNames));
      return this;
    }

    public Builder repositories(final Collection<String> repositoryNames) {
      this.repositories.addAll(repositoryNames);
      return this;
    }

    public Builder continuationToken(final String continuationToken) {
      this.continuationToken = continuationToken;
      return this;
    }

    /**
     * Sets the request to use a disjunction (OR) rather than default of conjunction (AND).
     */
    public Builder disjunction() {
      this.conjunction = false;
      return this;
    }

    /**
     * Sets the request to include asset data.
     */
    public Builder includeAssets() {
      this.includeAssets = true;
      return this;
    }

    public Builder sortDirection(final SortDirection sortDirection) {
      this.sortDirection = sortDirection;
      return this;
    }

    public Builder sortField(final String sortField) {
      this.sortField = sortField;
      return this;
    }

    public Builder limit(final Integer limit) {
      this.limit = limit;
      return this;
    }

    /**
     * Underlying implementation will choose one of the continuation token or offset.
     */
    public Builder offset(final Integer offset) {
      this.offset = offset;
      return this;
    }

    public Builder distinctNameAndNamespace() {
      this.distinctNameAndNamespace = true;
      return this;
    }

    public SearchRequest build() {
      if (this.limit == null) {
        this.limit = DEFAULT_LIMIT;
      }

      return new SearchRequest(this);
    }
  }
}
