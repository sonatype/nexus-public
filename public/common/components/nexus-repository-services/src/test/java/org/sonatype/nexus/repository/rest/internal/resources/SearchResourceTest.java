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
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
@WithUser(permissions = {"nexus:uploader-metadata:read"})
class SearchResourceTest
{
  @Mock
  private SearchUtils searchUtils;

  @Mock
  private SearchService searchService;

  @Mock
  private RepositoryManager repositoryManager;

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
        null);

    MultivaluedMap<String, String> emptyQueryParams = new MultivaluedHashMap<>();
    lenient().when(defaultUriInfo.getQueryParameters()).thenReturn(emptyQueryParams);

    lenient().when(searchUtils.getSearchFilters(any())).thenReturn(List.of(new SearchFilter("q", "example")));
    lenient().when(searchUtils.getComponentSearchFilters(any())).thenReturn(Collections.emptyList());
    lenient().when(searchUtils.isAssetSearchParam(any())).thenReturn(false);
    lenient().when(searchService.search(any(SearchRequest.class))).thenReturn(new SearchResponse());
    lenient().when(searchUtils.getRepository(any())).thenReturn(repository);

    // Mock repository configuration for AssetXO.from()
    Configuration configuration = mock(Configuration.class);
    NestedAttributesMap attributes = mock(NestedAttributesMap.class);
    lenient().when(repository.getConfiguration()).thenReturn(configuration);
    lenient().when(configuration.attributes(any())).thenReturn(attributes);
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
}
