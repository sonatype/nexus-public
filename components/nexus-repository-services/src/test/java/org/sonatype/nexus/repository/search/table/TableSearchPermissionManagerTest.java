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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.search.SqlSearchRepositoryNameUtil;
import org.sonatype.nexus.repository.search.sql.SqlSearchContentSelectorFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;
import org.sonatype.nexus.selector.SelectorConfiguration;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Iterables.get;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.SEARCH_REPOSITORY_NAME;

public class TableSearchPermissionManagerTest
    extends TestSupport
{
  private static final String REPO1 = "repo1";

  private static final String REPO2 = "repo2";

  private static final String RAW = "raw";

  public static final String CONTENT_SELECTOR_FILTER_FORMAT = "contentSelectorFilterFormat";

  public static final Map<String, String> CONTENT_SELECTOR_PARAMS = ImmutableMap.of("key1", "value1");

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SqlSearchQueryConditionBuilderMapping conditionBuilderMapping;

  @Mock
  private SqlSearchQueryConditionBuilder conditionBuilder;

  @Mock
  private SqlSearchRepositoryNameUtil repositoryNameUtil;

  @Mock
  private TableSearchRepositoryPermissionUtil repositoryPermissionUtil;

  @Mock
  private TableSearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator;

  @Mock
  private SqlSearchQueryBuilder queryBuilder;

  @Mock
  private SqlSearchQueryCondition browsableReposCondition;

  @Mock
  private SqlSearchQueryCondition allPermittedReposCondition;

  @Mock
  private SelectorConfiguration selectorConfig1;

  @Mock
  private SqlSearchContentSelectorFilter contentSelectorFilter;

  private TableSearchPermissionManager underTest;

  @Before
  public void setup() {
    Map<String, SearchMappings> searchMappings = new HashMap<>();
    searchMappings.put("default", new DefaultSearchMappings());
    when(conditionBuilderMapping.getConditionBuilder(any())).thenReturn(conditionBuilder);

    underTest = new TableSearchPermissionManager(conditionBuilderMapping, repositoryNameUtil,
        repositoryPermissionUtil, searchMappings, contentSelectorFilterGenerator, repositoryManager);
  }

  @Test(expected = SqlSearchPermissionException.class)
  public void throwExceptionWhenSpecifiedRepositoryIsNotPermitted() {
    String repositoryFilter = REPO1 + " " + REPO2;
    when(repositoryNameUtil.getRepositoryNames(repositoryFilter)).thenReturn(of(REPO1, REPO2));

    underTest.addPermissionFilters(queryBuilder, repositoryFilter);
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryIsBrowsable() {
    Set<String> repositories = of(REPO1);
    when(repositoryNameUtil.getRepositoryNames(REPO1)).thenReturn(repositories);
    mockRepositoryManager(repositories);
    mockBrowsableRepositories(repositories, repositories);

    underTest.addPermissionFilters(queryBuilder, REPO1);

    verify(queryBuilder).add(allPermittedReposCondition);
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryHasContentSelectors() {
    Set<String> repositories = of(REPO1);
    when(repositoryNameUtil.getRepositoryNames(REPO1)).thenReturn(repositories);
    mockContentSelectorConfigurations(repositories);

    underTest.addPermissionFilters(queryBuilder, REPO1);

    verify(queryBuilder).add(allPermittedReposCondition);
    verify(contentSelectorFilterGenerator).createFilter(any(), any());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoriesAreBrowsableAndHasContentSelectors() {
    String repositoryFilter = REPO1 + " " + REPO2;
    Set<String> repositories = of(REPO1, REPO2);

    when(repositoryNameUtil.getRepositoryNames(repositoryFilter)).thenReturn(repositories);
    mockBrowsableRepositoriesAndContentSelectors(repositories);

    underTest.addPermissionFilters(queryBuilder, repositoryFilter);

    verify(queryBuilder).add(allPermittedReposCondition);
    verify(contentSelectorFilterGenerator).createFilter(any(), any());
    verify(conditionBuilder).combine(asList(browsableReposCondition, contentSelectorCondition()));
  }

  @Test(expected = SqlSearchPermissionException.class)
  public void throwExceptionWhenNoRepositoryPermittedForFormat() {
    mockRepositoryManager(of(REPO1, REPO2));

    underTest.addPermissionFilters(queryBuilder, null);
  }

  @Test
  public void shouldNotAddFilterWhenAllFormatRepositoriesAreBrowsable() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockRepositoryManager(repositoryNames);
    when(repositoryPermissionUtil.browsableAndUnknownRepositories(repositoryNames)).thenReturn(repositoryNames);

    underTest.addPermissionFilters(queryBuilder, null);

    verify(queryBuilder, never()).add(any());
    verify(repositoryPermissionUtil).browsableAndUnknownRepositories(repositoryNames);
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void shouldAddFiltersWhenFormatRepositoriesHaveContentSelectors() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockRepositoryManager(repositoryNames);
    mockContentSelectorConfigurations(repositoryNames);

    underTest.addPermissionFilters(queryBuilder, null);

    verify(queryBuilder).add(allPermittedReposCondition);
    verify(contentSelectorFilterGenerator).createFilter(any(), any());
  }

  private void mockBrowsableRepositories(
      final Set<String> repositories,
      final Set<String> browsableRepos)
  {
    when(repositoryPermissionUtil.browsableAndUnknownRepositories(repositories)).thenReturn(browsableRepos);

    Set<String> prefixedBrowseableRepos = browsableRepos.stream()
      .map(repositoryName -> "/" + repositoryName)
      .collect(Collectors.toSet());
    when(conditionBuilder.condition(SEARCH_REPOSITORY_NAME, prefixedBrowseableRepos))
        .thenReturn(browsableReposCondition);
    when(conditionBuilder.combine(singletonList(browsableReposCondition))).thenReturn(allPermittedReposCondition);
  }

  private void mockContentSelectorConfigurations(final Set<String> selectorRepositories)
  {
    when(repositoryPermissionUtil.selectorConfigurations(selectorRepositories))
        .thenReturn(singletonList(selectorConfig1));

    when(contentSelectorFilterGenerator.createFilter(singletonList(selectorConfig1), selectorRepositories))
        .thenReturn(contentSelectorFilter);

    when(contentSelectorFilter.hasFilters()).thenReturn(true);
    when(contentSelectorFilter.queryFormat()).thenReturn(CONTENT_SELECTOR_FILTER_FORMAT);
    when(contentSelectorFilter.queryParameters()).thenReturn(CONTENT_SELECTOR_PARAMS);
    when(conditionBuilder.combine(singletonList(contentSelectorCondition()))).thenReturn(allPermittedReposCondition);
  }

  private void mockBrowsableRepositoriesAndContentSelectors(final Set<String> repositories) {
    mockRepositoryManager(repositories);
    mockBrowsableRepositories(repositories, of(get(repositories, 1)));
    mockContentSelectorConfigurations(of(get(repositories, 0)));
    when(conditionBuilder.combine(asList(browsableReposCondition, contentSelectorCondition())))
        .thenReturn(allPermittedReposCondition);
  }

  private SqlSearchQueryCondition contentSelectorCondition() {
    return new SqlSearchQueryCondition(CONTENT_SELECTOR_FILTER_FORMAT, CONTENT_SELECTOR_PARAMS);
  }

  private void mockRepositoryManager(final Set<String> repositoryNames) {
    List<Repository> repositories = repositoryNames.stream()
        .map(TableSearchPermissionManagerTest::mockRepository)
        .collect(Collectors.toList());
    when(repositoryManager.browse()).thenReturn(repositories);
    when(repositoryNameUtil.getFormatRepositoryNames(RAW)).thenReturn(repositoryNames);
  }

  private static Repository mockRepository(final String name) {
    Repository repository = mock(Repository.class);

    when(repository.getName()).thenReturn(name);

    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(RAW);
    when(repository.getFormat()).thenReturn(format);

    return repository;
  }
}
