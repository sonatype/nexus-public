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
package org.sonatype.nexus.repository.search.sql;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.search.DefaultSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.index.SearchConstants;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder.queryBuilder;

/**
 * Translates SearchFilters to a {@link SqlSearchQueryBuilder} containing a condition format string and the search
 * values.
 *
 * @since 3.38
 */
@Named
@Singleton
public class SqlSearchUtils
    extends ComponentSupport
{
  public static final String FORMAT = "format";

  private final Map<String, SqlSearchQueryContribution> searchContributions;

  private final SqlSearchQueryContribution defaultSqlSearchQueryContribution;

  @Inject
  public SqlSearchUtils(final Map<String, SqlSearchQueryContribution> searchContributions) {
    this.searchContributions = checkNotNull(searchContributions);
    this.defaultSqlSearchQueryContribution =
        checkNotNull(searchContributions.get(DefaultSqlSearchQueryContribution.NAME));
  }

  public SqlSearchQueryBuilder buildQuery(final List<SearchFilter> searchFilters) {
    final SqlSearchQueryBuilder queryBuilder = queryBuilder();
    searchFilters.stream()
        .filter(searchFilter -> !isBlank(searchFilter.getValue()))
        .filter(filter -> !FORMAT.equalsIgnoreCase(filter.getProperty()))
        .filter(filter -> !SearchConstants.REPOSITORY_NAME.equalsIgnoreCase(filter.getProperty()))
        .forEach(searchFilter -> {
          SqlSearchQueryContribution searchContribution = searchContributions
              .getOrDefault(getContributionKey(searchFilter), defaultSqlSearchQueryContribution);
          searchContribution.contribute(queryBuilder, searchFilter);
        });

    log.debug("Query: {}", queryBuilder);

    return queryBuilder;
  }

  public Optional<String> getFormat(final List<SearchFilter> searchFilters) {
    return searchFilters.stream()
        .filter(searchFilter -> !isBlank(searchFilter.getValue()))
        .filter(filter -> FORMAT.equalsIgnoreCase(filter.getProperty()))
        .findFirst()
        .map(SearchFilter::getValue);
  }

  public Optional<SearchFilter> getRepositoryFilter(final List<SearchFilter> filters) {
    return filters.stream()
        .filter(searchFilter -> REPOSITORY_NAME.equals(searchFilter.getProperty()))
        .findFirst();
  }

  private String getContributionKey(final SearchFilter searchFilter) {
    return SqlSearchQueryContributionSupport.NAME_PREFIX + searchFilter.getProperty();
  }
}
