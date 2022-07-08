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
package org.sonatype.nexus.repository.content.search.table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchViewColumns;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.FORMAT_FIELD_1;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.NAME;

/**
 * Test {@link SearchTableDAO}.
 */
public class SearchTableDAOTest
    extends ExampleContentTestSupport
{
  private static final String FORMAT = "test";

  private static final int TABLE_RECORDS_TO_GENERATE = 2;

  private static final List<SearchTableData> GENERATED_DATA = new ArrayList<>(TABLE_RECORDS_TO_GENERATE);

  private DataSession<?> session;

  private SearchTableDAO searchDAO;

  private ContentRepositoryData repository;

  public SearchTableDAOTest() {
    super(SearchTableDAO.class);
  }

  @Before
  public void setupContent() {
    generateRandomNamespaces(5);
    generateRandomNames(5);
    generateRandomVersions(10);
    generateRandomPaths(10);
    generateConfiguration();

    ConfigurationData configuration = generatedConfigurations().get(0);
    generateSingleRepository(UUID.fromString(configuration.getRepositoryId().getValue()));
    repository = generatedRepositories().get(0);

    generateContent(TABLE_RECORDS_TO_GENERATE);

    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);

    for (int i = 0; i < TABLE_RECORDS_TO_GENERATE; i++) {
      Component component = generatedComponents().get(i);
      Asset asset = generatedAssets().get(i);
      AssetBlob blob = generatedAssetBlobs().get(i);

      SearchTableData tableData = new SearchTableData();
      //PK
      tableData.setRepositoryId(repository.contentRepositoryId());
      tableData.setComponentId(InternalIds.internalComponentId(component));
      tableData.setAssetId(InternalIds.internalAssetId(asset));
      tableData.setFormat(FORMAT);
      //tableData component
      tableData.setNamespace(component.namespace() + "_" + i);
      tableData.setComponentName(component.name() + "_" + i);
      tableData.setComponentKind(component.kind() + "_" + i);
      tableData.setVersion(component.version() + "_" + i);
      tableData.setRepositoryName(configuration.getRepositoryName() + "_" + i);
      tableData.setComponentCreated(OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
      //tableData asset
      tableData.setPath(asset.path() + "_" + i);
      //tableData blob
      tableData.setContentType(blob.contentType());
      tableData.setMd5(blob.checksums().get(MD5.name()) + "_" + i);
      tableData.setSha1(blob.checksums().get(SHA1.name()) + "_" + i);
      tableData.setSha256(blob.checksums().get(SHA256.name()) + "_" + i);
      tableData.setSha512(blob.checksums().get(SHA512.name()) + "_" + i);

      // custom format attributes
      tableData.setFormatField1("formatField1_" + i);
      tableData.setFormatField2("formatField2_" + i);
      tableData.setFormatField3("formatField3_" + i);
      GENERATED_DATA.add(tableData);
    }

    searchDAO = session.access(SearchTableDAO.class);
  }

  @After
  public void destroyContent() {
    GENERATED_DATA.clear();
    session.close();
  }

  @Test
  public void testCreateAndCount() {
    GENERATED_DATA.forEach(searchDAO::create);
    int count = searchDAO.count(null, null);
    assertThat(count, is(2));
  }

  @Test
  public void testSearchComponents() {
    GENERATED_DATA.forEach(searchDAO::create);
    int count = searchDAO.count(null, null);
    assertThat(count, is(2));

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
    assertThat(searchResult.created(), notNullValue());
  }

  @Test
  public void testSearchComponentsWithFilter() {
    GENERATED_DATA.forEach(searchDAO::create);

    List<String> componentNames = Arrays.asList("component", "foo_component", "test_component_name", "name");
    generateContent(componentNames);

    SqlSearchQueryConditionBuilder queryConditionBuilder = new SqlSearchQueryConditionBuilder();
    SqlSearchQueryCondition queryCondition = queryConditionBuilder.condition(NAME.getColumnName(), "*component*");
    Map<String, String> values = queryCondition.getValues();

    String conditionFormat = queryCondition.getSqlConditionFormat();

    int count = searchDAO.count(conditionFormat, values);
    assertThat(count, is(2));

    SqlSearchRequest request = SqlSearchRequest.builder()
        .searchFilter(conditionFormat)
        .searchFilterValues(values)
        .limit(10)
        .sortColumnName(SearchViewColumns.COMPONENT_ID.name())
        .defaultSortColumnName(SearchViewColumns.COMPONENT_ID.name())
        .build();
    Collection<SearchResult> results = searchDAO.searchComponents(request);

    assertThat(results.size(), is(2));
    assertThat(results.stream().filter(component -> component.componentName().equals("name")).count(), is(0L));
  }

  @Test
  public void testSearchComponentsWithOffset() {
    GENERATED_DATA.forEach(searchDAO::create);
    int count = searchDAO.count(null, null);
    assertThat(count, is(2));
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

  @Test
  public void testUpdateKind() {
    GENERATED_DATA.forEach(searchDAO::create);
    SearchTableData tableData = GENERATED_DATA.get(0);
    searchDAO.updateKind(tableData.getRepositoryId(), tableData.getComponentId(), FORMAT, "customKind");
    SqlSearchQueryConditionBuilder queryConditionBuilder = new SqlSearchQueryConditionBuilder();
    SqlSearchQueryCondition queryCondition = queryConditionBuilder.condition("component_kind", "customKind");
    Map<String, String> values = queryCondition.getValues();

    String conditionFormat = queryCondition.getSqlConditionFormat();

    int count = searchDAO.count(conditionFormat, values);
    assertThat(count, is(1));
  }

  @Test
  public void testUpdateFormatFields() {
    GENERATED_DATA.forEach(searchDAO::create);

    SearchTableData tableData = GENERATED_DATA.get(0);
    searchDAO.updateFormatFields(tableData.getRepositoryId(), tableData.getComponentId(), tableData.getAssetId(),
        FORMAT, "customField1", "customField2", "customField3");
    SqlSearchQueryConditionBuilder queryConditionBuilder = new SqlSearchQueryConditionBuilder();
    SqlSearchQueryCondition queryCondition =
        queryConditionBuilder.condition(FORMAT_FIELD_1.getColumnName(), "customField1");
    Map<String, String> values = queryCondition.getValues();

    String conditionFormat = queryCondition.getSqlConditionFormat();

    int count = searchDAO.count(conditionFormat, values);
    assertThat(count, is(1));
  }

  @Test
  public void testDelete() {
    GENERATED_DATA.forEach(searchDAO::create);
    SearchTableData tableData = GENERATED_DATA.get(0);
    searchDAO.delete(tableData.getRepositoryId(), tableData.getComponentId(), tableData.getAssetId(), FORMAT);
    int count = searchDAO.count(null, null);
    assertThat(count, is(1));
  }

  @Test
  public void testDeleteAllForRepository() {
    GENERATED_DATA.forEach(searchDAO::create);
    searchDAO.deleteAllForRepository(repository.contentRepositoryId(), FORMAT, 1);
    int count = searchDAO.count(null, null);
    assertThat(count, is(1));
  }

  @Test
  public void testDeleteAllForRepositoryWithoutLimit() {
    GENERATED_DATA.forEach(searchDAO::create);
    searchDAO.deleteAllForRepository(repository.contentRepositoryId(), FORMAT, 0);
    int count = searchDAO.count(null, null);
    assertThat(count, is(0));
  }

  @Test
  public void testSaveBatch() {
    searchDAO.saveBatch(GENERATED_DATA);

    int count = searchDAO.count(null, null);
    assertThat(count, is(TABLE_RECORDS_TO_GENERATE));
  }
}
