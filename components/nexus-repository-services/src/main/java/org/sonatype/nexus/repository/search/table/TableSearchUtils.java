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
package org.sonatype.nexus.repository.search.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.search.DefaultSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;

/**
 * Translates SearchFilters to a {@link SqlSearchQueryBuilder} containing a condition format string and the search
 * values.
 */
@Named
@Singleton
public class TableSearchUtils
    extends ComponentSupport
{
  public static final String FORMAT = "format";

  private final Map<String, SqlSearchQueryContribution> searchContributions;

  private final SqlSearchQueryContribution defaultSqlSearchQueryContribution;

  @Inject
  public TableSearchUtils(final Map<String, SqlSearchQueryContribution> searchContributions) {
    this.searchContributions = checkNotNull(searchContributions);
    this.defaultSqlSearchQueryContribution =
        checkNotNull(searchContributions.get(DefaultSqlSearchQueryContribution.NAME));
  }

  public SqlSearchQueryBuilder buildQuery(final SearchRequest request) {
    final SqlSearchQueryBuilder queryBuilder = request.isConjunction() ? SqlSearchQueryBuilder.conjunctionBuilder()
        : SqlSearchQueryBuilder.disjunctionBuilder();
    List<SearchFilter> searchFilters = request.getSearchFilters().stream()
        .filter(searchFilter -> !isBlank(searchFilter.getValue()))
        .filter(filter -> !REPOSITORY_NAME.equalsIgnoreCase(filter.getProperty()))
        .collect(Collectors.toList());

    generalizeDuplicateProperties(searchFilters);

    searchFilters.forEach(searchFilter -> {
          SqlSearchQueryContribution searchContribution = searchContributions
              .getOrDefault(getContributionKey(searchFilter), defaultSqlSearchQueryContribution);
          searchContribution.contribute(queryBuilder, searchFilter);
        });

    log.debug("Query: {}", queryBuilder);

    return queryBuilder;
  }

  private void generalizeDuplicateProperties(final List<SearchFilter> searchFilters) {
    List<String> properties = searchFilters.stream()
        .map(SearchFilter::getProperty)
        .collect(Collectors.toList());
    Set<String> duplicates = properties.stream()
        .filter(property -> Collections.frequency(properties, property) > 1)
        .collect(Collectors.toSet());

    duplicates.forEach(property -> {
      List<SearchFilter> filters =
          searchFilters.stream().filter(sf -> sf.getProperty().equals(property)).collect(Collectors.toList());
      searchFilters.removeAll(filters);

      String prop = filters.get(0).getProperty();
      StringBuilder value = new StringBuilder();
      filters.forEach(filter -> {
        if (value.length() > 0) {
          value.append(" ");
        }
        value.append(filter.getValue());
      });
      searchFilters.add(new SearchFilter(prop, value.toString()));
    });
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
