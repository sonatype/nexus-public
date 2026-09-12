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
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.repository.search.ComponentVersionPage;
import org.sonatype.nexus.repository.search.ComponentVersionQuery;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.store.ComponentVersionData;
import org.sonatype.nexus.repository.search.sql.store.ComponentVersionSearchRequest;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SqlComponentVersionSearchTest
{
  private SearchStore searchStore;

  private ExpressionBuilder expressionBuilder;

  private SearchConditionFactory conditionFactory;

  private SqlComponentVersionSearch underTest;

  @BeforeEach
  void setUp() {
    searchStore = mock(SearchStore.class);
    expressionBuilder = mock(ExpressionBuilder.class);
    conditionFactory = mock(SearchConditionFactory.class);
    underTest = new SqlComponentVersionSearch(searchStore, expressionBuilder, conditionFactory);
  }

  @Test
  void splitsRepositoryAggregateIntoAList() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    ComponentVersionData row = new ComponentVersionData();
    row.setVersion("1.0.0");
    row.setRepositories("releases,snapshots");
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of(row));
    when(searchStore.countComponentVersions(any())).thenReturn(1L);

    ComponentVersionPage page = underTest.browseVersions(query("version", 0, 20));

    assertThat(page.items()).hasSize(1);
    assertThat(page.items().get(0).repositories()).containsExactly("releases", "snapshots");
    assertThat(page.total()).isEqualTo(1L);
  }

  @Test
  void treatsNullAggregateAsAnEmptyRepositoryList() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    ComponentVersionData row = new ComponentVersionData();
    row.setVersion("1.0.0");
    row.setRepositories(null);
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of(row));
    when(searchStore.countComponentVersions(any())).thenReturn(1L);

    assertThat(underTest.browseVersions(query("version", 0, 20)).items().get(0).repositories()).isEmpty();
  }

  @Test
  void translatesPageAndSizeToLimitAndOffset() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of());
    when(searchStore.countComponentVersions(any())).thenReturn(0L);

    underTest.browseVersions(query("version", 3, 25));

    ArgumentCaptor<ComponentVersionSearchRequest> captor =
        ArgumentCaptor.forClass(ComponentVersionSearchRequest.class);
    verify(searchStore).browseComponentVersions(captor.capture());
    assertThat(captor.getValue().limit).isEqualTo(25);
    assertThat(captor.getValue().offset).isEqualTo(75);
  }

  @Test
  void mapsSortKeyToSqlExpression() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of());
    when(searchStore.countComponentVersions(any())).thenReturn(0L);

    underTest.browseVersions(query("repositories", 0, 20));

    ArgumentCaptor<ComponentVersionSearchRequest> captor =
        ArgumentCaptor.forClass(ComponentVersionSearchRequest.class);
    verify(searchStore).browseComponentVersions(captor.capture());
    assertThat(captor.getValue().sortExpression).isEqualTo("COUNT(DISTINCT cs.search_repository_name)");
  }

  @Test
  void returnsEmptyPageWithoutQueryingWhenNoConditionCanBeBuilt() {
    when(expressionBuilder.from(any())).thenReturn(Optional.empty());

    ComponentVersionPage page = underTest.browseVersions(query("version", 0, 20));

    assertThat(page.items()).isEmpty();
    assertThat(page.total()).isZero();
    verifyNoInteractions(searchStore);
  }

  private void stubCondition(final String sqlFilter, final Map<String, Object> params) {
    ExpressionGroup group = mock(ExpressionGroup.class);
    when(expressionBuilder.from(any())).thenReturn(Optional.of(group));
    SqlSearchQueryConditionGroup conditionGroup = mock(SqlSearchQueryConditionGroup.class);
    when(conditionGroup.getComponentCondition())
        .thenReturn(new SqlSearchQueryCondition(sqlFilter, params));
    when(conditionFactory.build(group)).thenReturn(conditionGroup);
  }

  @Test
  void buildsCaseInsensitiveSubstringPatternForVersionFilter() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of());
    when(searchStore.countComponentVersions(any())).thenReturn(0L);

    underTest.browseVersions(queryWithFilter("1.0.0-RC1"));

    assertThat(capturedRequest().versionLikePattern).isEqualTo("%1.0.0-rc1%");
  }

  @Test
  void escapesLikeWildcardsInVersionFilter() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of());
    when(searchStore.countComponentVersions(any())).thenReturn(0L);

    underTest.browseVersions(queryWithFilter("50%_a\\b"));

    // literal %, _ and \ must be escaped so they do not act as wildcards
    assertThat(capturedRequest().versionLikePattern).isEqualTo("%50\\%\\_a\\\\b%");
  }

  @Test
  void leavesVersionPatternNullWhenFilterBlank() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of());
    when(searchStore.countComponentVersions(any())).thenReturn(0L);

    underTest.browseVersions(queryWithFilter("   "));

    assertThat(capturedRequest().versionLikePattern).isNull();
  }

  /**
   * The version filter must not reach ExpressionBuilder: its exact-match and wildcard-length
   * rules are what made short substrings like "10" return nothing.
   */
  @Test
  void doesNotPassVersionFilterThroughExpressionBuilder() {
    stubCondition("cs.format = #{filterParams.p0}", Map.of("p0", "maven2"));
    when(searchStore.browseComponentVersions(any())).thenReturn(List.of());
    when(searchStore.countComponentVersions(any())).thenReturn(0L);

    underTest.browseVersions(queryWithFilter("10"));

    ArgumentCaptor<org.sonatype.nexus.repository.search.SearchRequest> captor =
        ArgumentCaptor.forClass(org.sonatype.nexus.repository.search.SearchRequest.class);
    verify(expressionBuilder).from(captor.capture());
    assertThat(captor.getValue().getSearchFilters())
        .extracting("property")
        .doesNotContain("version");
  }

  private ComponentVersionSearchRequest capturedRequest() {
    ArgumentCaptor<ComponentVersionSearchRequest> captor =
        ArgumentCaptor.forClass(ComponentVersionSearchRequest.class);
    verify(searchStore).browseComponentVersions(captor.capture());
    return captor.getValue();
  }

  private ComponentVersionQuery queryWithFilter(final String versionFilter) {
    return new ComponentVersionQuery("maven2", "org.test", "artifact", versionFilter, 0, 20,
        "version", SortDirection.DESC);
  }

  private ComponentVersionQuery query(final String sort, final int page, final int size) {
    return new ComponentVersionQuery("maven2", "org.test", "artifact", null, page, size, sort,
        SortDirection.DESC);
  }
}
