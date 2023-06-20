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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.sql.SqlSearchContentSelectorFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilderMapping;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Iterables.get;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.SEARCH_REPOSITORY_NAME;

public class SqlSearchPermissionBuilderTest
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
  private TableSearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator;

  @Mock
  private SqlSearchQueryBuilder queryBuilder;

  @Mock
  private SqlSearchContentSelectorFilter contentSelectorFilter;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  private SqlSearchQueryCondition browsableReposCondition;

  private SqlSearchQueryCondition contentSelectorsCondition;

  private SqlSearchPermissionBuilder underTest;

  @Before
  public void setup() {
    browsableReposCondition = new SqlSearchQueryCondition("browsableReposCondition", new HashMap<>());
    contentSelectorsCondition = new SqlSearchQueryCondition("contentSelectorsCondition", new HashMap<>());
    when(conditionBuilderMapping.getConditionBuilder(any())).thenReturn(conditionBuilder);

    Map<String, SearchMappings> searchMappings = new HashMap<>();
    searchMappings.put("default", new DefaultSearchMappings());
    underTest = new SqlSearchPermissionBuilder(searchMappings, repositoryManager, securityHelper, selectorManager,
        conditionBuilderMapping, contentSelectorFilterGenerator);
  }

  @Test(expected = SqlSearchPermissionException.class)
  public void throwExceptionWhenSpecifiedRepositoryIsNotPermitted() {
    Set<String> repositories = of(REPO1, REPO2);
    mockRepositoryManager(repositories);

    underTest.build(queryBuilder, searchRequest(repositories));
  }

  @Test(expected = UnknownRepositoriesException.class)
  public void throwExceptionWhenUnknownRepositoriesSpecified() {
    underTest.build(queryBuilder, searchRequest(of(REPO1, REPO2)));
  }

  @Test
  public void shouldIgnoreUnknownRepositoryWhenRequestAlsoHasKnownRepository() {
    Set<String> knownRepositories = of(REPO1);
    mockRepositoryManager(knownRepositories);
    mockBrowsableRepositories(knownRepositories);

    Set<String> specifiedRepositories = new HashSet<>(knownRepositories);
    specifiedRepositories.add(REPO2);
    SqlSearchQueryBuilder result = underTest.build(queryBuilder, searchRequest(specifiedRepositories));

    assertQueryCondition(result, browsableReposCondition.getSqlConditionFormat());
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any(), anyInt());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryIsBrowsable() {
    Set<String> repositories = of(REPO1);
    mockRepositoryManager(repositories);
    mockBrowsableRepositories(repositories);

    SqlSearchQueryBuilder result = underTest.build(queryBuilder, searchRequest(repositories));

    assertQueryCondition(result, browsableReposCondition.getSqlConditionFormat());
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any(), anyInt());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryHasContentSelectors() {
    Set<String> repositories = of(REPO1);
    mockRepositoryManager(repositories);
    mockContentSelectorConfigurations(repositories);

    SqlSearchQueryBuilder result = underTest.build(queryBuilder, searchRequest(repositories));

    assertQueryCondition(result, contentSelectorsCondition.getSqlConditionFormat());
    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO1)), anyInt());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoriesAreBrowsableAndHasContentSelectors() {
    Set<String> repositories = of(REPO1, REPO2);

    mockBrowsableRepositoriesAndContentSelectors(repositories);

    SqlSearchQueryBuilder result = underTest.build(queryBuilder, searchRequest(repositories));

    assertQueryCondition(result, wrapInBrackets( browsableReposCondition.getSqlConditionFormat()) + " OR "
        + wrapInBrackets(contentSelectorsCondition.getSqlConditionFormat()));

    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO1)), anyInt());
  }

  @Test(expected = SqlSearchPermissionException.class)
  public void throwExceptionWhenNoRepositoryPermittedForFormat() {
    mockRepositoryManager(of(REPO1, REPO2));

    underTest.build(queryBuilder, searchRequest());
  }

  @Test
  public void shouldAddFiltersWhenFormatRepositoriesHaveContentSelectors() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockRepositoryManager(repositoryNames);
    mockContentSelectorConfigurations(repositoryNames);

    SqlSearchQueryBuilder result = underTest.build(queryBuilder, searchRequest());

    assertQueryCondition(result, wrapInBrackets(contentSelectorsCondition.getSqlConditionFormat()) +
        " OR " + wrapInBrackets(contentSelectorsCondition.getSqlConditionFormat()));
    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO1)), anyInt());
    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO2)), anyInt());
  }

  @Test
  public void shouldNotAddFilterWhenRequestContainsNoRepositoriesAndAllRepositoriesAreBrowsable() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockRepositoryManager(repositoryNames);
    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(true);

    SqlSearchQueryBuilder result = underTest.build(queryBuilder, searchRequest());

    assertThat(result, is(queryBuilder));

    verify(securityHelper, times(repositoryNames.size())).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager, never()).browseActive(any(), any());
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any(), anyInt());
  }

  @Test
  public void shouldCheckBrowsePermsButNotContentSelectorsWhenAuthorizationIsFalse() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockBrowsableRepositoriesAndContentSelectors(repositoryNames);

    SqlSearchQueryBuilder result = underTest.build(queryBuilder, SearchRequest.builder().repositories(repositoryNames).disableAuthorization().build());

    assertThat(result.buildQuery(),
        is(Optional.of(new SqlSearchQueryCondition("browsableReposCondition", emptyMap()))));

    verify(securityHelper, times(repositoryNames.size())).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager, never()).browseActive(any(), any());
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any(), anyInt());
  }

  @Test
  public void shouldThrowExceptionWhenLeadingWildcard() {
    Assert.assertThrows("Leading wildcards are prohibited",
        ValidationErrorsException.class,
        () -> underTest.build(queryBuilder, searchRequest("*osted-repo1")));
  }

  @Test
  public void shouldThrowExceptionWhenWildcardHasLessThenThreeSymbols() {
    Assert.assertThrows("3 characters or more are required with a trailing wildcard (*)",
        ValidationErrorsException.class,
        () -> underTest.build(queryBuilder, searchRequest("ho*")));
  }

  private void assertQueryCondition(final SqlSearchQueryBuilder queryBuilder, final String sqlConditionFormat) {
    Optional<SqlSearchQueryCondition> query = queryBuilder.buildQuery();
    assertThat(query.isPresent(), is(true));
    assertThat(query.get().getSqlConditionFormat(), is(sqlConditionFormat));
  }

  private void mockBrowsableRepositories(final Set<String> browsableRepos) {
    browsableRepos.forEach(repository -> {
      when(securityHelper.anyPermitted(new RepositoryViewPermission(RAW, repository, BreadActions.READ)))
          .thenReturn(true);
    });

    Set<String> prefixedBrowseableRepos = browsableRepos.stream()
      .map(repositoryName -> "/" + repositoryName)
      .collect(Collectors.toSet());
    when(conditionBuilder.condition(SEARCH_REPOSITORY_NAME, prefixedBrowseableRepos))
        .thenReturn(browsableReposCondition);
  }

  private void mockContentSelectorConfigurations(final Set<String> selectorRepositories)
  {
    selectorRepositories.forEach(repository -> {
      SelectorConfiguration selector = mock(SelectorConfiguration.class);
      when(selectorManager.browseActive(eq(Collections.singleton(repository)), eq(Collections.singleton(RAW))))
          .thenReturn(Collections.singletonList(selector));

      Optional<SqlSearchQueryCondition> contentSelectorFilter = Optional.of(contentSelectorsCondition);
      when(contentSelectorFilterGenerator.createFilter(eq(selector), eq(Collections.singleton(repository)),
          anyInt())).thenReturn(contentSelectorFilter);
    });

    when(contentSelectorFilter.hasFilters()).thenReturn(true);
    when(contentSelectorFilter.queryFormat()).thenReturn(CONTENT_SELECTOR_FILTER_FORMAT);
    when(contentSelectorFilter.queryParameters()).thenReturn(CONTENT_SELECTOR_PARAMS);
  }

  private void mockBrowsableRepositoriesAndContentSelectors(final Set<String> repositories) {
    mockRepositoryManager(repositories);
    mockContentSelectorConfigurations(of(get(repositories, 0)));
    mockBrowsableRepositories(of(get(repositories, 1)));
  }

  private void mockRepositoryManager(final Set<String> repositoryNames) {
    List<Repository> repositories = new ArrayList<>();

    repositoryNames.forEach(name -> {
      Repository repository = mockRepository(name);
      repositories.add(repository);
      when(repositoryManager.get(name)).thenReturn(repository);
    });

    when(repositoryManager.browse()).thenReturn(repositories);
  }

  private static Repository mockRepository(final String name) {
    Repository repository = mock(Repository.class);

    when(repository.getName()).thenReturn(name);

    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(RAW);
    when(repository.getFormat()).thenReturn(format);

    return repository;
  }

  private static SearchRequest searchRequest(final String... repositories) {
    return SearchRequest.builder()
        .repositories(repositories)
        .build();
  }

  private static SearchRequest searchRequest(final Collection<String> repositories) {
    return SearchRequest.builder()
        .repositories(repositories)
        .build();
  }

  private static String wrapInBrackets(final String input) {
    return "(" + input + ")";
  }
}
