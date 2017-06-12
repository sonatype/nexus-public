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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.rest.Page;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class SearchResourceTest
    extends RepositoryResourceTestSupport
{
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  SearchService searchService;

  @Mock
  UriInfo uriInfo;

  @Mock
  SearchResponse searchResponse;

  @Mock
  SearchHits searchHits;

  @Mock
  SearchHit searchHit;

  @Mock
  SearchHit searchHit1;

  @Captor
  ArgumentCaptor<QueryBuilder> queryBuilderArgumentCaptor;

  @Mock
  Repository repository;

  @Mock
  BrowseResult<Asset> browseResult;

  SearchResource underTest;

  @Before
  public void setup() {
    configureMockedRepository(repository, "test-repo", "http://localhost:8081/test");
    setupResponse();

    underTest = new SearchResource(repositoryManagerRESTAdapter, browseService, searchService, new GroupType(),
        new TokenEncoder());
  }

  private void setupResponse() {
    when(searchResponse.getHits()).thenReturn(searchHits);

    Map<String, Object> source = of("group", "test", "repository_name", "test-repo", "name", "foo", "version", "1.0");
    when(searchHit.sourceAsMap()).thenReturn(source);
    when(searchHit.getSource()).thenReturn(source);
    when(searchHit.getId()).thenReturn("id1");

    when(browseService.browseComponentAssets(eq(repository), anyString())).thenReturn(browseResult);
    Asset mockedAsset = getMockedAsset("first", "one");
    Asset mockedAsset1 = getMockedAsset("second", "two");

    when(browseResult.getResults()).thenReturn(asList(mockedAsset,
        mockedAsset1));

    Map<String, Object> source2 = of("name", "bar", "repository_name", "test-repo");
    when(searchHit1.sourceAsMap()).thenReturn(source2);
    when(searchHit1.getSource()).thenReturn(source2);
    when(searchHit1.getId()).thenReturn("id2");

    when(searchHits.hits()).thenReturn(new SearchHit[]{searchHit, searchHit1});
  }

  @Test
  public void testSearch() {
    //URI Info containing the search parameters
    MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap();
    multivaluedMap.putSingle("format", "maven2");
    multivaluedMap.putSingle("notValid", "missing"); //this one should be ignored

    when(uriInfo.getQueryParameters()).thenReturn(multivaluedMap);

    //the expected query
    QueryBuilder expected = boolQuery()
        .filter(termQuery("format", "maven2"));

    when(searchService.search(queryBuilderArgumentCaptor.capture(), eq(emptyList()), eq(0), eq(50)))
        .thenReturn(searchResponse);

    Page<ComponentXO> componentPage = underTest.search(null, uriInfo);

    List<ComponentXO> items = componentPage.getItems();

    assertThat(items, hasSize(2));
    
    ComponentXO componentXO = items.stream().filter(item -> item.getName().equals("foo")).findFirst().get();
    assertThat(componentXO.getGroup(), is("test"));
    assertThat(componentXO.getVersion(), is("1.0"));

    ComponentXO componentXO1 = items.stream().filter(item -> item.getName().equals("bar")).findFirst().get();
    assertThat(componentXO1.getGroup(), nullValue());
    assertThat(componentXO1.getVersion(), nullValue());
    
    assertThat(queryBuilderArgumentCaptor.getValue().toString(), is(expected.toString()));
  }

  @Test
  public void testBuildQuery() {
    MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap();
    multivaluedMap.putSingle("format", "maven2");
    multivaluedMap.putSingle("notValid", "missing"); //this one should be ignored
    multivaluedMap.put("sha256", emptyList()); //this one should be ignored as well because it is empty
    multivaluedMap.putSingle("q", "some kind of string query");

    when(uriInfo.getQueryParameters()).thenReturn(multivaluedMap);

    //the expected query
    QueryBuilder expected = boolQuery()
        .must(queryStringQuery("some kind of string query"))
        .filter(termQuery("format", "maven2"));

    QueryBuilder actual = underTest.buildQuery(uriInfo);

    assertThat(actual.toString(), is(expected.toString()));
  }
}
