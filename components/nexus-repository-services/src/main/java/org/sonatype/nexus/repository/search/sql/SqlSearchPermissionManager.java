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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.SqlSearchRepositoryNameUtil;
import org.sonatype.nexus.selector.SelectorConfiguration;

import org.apache.commons.collections.CollectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryContributionSupport.fieldMappingsByAttribute;

/**
 * Adds permitted repositories and content selectors to the sql search query.
 *
 * The intent is that only repositories that the user has browse permissions for
 * and/or content selectors for are included in the search request.
 *
 * @since 3.next
 */
@Named
public class SqlSearchPermissionManager
    extends ComponentSupport
{
  private final SqlSearchQueryConditionBuilder conditionBuilder;

  private final SqlSearchRepositoryNameUtil repositoryNameUtil;

  private final SqlSearchRepositoryPermissionUtil repositoryPermissionUtil;

  private final SqlSearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator;

  private final Map<String, SearchFieldSupport> fieldMappings;

  @Inject
  public SqlSearchPermissionManager(
      final SqlSearchQueryConditionBuilder conditionBuilder,
      final SqlSearchRepositoryNameUtil repositoryNameUtil,
      final SqlSearchRepositoryPermissionUtil repositoryPermissionUtil,
      final Map<String, SearchMappings> searchMappings,
      final SqlSearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator)
  {
    this.conditionBuilder = checkNotNull(conditionBuilder);
    this.repositoryNameUtil = checkNotNull(repositoryNameUtil);
    this.repositoryPermissionUtil = checkNotNull(repositoryPermissionUtil);
    this.fieldMappings = unmodifiableMap(fieldMappingsByAttribute(checkNotNull(searchMappings)));
    this.contentSelectorFilterGenerator = checkNotNull(contentSelectorFilterGenerator);
  }

  /**
   * Adds permitted repositories and content selectors to the sql search query.
   *
   * The intent is that only repositories that the user has browse permissions for and/or content selectors for are
   * included in the search request.
   */
  public void addPermissionFilters(
      final SqlSearchQueryBuilder queryBuilder,
      final String format,
      @Nullable final String searchFilter)
  {
    if (searchFilter != null) {
      addRepositoryPermissions(queryBuilder, searchFilter, format);
    }
    else {
      addRepositoryPermissions(queryBuilder, format);
    }
  }

  private void addRepositoryPermissions(
      final SqlSearchQueryBuilder queryBuilder,
      final String repositoryFilter,
      final String format)
  {
    Set<String> repositories = repositoryNameUtil.getRepositoryNames(repositoryFilter);
    if (!repositories.isEmpty()) {
      Set<String> browsableRepositories = getBrowsableRepositories(format, repositories);

      if (!repositories.equals(browsableRepositories)) {
        addBrowsableRepositoriesAndSelectorPermissions(queryBuilder, format, repositories, browsableRepositories);
      }
      else {
        createSqlCondition(browsableRepositories, emptySet(), emptyList(), format).ifPresent(queryBuilder::add);
      }
    }
  }

  private void addRepositoryPermissions(final SqlSearchQueryBuilder queryBuilder, final String format) {
    Set<String> repositories = repositoryNameUtil.getFormatRepositoryNames(format);
    Set<String> browsableRepositories = getBrowsableRepositories(format, repositories);

    if (!repositories.equals(browsableRepositories)) {
      addBrowsableRepositoriesAndSelectorPermissions(queryBuilder, format, repositories, browsableRepositories);
    }
  }

  private Set<String> getBrowsableRepositories(final String format, final Set<String> repositories) {
    return repositoryPermissionUtil.browsableAndUnknownRepositories(format, repositories);
  }

  private Set<String> getSelectorRepositories(final Set<String> repositories, final Set<String> browsableRepositories) {
    Set<String> selectorRepositories = new HashSet<>(repositories);
    selectorRepositories.removeAll(browsableRepositories);
    return selectorRepositories;
  }

  private List<SelectorConfiguration> getSelectorConfigurations(final String format, final Set<String> repositories) {
    return repositoryPermissionUtil.selectorConfigurations(repositories, singletonList(format));
  }

  private void addBrowsableRepositoriesAndSelectorPermissions(
      final SqlSearchQueryBuilder queryBuilder,
      final String format,
      final Set<String> repositories,
      final Set<String> browsableRepositories)
  {
    Set<String> selectorRepositories = getSelectorRepositories(repositories, browsableRepositories);
    List<SelectorConfiguration> selectors = getSelectorConfigurations(format, selectorRepositories);
    createSqlCondition(browsableRepositories, selectorRepositories, selectors, format).ifPresent(queryBuilder::add);
  }

  private Optional<SqlSearchQueryCondition> createSqlCondition(
      final Set<String> browsableRepositories,
      final Set<String> selectorRepositories,
      final List<SelectorConfiguration> selectorConfigs,
      final String format)
  {

    if (browsableRepositories.isEmpty() && selectorConfigs.isEmpty()) {
      throw new SqlSearchPermissionException("User is not permitted.");
    }

    List<SqlSearchQueryCondition> conditions = new ArrayList<>();
    of(browsableRepositories).flatMap(this::createRepositoryCondition).ifPresent(conditions::add);
    of(selectorRepositories).flatMap(repos -> createContentSelectorCondition(selectorConfigs, repos, format))
        .ifPresent(conditions::add);

    return of(conditions)
        .filter(CollectionUtils::isNotEmpty)
        .map(conditionBuilder::combine);
  }

  private Optional<SqlSearchQueryCondition> createRepositoryCondition(final Set<String> repositories) {
    if (repositories.isEmpty()) {
      return empty();
    }
    return ofNullable(fieldMappings.get(REPOSITORY_NAME))
        .map(SearchFieldSupport::getColumnName)
        .map(field -> conditionBuilder.condition(field, repositories));
  }

  private Optional<SqlSearchQueryCondition> createContentSelectorCondition(
      final List<SelectorConfiguration> selectors,
      final Set<String> repositories,
      final String format)
  {
    if (selectors.isEmpty()) {
      return empty();
    }

    SqlSearchContentSelectorFilter contentAuthFilter =
        contentSelectorFilterGenerator.createFilter(selectors, repositories, format);

    if (contentAuthFilter.hasFilters()) {
      return of(new SqlSearchQueryCondition(contentAuthFilter.queryFormat(), contentAuthFilter.queryParameters()));
    }
    return empty();
  }
}
