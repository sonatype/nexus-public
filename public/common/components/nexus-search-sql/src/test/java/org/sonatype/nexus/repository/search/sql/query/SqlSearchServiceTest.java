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

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.security.AssetPermissionChecker;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.SearchResult;
import org.sonatype.nexus.repository.search.sql.SqlSearchResultDecorator;
import org.sonatype.nexus.repository.search.sql.index.SqlSearchEventHandler;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlSearchServiceTest
{
  private static final String FORMAT = "maven2";

  @Mock
  private SearchStore searchStore;

  @Mock
  private SqlSearchSortUtil sqlSearchSortUtil;

  @Mock
  private ExpressionBuilder expressionBuilder;

  @Mock
  private ExpressionGroup expressionGroup;

  @Mock
  private SearchRequestModifier requestModifier;

  @Mock
  private SearchConditionFactory queryFactory;

  @Mock
  private TestFormatStoreManager formatStoreManager;

  @Mock
  private Set<SqlSearchResultDecorator> decorators;

  @Mock
  private SqlSearchEventHandler sqlSearchEventHandler;

  @Mock
  private AssetPermissionChecker assetPermissionChecker;

  private SqlSearchService underTest;

  @BeforeEach
  public void setUp() {
    underTest = new SqlSearchService(searchStore, sqlSearchSortUtil, List.of(formatStoreManager), expressionBuilder,
        requestModifier, queryFactory, decorators, sqlSearchEventHandler, assetPermissionChecker);
  }

  /**
   * Test that the browse method returns an iterator that fetches results in batches
   */
  @Test
  void testBrowseMethodWithBatching() {
    // Mock requestModifier to return the original request
    when(requestModifier.modify(any(SearchRequest.class))).thenAnswer(i -> i.getArgument(0));

    // Create a search request with limit of 2 to test small batches
    SearchRequest searchRequest = SearchRequest.builder().limit(2).build();

    // Mock the response from searchStore for the first batch
    List<SearchResult> firstBatchResults =
        List.of(createMockSearchResult("repo1", "component1", 1), createMockSearchResult("repo1", "component2", 2));

    // Mock the response from searchStore for the second batch
    List<SearchResult> secondBatchResults =
        List.of(createMockSearchResult("repo1", "component3", 3), createMockSearchResult("repo1", "component4", 4));

    // Mock the response from searchStore for the third batch (empty to end iteration)
    List<SearchResult> emptyBatchResults = Collections.emptyList();

    // Set up the sorting utilities - no sort field in this request, so it won't be called
    // No mocking needed as sortField is null

    // Mock the expression builder and query factory
    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    // Set up the searchStore to return different results for different offset values
    // First call with offset 0 returns firstBatchResults
    when(searchStore.searchComponents(eq(2), eq(0), eq(queryCondition), isNull(), isNull(), eq(false)))
        .thenReturn(firstBatchResults);

    // Second call with offset 2 returns secondBatchResults
    when(searchStore.searchComponents(eq(2), eq(2), eq(queryCondition), isNull(), isNull(), eq(false)))
        .thenReturn(secondBatchResults);

    // Third call with offset 4 returns emptyBatchResults
    when(searchStore.searchComponents(eq(2), eq(4), eq(queryCondition), isNull(), isNull(), eq(false)))
        .thenReturn(emptyBatchResults);

    // Execute the browse method to get the iterator
    Iterable<ComponentSearchResult> iterable = underTest.browse(searchRequest);
    Iterator<ComponentSearchResult> iterator = iterable.iterator();

    // Collect results from the iterator
    List<ComponentSearchResult> allResults = new ArrayList<>();
    while (iterator.hasNext()) {
      allResults.add(iterator.next());
    }

    // Verify results
    assertThat(allResults, hasSize(4));

    // Verify component names
    assertThat(allResults.get(0).getName(), equalTo("component1"));
    assertThat(allResults.get(1).getName(), equalTo("component2"));
    assertThat(allResults.get(2).getName(), equalTo("component3"));
    assertThat(allResults.get(3).getName(), equalTo("component4"));

    // Verify the store was called multiple times with different offsets
    verify(searchStore, times(1)).searchComponents(eq(2), eq(0), any(), any(), any(), any());
    verify(searchStore, times(1)).searchComponents(eq(2), eq(2), any(), any(), any(), any());
    verify(searchStore, times(1)).searchComponents(eq(2), eq(4), any(), any(), any(), any());
  }

  @Test
  void testBuildAssetSearchMapsBlobCreatedFromAssetCreated() {
    // Create specific timestamps for blobCreated and asset created
    OffsetDateTime blobCreatedTime = OffsetDateTime.now().minusDays(1);
    OffsetDateTime assetCreatedTime = OffsetDateTime.now().minusDays(2);

    AssetData asset = spy(new AssetData());
    asset.setAssetId(100);
    asset.setPath("/path/to/asset.jar");
    asset.setCreated(assetCreatedTime);

    AssetBlob blob = mock();
    when(asset.blob()).thenReturn(Optional.of(blob));
    when(blob.contentType()).thenReturn("application/java-archive");
    when(blob.blobCreated()).thenReturn(blobCreatedTime);
    when(blob.createdBy()).thenReturn(Optional.of("admin"));
    when(blob.createdByIp()).thenReturn(Optional.of("127.0.0.1"));
    when(blob.blobSize()).thenReturn(1024L);
    when(blob.checksums()).thenReturn(Map.of());

    SearchResult componentInfo = mock();

    AssetSearchResult result = SqlSearchService.buildAssetSearch(asset, "maven-central", componentInfo);

    // Verify that blobCreated comes from asset.created()
    Date expectedBlobCreated = Date.from(assetCreatedTime.toInstant());
    assertThat(result.getBlobCreated(), equalTo(expectedBlobCreated));

    // Also verify that lastModified comes from asset.blobCreated()
    Date expectedLastModified = Date.from(blobCreatedTime.toInstant());
    assertThat(result.getLastModified(), equalTo(expectedLastModified));

    // Verify other fields
    assertThat(result.getPath(), equalTo("/path/to/asset.jar"));
    assertThat(result.getRepository(), equalTo("maven-central"));
    assertThat(result.getContentType(), equalTo("application/java-archive"));
    assertThat(result.getUploader(), equalTo("admin"));
    assertThat(result.getUploaderIp(), equalTo("127.0.0.1"));
    assertThat(result.getFileSize(), equalTo(1024L));
  }

  /**
   * Verifies that distinctNameAndNamespace flag is properly passed through to searchStore
   */
  @Test
  void testSearch_withDistinctNameAndNamespace() {
    // Create a search request with distinctNameAndNamespace enabled
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(100)
        .distinctNameAndNamespace()
        .build();

    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    SearchResult searchResult = createMockSearchResult("npm-hosted", "@scope/package", 1);
    when(searchStore.searchComponents(anyInt(), anyInt(), any(), any(), any(), eq(true)))
        .thenReturn(List.of(searchResult));

    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    // Execute the search
    underTest.search(searchRequest);

    // Verify that searchStore.searchComponents was called with distinct=true
    verify(searchStore).searchComponents(
        eq(100),
        eq(0),
        eq(queryCondition),
        isNull(),
        isNull(),
        eq(true));
  }

  /**
   * Verifies that distinctNameAndNamespace flag defaults to false when not specified
   */
  @Test
  void testSearch_withoutDistinctNameAndNamespace() {
    // Create a search request without distinctNameAndNamespace (should default to false)
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(100)
        .build();

    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    SearchResult searchResult = createMockSearchResult("npm-hosted", "@scope/package", 1);
    when(searchStore.searchComponents(anyInt(), anyInt(), any(), any(), any(), eq(false)))
        .thenReturn(List.of(searchResult));

    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    // Execute the search
    underTest.search(searchRequest);

    // Verify that searchStore.searchComponents was called with distinct=false
    verify(searchStore).searchComponents(
        eq(100),
        eq(0),
        eq(queryCondition),
        isNull(),
        isNull(),
        eq(false));
  }

  /**
   * Verifies that OrderBy returns null when distinctNameAndNamespace is true and sorting by namespace
   */
  @Test
  void testSearch_distinctNameAndNamespace_withNamespaceSortReturnsNullOrderBy() {
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(100)
        .distinctNameAndNamespace()
        .sortField("namespace")
        .sortDirection(SortDirection.ASC)
        .build();

    SqlSearchQueryConditionGroup queryCondition = mockQueryCondition(searchRequest);
    when(sqlSearchSortUtil.getSortExpression("namespace")).thenReturn(Optional.of("cs.namespace"));

    SearchResult searchResult = createMockSearchResult("npm-hosted", "@scope/package", 1);
    when(searchStore.searchComponents(anyInt(), anyInt(), any(), isNull(), isNull(), eq(true)))
        .thenReturn(List.of(searchResult));

    underTest.search(searchRequest);

    verify(searchStore).searchComponents(
        eq(100),
        eq(0),
        eq(queryCondition),
        isNull(),
        isNull(),
        eq(true));
  }

  /**
   * Verifies that OrderBy returns null when distinctNameAndNamespace is true and sorting by name
   */
  @Test
  void testSearch_distinctNameAndNamespace_withNameSortReturnsNullOrderBy() {
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(100)
        .distinctNameAndNamespace()
        .sortField("name")
        .sortDirection(SortDirection.ASC)
        .build();

    SqlSearchQueryConditionGroup queryCondition = mockQueryCondition(searchRequest);
    when(sqlSearchSortUtil.getSortExpression("name")).thenReturn(Optional.of("cs.search_component_name"));

    SearchResult searchResult = createMockSearchResult("npm-hosted", "@scope/package", 1);
    when(searchStore.searchComponents(anyInt(), anyInt(), any(), isNull(), isNull(), eq(true)))
        .thenReturn(List.of(searchResult));

    underTest.search(searchRequest);

    verify(searchStore).searchComponents(
        eq(100),
        eq(0),
        eq(queryCondition),
        isNull(),
        isNull(),
        eq(true));
  }

  /**
   * Verifies that OrderBy returns normal values when distinctNameAndNamespace is true but sorting by other field
   */
  @Test
  void testSearch_distinctNameAndNamespace_withVersionSortReturnsNormalOrderBy() {
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(100)
        .distinctNameAndNamespace()
        .sortField("version")
        .sortDirection(SortDirection.DESC)
        .build();

    SqlSearchQueryConditionGroup queryCondition = mockQueryCondition(searchRequest);
    when(sqlSearchSortUtil.getSortExpression("version")).thenReturn(Optional.of("cs.version"));

    SearchResult searchResult = createMockSearchResult("npm-hosted", "@scope/package", 1);
    when(searchStore.searchComponents(anyInt(), anyInt(), any(), eq("cs.version"), eq(SortDirection.DESC), eq(true)))
        .thenReturn(List.of(searchResult));

    underTest.search(searchRequest);

    verify(searchStore).searchComponents(
        eq(100),
        eq(0),
        eq(queryCondition),
        eq("cs.version"),
        eq(SortDirection.DESC),
        eq(true));
  }

  /**
   * Verifies that assets for a component are filtered by the user's privileges
   */
  @Test
  void testSearch_filtersAssetsByPrivileges() {
    SearchRequest searchRequest = mock();
    when(searchRequest.getLimit()).thenReturn(100);
    when(searchRequest.isIncludeAssets()).thenReturn(true);
    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    SearchResult searchResult = createMockSearchResult("maven-central", "catalina", 1);
    when(searchStore.searchComponents(anyInt(), anyInt(), any(), any(), any(), any()))
        .thenReturn(List.of(searchResult));

    AssetStore<?> assetStore = mock();
    when(formatStoreManager.assetStore("nexus")).thenReturn(assetStore);
    when(assetStore.findByComponentIds(eq(Set.of(1)), any(), any(), anyBoolean()))
        .thenReturn(List.of(createAsset(1, 1), createAsset(1, 2)));

    when(assetPermissionChecker.findPermittedAssets(any(), any(), any()))
        .thenAnswer(i -> i.getArgument(0, List.class).subList(0, 1).stream().map(a -> new SimpleImmutableEntry(a, "")));

    List<ComponentSearchResult> result = underTest.search(searchRequest).getSearchResults();

    assertThat(result, hasSize(1));

    // we expect that we checked permissions for 2 assets but the user only has access to 1
    assertThat(result.get(0).getAssets(), hasSize(1));
    verify(assetPermissionChecker).findPermittedAssets(argThat(assets -> assets.size() == 2), any(), any());
  }

  @Qualifier(FORMAT)
  private static class TestFormatStoreManager
      extends FormatStoreManager
  {

    protected TestFormatStoreManager() {
      super(FORMAT);
    }
  }

  private static AssetData createAsset(final int componentId, final int assetId) {
    AssetData asset = new AssetData();
    asset.setAssetId(assetId);
    try {
      Field field = AssetData.class.getDeclaredField("componentId");
      field.setAccessible(true);
      field.set(asset, componentId);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

    return spy(asset);
  }

  @Test
  void testSearch_throwsValidationErrorsExceptionForInvalidSortField() {
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(10)
        .sortField("invalid_field")
        .build();

    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    when(sqlSearchSortUtil.getSortExpression("invalid_field")).thenReturn(Optional.empty());

    ValidationErrorsException exception = assertThrows(ValidationErrorsException.class,
        () -> underTest.search(searchRequest));

    assertThat(exception.getMessage(), equalTo("Invalid sort field: 'invalid_field'. Please use a valid sort field."));
  }

  @Test
  void testSearch_successWithValidSortField() {
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(10)
        .sortField("name")
        .sortDirection(SortDirection.ASC)
        .build();

    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    when(sqlSearchSortUtil.getSortExpression("name")).thenReturn(Optional.of("component_name"));
    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    when(searchStore.searchComponents(eq(10), eq(0), eq(queryCondition), eq("component_name"),
        eq(SortDirection.ASC), eq(false))).thenReturn(Collections.emptyList());
    when(searchStore.count(queryCondition)).thenReturn(0L);

    underTest.search(searchRequest);

    verify(searchStore).searchComponents(eq(10), eq(0), eq(queryCondition), eq("component_name"),
        eq(SortDirection.ASC), eq(false));
  }

  @Test
  void testSearch_successWithNullSortField() {
    SearchRequest searchRequest = SearchRequest.builder()
        .limit(10)
        .build();

    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    when(searchStore.searchComponents(eq(10), eq(0), eq(queryCondition), isNull(), isNull(), eq(false)))
        .thenReturn(Collections.emptyList());
    when(searchStore.count(queryCondition)).thenReturn(0L);

    underTest.search(searchRequest);

    verify(searchStore).searchComponents(eq(10), eq(0), eq(queryCondition), isNull(), isNull(), eq(false));
  }

  /**
   * Helper method to set up common mocks for expression builder and query factory
   */
  private SqlSearchQueryConditionGroup mockQueryCondition(final SearchRequest searchRequest) {
    when(requestModifier.modify(searchRequest)).thenReturn(searchRequest);

    SqlSearchQueryConditionGroup queryCondition = mock(SqlSearchQueryConditionGroup.class);
    Optional<ExpressionGroup> expression = Optional.of(expressionGroup);
    when(expressionBuilder.from(any(SearchRequest.class))).thenReturn(expression);
    when(queryFactory.build(expression.get())).thenReturn(queryCondition);

    return queryCondition;
  }

  /**
   * Helper method to create a mock SearchResult with specified values
   */
  private static SearchResult createMockSearchResult(
      final String repositoryName,
      final String componentName,
      final Integer componentId)
  {
    SearchResult result = mock(SearchResult.class);
    when(result.repositoryName()).thenReturn(repositoryName);
    when(result.componentName()).thenReturn(componentName);
    when(result.componentId()).thenReturn(componentId);
    when(result.format()).thenReturn(FORMAT);
    when(result.namespace()).thenReturn("group");
    when(result.version()).thenReturn("1.0");
    when(result.lastModified()).thenReturn(OffsetDateTime.now());
    return result;
  }

  @Test
  void testWaitForCalmReturnsSuccessfullyWhenCalmAfterMinimumWait() {
    // Path 1: isCalm=true AND minWaitComplete=true -> returns successfully
    when(sqlSearchEventHandler.getMinimumWaitTimeMs()).thenReturn(1000L);
    when(sqlSearchEventHandler.isCalmPeriod()).thenReturn(true);

    underTest.waitForCalm();

    // Verify minimum wait logic was consulted
    verify(sqlSearchEventHandler).getMinimumWaitTimeMs();
    // Verify calm period was checked
    verify(sqlSearchEventHandler, atLeastOnce()).isCalmPeriod();
  }

  @Test
  void testWaitForCalmPollsUntilCalmPeriodReached() {
    // Path 2: isCalm=false -> continues polling until calm
    when(sqlSearchEventHandler.getMinimumWaitTimeMs()).thenReturn(1000L);
    // Return false for first 10 polls, then true to allow minimum wait to complete and exit
    when(sqlSearchEventHandler.isCalmPeriod())
        .thenReturn(false, false, false, false, false, false, false, false, false, false)
        .thenReturn(true);

    underTest.waitForCalm();

    // Verify it consulted minimum wait time
    verify(sqlSearchEventHandler).getMinimumWaitTimeMs();
    // Verify it polled multiple times until calm
    verify(sqlSearchEventHandler, atLeast(10)).isCalmPeriod();
    // Verify it eventually checked after minimum wait completed
    verify(sqlSearchEventHandler, atLeast(11)).isCalmPeriod();
  }

  @Test
  void testWaitForCalmRespectsMinimumWaitTimeEvenWhenImmediatelyCalm() {
    // Path 3: minWaitComplete=false -> continues polling even if calm
    // This tests the race condition fix: must wait minimum time before checking calm
    when(sqlSearchEventHandler.getMinimumWaitTimeMs()).thenReturn(1000L);
    // Calm immediately but must still wait minimum time
    when(sqlSearchEventHandler.isCalmPeriod()).thenReturn(true);

    underTest.waitForCalm();

    // Verify minimum wait was calculated
    verify(sqlSearchEventHandler).getMinimumWaitTimeMs();
    // Verify it checked calm period multiple times during minimum wait (polls every 100ms for 1000ms = ~10 polls)
    verify(sqlSearchEventHandler, atLeast(9)).isCalmPeriod();
  }
}
