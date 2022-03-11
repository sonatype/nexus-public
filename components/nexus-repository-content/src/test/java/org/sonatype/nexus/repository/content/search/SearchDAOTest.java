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

import java.util.Collection;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.example.TestSearchDAO;
import org.sonatype.nexus.repository.search.SortDirection;

import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

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
    generateContent(1, 1);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchDAO searchDAO = session.access(TestSearchDAO.class);

      int count = searchDAO.count();
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
      assertThat("Data fetched from DB is NOT the same as generated",
          isProvidedSearchDataAreEqualAndNotNull(componentSearch.get(), getGeneratedData()));
    }
  }

  @Test
  public void testSearchComponentsWithOffset() {
    generateConfiguration();
    EntityId repositoryId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateContent(10, 10);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchDAO searchDAO = session.access(TestSearchDAO.class);

      int count = searchDAO.count();
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

  private boolean isProvidedSearchDataAreEqualAndNotNull(final SearchResult left, final SearchResultData right) {
    if (isNull(left) || isNull(right)) {
      return false;
    }

    return Objects.equals(left.componentId(), right.componentId()) &&
        Objects.equals(left.namespace(), right.namespace()) &&
        Objects.equals(left.componentName(), right.componentName()) &&
        Objects.equals(left.version(), right.version()) &&
        Objects.equals(left.repositoryName(), right.repositoryName()) &&
        Objects.equals(left.path(), right.path()) &&
        Objects.equals(left.contentType(), right.contentType()) &&
        Objects.equals(left.checksums(), right.checksums());
  }

  private SearchResultData getGeneratedData() {
    List<Component> components = generatedComponents();
    List<ConfigurationData> configurations = generatedConfigurations();
    if (components.isEmpty() || configurations.isEmpty()) {
      return new SearchResultData();
    }
    SearchResultData searchData = getSearchDataFromComponent((ComponentData) components.get(0));

    searchData.setRepositoryName(configurations.get(0).getRepositoryName());
    Asset asset = generatedAssets().get(0);
    searchData.setAttributes(asset.attributes());
    searchData.setPath(asset.path());

    Optional<AssetBlob> blobOpt = asset.blob();
    if (blobOpt.isPresent()) {
      AssetBlob assetBlob = blobOpt.get();
      searchData.setContentType(assetBlob.contentType());
      searchData.setChecksums(assetBlob.checksums());
    }

    return searchData;
  }
}
