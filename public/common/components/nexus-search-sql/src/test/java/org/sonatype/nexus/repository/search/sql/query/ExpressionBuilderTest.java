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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMapping.FilterType;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.query.security.SqlSearchPermissionBuilder;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.sql.SearchField.ASSET_FORMAT_VALUE_1;
import static org.sonatype.nexus.repository.rest.sql.SearchField.ASSET_FORMAT_VALUE_2;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ExpressionBuilderTest
{
  @Mock
  private SqlSearchQueryContribution defaultHandler;

  @Mock
  private SqlSearchQueryContribution groupRawHandler;

  @Mock
  private SqlSearchQueryContribution versionHandler;

  @Mock
  private SqlSearchQueryContribution architectureHandler;

  @Mock
  private SqlSearchQueryContribution osHandler;

  @Mock
  private SqlSearchPermissionBuilder permissions;

  private ExpressionBuilder underTest;

  private MockedStatic<QualifierUtil> mockedStatic;

  @Before
  public void setup() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    Map<String, SqlSearchQueryContribution> handlers = getHandlersMap();
    Map<String, SearchMappings> searchMappings = getSearchMappingsMap();

    List<SqlSearchQueryContribution> handlersList = new ArrayList<>(handlers.values());
    List<SearchMappings> searchMappingsList = new ArrayList<>(searchMappings.values());

    when(defaultHandler.createPredicate(any())).thenReturn(Optional.empty());
    when(QualifierUtil.buildQualifierBeanMap(handlersList)).thenReturn(handlers);
    when(QualifierUtil.buildQualifierBeanMap(searchMappingsList)).thenReturn(searchMappings);
    underTest = new ExpressionBuilder(permissions, handlersList, searchMappingsList);
  }

  @After
  public void tearDown() {
    mockedStatic.close();
  }

  @Test
  public void shouldBuildQueryConditionsForSearchFilters() {
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "junit org.mockito");
    SearchFilter versionFilter = new SearchFilter("version", "4.13 3.2.0");
    SearchRequest request = request(groupRawFilter, versionFilter);

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);

    verifyNoMoreInteractions(defaultHandler, versionHandler, groupRawHandler);
  }

  @Test
  public void shouldBuildQueryConditionsForAssetSearchFilters() {
    SearchFilter architectureFilter = new SearchFilter("architecture", "x86");
    SearchFilter osFilter = new SearchFilter("os", "Windows");
    SearchRequest request = request(architectureFilter, osFilter);

    Expression architectureExpression = mock(Expression.class);
    when(architectureHandler.createPredicate(architectureFilter)).thenReturn(Optional.of(architectureExpression));

    Expression osExpression = mock(Expression.class);
    when(osHandler.createPredicate(osFilter)).thenReturn(Optional.of(osExpression));

    Expression assetExpressions = underTest.from(request).map(ExpressionGroup::getAssetFilters).orElse(null);

    assertThat(assetExpressions, is(notNullValue()));
    assertThat(assetExpressions, instanceOf(SqlClause.class));
    assertThat(((SqlClause) assetExpressions).expressions(), containsInAnyOrder(architectureExpression, osExpression));

    verify(architectureHandler).createPredicate(architectureFilter);
    verify(osHandler).createPredicate(osFilter);
    verify(permissions).build(request);

    verifyNoMoreInteractions(architectureHandler, osHandler, permissions);
  }

  @Test
  public void shouldBuildQueryConditionForBlankValueFilter() {
    SearchFilter versionFilter = new SearchFilter("version", "4.13 3.2.0");
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "");
    SearchRequest request = request(groupRawFilter, versionFilter);

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, notNullValue());
    assertThat(expression, instanceOf(SqlClause.class));
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);

    verifyNoMoreInteractions(defaultHandler, versionHandler, groupRawHandler);
  }

  private static SearchRequest request(final SearchFilter... filters) {
    return SearchRequest.builder().searchFilters(Arrays.asList(filters)).build();
  }

  @Nonnull
  private static Map<String, SearchMappings> getSearchMappingsMap() {
    SearchMappings searchMappings = () -> ImmutableList.<SearchMapping>builder()
        .addAll(new DefaultSearchMappings().get())
        .add(new SearchMapping("attributes.architecture", "architecture", "", ASSET_FORMAT_VALUE_1, FilterType.ASSET),
            new SearchMapping("attributes.os", "os", "", ASSET_FORMAT_VALUE_2, FilterType.ASSET))
        .build();
    return ImmutableMap.of("test", searchMappings);
  }

  @Nonnull
  private Map<String, SqlSearchQueryContribution> getHandlersMap() {
    Map<String, SqlSearchQueryContribution> handlers = new HashMap<>();

    handlers.put("default", defaultHandler);
    handlers.put("group.raw", groupRawHandler);
    handlers.put("version", versionHandler);
    handlers.put("architecture", architectureHandler);
    handlers.put("os", osHandler);
    return handlers;
  }

  @Test
  public void testExplicitAndFilter_withDisjunction() {
    // Create request with disjunction mode (global OR) and an explicit AND filter
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "junit");
    SearchFilter versionFilter = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchRequest request = SearchRequest.builder()
        .disjunction()
        .searchFilters(Arrays.asList(groupRawFilter, versionFilter))
        .build();

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));

    // Both expressions should be present - the explicit AND filter should not be OR'ed
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);
  }

  @Test
  public void testExplicitOrFilter_withConjunction() {
    // Create request with conjunction mode (global AND, default) and an explicit OR filter
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "junit");
    SearchFilter versionFilter = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.OR);
    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(groupRawFilter, versionFilter))
        .build();

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));

    // Both expressions should be present - the explicit OR filter should not be AND'ed
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);
  }

  @Test
  public void testMixedFilters_globalAndExplicit() {
    // Mix filters with no operator (use global flag) and explicit operators
    SearchFilter groupRawFilter = new SearchFilter("group.raw", "junit"); // uses global conjunction
    SearchFilter versionFilter = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(groupRawFilter, versionFilter))
        .build();

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));

    Expression groupRawExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(groupRawFilter)).thenReturn(Optional.of(groupRawExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));

    // Both expressions should be present and properly combined
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(versionExpression, groupRawExpression));

    verify(groupRawHandler).createPredicate(groupRawFilter);
    verify(versionHandler).createPredicate(versionFilter);
    verify(permissions).build(request);
  }

  @Test
  public void testMultipleAndFilters() {
    // Multiple explicit AND filters should all be AND'ed together
    SearchFilter filter1 = new SearchFilter("group.raw", "junit", SearchFilter.FilterOperator.AND);
    SearchFilter filter2 = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(filter1, filter2))
        .build();

    Expression expression1 = mock(Expression.class);
    when(groupRawHandler.createPredicate(filter1)).thenReturn(Optional.of(expression1));

    Expression expression2 = mock(Expression.class);
    when(versionHandler.createPredicate(filter2)).thenReturn(Optional.of(expression2));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));

    // Both AND filters should be present
    assertThat(((SqlClause) expression).expressions(), containsInAnyOrder(expression1, expression2));

    verify(groupRawHandler).createPredicate(filter1);
    verify(versionHandler).createPredicate(filter2);
    verify(permissions).build(request);
  }

  @Test
  public void testMultipleOrFilters() {
    // Multiple explicit OR filters should be grouped with OR
    SearchFilter filter1 = new SearchFilter("group.raw", "junit", SearchFilter.FilterOperator.OR);
    SearchFilter filter2 = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.OR);
    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(filter1, filter2))
        .build();

    Expression expression1 = mock(Expression.class);
    when(groupRawHandler.createPredicate(filter1)).thenReturn(Optional.of(expression1));

    Expression expression2 = mock(Expression.class);
    when(versionHandler.createPredicate(filter2)).thenReturn(Optional.of(expression2));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));

    // OR filters should be grouped together
    verify(groupRawHandler).createPredicate(filter1);
    verify(versionHandler).createPredicate(filter2);
    verify(permissions).build(request);
  }

  @Test
  public void testSingleExplicitFilter() {
    // Single filter with explicit operator should work correctly
    SearchFilter filter = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(filter))
        .build();

    Expression versionExpression = mock(Expression.class);
    when(versionHandler.createPredicate(filter)).thenReturn(Optional.of(versionExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));

    // Single filter should be returned without unnecessary wrapping
    verify(versionHandler).createPredicate(filter);
    verify(permissions).build(request);
  }

  // Unit tests for refactored helper methods

  @Test
  public void testPartitionFilters_withMixedOperators() {
    SearchFilter filterWithOperator = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchFilter filterWithoutOperator = new SearchFilter("group.raw", "junit");
    SearchFilter nullFilter = null;

    List<SearchFilter> filters = Arrays.asList(filterWithOperator, filterWithoutOperator, nullFilter);

    ExpressionBuilder.PartitionedFilters partitioned = underTest.partitionFilters(filters);

    assertThat(partitioned.withExplicitOperator.size(), is(1));
    assertThat(partitioned.withExplicitOperator.get(0), is(filterWithOperator));
    assertThat(partitioned.withGlobalFlag.size(), is(1));
    assertThat(partitioned.withGlobalFlag.get(0), is(filterWithoutOperator));
  }

  @Test
  public void testPartitionFilters_filtersOutRepositoryName() {
    SearchFilter repositoryFilter = new SearchFilter("repository_name", "maven-central");
    SearchFilter normalFilter = new SearchFilter("version", "4.13");

    List<SearchFilter> filters = Arrays.asList(repositoryFilter, normalFilter);

    ExpressionBuilder.PartitionedFilters partitioned = underTest.partitionFilters(filters);

    // Repository name filter should be excluded
    assertThat(partitioned.withGlobalFlag.size(), is(1));
    assertThat(partitioned.withGlobalFlag.get(0), is(normalFilter));
  }

  @Test
  public void testIsNotRepositoryNameFilter_returnsTrue() {
    SearchFilter filter = new SearchFilter("version", "4.13");
    assertThat(underTest.isNotRepositoryNameFilter(filter), is(true));
  }

  @Test
  public void testIsNotRepositoryNameFilter_returnsFalse() {
    SearchFilter filter = new SearchFilter("repository_name", "maven-central");
    assertThat(underTest.isNotRepositoryNameFilter(filter), is(false));
  }

  @Test
  public void testIsNotRepositoryNameFilter_caseInsensitive() {
    SearchFilter filter1 = new SearchFilter("REPOSITORY_NAME", "maven-central");
    SearchFilter filter2 = new SearchFilter("Repository_Name", "maven-central");

    assertThat(underTest.isNotRepositoryNameFilter(filter1), is(false));
    assertThat(underTest.isNotRepositoryNameFilter(filter2), is(false));
  }

  @Test
  public void testConvertFiltersToExpressions_success() {
    SearchFilter versionFilter = new SearchFilter("version", "4.13");
    SearchFilter groupFilter = new SearchFilter("group.raw", "junit");

    Expression versionExpression = mock(Expression.class);
    Expression groupExpression = mock(Expression.class);

    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));
    when(groupRawHandler.createPredicate(groupFilter)).thenReturn(Optional.of(groupExpression));

    List<Expression> expressions = underTest.convertFiltersToExpressions(Arrays.asList(versionFilter, groupFilter));

    assertThat(expressions.size(), is(2));
    assertThat(expressions, containsInAnyOrder(versionExpression, groupExpression));
  }

  @Test
  public void testConvertFiltersToExpressions_filtersOutEmpty() {
    SearchFilter versionFilter = new SearchFilter("version", "4.13");
    SearchFilter emptyFilter = new SearchFilter("unknown", "value");

    Expression versionExpression = mock(Expression.class);

    when(versionHandler.createPredicate(versionFilter)).thenReturn(Optional.of(versionExpression));
    when(defaultHandler.createPredicate(emptyFilter)).thenReturn(Optional.empty());

    List<Expression> expressions = underTest.convertFiltersToExpressions(Arrays.asList(versionFilter, emptyFilter));

    assertThat(expressions.size(), is(1));
    assertThat(expressions.get(0), is(versionExpression));
  }

  @Test
  public void testGroupByOperator_separatesAndOr() {
    SearchFilter andFilter1 = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchFilter andFilter2 = new SearchFilter("group.raw", "junit", SearchFilter.FilterOperator.AND);
    SearchFilter orFilter = new SearchFilter("architecture", "x86", SearchFilter.FilterOperator.OR);

    Expression expr1 = mock(Expression.class);
    Expression expr2 = mock(Expression.class);
    Expression expr3 = mock(Expression.class);

    when(versionHandler.createPredicate(andFilter1)).thenReturn(Optional.of(expr1));
    when(groupRawHandler.createPredicate(andFilter2)).thenReturn(Optional.of(expr2));
    when(architectureHandler.createPredicate(orFilter)).thenReturn(Optional.of(expr3));

    Map<SearchFilter.FilterOperator, List<Expression>> grouped =
        underTest.groupByOperator(Arrays.asList(andFilter1, andFilter2, orFilter));

    assertThat(grouped.get(SearchFilter.FilterOperator.AND).size(), is(2));
    assertThat(grouped.get(SearchFilter.FilterOperator.AND), containsInAnyOrder(expr1, expr2));
    assertThat(grouped.get(SearchFilter.FilterOperator.OR).size(), is(1));
    assertThat(grouped.get(SearchFilter.FilterOperator.OR).get(0), is(expr3));
  }

  @Test
  public void testWrapExpressionsWithOperand_emptyList() {
    List<Expression> expressions = new ArrayList<>();
    Optional<Expression> result = underTest.wrapExpressionsWithOperand(expressions,
        org.sonatype.nexus.repository.search.sql.query.syntax.Operand.AND);

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testWrapExpressionsWithOperand_singleExpression() {
    Expression expr = mock(Expression.class);
    List<Expression> expressions = Arrays.asList(expr);

    Optional<Expression> result = underTest.wrapExpressionsWithOperand(expressions,
        org.sonatype.nexus.repository.search.sql.query.syntax.Operand.AND);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), is(expr));
  }

  @Test
  public void testWrapExpressionsWithOperand_multipleExpressions() {
    Expression expr1 = mock(Expression.class);
    Expression expr2 = mock(Expression.class);
    List<Expression> expressions = Arrays.asList(expr1, expr2);

    Optional<Expression> result = underTest.wrapExpressionsWithOperand(expressions,
        org.sonatype.nexus.repository.search.sql.query.syntax.Operand.AND);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), instanceOf(SqlClause.class));
    assertThat(((SqlClause) result.get()).expressions(), containsInAnyOrder(expr1, expr2));
  }

  @Test
  public void testAllThreeFilterTypes_globalAndExplicitAndOr() {
    // Test mixing global filter + explicit AND + explicit OR (all component-level filters)
    SearchFilter globalFilter = new SearchFilter("group.raw", "junit"); // uses global conjunction (AND)
    SearchFilter explicitAndFilter1 = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchFilter explicitAndFilter2 = new SearchFilter("version", "5.0", SearchFilter.FilterOperator.AND);
    SearchFilter explicitOrFilter = new SearchFilter("group.raw", "mockito", SearchFilter.FilterOperator.OR);

    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(globalFilter, explicitAndFilter1, explicitAndFilter2, explicitOrFilter))
        .build();

    Expression globalExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(globalFilter)).thenReturn(Optional.of(globalExpression));

    Expression andExpression1 = mock(Expression.class);
    when(versionHandler.createPredicate(explicitAndFilter1)).thenReturn(Optional.of(andExpression1));

    Expression andExpression2 = mock(Expression.class);
    when(versionHandler.createPredicate(explicitAndFilter2)).thenReturn(Optional.of(andExpression2));

    Expression orExpression = mock(Expression.class);
    when(groupRawHandler.createPredicate(explicitOrFilter)).thenReturn(Optional.of(orExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));

    // All four filter instances should be processed
    verify(groupRawHandler).createPredicate(globalFilter);
    verify(versionHandler).createPredicate(explicitAndFilter1);
    verify(versionHandler).createPredicate(explicitAndFilter2);
    verify(groupRawHandler).createPredicate(explicitOrFilter);
    verify(permissions).build(request);

    // Top-level should have 4 expressions: 1 global + 2 explicit AND + 1 explicit OR
    assertThat(((SqlClause) expression).expressions().size(), is(4));
  }

  @Test
  public void testExplicitOperatorsOnly_andPlusOr() {
    // Test with both explicit AND and OR, but no global filters (all component-level)
    SearchFilter explicitAndFilter = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.AND);
    SearchFilter explicitOrFilter1 = new SearchFilter("group.raw", "junit", SearchFilter.FilterOperator.OR);
    SearchFilter explicitOrFilter2 = new SearchFilter("group.raw", "mockito", SearchFilter.FilterOperator.OR);

    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(explicitAndFilter, explicitOrFilter1, explicitOrFilter2))
        .build();

    Expression andExpression = mock(Expression.class);
    when(versionHandler.createPredicate(explicitAndFilter)).thenReturn(Optional.of(andExpression));

    Expression orExpression1 = mock(Expression.class);
    when(groupRawHandler.createPredicate(explicitOrFilter1)).thenReturn(Optional.of(orExpression1));

    Expression orExpression2 = mock(Expression.class);
    when(groupRawHandler.createPredicate(explicitOrFilter2)).thenReturn(Optional.of(orExpression2));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));
    assertThat(expression, instanceOf(SqlClause.class));

    // AND expression is added directly, OR expressions are wrapped together in an OR clause
    // Then all top-level expressions are AND'ed: 1 AND + 1 OR clause = 2 top-level expressions
    verify(versionHandler).createPredicate(explicitAndFilter);
    verify(groupRawHandler).createPredicate(explicitOrFilter1);
    verify(groupRawHandler).createPredicate(explicitOrFilter2);
    verify(permissions).build(request);

    // Should have 2 top-level expressions: 1 AND + 1 OR clause (containing 2 OR expressions)
    assertThat(((SqlClause) expression).expressions().size(), is(2));
  }

  @Test
  public void testSingleExplicitOrFilter() {
    // Test with just a single explicit OR filter
    SearchFilter orFilter = new SearchFilter("version", "4.13", SearchFilter.FilterOperator.OR);

    SearchRequest request = SearchRequest.builder()
        .searchFilters(Arrays.asList(orFilter))
        .build();

    Expression orExpression = mock(Expression.class);
    when(versionHandler.createPredicate(orFilter)).thenReturn(Optional.of(orExpression));

    Expression expression = underTest.from(request).map(ExpressionGroup::getComponentFilters).orElse(null);

    assertThat(expression, is(notNullValue()));

    // Single OR filter should be returned without unnecessary wrapping
    verify(versionHandler).createPredicate(orFilter);
    verify(permissions).build(request);
  }
}
