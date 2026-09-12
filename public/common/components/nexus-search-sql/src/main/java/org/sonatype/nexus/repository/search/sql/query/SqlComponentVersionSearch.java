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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.search.ComponentVersion;
import org.sonatype.nexus.repository.search.ComponentVersionPage;
import org.sonatype.nexus.repository.search.ComponentVersionQuery;
import org.sonatype.nexus.repository.search.ComponentVersionSearch;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.sql.store.ComponentVersionData;
import org.sonatype.nexus.repository.search.sql.store.ComponentVersionSearchRequest;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * SQL implementation of {@link ComponentVersionSearch}.
 *
 * <p>
 * The component and permission parts of the WHERE clause are built by the shared
 * {@link ExpressionBuilder} so that repository and content-selector permission filtering is
 * inherited from ordinary search without duplication. Assets are never hydrated: this list does
 * not display them.
 *
 * <p>
 * The version filter is the one exception and is applied as a separate {@code LIKE} predicate.
 * {@code ExpressionBuilder} implements the programmatic search API's rules — exact match,
 * leading wildcards prohibited, three characters required before a trailing wildcard — which
 * cannot express the incremental substring filtering this list needs (typing {@code 10} has to
 * match {@code 10.0.0}, {@code 100.0.0} and {@code 1.0.10}).
 */
@Component
public class SqlComponentVersionSearch
    implements ComponentVersionSearch
{
  private static final Logger log = LoggerFactory.getLogger(SqlComponentVersionSearch.class);

  private final SearchStore searchStore;

  private final ExpressionBuilder expressionBuilder;

  private final SearchConditionFactory conditionFactory;

  public SqlComponentVersionSearch(
      final SearchStore searchStore,
      final ExpressionBuilder expressionBuilder,
      final SearchConditionFactory conditionFactory)
  {
    this.searchStore = checkNotNull(searchStore);
    this.expressionBuilder = checkNotNull(expressionBuilder);
    this.conditionFactory = checkNotNull(conditionFactory);
  }

  @Override
  public ComponentVersionPage browseVersions(final ComponentVersionQuery query) {
    Optional<SqlSearchQueryCondition> condition = componentCondition(query);
    if (condition.isEmpty()) {
      log.debug("No component condition could be built for {}; returning an empty page", query);
      return new ComponentVersionPage(List.of(), 0L, query.page(), query.size());
    }

    ComponentVersionSortField sortField = ComponentVersionSortField.fromKey(query.sort())
        .orElse(ComponentVersionSortField.VERSION);

    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter(condition.get().sqlFilter())
        .filterParams(condition.get().parameters())
        .versionLikePattern(toVersionLikePattern(query.versionFilter()))
        .sortExpression(sortField.sqlExpression())
        .sortDirection(query.direction().name())
        .limit(query.size())
        .offset(query.page() * query.size())
        .build();

    List<ComponentVersion> items = searchStore.browseComponentVersions(request)
        .stream()
        .map(SqlComponentVersionSearch::toComponentVersion)
        .toList();
    long total = searchStore.countComponentVersions(request);

    return new ComponentVersionPage(items, total, query.page(), query.size());
  }

  private Optional<SqlSearchQueryCondition> componentCondition(final ComponentVersionQuery query) {
    SearchRequest.Builder builder = SearchRequest.builder()
        .searchFilter("format", query.format())
        .searchFilter("name", query.name());
    if (StringUtils.isNotBlank(query.namespace())) {
      builder.searchFilter("group", query.namespace());
    }
    // The version filter is deliberately NOT added here — see the class javadoc. Routing it
    // through ExpressionBuilder yields exact-match semantics and rejects short or leading
    // wildcards, so it is applied as a LIKE predicate instead.
    // includeAssets is deliberately left unset: this list never displays assets.

    return expressionBuilder.from(builder.build())
        .map(conditionFactory::build)
        .map(SqlSearchQueryConditionGroup::getComponentCondition);
  }

  /**
   * Turns the user's filter text into a case-insensitive substring {@code LIKE} pattern.
   *
   * <p>
   * Wildcards are escaped so a literal {@code %} or {@code _} typed into the filter box matches
   * literally rather than matching everything. The backslash must be escaped first, otherwise the
   * escapes added for {@code %} and {@code _} would themselves be re-escaped.
   */
  @Nullable
  private static String toVersionLikePattern(@Nullable final String versionFilter) {
    if (StringUtils.isBlank(versionFilter)) {
      return null;
    }
    String escaped = versionFilter.toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
    return "%" + escaped + "%";
  }

  /**
   * Splits the aggregate produced by {@code STRING_AGG(..., ',')} in {@code browseComponentVersions}
   * back into a list.
   *
   * <p>
   * This relies on repository names never containing a comma, which holds because they are
   * validated against {@code NamePatternConstants#REGEX}
   * ({@code ^[a-zA-Z0-9\-]{1}[a-zA-Z0-9_\-\.]*$}) via {@code AbstractRepositoryApiRequest}. If that
   * pattern is ever relaxed to admit commas, the separator here and in the DAO query must change
   * together, or a name containing one will be silently split into two repositories.
   */
  private static ComponentVersion toComponentVersion(final ComponentVersionData data) {
    List<String> repositories = StringUtils.isBlank(data.getRepositories())
        ? List.of()
        : List.of(data.getRepositories().split(","));
    return new ComponentVersion(data.getVersion(), data.getLastModified(), repositories);
  }
}
