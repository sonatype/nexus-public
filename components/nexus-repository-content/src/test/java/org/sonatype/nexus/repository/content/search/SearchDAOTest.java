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

import java.util.List;
import java.util.UUID;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ComponentSearch;
import org.sonatype.nexus.repository.content.browse.store.example.TestSearchDAO;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;

import org.junit.Before;
import org.junit.Test;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
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
  public void testSearchComponents() throws InterruptedException {
    generateConfiguration();
    final EntityId repositoryId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateRandomContent(1, 1);

    final SearchData actualData;

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      SearchDAO searchDAO = session.access(TestSearchDAO.class);

      final Continuation<ComponentSearch> actual =
          searchDAO.searchComponents(1000, null, null, null, false, null);

      actualData = actual.stream().findFirst()
          .map(actualSearch -> (SearchData) actualSearch).orElseGet(SearchData::new);
    }

    assertThat("Data fetched from DB is same as generated",
        isProvidedSearchDataAreEqualAndNotNull(actualData, getGeneratedData()));
  }

  private boolean isProvidedSearchDataAreEqualAndNotNull(SearchData left, SearchData right) {
    if (isNull(left)
        || isNull(right)
        || isNull(left.componentId)
        || isNull(left.namespace())
        || isNull(left.componentName())
        || isNull(left.version())
        || isNull(left.repositoryName())
        || isNull(left.repositoryName())) {
      return false;
    }

    return left.componentId.equals(right.componentId)
        && left.namespace().equals(right.namespace())
        && left.componentName().equals(right.componentName())
        && left.version().equals(right.version())
        && left.repositoryName().equals(right.repositoryName());
  }

  private SearchData getGeneratedData() {
    final List<Component> components = generatedComponents();
    final List<ConfigurationData> configurations = generatedConfigurations();
    if (components.isEmpty() || configurations.isEmpty()) {
      return new SearchData();
    }
    SearchData searchData =
        getSearchDataFromComponent((ComponentData) components.get(0));

    searchData.setRepositoryName(configurations.get(0).getRepositoryName());
    searchData.setAttributes(generatedAssets().get(0).attributes());

    return searchData;
  }
}
