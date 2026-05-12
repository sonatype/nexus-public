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
package org.sonatype.nexus.repository.search.sql.query.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Iterables.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.IN;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SqlSearchPermissionBuilderTest
{
  private static final String REPO1 = "repo1";

  private static final String REPO2 = "repo2";

  private static final String REPO3 = "repo3";

  private static final String RAW = "raw";

  public static final String CONTENT_SELECTOR_FILTER_FORMAT = "contentSelectorFilterFormat";

  public static final Map<String, String> CONTENT_SELECTOR_PARAMS = ImmutableMap.of("key1", "value1");

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SearchContentSelectorSqlFilterGenerator contentSelectorFilterGenerator;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  private Expression contentSelectorsCondition;

  private SqlSearchPermissionBuilder underTest;

  @Before
  public void setup() {
    contentSelectorsCondition = mock(Expression.class);
    // when(conditionBuilderMapping.getConditionBuilder(any())).thenReturn(conditionBuilder);

    Map<String, SearchMappings> searchMappings = new HashMap<>();
    searchMappings.put("default", new DefaultSearchMappings());
    underTest = new SqlSearchPermissionBuilder(repositoryManager, securityHelper, selectorManager,
        contentSelectorFilterGenerator);
  }

  @Test(expected = SqlSearchPermissionException.class)
  public void throwExceptionWhenSpecifiedRepositoryIsNotPermitted() {
    Set<String> repositories = of(REPO1, REPO2);
    mockRepositoryManager(repositories);

    underTest.build(searchRequest(repositories));
  }

  @Test(expected = UnknownRepositoriesException.class)
  public void throwExceptionWhenUnknownRepositoriesSpecified() {
    underTest.build(searchRequest(of(REPO1, REPO2)));
  }

  @Test
  public void shouldIgnoreUnknownRepositoryWhenRequestAlsoHasKnownRepository() {
    Set<String> knownRepositories = of(REPO1);
    mockRepositoryManager(knownRepositories);
    mockBrowsableRepositories(knownRepositories);

    Set<String> specifiedRepositories = new HashSet<>(knownRepositories);
    specifiedRepositories.add(REPO2);
    Optional<Expression> result = underTest.build(searchRequest(specifiedRepositories));

    assertQueryCondition(result, new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME, new ExactTerm(REPO1)));
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryIsBrowsable() {
    Set<String> repositories = of(REPO1);
    mockRepositoryManager(repositories);
    mockBrowsableRepositories(repositories);

    Optional<Expression> result = underTest.build(searchRequest(repositories));

    assertQueryCondition(result, new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME, new ExactTerm(REPO1)));
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryIsReadableOnly() {
    Set<String> repositories = of(REPO1);
    mockRepositoryManager(repositories);
    mockReadableRepositories(repositories);

    Optional<Expression> result = underTest.build(searchRequest(repositories));

    assertQueryCondition(result, new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME, new ExactTerm(REPO1)));
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoryHasContentSelectors() {
    Set<String> repositories = of(REPO1);
    mockRepositoryManager(repositories);
    mockContentSelectorConfigurations(repositories);

    Optional<Expression> result = underTest.build(searchRequest(repositories));

    assertQueryCondition(result, contentSelectorsCondition);
    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO1)));
  }

  @Test
  public void addFiltersWhenSpecifiedRepositoriesAreBrowsableAndHasContentSelectors() {
    Set<String> repositories = of(REPO1, REPO2);

    mockBrowsableRepositoriesAndContentSelectors(repositories);

    Optional<Expression> result = underTest.build(searchRequest(repositories));

    assertQueryCondition(result,
        or(new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME, new ExactTerm(REPO2)), contentSelectorsCondition));

    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO1)));
  }

  @Test(expected = SqlSearchPermissionException.class)
  public void throwExceptionWhenNoRepositoryPermittedForFormat() {
    mockRepositoryManager(of(REPO1, REPO2));

    underTest.build(searchRequest());
  }

  @Test
  public void shouldAddFiltersWhenFormatRepositoriesHaveContentSelectors() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockRepositoryManager(repositoryNames);
    mockContentSelectorConfigurations(repositoryNames);

    Optional<Expression> result = underTest.build(searchRequest());

    assertQueryCondition(result, or(contentSelectorsCondition, contentSelectorsCondition));
    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO1)));
    verify(contentSelectorFilterGenerator).createFilter(any(), eq(Collections.singleton(REPO2)));
  }

  @Test
  public void shouldNotAddFilterWhenRequestContainsNoRepositoriesAndAllRepositoriesAreBrowsable() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockRepositoryManager(repositoryNames);
    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class), any(RepositoryViewPermission.class)))
        .thenReturn(true);

    Optional<Expression> result = underTest.build(searchRequest());

    assertFalse(result.isPresent());

    verify(securityHelper, times(repositoryNames.size())).anyPermitted(any(RepositoryViewPermission.class),
        any(RepositoryViewPermission.class));
    verify(selectorManager, never()).browseActive(any(), any());
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void shouldCheckBrowsePermsButNotContentSelectorsWhenAuthorizationIsFalse() {
    Set<String> repositoryNames = of(REPO1, REPO2);
    mockBrowsableRepositoriesAndContentSelectors(repositoryNames);

    Optional<Expression> result =
        underTest.build(SearchRequest.builder().repositories(repositoryNames).disableAuthorization().build());

    assertThat(result,
        is(Optional.of(new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME, new ExactTerm(REPO2)))));

    verify(securityHelper, times(repositoryNames.size())).anyPermitted(any(RepositoryViewPermission.class),
        any(RepositoryViewPermission.class));
    verify(selectorManager, never()).browseActive(any(), any());
    verify(contentSelectorFilterGenerator, never()).createFilter(any(), any());
  }

  @Test
  public void shouldSplitRepositoryNameFilter() {
    mockRepositoryManager(Arrays.asList(REPO1, REPO2, REPO3));
    mockBrowsableRepositories(Arrays.asList(REPO1, REPO2, REPO3));
    String repositoryFilter = String.format("%s %s   %s", REPO1, REPO2, REPO3);
    SearchRequest searchRequest = SearchRequest.builder().searchFilter(REPOSITORY_NAME, repositoryFilter).build();

    Optional<Expression> exp = underTest.build(searchRequest);

    assertTrue(exp.isPresent());

    assertThat(exp.get(), is(new SqlPredicate(IN, SearchField.REPOSITORY_NAME,
        TermCollection.create(new ExactTerm("repo1"), new ExactTerm("repo2"), new ExactTerm("repo3")))));
  }

  @Test(expected = ValidationErrorsException.class)
  public void shouldThrowExceptionForLeadingWildcard() {
    Set<String> repositories = of("maven-re*", "maven-s*", "*maven");
    mockRepositoryManager(repositories);
    mockBrowsableRepositories(repositories);

    underTest.build(searchRequest(repositories));
  }

  @Test(expected = ValidationErrorsException.class)
  public void shouldThrowExceptionForShortTrailingWildcard() {
    Set<String> repositories = of("maven-re*", "maven-s*", "1.*");
    mockRepositoryManager(repositories);
    mockBrowsableRepositories(repositories);

    underTest.build(searchRequest(repositories));
  }

  private void assertQueryCondition(final Optional<Expression> expression, final Expression expected) {
    assertThat(expression.isPresent(), is(true));
    assertThat(expression.get(), is(expected));
  }

  private void mockBrowsableRepositories(final Collection<String> browsableRepos) {
    browsableRepos.forEach(repository -> {
      when(securityHelper.anyPermitted(new RepositoryViewPermission(RAW, repository, BreadActions.BROWSE),
          new RepositoryViewPermission(RAW, repository, BreadActions.READ))).thenReturn(true);
    });
  }

  private void mockReadableRepositories(final Collection<String> readableRepos) {
    readableRepos.forEach(repository -> {
      when(securityHelper.anyPermitted(new RepositoryViewPermission(RAW, repository, BreadActions.BROWSE),
          new RepositoryViewPermission(RAW, repository, BreadActions.READ))).thenReturn(true);
      when(securityHelper.anyPermitted(new RepositoryViewPermission(RAW, repository, BreadActions.BROWSE)))
          .thenReturn(false);
    });
  }

  private void mockContentSelectorConfigurations(final Set<String> selectorRepositories) {
    selectorRepositories.forEach(repository -> {
      SelectorConfiguration selector = mock(SelectorConfiguration.class);
      when(selectorManager.browseActive(Collections.singleton(repository), Collections.singleton(RAW)))
          .thenReturn(Collections.singletonList(selector));

      Optional<Expression> contentSelectorFilter = Optional.of(contentSelectorsCondition);
      when(contentSelectorFilterGenerator.createFilter(selector, Collections.singleton(repository)))
          .thenReturn(contentSelectorFilter);
    });
  }

  private void mockBrowsableRepositoriesAndContentSelectors(final Set<String> repositories) {
    mockRepositoryManager(repositories);
    mockContentSelectorConfigurations(of(get(repositories, 0)));
    mockBrowsableRepositories(of(get(repositories, 1)));
  }

  private void mockRepositoryManager(final Collection<String> repositoryNames) {
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
    return SearchRequest.builder().repositories(repositories).build();
  }

  private static SearchRequest searchRequest(final Collection<String> repositories) {
    return SearchRequest.builder().repositories(repositories).build();
  }

  private static Expression or(final Expression... expressions) {
    return SqlClause.create(Operand.OR, expressions);
  }
}
