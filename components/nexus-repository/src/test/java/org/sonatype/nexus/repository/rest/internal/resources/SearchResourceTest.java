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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.SearchResourceExtension;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.rest.Page;

import com.google.common.collect.ImmutableSet;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.internal.resources.ResourcesTestUtils.createAsset;
import static org.sonatype.nexus.repository.rest.internal.resources.ResourcesTestUtils.createComponent;

public class SearchResourceTest
    extends RepositoryResourceTestSupport
{
  @Mock
  SearchService searchService;

  @Captor
  ArgumentCaptor<SearchRequest> queryBuilderArgumentCaptor;

  @Mock
  Repository repository;

  @Mock
  SearchResourceExtension searchResourceExtension;

  @Mock
  EventManager eventManager;

  SearchResource underTest;

  private SearchResultFilterUtils searchResultFilterUtils;

  SearchResponse searchResponse;

  ComponentSearchResult searchHitMaven;

  ComponentSearchResult searchHitMaven_withMultipleAssets;;

  ComponentSearchResult searchHitNpm;

  @Before
  public void setup() {
    configureMockedRepository(repository, "test-repo", "http://localhost:8081/test");
    setupResponse();

    when(searchResourceExtension.updateComponentXO(any(), any(ComponentSearchResult.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    Map<String, AssetXODescriptor> descriptors =
        Collections.singletonMap("maven2", () -> ImmutableSet.of("extension", "classifier", "version"));

    searchResultFilterUtils = new SearchResultFilterUtils(searchUtils, Arrays.asList());
    underTest = new SearchResource(searchUtils, searchResultFilterUtils, searchService,
        new ComponentXOFactory(emptySet()), ImmutableSet.of(searchResourceExtension), eventManager, descriptors);
  }

  private void setupResponse() {
    searchResponse = new SearchResponse();

    List<AssetSearchResult> assets = newArrayList(
        createAsset("antlr.jar", "maven2", "test-repo", "first-sha1", of("extension", "jar", "classifier", "foo")),
        createAsset("antlr.pom", "maven2", "test-repo", "second-sha1", of("extension", "pom"))
    );
    searchHitMaven = createComponent("foo", "test-repo", "format", "test", "1.0", assets);
    searchHitMaven.setId("id1");

    List<AssetSearchResult> mulitple_assets = newArrayList(
        createAsset("antlr-fooz.jar", "maven2", "test-repo", "first-sha1",
            of("extension", "jar", "classifier", "fooz", "version", "2.0")),
        createAsset("antlr.jar", "maven2", "test-repo", "first-sha1", of("extension", "jar")),
        createAsset("antlr.pom", "maven2", "test-repo", "first-sha1", of("extension", "pom"))
    );
    searchHitMaven_withMultipleAssets =
        createComponent("fooz", "test-repo", "maven2", "test", "2.0", mulitple_assets);
    searchHitMaven_withMultipleAssets.setId("id2");

    List<AssetSearchResult> assets2 = newArrayList(
        createAsset("bar.one", "npm", "test-repo", "third-sha1", of("extension", "one")),
        createAsset("bar.two", "npm", "test-repo", "fourth-sha1", of("extension", "two")),
        createAsset("bar.three", "npm", "test-repo", "fifth-sha1", of("extension", "three"))
    );
    searchHitNpm = createComponent("bar", "test-repo", "npm", "group2", "2.0", assets2);
    searchHitNpm.setId("id2");

    searchResponse.setSearchResults(Arrays.asList(searchHitMaven, searchHitNpm));
  }

  @Test
  public void testSearch() {
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<ComponentXO> componentPage = underTest.search(null, null, null, null, uriInfo("?format=maven2"));

    List<ComponentXO> items = componentPage.getItems();

    assertThat(items, hasSize(2));

    ComponentXO componentXO = items.stream().filter(item -> item.getName().equals("foo")).findFirst().get();
    assertThat(componentXO.getGroup(), is("test"));
    assertThat(componentXO.getVersion(), is("1.0"));

    ComponentXO componentXO1 = items.stream().filter(item -> item.getName().equals("bar")).findFirst().get();
    assertThat(componentXO1.getGroup(), is("group2"));
    assertThat(componentXO1.getVersion(), is("2.0"));
    assertThat(componentXO1.getAssets().get(0).getChecksum().get("sha1"), is("third-sha1"));

    SearchRequest request = queryBuilderArgumentCaptor.getValue();
    assertThat(request.getSearchFilters(), hasItem(new SearchFilter("format", "maven2")));

    verify(searchResourceExtension, times(2)).updateComponentXO(any(ComponentXO.class),
        any(ComponentSearchResult.class));
  }

  @Test
  public void testSearchWithChecksum() {
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null, uriInfo("?format=npm"));

    List<AssetXO> items = assets.getItems();

    assertThat(items, hasSize(3));

    AssetXO assetXO = items.stream().filter(item -> item.getPath().equals("bar.one")).findFirst().get();
    assertThat(assetXO.getChecksum().get("sha1"), is("third-sha1"));
  }

  @Test
  public void testSearch_Multiple_By_Extension_Empty_Classifier_Return_Maven_Asset_Without_Classifier() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?assets.attributes.maven2.extension=jar&maven.classifier"));
    List<AssetXO> items = assets.getItems();
    assertThat(items, hasSize(1));
    assertThat(items.get(0).getPath(), is("antlr.jar"));
  }

  @Test
  public void testSearch_Multiple_By_AVE_And_Empty_Classifier_Return_Maven_WithOut_Classifier() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?maven.artifactId=antlr&maven.version=2.0&maven.extension=jar&maven.classifier"));
    List<AssetXO> items = assets.getItems();
    assertThat(items, hasSize(1));
    assertThat(items.get(0).getPath(), is("antlr.jar"));

    assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?maven.artifactId=antlr&maven.version=2.0&maven.extension=pom&maven.classifier"));
    items = assets.getItems();
    assertThat(items, hasSize(1));
    assertThat(items.get(0).getPath(), is("antlr.pom"));
  }

  @Test
  public void testSearch_Multiple_By_Classifier_Return_Maven_With_Classifier() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?assets.attributes.maven2.classifier=fooz"));
    List<AssetXO> items = assets.getItems();
    assertThat(items, hasSize(1));
    assertThat(items.get(0).getPath(), is("antlr-fooz.jar"));
  }

  @Test
  public void testSearch_Multiple_By_GA_And_Classifier_Return_Maven_Asset_With_Classifier() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?maven.artifactId=antlr&maven.version=2.0&assets.attributes.maven2.classifier=fooz"));
    List<AssetXO> items = assets.getItems();
    assertThat(items, hasSize(1));
    assertThat(items.get(0).getPath(), is("antlr-fooz.jar"));
  }

  @Test
  public void testSearch_Using_Long_And_Short_AssetParamNames() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven, searchHitNpm));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets_longName = underTest.searchAssets(null, null, null, null,
        uriInfo("?assets.attributes.maven2.extension=jar"));
    List<AssetXO> items_longName = assets_longName.getItems();
    assertThat(items_longName, hasSize(1));

    Page<AssetXO> assets_shortName = underTest.searchAssets(null, null, null, null,
        uriInfo("?maven.extension=jar"));
    List<AssetXO> items_shortName = assets_shortName.getItems();
    assertThat(items_shortName, hasSize(1));
  }

  @Test
  public void testSearch_When_Multiple_Aliases_For_An_AssetAttribute() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven, searchHitNpm));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets_shortName = underTest.searchAssets(null, null, null, null,
        uriInfo("?maven.extension=jar"));
    List<AssetXO> items_shortName = assets_shortName.getItems();
    assertThat(items_shortName, hasSize(1));

    //Search using alternate alias mapped to the same attribute as maven.extension
    Page<AssetXO> assets_alternateName = underTest.searchAssets(null, null, null, null,
        uriInfo("?mvn.extension=jar"));
    List<AssetXO> items_alternateName = assets_alternateName.getItems();
    assertThat(items_alternateName, hasSize(1));
  }

  @Test
  public void testSearch_With_UnMapped_Long_AssetAttribute() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    //Positive case, 'classifier' is unmapped
    Page<AssetXO> assets_validAttribute = underTest.searchAssets(null, null, null, null,
        uriInfo("?assets.attributes.maven2.classifier=foo"));
    List<AssetXO> items_assets_validAttribute = assets_validAttribute.getItems();
    assertThat(items_assets_validAttribute, hasSize(1));

    //Negative case
    Page<AssetXO> assets_inValidAttribute = underTest.searchAssets(null, null, null, null,
        uriInfo("?assets.attributes.maven3.classifier=foo"));
    List<AssetXO> items_inValidAttribute = assets_inValidAttribute.getItems();
    assertThat(items_inValidAttribute, hasSize(0));
  }

  @Test
  public void testSearch_MultipleAssets_Pagination() {
    SearchResponse multipleAssetsSearchResponse1 = new SearchResponse();
    multipleAssetsSearchResponse1.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));
    multipleAssetsSearchResponse1.setContinuationToken("foo");
    SearchResponse multipleAssetsSearchResponse2 = new SearchResponse();
    multipleAssetsSearchResponse2.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));
    multipleAssetsSearchResponse2.setContinuationToken("foo");
    SearchResponse multipleAssetsSearchResponse3 = new SearchResponse();
    multipleAssetsSearchResponse3.setSearchResults(Collections.emptyList());

    when(searchService.search(any()))
        .thenReturn(multipleAssetsSearchResponse1, multipleAssetsSearchResponse2, multipleAssetsSearchResponse3);

    underTest.setPageSize(1);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?format=maven2"));
    assertThat(assets.getContinuationToken(), notNullValue());
    assertThat(assets.getItems(), hasSize(3));

    assets = underTest.searchAssets(assets.getContinuationToken(), null, null, null, uriInfo("?format=maven2"));
    assertThat(assets.getContinuationToken(), notNullValue());
    assertThat(assets.getItems(), hasSize(3));

    assets = underTest.searchAssets(assets.getContinuationToken(), null, null, null, uriInfo("?format=maven2"));
    assertThat(assets.getContinuationToken(), nullValue());
    assertThat(assets.getItems(), hasSize(0));
  }

  @Test
  public void testSearchAndDownload_NoAssetParams_WillReturnAll() {
    // mock Elastic is only returning npm
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?format=npm"));

    List<AssetXO> items = assets.getItems();

    assertThat(items, hasSize(3));

    AssetXO assetXO = items.stream().filter(item -> item.getPath().equals("bar.one")).findFirst().get();
    assertThat(assetXO.getRepository(), is("test-repo"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/test/bar.one"));

    AssetXO assetXO2 = items.stream().filter(item -> item.getPath().equals("bar.three")).findFirst().get();
    assertThat(assetXO2.getRepository(), is("test-repo"));
    assertThat(assetXO2.getDownloadUrl(), is("http://localhost:8081/test/bar.three"));

    //the expected query
    SearchRequest request = queryBuilderArgumentCaptor.getValue();
    assertThat(request.getSearchFilters(), hasItem(new SearchFilter("format", "npm")));
  }

  @Test
  public void testSearchAndDownload_SpecificAssetParam_WillReturnOne() throws Exception {
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));

    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?format=npm&sha1=fifth-sha1"));

    List<AssetXO> items = assets.getItems();

    assertThat(items, hasSize(1));

    AssetXO assetXO = items.get(0);
    assertThat(assetXO.getPath(), equalTo("bar.three"));
    assertThat(assetXO.getRepository(), is("test-repo"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/test/bar.three"));

    SearchRequest actual = queryBuilderArgumentCaptor.getValue();

    assertThat(actual.getSearchFilters(), containsInAnyOrder(new SearchFilter("format", "npm"),
        new SearchFilter("assets.attributes.checksum.sha1", "fifth-sha1")));
  }

  @Test
  public void testSearchAndDownload_SpecificAssetParam_NotFound() {
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Page<AssetXO> assets = underTest.searchAssets(null, null, null, null,
        uriInfo("?format=npm&sha1=notfound"));

    List<AssetXO> items = assets.getItems();

    assertThat(items, hasSize(0));
  }

  @Test
  public void testSearchAndDownload_SpecificAssetParam_NotFound_404_HTTP_RESPONSE() {
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);
    try {
      underTest.searchAndDownloadAssets(null, null, null, uriInfo("?format=npm&sha1=notfound"));
    }
    catch (WebApplicationException webEx) {
      assertThat(webEx.getResponse().getStatus(), equalTo(404));
    }
  }

  @Test
  public void testSearchAndDownload_SpecificAssetParam_NotFound_400_HTTP_RESPONSE() {
    // mock Elastic is only returning npm
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);
    try {
      underTest.searchAndDownloadAssets(null, null, null, uriInfo("?format=npm"));
    }
    catch (WebApplicationException webEx) {
      assertThat(webEx.getResponse().getStatus(), equalTo(400));
    }
  }

  @Test
  public void testSearchAndDownload_SpecificAssetParam_AssetFound_302_REDIRECT() {
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);
    Response response = underTest.searchAndDownloadAssets(null, null, null,
        uriInfo("?format=npm&sha1=fifth-sha1"));
    assertThat(response.getStatus(), equalTo(302));
    assertThat(response.getHeaderString("Location"), is("http://localhost:8081/test/bar.three"));
  }

  @Test
  public void testSearchAndDownload_WithLongAssetParam_AssetFound_302_REDIRECT() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);
    Response response = underTest.searchAndDownloadAssets(null, null, null,
        uriInfo("?assets.attributes.maven2.extension=jar"));
    assertThat(response.getStatus(), equalTo(302));
    assertThat(response.getHeaderString("Location"), is("http://localhost:8081/test/antlr.jar"));
  }

  @Test
  public void testSearchAndDownload_EmptyClassifier_JarAssetFound_302_REDIRECT() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);
    Response response =
        underTest.searchAndDownloadAssets(null, null, null,
            uriInfo("?assets.attributes.maven2.extension=jar&maven.classifier"));
    assertThat(response.getStatus(), equalTo(302));
    assertThat(response.getHeaderString("Location"), is("http://localhost:8081/test/antlr.jar"));
  }

  @Test
  public void testSearchAndDownload_Classifier_JarAssetFound_302_REDIRECT() {
    searchResponse.setSearchResults(Arrays.asList(searchHitMaven_withMultipleAssets));
    when(searchService.search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);
    Response response =
        underTest.searchAndDownloadAssets(null, null, null,
            uriInfo("?assets.attributes.maven2.version=2.0&maven.classifier=fooz"));
    assertThat(response.getStatus(), equalTo(302));
    assertThat(response.getHeaderString("Location"), is("http://localhost:8081/test/antlr-fooz.jar"));
  }

  @Test
  public void testSearchAndDownload_withSort() {
    searchResponse.setSearchResults(Arrays.asList(searchHitNpm));
    when(searchService
        .search(queryBuilderArgumentCaptor.capture()))
        .thenReturn(searchResponse);

    Response response = underTest.searchAndDownloadAssets("group", "desc", null, uriInfo("?format=npm&sha1=fifth-sha1"));

    assertThat(response.getStatus(), equalTo(302));
    assertThat(response.getHeaderString("Location"), is("http://localhost:8081/test/bar.three"));
  }

  @Test
  public void testBuildQuery() {
    String uri = "?format=maven2" +
        "&arbitrary.param=random" +
        "&sha256=" +
        "&q=someKindOfStringQuery";

    List<SearchFilter> actual = searchUtils.getSearchFilters(uriInfo(uri));

    assertThat(actual, containsInAnyOrder(new SearchFilter("arbitrary.param", "random"),
        new SearchFilter("format", "maven2"), new SearchFilter("keyword", "someKindOfStringQuery"),
        new SearchFilter("assets.attributes.checksum.sha256", "")));
  }

  @Test
  public void testGetAssetParams() {
    MultivaluedMap<String, String> result = underTest.getAssetParams(uriInfo("?sha1=thisisthesha1&name=antlr"));
    assertThat(result.size(), equalTo(1));
    assertThat(result, hasKey("sha1"));

    // put every single search param into the pam
    StringBuilder sb = new StringBuilder();
    Set<String> allKeys = searchUtils.getSearchParameters().keySet();
    allKeys.forEach(s -> sb.append(s).append("=valueDoesNotMatter&"));

    // asert only assert params remain
    result = underTest.getAssetParams(uriInfo("?" + sb.toString()));
    assertThat(result.size(), equalTo(searchUtils.getAssetSearchParameters().size()));
    assertThat(result.keySet(), equalTo(searchUtils.getAssetSearchParameters().keySet()));
  }

  @Test
  public void testGetAssetParams_ForLongAssetParamsEntries() {
    //Positive case for long asset search param entries
    MultivaluedMap<String, String> longNameResult = underTest.getAssetParams(
        uriInfo("?assets.attributes.maven2.extension=jar"));
    assertThat(longNameResult.size(), equalTo(1));
    assertThat(longNameResult, hasKey("assets.attributes.maven2.extension"));

    //Verify negative case
    MultivaluedMap<String, String> negativeCaseResult = underTest.getAssetParams(
        uriInfo("?attributes.not.asset.jar"));
    assertThat(negativeCaseResult.size(), equalTo(0));
  }

  @Test
  public void testGetAssetParams_ForParamsNotInSearchMappings() {
    //Verify a query string can contain asset attributes not in the search mappings
    MultivaluedMap<String, String> shortNameResult = underTest.getAssetParams(
        uriInfo("?assets.attributes.maven2.classifier=jar"));
    assertThat(shortNameResult.size(), equalTo(1));
  }

  private UriInfo uriInfo(final String uri) {
    return new ResteasyUriInfo(URI.create(uri));
  }
}
