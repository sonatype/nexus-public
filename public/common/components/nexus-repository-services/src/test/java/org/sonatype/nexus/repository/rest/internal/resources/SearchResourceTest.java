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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
class SearchResourceTest
{
  @Mock
  private SearchUtils searchUtils;

  @Mock
  private SearchService searchService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SearchResultFilterUtils searchResultFilterUtils;

  @Mock
  private ComponentXOFactory componentXOFactory;

  @Mock
  private Repository repository;

  @Mock
  private UriInfo defaultUriInfo;

  private SearchResource underTest;

  @BeforeEach
  void setUp() {
    underTest = new SearchResource(
        searchUtils,
        searchResultFilterUtils,
        searchService,
        componentXOFactory,
        Collections.emptySet(),
        mock(),
        repositoryManager,
        securityHelper,
        null);

    MultivaluedMap<String, String> emptyQueryParams = new MultivaluedHashMap<>();
    lenient().when(defaultUriInfo.getQueryParameters()).thenReturn(emptyQueryParams);

    lenient().when(searchUtils.getSearchFilters(any())).thenReturn(List.of(new SearchFilter("q", "example")));
    lenient().when(searchUtils.getComponentSearchFilters(any())).thenReturn(Collections.emptyList());
    lenient().when(searchUtils.isAssetSearchParam(any())).thenReturn(false);
    lenient().when(searchService.search(any(SearchRequest.class))).thenReturn(new SearchResponse());
    lenient().when(searchUtils.getRepository(any())).thenReturn(repository);
    lenient().when(securityHelper.anyPermitted(any(RepositoryViewPermission.class))).thenReturn(true);

    // Mock repository configuration for AssetXO.from()
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap attributes = mock(NestedAttributesMap.class);
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.attributes(any())).thenReturn(attributes);
  }

  @Test
  void testSearch_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.search(null, null, null, null, null));
  }

  @Test
  void testSearchAssets_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.searchAssets(null, null, null, null, null));
  }

  @Test
  void testSearchAndDownloadAssets_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.searchAndDownloadAssets(null, null, null, null));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void search() {
    SearchResponse response = new SearchResponse();
    response.setSearchResults(List.of());
    when(searchService.search(any())).thenReturn(response);
    assertDoesNotThrow(() -> underTest.search(null, null, "ASC", null, defaultUriInfo));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void searchAndDownloadAssets_noResult() {
    SearchResponse response = new SearchResponse();
    response.setSearchResults(List.of());
    when(searchService.search(any())).thenReturn(response);
    WebApplicationException e = assertThrows(WebApplicationException.class,
        () -> underTest.searchAndDownloadAssets(null, "ASC", null, defaultUriInfo));
    assertThat(e.getResponse().getStatus(), is(404));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void searchAssets() {
    SearchResponse response = new SearchResponse();
    response.setSearchResults(List.of());
    when(searchService.search(any())).thenReturn(response);
    assertDoesNotThrow(() -> underTest.searchAssets(null, null, "ASC", null, defaultUriInfo));
  }

  @Test
  void doSearch_withDirectionASC() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "ASC", null, defaultUriInfo));
  }

  @Test
  void doSearch_withDirectionAsc() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "asc", null, defaultUriInfo));
  }

  @Test
  void doSearch_withDirectionNull() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, null, null, defaultUriInfo));
  }

  @Test
  void doSearch_withDirectionEmptyString() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "", null, defaultUriInfo));
  }

  @Test
  void doSearch_withDirectionAllWhitespace() {
    assertDoesNotThrow(() -> underTest.doSearch(null, null, "   \t", null, defaultUriInfo));
  }

  @Test
  void doSearch_withInvalidDirection_throwsBadRequest() {
    WebApplicationMessageException e =
        assertThrows(WebApplicationMessageException.class,
            () -> underTest.doSearch(null, null, "INVALID", null, defaultUriInfo));
    assertThat(e.getResponse().getStatus(), is(400));
  }

  @Test
  void doSearch_withLastUpdatedSort() {
    assertDoesNotThrow(() -> underTest.doSearch(null, "last_updated", "asc", null, defaultUriInfo));
  }

  @Test
  void doSearch_withLastUpdatedSortDesc() {
    assertDoesNotThrow(() -> underTest.doSearch(null, "last_updated", "desc", null, defaultUriInfo));
  }

  @Test
  void doSearchWithOnlyAssetParametersFilters() {
    // Asset-level parameters (checksums) should be included in the SQL search query
    UriInfo uriInfo = mock(UriInfo.class);
    when(searchUtils.getSearchFilters(uriInfo)).thenReturn(List.of(
        new SearchFilter("assets.attributes.checksum.md5", "abc123"),
        new SearchFilter("assets.attributes.checksum.sha1", "xyz789")));

    underTest.doSearch(null, null, null, null, uriInfo);

    verify(searchUtils).getSearchFilters(uriInfo);
    ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(searchService).search(requestCaptor.capture());
    SearchRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getSearchFilters().size(), is(2));
  }

  @Test
  void doSearchComponentAndAssetFilters() {
    // Both component-level (name) and asset-level (md5) filters should be included in the SQL query
    UriInfo uriInfo = mock(UriInfo.class);
    when(searchUtils.getSearchFilters(uriInfo)).thenReturn(List.of(
        new SearchFilter("name.raw", "junit"),
        new SearchFilter("assets.attributes.checksum.md5", "abc123")));

    underTest.doSearch(null, null, null, null, uriInfo);

    ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(searchService).search(requestCaptor.capture());
    SearchRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getSearchFilters().size(), is(2));
  }

  @Test
  void doSearchComponentFiltersOnly() {
    UriInfo uriInfo = mock(UriInfo.class);
    when(searchUtils.getSearchFilters(uriInfo)).thenReturn(List.of(
        new SearchFilter("name.raw", "junit"),
        new SearchFilter("repository_name", "maven-central")));

    underTest.doSearch(null, null, null, null, uriInfo);

    ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(searchService).search(requestCaptor.capture());
    SearchRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getSearchFilters().size(), is(2));
  }

  @Test
  void testTryExtractRepositoryFromSearch() {
    // empty
    assertThat(underTest.tryExtractRepositoryFromSearch(List.of()).apply("foo"), is("foo"));

    // simple case, not a group, no bar repository
    Repository foo = mockRepository("foo");

    Function<String, String> nameSupplier =
        underTest.tryExtractRepositoryFromSearch(List.of(new SearchFilter("repository_name", "foo bar")));
    assertThat(nameSupplier.apply("foo"), is("foo"));
    assertThat(nameSupplier.apply("bar"), is("bar"));

    Repository bar = mockRepository("bar");
    GroupFacet group = mock();
    when(bar.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(group));
    List<Repository> members = List.of(foo, mockRepository("someRepo"));
    when(group.leafMembers()).thenReturn(members);

    nameSupplier = underTest.tryExtractRepositoryFromSearch(List.of(new SearchFilter("repository_name", "foo bar")));
    assertThat(nameSupplier.apply("foo"), is("foo"));
    assertThat(nameSupplier.apply("bar"), is("bar"));
    assertThat(nameSupplier.apply("someRepo"), is("bar"));
  }

  private Repository mockRepository(final String repoName) {
    Repository repo = mock();
    lenient().when(repo.getName()).thenReturn(repoName);
    lenient().when(repositoryManager.get(repoName)).thenReturn(repo);

    return repo;
  }

  // --- Helper methods for searchRepositoriesForVersion tests ---

  private ComponentSearchResult componentHit(String repositoryName, String version) {
    ComponentSearchResult hit = new ComponentSearchResult();
    hit.setRepositoryName(repositoryName);
    hit.setVersion(version);
    hit.setFormat("maven2");
    hit.setGroup("org.example");
    hit.setName("widget");
    return hit;
  }

  private Repository stubRepo(String name, String typeValue) {
    Repository r = mock(Repository.class);
    Type type = mock(Type.class);
    Format format = mock(Format.class);
    // Use lenient doReturn/when pattern to avoid UnfinishedStubbing errors
    lenient().when(type.getValue()).thenReturn(typeValue);
    lenient().when(r.getType()).thenReturn(type);
    lenient().when(r.getName()).thenReturn(name);
    lenient().when(format.getValue()).thenReturn("maven2");
    lenient().when(r.getFormat()).thenReturn(format);
    return r;
  }

  private SearchResponse response(List<ComponentSearchResult> hits, String continuationToken) {
    SearchResponse r = new SearchResponse();
    r.setSearchResults(hits);
    r.setContinuationToken(continuationToken);
    return r;
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_returnsRealRepositoryTypesForSelectedVersion() {
    // Create repository mocks first (before any stubbing)
    Repository hosted = stubRepo("maven-hosted", "hosted");
    Repository proxy = stubRepo("maven-proxy", "proxy");
    Repository group = stubRepo("maven-group", "group");

    // version-scoped rows: three repos with the selected version
    when(searchService.search(argThat(hasVersionFilter("1.2.3"))))
        .thenReturn(response(List.of(
            componentHit("maven-hosted", "1.2.3"),
            componentHit("maven-proxy", "1.2.3"),
            componentHit("maven-group", "1.2.3")), null));
    // component-wide rows (no version filter): same three repos, various versions
    when(searchService.search(argThat(hasNoVersionFilter())))
        .thenReturn(response(List.of(
            componentHit("maven-hosted", "1.2.3"),
            componentHit("maven-hosted", "1.2.2"),
            componentHit("maven-proxy", "1.2.3"),
            componentHit("maven-proxy", "1.1.0"),
            componentHit("maven-proxy", "1.0.0"),
            componentHit("maven-group", "1.2.3")), null));

    when(repositoryManager.get("maven-hosted")).thenReturn(hosted);
    when(repositoryManager.get("maven-proxy")).thenReturn(proxy);
    when(repositoryManager.get("maven-group")).thenReturn(group);

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "maven2", "org.example", "widget", "1.2.3");

    assertThat(result.items(), hasSize(3));
    assertThat(result.totalCount(), is(3));
    // Items are sorted by repositoryName: maven-group, maven-hosted, maven-proxy.
    assertThat(result.items().get(0), equalTo(new RepositoryForVersion("maven-group", "group", 1)));
    assertThat(result.items().get(1), equalTo(new RepositoryForVersion("maven-hosted", "hosted", 2)));
    assertThat(result.items().get(2), equalTo(new RepositoryForVersion("maven-proxy", "proxy", 3)));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_filtersOutDeletedRepositories() {
    // Create repository mock first (before any stubbing)
    Repository stillHere = stubRepo("still-here", "hosted");

    when(searchService.search(argThat(hasVersionFilter("1.0.0"))))
        .thenReturn(response(List.of(
            componentHit("still-here", "1.0.0"),
            componentHit("just-deleted", "1.0.0")), null));
    when(searchService.search(argThat(hasNoVersionFilter())))
        .thenReturn(response(List.of(
            componentHit("still-here", "1.0.0"),
            componentHit("just-deleted", "1.0.0")), null));

    when(repositoryManager.get("still-here")).thenReturn(stillHere);
    when(repositoryManager.get("just-deleted")).thenReturn(null); // repository removed

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "maven2", "org.example", "widget", "1.0.0");

    assertThat(result.items(), hasSize(1));
    assertThat(result.items().get(0).repositoryName(), equalTo("still-here"));
    assertThat(result.totalCount(), is(1));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_missingFormatReturns400() {
    WebApplicationMessageException ex = assertThrows(WebApplicationMessageException.class,
        () -> underTest.searchRepositoriesForVersion("", "org.example", "widget", "1.0.0"));
    assertThat(ex.getResponse().getStatus(), is(400));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_missingNameReturns400() {
    assertThrows(WebApplicationMessageException.class,
        () -> underTest.searchRepositoriesForVersion("maven2", "org.example", "", "1.0.0"));
  }

  /**
   * An absent version parameter is a caller bug: every caller knows which version it is asking
   * about, even when the answer is the empty string. Contrast
   * {@link #searchRepositoriesForVersion_blankVersionIsAcceptedForVersionlessFormats()} — a
   * present-but-empty version is a real value, not a missing one.
   */
  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_absentVersionReturns400() {
    assertThrows(WebApplicationMessageException.class,
        () -> underTest.searchRepositoriesForVersion("maven2", "org.example", "widget", null));
  }

  /**
   * Raw and every other versionless format store '' as the component's version — the search index
   * reports it and an exact `version = ''` predicate matches only those rows. So '' is the one
   * legitimate version such a component has, and the endpoint must query for it rather than reject
   * it. Rejecting it 400'd the Repositories tab and Overview's Repository row for every raw
   * component.
   */
  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_blankVersionIsAcceptedForVersionlessFormats() {
    Repository rawHosted = stubRepo("my-raw", "hosted");

    when(searchService.search(argThat(hasVersionFilter(""))))
        .thenReturn(response(List.of(componentHit("my-raw", "")), null));
    when(searchService.search(argThat(hasNoVersionFilter())))
        .thenReturn(response(List.of(componentHit("my-raw", "")), null));
    when(repositoryManager.get("my-raw")).thenReturn(rawHosted);

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "raw", "/animport/abc", "/animport/abc/file529423.txt", "");

    assertThat(result.items(), hasSize(1));
    assertThat(result.items().get(0), equalTo(new RepositoryForVersion("my-raw", "hosted", 1)));
    assertThat(result.totalCount(), is(1));
  }

  /**
   * And the empty version must reach the search service as a filter, not be quietly dropped: a
   * dropped filter widens the row set to every version of the component, which for a format that
   * has real versions would list repositories that do not hold the selected one.
   */
  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_blankVersionIsPassedThroughAsAFilter() {
    when(searchService.search(any())).thenReturn(response(List.of(), null));

    underTest.searchRepositoriesForVersion("raw", "", "some-file.zip", "");

    verify(searchService).search(argThat(hasVersionFilter("")));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_blankNamespaceIsAccepted() {
    when(searchService.search(any())).thenReturn(response(List.of(), null));
    // Empty namespace is legal (raw components) — must not throw.
    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "raw", "", "some-file.zip", "n/a");
    assertThat(result.totalCount(), is(0));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_pagesCountQueryUntilContinuationTokenIsNull() {
    // Create repository mock first (before any stubbing)
    Repository repoA = stubRepo("repo-a", "hosted");

    when(searchService.search(argThat(hasVersionFilter("2.0"))))
        .thenReturn(response(List.of(componentHit("repo-a", "2.0")), null));
    // Three pages from the count query:
    when(searchService.search(argThat(isCountQueryWithToken(null))))
        .thenReturn(response(List.of(componentHit("repo-a", "2.0")), "token-1"));
    when(searchService.search(argThat(isCountQueryWithToken("token-1"))))
        .thenReturn(response(List.of(componentHit("repo-a", "1.5")), "token-2"));
    when(searchService.search(argThat(isCountQueryWithToken("token-2"))))
        .thenReturn(response(List.of(componentHit("repo-a", "1.0")), null));

    when(repositoryManager.get("repo-a")).thenReturn(repoA);

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "maven2", "org.example", "widget", "2.0");

    assertThat(result.items().get(0).versionCount(), is(3L));
    // Row-set query = 1 call. Count query = 3 pages. Total = 4.
    verify(searchService, times(4)).search(any(SearchRequest.class));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_countQuerySafetyCapTripsWithoutFailing() {
    // Create repository mock first (before any stubbing)
    Repository repoR = stubRepo("r", "hosted");

    when(searchService.search(argThat(hasVersionFilter("v")))).thenReturn(response(
        List.of(componentHit("r", "v")), null));
    // Runaway continuation token — every count page returns a non-null token.
    when(searchService.search(argThat(isCountQueryWithAnyToken()))).thenReturn(response(
        List.of(componentHit("r", "vX")), "keep-going"));

    when(repositoryManager.get("r")).thenReturn(repoR);

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "maven2", "org.example", "widget", "v");

    // Must not blow up. Verify the row is returned.
    assertThat(result.items(), hasSize(1));
    // Row-set (1) + count pages capped at COUNT_QUERY_PAGE_CAP (20) = 21 total calls.
    verify(searchService, times(21)).search(any(SearchRequest.class));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_handlesNullSearchResults() {
    // Mock both queries to return SearchResponse with null searchResults list
    SearchResponse nullResultsResponse = new SearchResponse();
    nullResultsResponse.setSearchResults(null);
    nullResultsResponse.setContinuationToken(null);

    when(searchService.search(any(SearchRequest.class))).thenReturn(nullResultsResponse);

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "maven2", "org.example", "widget", "1.0.0");

    // Must not throw NPE — returns empty items
    assertThat(result.items(), hasSize(0));
    assertThat(result.totalCount(), is(0));
  }

  @Test
  @WithUser("admin")
  void searchRepositoriesForVersion_filtersOutRepositoriesWithoutBrowsePermission() {
    when(searchService.search(argThat(hasVersionFilter("1.0"))))
        .thenReturn(response(List.of(
            componentHit("permitted-repo", "1.0"),
            componentHit("forbidden-repo", "1.0")), null));
    when(searchService.search(argThat(hasNoVersionFilter())))
        .thenReturn(response(List.of(
            componentHit("permitted-repo", "1.0"),
            componentHit("forbidden-repo", "1.0")), null));

    Repository permitted = stubRepo("permitted-repo", "hosted");
    Repository forbidden = stubRepo("forbidden-repo", "proxy");

    when(repositoryManager.get("permitted-repo")).thenReturn(permitted);
    when(repositoryManager.get("forbidden-repo")).thenReturn(forbidden);

    when(securityHelper.anyPermitted(any(RepositoryViewPermission.class)))
        .thenAnswer(inv -> {
          RepositoryViewPermission p = inv.getArgument(0);
          return "permitted-repo".equals(p.getName());
        });

    RepositoriesForVersionResponse result = underTest.searchRepositoriesForVersion(
        "maven2", "org.example", "widget", "1.0");

    assertThat(result.items(), hasSize(1));
    assertThat(result.items().get(0).repositoryName(), equalTo("permitted-repo"));
    assertThat(result.totalCount(), is(1));
  }

  // --- ArgumentMatchers for the two-query shape ---

  private static ArgumentMatcher<SearchRequest> hasVersionFilter(String version) {
    return req -> req != null
        && req.getSearchFilters()
            .stream()
            .anyMatch(f -> "version".equals(f.getProperty()) && version.equals(f.getValue()));
  }

  private static ArgumentMatcher<SearchRequest> hasNoVersionFilter() {
    return req -> req != null
        && req.getSearchFilters()
            .stream()
            .noneMatch(f -> "version".equals(f.getProperty()));
  }

  private static ArgumentMatcher<SearchRequest> isCountQueryWithToken(String expected) {
    return req -> req != null
        && req.getSearchFilters().stream().noneMatch(f -> "version".equals(f.getProperty()))
        && java.util.Objects.equals(expected, req.getContinuationToken());
  }

  private static ArgumentMatcher<SearchRequest> isCountQueryWithAnyToken() {
    return req -> req != null
        && req.getSearchFilters().stream().noneMatch(f -> "version".equals(f.getProperty()));
  }
}
