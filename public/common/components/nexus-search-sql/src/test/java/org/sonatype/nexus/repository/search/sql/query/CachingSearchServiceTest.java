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

import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;

import com.google.common.cache.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingSearchServiceTest
{
  @Mock
  private SearchService delegate;

  private CachingSearchService underTest;

  @BeforeEach
  void setUp() {
    underTest = new CachingSearchService(delegate, true, 100L, 30L);
  }

  @Test
  void testCacheHitReturnsResponseWithoutCallingDelegate() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // First call - cache miss, delegate called
    SearchResponse response1 = underTest.search(request);
    assertThat(response1, sameInstance(mockResponse));
    verify(delegate, times(1)).search(any(SearchRequest.class));

    // Second call with same request - cache hit, delegate NOT called again
    SearchResponse response2 = underTest.search(request);
    assertThat(response2, sameInstance(mockResponse));
    verify(delegate, times(1)).search(any(SearchRequest.class)); // Still only 1 call
  }

  @Test
  void testCacheMissCallsDelegateAndCachesResult() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // First call - cache miss
    SearchResponse response = underTest.search(request);

    assertThat(response, sameInstance(mockResponse));
    verify(delegate, times(1)).search(any(SearchRequest.class));
    assertThat(underTest.getCacheSize(), equalTo(1L));
  }

  @Test
  void testDifferentRequestsHaveDifferentCacheKeys() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request1 = SearchRequest.builder()
        .searchFilter("name", "test1")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchRequest request2 = SearchRequest.builder()
        .searchFilter("name", "test2")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse1 = createMockResponse("token1");
    SearchResponse mockResponse2 = createMockResponse("token2");

    when(delegate.search(any(SearchRequest.class)))
        .thenReturn(mockResponse1)
        .thenReturn(mockResponse2);

    // First request
    SearchResponse response1 = underTest.search(request1);
    assertThat(response1, sameInstance(mockResponse1));

    // Second request with different filter - should be cache miss
    SearchResponse response2 = underTest.search(request2);
    assertThat(response2, sameInstance(mockResponse2));

    // Both should have been cached separately
    assertThat(underTest.getCacheSize(), equalTo(2L));
    verify(delegate, times(2)).search(any(SearchRequest.class));
  }

  @Test
  void testComponentCreatedEventInvalidatesCache() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // Populate cache
    underTest.search(request);
    assertThat(underTest.getCacheSize(), equalTo(1L));

    // Trigger event (mock the event to avoid component casting issues)
    ComponentCreatedEvent event = mock(ComponentCreatedEvent.class);
    underTest.onComponentCreated(event);

    // Cache should be empty
    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  @Test
  void testComponentUpdatedEventInvalidatesCache() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // Populate cache
    underTest.search(request);
    assertThat(underTest.getCacheSize(), equalTo(1L));

    // Create a mock ComponentUpdatedEvent using a mock Component
    // Note: ComponentUpdatedEvent constructor is protected, so we use mocking
    ComponentUpdatedEvent event = mock(ComponentUpdatedEvent.class);
    underTest.onComponentUpdated(event);

    // Cache should be empty
    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  @Test
  void testComponentDeletedEventInvalidatesCache() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // Populate cache
    underTest.search(request);
    assertThat(underTest.getCacheSize(), equalTo(1L));

    // Trigger event (mock the event to avoid component casting issues)
    ComponentDeletedEvent event = mock(ComponentDeletedEvent.class);
    underTest.onComponentDeleted(event);

    // Cache should be empty
    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  @Test
  void testComponentPurgedEventInvalidatesCache() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // Populate cache
    underTest.search(request);
    assertThat(underTest.getCacheSize(), equalTo(1L));

    // Trigger event
    ComponentPurgedEvent event = new ComponentPurgedEvent(1, new int[]{1, 2, 3});
    underTest.onComponentPurged(event);

    // Cache should be empty
    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  @Test
  void testDisabledCacheBypassesCacheAndCallsDelegateDirectly() {
    CachingSearchService disabledCache = new CachingSearchService(delegate, false, 100L, 30L);

    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // First call
    disabledCache.search(request);
    // Second call
    disabledCache.search(request);

    // Delegate should be called twice (no caching)
    verify(delegate, times(2)).search(any(SearchRequest.class));
    assertThat(disabledCache.getCacheSize(), equalTo(0L));
  }

  @Test
  void testBrowseDelegatesToUnderlying() {
    SearchRequest request = SearchRequest.builder().build();
    Iterable<ComponentSearchResult> mockResults = Collections.emptyList();

    when(delegate.browse(any(SearchRequest.class))).thenReturn(mockResults);

    Iterable<ComponentSearchResult> result = underTest.browse(request);

    assertThat(result, sameInstance(mockResults));
    verify(delegate, times(1)).browse(any(SearchRequest.class));
  }

  @Test
  void testCountDelegatesToUnderlying() {
    SearchRequest request = SearchRequest.builder().build();

    when(delegate.count(any(SearchRequest.class))).thenReturn(42L);

    long count = underTest.count(request);

    assertThat(count, equalTo(42L));
    verify(delegate, times(1)).count(any(SearchRequest.class));
  }

  @Test
  void testWaitForCalmDelegatesToUnderlying() {
    underTest.waitForCalm();
    verify(delegate, times(1)).waitForCalm();
  }

  @Test
  void testWaitForReadyDelegatesToUnderlying() {
    underTest.waitForReady();
    verify(delegate, times(1)).waitForReady();
  }

  @Test
  void testCacheStatsRecorded() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // First call - miss
    underTest.search(request);

    // Second call - hit
    underTest.search(request);

    CacheStats stats = underTest.getStats();
    assertThat(stats.missCount(), equalTo(1L));
    assertThat(stats.hitCount(), equalTo(1L));
  }

  @Test
  void testIsEnabledReturnsCorrectValue() {
    assertThat(underTest.isEnabled(), is(true));

    CachingSearchService disabledCache = new CachingSearchService(delegate, false, 100L, 30L);
    assertThat(disabledCache.isEnabled(), is(false));
  }

  @Test
  void testCacheKeyIncludesSortParameters() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request1 = SearchRequest.builder()
        .searchFilter("name", "test")
        .sortField("name")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchRequest request2 = SearchRequest.builder()
        .searchFilter("name", "test")
        .sortField("version")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse1 = createMockResponse("token1");
    SearchResponse mockResponse2 = createMockResponse("token2");

    when(delegate.search(any(SearchRequest.class)))
        .thenReturn(mockResponse1)
        .thenReturn(mockResponse2);

    underTest.search(request1);
    underTest.search(request2);

    // Both should be cached separately due to different sort fields
    assertThat(underTest.getCacheSize(), equalTo(2L));
  }

  @Test
  void testCacheKeyIncludesContinuationToken() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request1 = SearchRequest.builder()
        .searchFilter("name", "test")
        .continuationToken("page1")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchRequest request2 = SearchRequest.builder()
        .searchFilter("name", "test")
        .continuationToken("page2")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchResponse mockResponse1 = createMockResponse("token1");
    SearchResponse mockResponse2 = createMockResponse("token2");

    when(delegate.search(any(SearchRequest.class)))
        .thenReturn(mockResponse1)
        .thenReturn(mockResponse2);

    underTest.search(request1);
    underTest.search(request2);

    // Both should be cached separately due to different continuation tokens
    assertThat(underTest.getCacheSize(), equalTo(2L));
  }

  @Test
  void testInvalidateAllClearsCache() {
    // Note: Must disable authorization to test caching - authorized searches bypass cache
    SearchRequest request1 = SearchRequest.builder().searchFilter("name", "test1").disableAuthorization().build();
    SearchRequest request2 = SearchRequest.builder().searchFilter("name", "test2").disableAuthorization().build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    underTest.search(request1);
    underTest.search(request2);
    assertThat(underTest.getCacheSize(), equalTo(2L));

    underTest.invalidateAll();

    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  // ===================================================================================
  // SECURITY TESTS: Authorization bypass prevention
  // ===================================================================================

  @Test
  void testCacheBypassedWhenAuthorizationEnabled_SecurityFix() {
    // SECURITY: When checkAuthorization=true (the default), results are user-specific
    // (based on permissions and content selectors). Caching these would allow
    // one user's results to be served to another user, bypassing RBAC.
    SearchRequest authorizedRequest = SearchRequest.builder()
        .searchFilter("name", "test")
        // Note: checkAuthorization defaults to true, so we don't need to set it
        .limit(10)
        .build();

    // Verify the request has authorization enabled (default)
    assertThat(authorizedRequest.isCheckAuthorization(), is(true));

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // First call with authorization
    underTest.search(authorizedRequest);
    // Second call with same authorized request
    underTest.search(authorizedRequest);

    // Delegate should be called TWICE - no caching for authorized searches
    verify(delegate, times(2)).search(any(SearchRequest.class));
    // Cache should remain empty for authorized requests
    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  @Test
  void testCacheUsedWhenAuthorizationDisabled() {
    // When authorization is disabled (anonymous/system searches), caching is safe
    SearchRequest anonymousRequest = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization() // No authorization - safe to cache
        .limit(10)
        .build();

    SearchResponse mockResponse = createMockResponse("token1");
    when(delegate.search(any(SearchRequest.class))).thenReturn(mockResponse);

    // First call
    underTest.search(anonymousRequest);
    // Second call
    underTest.search(anonymousRequest);

    // Delegate should be called ONCE - cache hit on second call
    verify(delegate, times(1)).search(any(SearchRequest.class));
    // Result should be cached
    assertThat(underTest.getCacheSize(), equalTo(1L));
  }

  @Test
  void testMixedAuthorizationRequestsDoNotShareCache() {
    // Ensure that an unauthorized (cached) search doesn't serve results
    // to a subsequent authorized search
    SearchRequest anonymousRequest = SearchRequest.builder()
        .searchFilter("name", "test")
        .disableAuthorization()
        .limit(10)
        .build();

    SearchRequest authorizedRequest = SearchRequest.builder()
        .searchFilter("name", "test") // Same filter
        // Authorization enabled by default (checkAuthorization=true)
        .limit(10)
        .build();

    // Verify our test setup
    assertThat(anonymousRequest.isCheckAuthorization(), is(false));
    assertThat(authorizedRequest.isCheckAuthorization(), is(true));

    SearchResponse anonymousResponse = createMockResponse("anon-token");
    SearchResponse authorizedResponse = createMockResponse("auth-token");

    when(delegate.search(any(SearchRequest.class)))
        .thenReturn(anonymousResponse)
        .thenReturn(authorizedResponse);

    // First: anonymous search (gets cached)
    SearchResponse result1 = underTest.search(anonymousRequest);
    assertThat(result1.getContinuationToken(), equalTo("anon-token"));
    assertThat(underTest.getCacheSize(), equalTo(1L));

    // Second: authorized search (must bypass cache and call delegate)
    SearchResponse result2 = underTest.search(authorizedRequest);
    assertThat(result2.getContinuationToken(), equalTo("auth-token"));

    // Verify both searches called delegate (anonymous once, authorized once)
    verify(delegate, times(2)).search(any(SearchRequest.class));
  }

  @Test
  void testAuthorizedSearchesDontPolluteCacheForOtherUsers() {
    // Simulates the scenario where different users make authorized searches
    // None should be cached, ensuring each user gets fresh results
    SearchRequest authorizedRequest = SearchRequest.builder()
        .searchFilter("name", "log4j")
        // Authorization enabled by default (checkAuthorization=true)
        .limit(10)
        .build();

    // Verify authorization is enabled
    assertThat(authorizedRequest.isCheckAuthorization(), is(true));

    SearchResponse userAResponse = createMockResponse("userA-results");
    SearchResponse userBResponse = createMockResponse("userB-results");

    when(delegate.search(any(SearchRequest.class)))
        .thenReturn(userAResponse)
        .thenReturn(userBResponse);

    // User A searches (should not cache)
    SearchResponse resultA = underTest.search(authorizedRequest);
    assertThat(resultA.getContinuationToken(), equalTo("userA-results"));

    // User B searches with same parameters (should call delegate again, not get User A's results)
    SearchResponse resultB = underTest.search(authorizedRequest);
    assertThat(resultB.getContinuationToken(), equalTo("userB-results"));

    // Both calls went to delegate
    verify(delegate, times(2)).search(any(SearchRequest.class));
    // No caching occurred
    assertThat(underTest.getCacheSize(), equalTo(0L));
  }

  private SearchResponse createMockResponse(String continuationToken) {
    SearchResponse response = new SearchResponse();
    response.setContinuationToken(continuationToken);
    response.setSearchResults(List.of(new ComponentSearchResult()));
    response.setTotalHits(1L);
    return response;
  }
}
