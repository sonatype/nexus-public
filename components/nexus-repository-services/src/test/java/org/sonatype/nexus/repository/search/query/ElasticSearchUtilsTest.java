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
package org.sonatype.nexus.repository.search.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.rest.sql.SearchField;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.hamcrest.Matcher;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElasticSearchUtilsTest
    extends TestSupport
{
  static final String VALID_SHA1_ATTRIBUTE_NAME = "assets.attributes.checksum.sha1";

  static final String INVALID_SHA1_ATTRIBUTE_NAME = "asset.attributes.checksum.sha1";

  static final String SHA1_ALIAS = "sha1";

  private static final String QUERY_STRING = "continuationToken=1&parameter=test&wait=false";

  private static final String URI = "http://localhost";

  private static final String CONTEXT_PATH = "/";

  private static final Field fieldNameField = getField(FieldSortBuilder.class, "fieldName");

  private static final Field orderField = getField(FieldSortBuilder.class, "order");

  @Mock
  RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  ElasticSearchUtils underTest;

  @Before
  public void setup() {

    Map<String, SearchMappings> searchMappings = ImmutableMap.of(
        "default", () -> ImmutableList.of(
            new SearchMapping(SHA1_ALIAS, VALID_SHA1_ATTRIBUTE_NAME, "", SearchField.SHA1)),
        "testFormat", () -> ImmutableList.of(
            new SearchMapping("format.custom", "attributes.format.custom", "", SearchField.FORMAT_FIELD_1),
            new SearchMapping("format.test", "attributes.format.test", "", SearchField.FORMAT_FIELD_2)));

    Map<String, ElasticSearchContribution> searchContributions = new HashMap<>();
    searchContributions.put(DefaultElasticSearchContribution.NAME, new DefaultElasticSearchContribution());
    searchContributions.put(BlankValueElasticSearchContribution.NAME, new BlankValueElasticSearchContribution());
    searchContributions.put(KeywordElasticSearchContribution.NAME, new KeywordElasticSearchContribution());
    underTest = new ElasticSearchUtils(repositoryManagerRESTAdapter, searchMappings, searchContributions, emptyMap());
  }

  @Test
  public void testIsFullAssetAttributeName() {
    assertTrue(underTest.isFullAssetAttributeName(VALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName_Invalid_LongForm_Attribute_ReturnsFalse() {
    assertFalse(underTest.isFullAssetAttributeName(INVALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName_MappedAlias_ReturnsFalse() {
    assertFalse(underTest.isFullAssetAttributeName(SHA1_ALIAS));
  }

  @Test
  public void buildQueryRemoveContinuationTokenByDefault() {
    ResteasyUriInfo uriInfo = new ResteasyUriInfo(URI, QUERY_STRING, CONTEXT_PATH);
    String query = underTest.buildQuery(uriInfo).toString();

    assertQueryParameters(query, containsString("wait"));
  }

  @Test
  public void buildQueryRemoveSelectedParametersIncludingDefault() {
    ResteasyUriInfo uriInfo = new ResteasyUriInfo(URI, QUERY_STRING, CONTEXT_PATH);
    String query = underTest.buildQuery(uriInfo, singletonList("wait")).toString();

    assertQueryParameters(query, not(containsString("wait")));
  }

  private void assertQueryParameters(final String query, final Matcher<String> parameterMatcher) {
    assertThat(query, containsString("parameter"));
    assertThat(query, parameterMatcher);
    assertThat(query, not(containsString("continuationToken")));
  }

  @Test
  public void testGetSortBuilders_byGroup() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("group", "asc");
    assertThat(sortBuilders.size(), is(3));
    assertSearchBuilder(sortBuilders.get(0), "group.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(1), "name.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(2), "version", "asc");
  }

  @Test
  public void testGetSortBuilders_byGroupDescending() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("group", "desc");
    assertThat(sortBuilders.size(), is(3));
    assertSearchBuilder(sortBuilders.get(0), "group.case_insensitive", "desc");
    assertSearchBuilder(sortBuilders.get(1), "name.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(2), "version", "asc");
  }

  @Test
  public void testGetSortBuilders_byGroupDefaultSort() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("group", null);
    assertThat(sortBuilders.size(), is(3));
    assertSearchBuilder(sortBuilders.get(0), "group.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(1), "name.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(2), "version", "asc");
  }

  @Test
  public void testGetSortBuilders_byName() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("name", "asc");
    assertThat(sortBuilders.size(), is(3));
    assertSearchBuilder(sortBuilders.get(0), "name.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(1), "version", "asc");
    assertSearchBuilder(sortBuilders.get(2), "group.case_insensitive", "asc");
  }

  @Test
  public void testGetSortBuilders_byNameDescending() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("name", "desc");
    assertThat(sortBuilders.size(), is(3));
    assertSearchBuilder(sortBuilders.get(0), "name.case_insensitive", "desc");
    assertSearchBuilder(sortBuilders.get(1), "version", "asc");
    assertSearchBuilder(sortBuilders.get(2), "group.case_insensitive", "asc");
  }

  @Test
  public void testGetSortBuilders_byNameDefaultSort() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("name", null);
    assertThat(sortBuilders.size(), is(3));
    assertSearchBuilder(sortBuilders.get(0), "name.case_insensitive", "asc");
    assertSearchBuilder(sortBuilders.get(1), "version", "asc");
    assertSearchBuilder(sortBuilders.get(2), "group.case_insensitive", "asc");
  }

  @Test
  public void testGetSortBuilders_byRepository() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("repository", "asc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "repository_name", "asc");
  }

  @Test
  public void testGetSortBuilders_byRepositoryDescending() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("repository", "desc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "repository_name", "desc");
  }

  @Test
  public void testGetSortBuilders_byRepositoryDefaultSort() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("repository", null);
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "repository_name", "asc");
  }

  @Test
  public void testGetSortBuilders_byRepositoryName() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("repositoryName", "asc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "repository_name", "asc");
  }

  @Test
  public void testGetSortBuilders_byRepositoryNameDescending() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("repositoryName", "desc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "repository_name", "desc");
  }

  @Test
  public void testGetSortBuilders_byRepositoryNameDefaultSort() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("repositoryName", null);
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "repository_name", "asc");
  }

  @Test
  public void testGetSortBuilders_byVersion() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("version", "asc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "normalized_version", "asc");
  }

  @Test
  public void testGetSortBuilders_byVersionDescending() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("version", "desc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "normalized_version", "desc");
  }

  @Test
  public void testGetSortBuilders_byVersionDefaultSort() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("version", null);
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "normalized_version", "desc");
  }

  @Test
  public void testGetSortBuilders_byOtherField() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("otherfield", "asc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "otherfield", "asc");
  }

  @Test
  public void testGetSortBuilders_byOtherFieldDescending() throws Exception {
    List<SortBuilder> sortBuilders = underTest.getSortBuilders("otherfield", "desc");
    assertThat(sortBuilders.size(), is(1));
    assertSearchBuilder(sortBuilders.get(0), "otherfield", "desc");
  }

  @Test
  public void constructSameQueryForSameFilters() {
    SearchFilter a1 = new SearchFilter("a", "1");
    SearchFilter a2 = new SearchFilter("a", "2");
    SearchFilter b1 = new SearchFilter("b", "1");
    SearchFilter b2 = new SearchFilter("b", "2");
    SearchFilter c1 = new SearchFilter("c", "1");

    List<SearchFilter> forwards = newArrayList(a1, a2, b1, b2, c1);
    List<SearchFilter> backward = newArrayList(c1, b2, b1, a2, a1);
    List<SearchFilter> mixed = newArrayList(a1, c1, b1, b2, a2);

    String forwardQuery = underTest.buildQuery(forwards).toString();
    String backwardQuery = underTest.buildQuery(backward).toString();
    String mixedQuery = underTest.buildQuery(mixed).toString();

    assertThat(forwardQuery.equals(backwardQuery), is(true));
    assertThat(forwardQuery.equals(mixedQuery), is(true));
  }

  @Test
  public void buildSearchRequestFromSearchFilters() {
    Collection<SearchFilter> searchFilters = new ArrayList<>();
    searchFilters.add(new SearchFilter("keyword", "org.junit"));
    searchFilters.add(new SearchFilter("repository", "maven_central"));
    searchFilters.add(new SearchFilter("format", "maven"));
    // alias should be mapped to full attribute name
    searchFilters.add(new SearchFilter("format.custom", "testAlias"));
    // attribute name should also be included in query
    searchFilters.add(new SearchFilter("attributes.format.test", "testAttribute"));
    // filters without mapping also included
    searchFilters.add(new SearchFilter("new.prop", "aProp"));

    QueryBuilder queryBuilder = underTest.buildQuery(searchFilters);

    assertThat(queryBuilder.toString(), is("{\n" +
        "  \"bool\" : {\n" +
        "    \"must\" : [ {\n" +
        "      \"query_string\" : {\n" +
        "        \"query\" : \"testAlias\",\n" +
        "        \"fields\" : [ \"attributes.format.custom\" ],\n" +
        "        \"lowercase_expanded_terms\" : false\n" +
        "      }\n" +
        "    }, {\n" +
        "      \"query_string\" : {\n" +
        "        \"query\" : \"testAttribute\",\n" +
        "        \"fields\" : [ \"attributes.format.test\" ],\n" +
        "        \"lowercase_expanded_terms\" : false\n" +
        "      }\n" +
        "    }, {\n" +
        "      \"query_string\" : {\n" +
        "        \"query\" : \"maven\",\n" +
        "        \"fields\" : [ \"format\" ],\n" +
        "        \"lowercase_expanded_terms\" : false\n" +
        "      }\n" +
        "    }, {\n" +
        "      \"query_string\" : {\n" +
        "        \"query\" : \"org.junit\",\n" +
        "        \"fields\" : [ \"name.case_insensitive\", \"group.case_insensitive\", \"_all\" ]\n" +
        "      }\n" +
        "    }, {\n" +
        "      \"query_string\" : {\n" +
        "        \"query\" : \"aProp\",\n" +
        "        \"fields\" : [ \"new.prop\" ],\n" +
        "        \"lowercase_expanded_terms\" : false\n" +
        "      }\n" +
        "    }, {\n" +
        "      \"query_string\" : {\n" +
        "        \"query\" : \"maven_central\",\n" +
        "        \"fields\" : [ \"repository\" ],\n" +
        "        \"lowercase_expanded_terms\" : false\n" +
        "      }\n" +
        "    } ]\n" +
        "  }\n" +
        "}"));
  }

  private static void assertSearchBuilder(
      final SortBuilder sortBuilder,
      final String field,
      final String order) throws Exception
  {
    // see https://github.com/elastic/elasticsearch/issues/20853 as to why i can't do something simple like
    // assertThat(sortBuilder.toString(), is("somejson"));

    assertThat(fieldNameField.get(sortBuilder), is(field));
    assertThat(orderField.get(sortBuilder).toString(), is(order));
  }

  private static Field getField(final Class<?> clazz, final String fieldName) {
    try {
      Field field = FieldSortBuilder.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
