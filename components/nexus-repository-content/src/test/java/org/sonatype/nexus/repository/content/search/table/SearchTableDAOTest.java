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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.sonatype.nexus.content.testsuite.groups.PostgresTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.normalize.VersionNumberExpander;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;
import org.sonatype.nexus.repository.search.sql.textsearch.PostgresFullTextSearchQueryBuilder;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.FORMAT_FIELD_1;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.KEYWORD;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.NAME;

/**
 * Test {@link SearchTableDAO}.
 */
@Category(PostgresTestGroup.class)
public class SearchTableDAOTest
    extends ExampleContentTestSupport
{
  private static final String FORMAT = "test";

  private static final long TABLE_RECORDS_TO_GENERATE = 2L;

  private static final List<SearchTableData> GENERATED_DATA = new ArrayList<>((int) TABLE_RECORDS_TO_GENERATE);

  private DataSession<?> session;

  private SearchTableDAO searchDAO;

  private ContentRepositoryData repository;

  private CountDownLatch beginWork;

  private CountDownLatch workDone;

  private final ExecutorService executorService = newFixedThreadPool(2);

  private final SqlSearchQueryConditionBuilder conditionBuilder = new PostgresFullTextSearchQueryBuilder();

  public SearchTableDAOTest() {
    super(SearchTableDAO.class);
  }

  @Before
  public void setupContent() {
    beginWork = new CountDownLatch(1);
    workDone = new CountDownLatch(2);

    generateRandomNamespaces(5);
    generateRandomNames(5);
    generateRandomVersions(10);
    generateRandomPaths(10);
    generateConfiguration();

    ConfigurationData configuration = generatedConfigurations().get(0);
    generateSingleRepository(UUID.fromString(configuration.getRepositoryId().getValue()));
    repository = generatedRepositories().get(0);

    generateContent((int) TABLE_RECORDS_TO_GENERATE, true);

    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    TestComponentDAO componentDAO = session.access(TestComponentDAO.class);

    for (int i = 0; i < TABLE_RECORDS_TO_GENERATE; i++) {
      Component component = generatedComponents().get(i);
      AssetBlob blob = generatedAssetBlobs().get(i);

      SearchTableData tableData = new SearchTableData();
      //PK
      tableData.setRepositoryId(repository.contentRepositoryId());
      tableData.setComponentId(internalComponentId(component));
      tableData.setFormat(FORMAT);
      //tableData component
      tableData.setNamespace(component.namespace() + "_" + i);
      tableData.setComponentName(component.name() + "_" + i);
      tableData.addAliasComponentName(component.name() + "_" + i );
      tableData.setComponentKind(component.kind() + "_" + i);
      tableData.setVersion(component.version() + "_" + i);
      tableData.setNormalisedVersion(VersionNumberExpander.expand(component.version()));
      tableData.setRepositoryName(configuration.getRepositoryName() + "_" + i);
      tableData.setLastModified(OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
      //tableData blob
      tableData.addKeyword(blob.contentType());
      tableData.addMd5(blob.checksums().get(MD5.name()) + "_" + i);
      tableData.addSha1(blob.checksums().get(SHA1.name()) + "_" + i);
      tableData.addSha256(blob.checksums().get(SHA256.name()) + "_" + i);
      tableData.addSha512(blob.checksums().get(SHA512.name()) + "_" + i);

      // custom format attributes
      tableData.addFormatFieldValue1("formatField1_" + i);
      tableData.addFormatFieldValue2("formatField2_" + i);
      tableData.addFormatFieldValue3("formatField3_" + i);
      tableData.addFormatFieldValue3("formatField4_" + i);
      tableData.addFormatFieldValue3("formatField5_" + i);

      // uploader info
      tableData.addUploader("uploader-name");
      tableData.addUploaderIp("uploader-ip-address");

      componentDAO.readComponent(internalComponentId(component))
          .map(Component::entityVersion).ifPresent(tableData::setEntityVersion);

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
  public void shouldAcceptUpdatesWithCurrentEntityVersion() {
    assertEntityVersionsAreSameAsSearchTableDataToSave();

    GENERATED_DATA.forEach(searchDAO::save);

    long count = searchDAO.count(null, null);

    assertThat(count, is(2L));
  }

  @Test
  public void ignoreUpdatesWithLesserEntityVersion() {
    assertEntityVersionsAreSameAsSearchTableDataToSave();

    GENERATED_DATA.forEach(data -> {
      data.setEntityVersion(data.getEntityVersion() - 1);
      searchDAO.save(data);
    });

    long count = searchDAO.count(null, null);

    assertThat(count, is(0L));
  }

  @Test
  public void concurrentlyAcceptUpdatesWithCurrentEntityVersion() throws Exception {
    assertThat(GENERATED_DATA.get(0).getEntityVersion(), is(GENERATED_DATA.get(1).getEntityVersion()));

    GENERATED_DATA.get(0).addKeyword("path-1");
    GENERATED_DATA.get(1).addKeyword("path-2");

    runConcurrentUpdate();

    SqlSearchQueryCondition condition = conditionBuilder.condition(KEYWORD.getColumnName(), ImmutableSet.of("path-1", "path-2"));

    long count = searchDAO.count(condition.getSqlConditionFormat(), condition.getValues());

    assertThat(count, is(2L));
  }

  @Test
  public void concurrentlyIgnoreUpdatesWithLesserEntityVersion() throws Exception {
    GENERATED_DATA.get(0).setEntityVersion(GENERATED_DATA.get(0).getEntityVersion() - 1);
    GENERATED_DATA.get(0).addKeyword("path-1");
    GENERATED_DATA.get(1).addKeyword("path-2");

    runConcurrentUpdate();

    SqlSearchQueryCondition condition = conditionBuilder.condition(KEYWORD.getColumnName(), "path-1");

    assertThat(searchDAO.count(condition.getSqlConditionFormat(), condition.getValues()), is(0L));

    condition = conditionBuilder.condition(KEYWORD.getColumnName(), "path-2");

    assertThat(searchDAO.count(condition.getSqlConditionFormat(), condition.getValues()), is(1L));
  }

  @Test
  public void testSearchComponents() {
    GENERATED_DATA.forEach(searchDAO::save);
    long count = searchDAO.count(null, null);
    assertThat(count, is(2L));

    SqlSearchRequest request = SqlSearchRequest.builder()
        .limit(10)
        .sortDirection(SortDirection.ASC.name())
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
    assertThat(searchResult.lastModified(), notNullValue());
  }

  @Test
  public void testSearchComponentsWithFilter() {
    GENERATED_DATA.forEach(searchDAO::save);

    List<String> componentNames = Arrays.asList("component", "foo_component", "test_component_name", "name");
    generateContent(componentNames, false);

    SqlSearchQueryConditionBuilder queryConditionBuilder = new PostgresFullTextSearchQueryBuilder();
    SqlSearchQueryCondition queryCondition = queryConditionBuilder.condition(NAME.getColumnName(), "component*");
    Map<String, String> values = queryCondition.getValues();

    String conditionFormat = queryCondition.getSqlConditionFormat();

    long count = searchDAO.count(conditionFormat, values);
    assertThat(count, is(2L));

    SqlSearchRequest request = SqlSearchRequest.builder()
        .searchFilter(conditionFormat)
        .searchFilterValues(values)
        .limit(10)
        .build();
    Collection<SearchResult> results = searchDAO.searchComponents(request);

    assertThat(results.size(), is(2));
    assertThat(results.stream().filter(component -> component.componentName().equals("name")).count(), is(0L));
  }

  @Test
  public void testSearchComponentsWithOffset() {
    GENERATED_DATA.forEach(searchDAO::save);
    long count = searchDAO.count(null, null);
    assertThat(count, is(2L));
    SqlSearchRequest request = SqlSearchRequest.builder()
        .limit(10)
        .offset(10)
        .sortDirection(SortDirection.ASC.name())
        .build();

    Collection<SearchResult> actual = searchDAO.searchComponents(request);
    assertThat(actual.isEmpty(), is(true));
  }

  @Test
  public void testUpdate() {
    GENERATED_DATA.forEach(searchDAO::save);

    SearchTableData tableData = GENERATED_DATA.get(0);
    final SearchTableData searchTableData = new SearchTableData();
    searchTableData.setEntityVersion(tableData.getEntityVersion());
    searchTableData.setRepositoryId(tableData.getRepositoryId());
    searchTableData.setComponentId(tableData.getComponentId());
    searchTableData.setFormat(tableData.getFormat());
    searchTableData.setNamespace(tableData.getNamespace());
    searchTableData.setRepositoryName(tableData.getRepositoryName());
    searchTableData.setComponentName(tableData.getComponentName());
    searchTableData.addAliasComponentName(tableData.getComponentName());
    searchTableData.setVersion(tableData.getVersion());
    searchTableData.setNormalisedVersion(tableData.getNormalisedVersion());
    searchTableData.setComponentKind("jar");
    searchTableData.addFormatFieldValue1("customField1");
    searchTableData.addFormatFieldValue2("customField2");
    searchTableData.addFormatFieldValue3("customField3");
    searchTableData.addFormatFieldValue4("customField4");
    searchTableData.addFormatFieldValue5("customField5");
    searchTableData.addFormatFieldValue6("customField6");
    searchTableData.addFormatFieldValue7("customField7");
    searchDAO.save(searchTableData);
    SqlSearchQueryCondition queryCondition =
        conditionBuilder.condition(FORMAT_FIELD_1.getColumnName(), "customField1");
    Map<String, String> values = queryCondition.getValues();

    String conditionFormat = queryCondition.getSqlConditionFormat();

    long count = searchDAO.count(conditionFormat, values);
    assertThat(count, is(1L));
  }

  @Test
  public void testDelete() {
    GENERATED_DATA.forEach(searchDAO::save);
    SearchTableData tableData = GENERATED_DATA.get(0);
    searchDAO.delete(tableData.getRepositoryId(), tableData.getComponentId(), FORMAT);
    long count = searchDAO.count(null, null);
    assertThat(count, is(1L));
  }

  @Test
  public void testDeleteAllForRepository() {
    GENERATED_DATA.forEach(searchDAO::save);
    searchDAO.deleteAllForRepository(repository.contentRepositoryId(), FORMAT, 1);
    long count = searchDAO.count(null, null);
    assertThat(count, is(1L));
  }

  @Test
  public void testDeleteAllForRepositoryWithoutLimit() {
    GENERATED_DATA.forEach(searchDAO::save);
    searchDAO.deleteAllForRepository(repository.contentRepositoryId(), FORMAT, 0);
    long count = searchDAO.count(null, null);
    assertThat(count, is(0L));
  }

  @Test
  public void testSaveBatch() {
    searchDAO.saveBatch(GENERATED_DATA);

    long count = searchDAO.count(null, null);
    assertThat(count, is(TABLE_RECORDS_TO_GENERATE));
  }

  @Test
  public void testCountRepositorySearchIndexes() {
    GENERATED_DATA.forEach(data -> {
      assertFalse(searchDAO.hasRepositoryEntries(data.getRepositoryName()));
    });

    searchDAO.saveBatch(GENERATED_DATA);

    GENERATED_DATA.forEach(data -> {
      assertTrue(searchDAO.hasRepositoryEntries(data.getRepositoryName()));
    });
  }

  private void assertEntityVersionsAreSameAsSearchTableDataToSave() {
    ComponentDAO componentDAO = session.access(TestComponentDAO.class);
    Optional<Component> savedComponent1 = componentDAO.readComponent(internalComponentId(generatedComponents().get(0)));
    assertThat(savedComponent1.isPresent(), is(true));
    assertThat(GENERATED_DATA.get(0).getEntityVersion(), is(savedComponent1.get().entityVersion()));

    Optional<Component> savedComponent2 = componentDAO.readComponent(internalComponentId(generatedComponents().get(1)));
    assertThat(savedComponent2.isPresent(), is(true));
    assertThat(GENERATED_DATA.get(1).getEntityVersion(), is(savedComponent2.get().entityVersion()));
  }

  private void runConcurrentUpdate() throws InterruptedException {
    executorService.submit(() -> queueUpdate(GENERATED_DATA.get(0)));
    executorService.submit(() -> queueUpdate(GENERATED_DATA.get(1)));
    beginWork.countDown();
    executorService.shutdown();
    workDone.await();
  }

  private void queueUpdate(final SearchTableData searchTableData) {
    try {
      beginWork.await();
      searchDAO.save(searchTableData);
      workDone.countDown();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
