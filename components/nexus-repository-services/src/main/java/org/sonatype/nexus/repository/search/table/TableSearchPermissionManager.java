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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.SearchFieldSupport;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.search.SqlSearchRepositoryNameUtil;
import org.sonatype.nexus.repository.search.sql.SqlSearchContentSelectorFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.selector.SelectorConfiguration;

import org.apache.commons.collections.CollectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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
 * @since 3.41
 */
@Named
public class TableSearchPermissionManager
    extends ComponentSupport
{
  private final SqlSearchQueryConditionBuilder conditionBuilder;

  private final SqlSearchRepositoryNameUtil repositoryNameUtil;

  private final TableSearchRepositoryPermissionUtil repositoryPermissionUtil;

  private final TableSearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator;

  private final Map<String, SearchFieldSupport> fieldMappings;

  private final RepositoryManager repositoryManager;

  @Inject
  public TableSearchPermissionManager(
      final SqlSearchQueryConditionBuilder conditionBuilder,
      final SqlSearchRepositoryNameUtil repositoryNameUtil,
      final TableSearchRepositoryPermissionUtil repositoryPermissionUtil,
      final Map<String, SearchMappings> searchMappings,
      final TableSearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator,
      final RepositoryManager repositoryManager)
  {
    this.conditionBuilder = checkNotNull(conditionBuilder);
    this.repositoryNameUtil = checkNotNull(repositoryNameUtil);
    this.repositoryPermissionUtil = checkNotNull(repositoryPermissionUtil);
    this.fieldMappings = unmodifiableMap(fieldMappingsByAttribute(checkNotNull(searchMappings)));
    this.contentSelectorFilterGenerator = checkNotNull(contentSelectorFilterGenerator);
    this.repositoryManager = repositoryManager;
  }

  public void addPermissionFilters(
      final SqlSearchQueryBuilder queryBuilder,
      @Nullable final String searchFilter)
  {
    if (searchFilter != null) {
      addRepositoryPermissions(queryBuilder, searchFilter);
    }
    else {
      addRepositoryPermissions(queryBuilder);
    }
  }

  private void addRepositoryPermissions(
      final SqlSearchQueryBuilder queryBuilder,
      final String repositoryFilter)
  {
    Set<String> repositories = repositoryNameUtil.getRepositoryNames(repositoryFilter);
    if (!repositories.isEmpty()) {
      Set<String> browsableRepositories = getBrowsableRepositories(repositories);

      if (!repositories.equals(browsableRepositories)) {
        addBrowsableRepositoriesAndSelectorPermissions(queryBuilder, repositories, browsableRepositories);
      }
      else {
        createSqlCondition(browsableRepositories, emptySet(), emptyList()).ifPresent(queryBuilder::add);
      }
    }
  }

  private void addRepositoryPermissions(final SqlSearchQueryBuilder queryBuilder) {
    Set<String> repositories = StreamSupport
        .stream(repositoryManager.browse().spliterator(), false)
        .map(Repository::getName)
        .collect(Collectors.toSet());

    Set<String> browsableRepositories = getBrowsableRepositories(repositories);

    if (!repositories.equals(browsableRepositories)) {
      addBrowsableRepositoriesAndSelectorPermissions(queryBuilder, repositories, browsableRepositories);
    }
  }

  private Set<String> getBrowsableRepositories(final Set<String> repositories) {
    return repositoryPermissionUtil.browsableAndUnknownRepositories(repositories);
  }

  private Set<String> getSelectorRepositories(final Set<String> repositories, final Set<String> browsableRepositories) {
    Set<String> selectorRepositories = new HashSet<>(repositories);
    selectorRepositories.removeAll(browsableRepositories);
    return selectorRepositories;
  }

  private List<SelectorConfiguration> getSelectorConfigurations(final Set<String> repositories) {
    return repositoryPermissionUtil.selectorConfigurations(repositories);
  }

  private void addBrowsableRepositoriesAndSelectorPermissions(
      final SqlSearchQueryBuilder queryBuilder,
      final Set<String> repositories,
      final Set<String> browsableRepositories)
  {
    Set<String> selectorRepositories = getSelectorRepositories(repositories, browsableRepositories);
    List<SelectorConfiguration> selectors = getSelectorConfigurations(selectorRepositories);
    createSqlCondition(browsableRepositories, selectorRepositories, selectors).ifPresent(queryBuilder::add);
  }

  private Optional<SqlSearchQueryCondition> createSqlCondition(
      final Set<String> browsableRepositories,
      final Set<String> selectorRepositories,
      final List<SelectorConfiguration> selectorConfigs)
  {

    if (browsableRepositories.isEmpty() && selectorConfigs.isEmpty()) {
      throw new SqlSearchPermissionException("User is not permitted.");
    }

    List<SqlSearchQueryCondition> conditions = new ArrayList<>();
    of(browsableRepositories).flatMap(this::createRepositoryCondition).ifPresent(conditions::add);
    of(selectorRepositories).flatMap(repos -> createContentSelectorCondition(selectorConfigs, repos))
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
      final Set<String> repositories)
  {
    if (selectors.isEmpty()) {
      return empty();
    }

    SqlSearchContentSelectorFilter contentAuthFilter =
        contentSelectorFilterGenerator.createFilter(selectors, repositories);

    if (contentAuthFilter.hasFilters()) {
      return of(new SqlSearchQueryCondition(contentAuthFilter.queryFormat(), contentAuthFilter.queryParameters()));
    }
    return empty();
  }
}
