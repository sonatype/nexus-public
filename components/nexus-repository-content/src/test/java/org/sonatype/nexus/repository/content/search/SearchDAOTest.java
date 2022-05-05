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
package org.sonatype.nexus.repository.content.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.example.TestSearchDAO;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.NAME;

/**
 * Test {@link SearchDAO}.
 */
public class SearchDAOTest
    extends ExampleContentTestSupport
{
  public SearchDAOTest() {
    super(TestSearchDAO.class);
  }

  @Before
  public void setupContent() {
    generateRandomNamespaces(5);
    generateRandomNames(5);
    generateRandomVersions(10);
    generateRandomPaths(10);
  }

  /**
   * This test exists to ensure the stubbed DAO doesn't break or cause exceptions
   */
  @Test
  public void testEmptyCreateSchema() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(TestSearchDAO.class);
    }
  }

  @Test
  public void testSearchComponents() {
    generateConfiguration();
    EntityId repositoryId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateContent(1);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchDAO searchDAO = session.access(TestSearchDAO.class);

      int count = searchDAO.count(null, null);
      assertThat(count, is(1));

      SqlSearchRequest request = SqlSearchRequest.builder()
          .limit(10)
          .sortColumnName(SearchViewColumns.COMPONENT_ID.name())
          .sortDirection(SortDirection.ASC.name())
          .defaultSortColumnName(SearchViewColumns.COMPONENT_ID.name())
          .build();
      Collection<SearchResult> actual = searchDAO.searchComponents(request);
      Optional<SearchResult> componentSearch = actual.stream().findFirst();

      assertThat(componentSearch.isPresent(), is(true));
      SearchResult searchResult = componentSearch.get();
      assertThat(searchResult.componentId(), notNullValue());
      assertThat(searchResult.namespace(), notNullValue());
      assertThat(searchResult.componentName(), notNullValue());
      assertThat(searchResult.version(), notNullValue());
      assertThat(searchResult.repositoryName(), notNullValue());
    }
  }

  @Test
  public void testSearchComponentsWithFilter() {
    generateConfiguration();
    EntityId repositoryId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    List<String> componentNames = Arrays.asList("component", "foo_component", "test_component_name", "name");
    generateContent(componentNames);

    SqlSearchQueryConditionBuilder queryConditionBuilder = new SqlSearchQueryConditionBuilder();
    SqlSearchQueryCondition queryCondition = queryConditionBuilder.condition(NAME.getColumnName(), "*component*");

    String conditionFormat = queryCondition.getSqlConditionFormat();
    Map<String, String> values = queryCondition.getValues();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchDAO searchDAO = session.access(TestSearchDAO.class);

      int count = searchDAO.count(conditionFormat, values);
      assertThat(count, is(3));

      SqlSearchRequest request = SqlSearchRequest.builder()
          .searchFilter(conditionFormat)
          .searchFilterValues(values)
          .limit(10)
          .sortColumnName(SearchViewColumns.COMPONENT_ID.name())
          .defaultSortColumnName(SearchViewColumns.COMPONENT_ID.name())
          .build();
      Collection<SearchResult> results = searchDAO.searchComponents(request);

      assertThat(results.size(), is(3));
      assertThat(results.stream().filter(component -> component.componentName().equals("name")).count(), is(0L));
    }
  }

  @Test
  public void testSearchComponentsWithOffset() {
    generateConfiguration();
    EntityId repositoryId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateContent(10);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchDAO searchDAO = session.access(TestSearchDAO.class);

      int count = searchDAO.count(null, null);
      assertThat(count, is(10));
      SqlSearchRequest request = SqlSearchRequest.builder()
          .limit(10)
          .offset(10)
          .sortColumnName(SearchViewColumns.COMPONENT_ID.name())
          .sortDirection(SortDirection.ASC.name())
          .defaultSortColumnName(SearchViewColumns.COMPONENT_ID.name())
          .build();

      Collection<SearchResult> actual = searchDAO.searchComponents(request);
      assertThat(actual.isEmpty(), is(true));
    }
  }
}
