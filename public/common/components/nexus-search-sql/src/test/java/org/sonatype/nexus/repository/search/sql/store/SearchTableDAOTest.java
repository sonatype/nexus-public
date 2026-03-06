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
package org.sonatype.nexus.repository.search.sql.store;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.normalize.VersionNumberExpander;
import org.sonatype.nexus.repository.search.sql.ExpressionGroup;
import org.sonatype.nexus.repository.search.sql.SearchResult;
import org.sonatype.nexus.repository.search.sql.query.SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryConditionGroup;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchRequest;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchConditionFactory;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchDB;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_1;
import static org.sonatype.nexus.repository.rest.sql.SearchField.KEYWORDS;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAME;
import static org.sonatype.nexus.repository.rest.sql.SearchField.PATHS;
import static org.sonatype.nexus.repository.rest.sql.SearchField.TAGS;
import static org.sonatype.nexus.repository.search.sql.query.syntax.Operand.EQ;

/**
 * Test {@link SearchTableDAO}.
 */
@DisabledIf("isPostgres")
public class SearchTableDAOTest
    extends ExampleContentTestSupport
{
  private static final String TAG_NAME = "my-Tag-With-Mixed-Case";

  private static final String FORMAT = "test";

  private static final long TABLE_RECORDS_TO_GENERATE = 2L;

  private static final List<SearchRecordData> GENERATED_DATA = new ArrayList<>((int) TABLE_RECORDS_TO_GENERATE);

  private DataSession<?> session;

  private SearchTableDAO searchDAO;

  private ContentRepositoryData repository;

  private CountDownLatch beginWork;

  private CountDownLatch workDone;

  private final ExecutorService executorService = newFixedThreadPool(2);

  private SearchConditionFactory conditionBuilder;

  @BeforeEach
  public void setupContent() {
    sessionRule.register(SearchTableDAO.class);

    conditionBuilder = createSearchConditionFactory();

    beginWork = new CountDownLatch(1);
    workDone = new CountDownLatch(2);

    generateNamespaces(5);
    generateNames(5);
    generateVersions(10);
    generatePaths(10);
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
      SearchRecordData tableData;
      if (isOnH2()) {
        tableData = new SearchRecordData(false);
      }
      else {
        tableData = new SearchRecordData(true);
      }
      // PK
      tableData.setRepositoryId(repository.contentRepositoryId());
      tableData.setComponentId(internalComponentId(component));
      tableData.setFormat(FORMAT);
      // tableData component
      tableData.setNamespace(component.namespace() + "_" + i);
      tableData.addNamespaceNames(component.namespace() + "_" + i);
      tableData.setComponentName(component.name() + "_" + i);
      tableData.addAliasComponentName(component.name() + "_" + i);
      tableData.setComponentKind(component.kind() + "_" + i);
      tableData.setVersion(component.version() + "_" + i);
      tableData.addVersionNames(component.version() + "_" + i);
      tableData.setNormalisedVersion(VersionNumberExpander.expand(component.version()));
      tableData.setRepositoryName(configuration.getRepositoryName() + "_" + i);
      tableData.setLastModified(OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
      // tableData blob
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

      tableData.setTags(List.of(TAG_NAME));

      // uploader info
      tableData.addUploader("uploader-name");
      tableData.addUploaderIp("uploader-ip-address");

      componentDAO.readComponent(internalComponentId(component))
          .map(Component::entityVersion)
          .ifPresent(tableData::setEntityVersion);

      GENERATED_DATA.add(tableData);
    }

    searchDAO = session.access(SearchTableDAO.class);
  }

  @AfterEach
  public void destroyContent() {
    GENERATED_DATA.clear();
    if (session != null) {
      session.close();
    }
  }

  @Test
  public void shouldAcceptUpdatesWithCurrentEntityVersion() {
    assertEntityVersionsAreSameAsSearchTableDataToSave();

    GENERATED_DATA.forEach(searchDAO::save);

    SqlSearchRequest request = SqlSearchRequest.builder()
        .limit(10)
        .build();

    long count = searchDAO.count(request);

    assertThat(count, is(2L));
  }

  @Test
  public void ignoreUpdatesWithLesserEntityVersion() {
    assertEntityVersionsAreSameAsSearchTableDataToSave();

    GENERATED_DATA.forEach(data -> {
      data.setEntityVersion(data.getEntityVersion() - 1);
      searchDAO.save(data);
    });

    SqlSearchRequest request = SqlSearchRequest.builder()
        .limit(10)
        .build();

    long count = searchDAO.count(request);

    assertThat(count, is(0L));
  }

  @Test
  public void concurrentlyAcceptUpdatesWithCurrentEntityVersion() throws Exception {
    assertThat(GENERATED_DATA.get(0).getEntityVersion(), is(GENERATED_DATA.get(1).getEntityVersion()));

    GENERATED_DATA.get(0).addKeyword("path-1");
    GENERATED_DATA.get(1).addKeyword("path-2");

    runConcurrentUpdate();

    SqlSearchQueryCondition condition =
        conditionBuilder.build(
            new ExpressionGroup(
                SqlClause.create(Operand.OR, new SqlPredicate(EQ, KEYWORDS, new LenientTerm("path-1")),
                    new SqlPredicate(EQ, KEYWORDS, new LenientTerm("path-2"))),
                null))
            .getComponentCondition();

    SqlSearchRequest request = prepareSearchRequest(10, 0, condition);

    long count = searchDAO.count(request);

    assertThat(count, is(2L));
  }

  @Test
  public void concurrentlyIgnoreUpdatesWithLesserEntityVersion() throws Exception {
    GENERATED_DATA.get(0).setEntityVersion(GENERATED_DATA.get(0).getEntityVersion() - 1);
    GENERATED_DATA.get(0).addKeyword("path-1");
    GENERATED_DATA.get(1).addKeyword("path-2");

    runConcurrentUpdate();

    SqlSearchQueryCondition condition = conditionBuilder.build(
        new ExpressionGroup(
            new SqlPredicate(EQ, KEYWORDS, new LenientTerm("path-1")), null))
        .getComponentCondition();

    SqlSearchRequest request = prepareSearchRequest(10, 0, condition);

    assertThat(searchDAO.count(request), is(0L));

    condition = conditionBuilder.build(new ExpressionGroup(
        new SqlPredicate(EQ, KEYWORDS, new LenientTerm("path-2")), null)).getComponentCondition();

    request = prepareSearchRequest(10, 0, condition);

    assertThat(searchDAO.count(request), is(1L));
  }

  @Test
  public void testSearchComponents() {
    GENERATED_DATA.forEach(searchDAO::save);
    SqlSearchRequest request = SqlSearchRequest.builder()
        .limit(10)
        .sortDirection(SortDirection.ASC.name())
        .build();

    long count = searchDAO.count(request);
    assertThat(count, is(2L));

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

    SqlSearchQueryCondition queryCondition = conditionBuilder.build(
        new ExpressionGroup(new SqlPredicate(EQ, NAME, new WildcardTerm("component*")), null)).getComponentCondition();
    Map<String, Object> values = queryCondition.getParameters();

    String conditionFormat = queryCondition.getSqlFilter();

    SqlSearchRequest firstRequest = prepareSearchRequest(10, 0, queryCondition);

    long count = searchDAO.count(firstRequest);
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

    SqlSearchRequest countRequest = SqlSearchRequest.builder()
        .limit(10)
        .offset(0)
        .build();
    Long count = searchDAO.count(countRequest);

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

    SearchRecordData tableData = GENERATED_DATA.get(0);
    final SearchRecordData searchTableData = new SearchRecordData();
    searchTableData.setEntityVersion(tableData.getEntityVersion());
    searchTableData.setRepositoryId(tableData.getRepositoryId());
    searchTableData.setComponentId(tableData.getComponentId());
    searchTableData.setFormat(tableData.getFormat());
    searchTableData.setNamespace(tableData.getNamespace());
    searchTableData.addNamespaceNames(tableData.getNamespace());
    searchTableData.setRepositoryName(tableData.getRepositoryName());
    searchTableData.setComponentName(tableData.getComponentName());
    searchTableData.addAliasComponentName(tableData.getComponentName());
    searchTableData.setVersion(tableData.getVersion());
    searchTableData.addVersionNames(tableData.getVersion());
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
        conditionBuilder.build(new ExpressionGroup(
            new SqlPredicate(EQ, FORMAT_FIELD_1, new LenientTerm("customField1")), null))
            .getComponentCondition();

    SqlSearchRequest request = prepareSearchRequest(10, 0, queryCondition);

    long count = searchDAO.count(request);
    assertThat(count, is(1L));
  }

  @Test
  public void testSaveWithManyPaths() {
    SearchRecordData tableData = GENERATED_DATA.get(0);
    for (int i = 0; i < 120; i++) {
      tableData.addPath(String.format("/org/sonatype/test/file_%s.jar", i));
    }
    searchDAO.save(tableData);

    Expression clause = SqlClause.create(Operand.AND, tableData.getPaths()
        .stream()
        .map(path -> new SqlPredicate(EQ, PATHS, new ExactTerm(path)))
        .toArray(SqlPredicate[]::new));

    SqlSearchQueryConditionGroup queryCondition =
        conditionBuilder.build(new ExpressionGroup(clause, null));
    String conditionFormat = queryCondition.getComponentCondition().getSqlFilter();
    Map<String, Object> values = queryCondition.getComponentCondition().getParameters();

    Collection<SearchResult> results = searchDAO.searchComponents(
        SqlSearchRequest.builder()
            .limit(10)
            .searchFilter(conditionFormat)
            .searchFilterValues(values)
            .build());

    assertThat(results, hasSize(1));
  }

  @Test
  public void testDelete() {
    GENERATED_DATA.forEach(searchDAO::save);
    SearchRecordData tableData = GENERATED_DATA.get(0);
    searchDAO.delete(tableData.getRepositoryId(), tableData.getComponentId(), FORMAT);

    SqlSearchRequest countRequest = SqlSearchRequest.builder()
        .limit(10)
        .offset(0)
        .build();

    long count = searchDAO.count(countRequest);
    assertThat(count, is(1L));
  }

  @Test
  public void testDeleteAllForRepository() {
    GENERATED_DATA.forEach(searchDAO::save);
    searchDAO.deleteAllForRepository(repository.contentRepositoryId(), FORMAT, 1);

    SqlSearchRequest countRequest = SqlSearchRequest.builder()
        .limit(1)
        .offset(0)
        .build();

    long count = searchDAO.count(countRequest);
    assertThat(count, is(1L));
  }

  @Test
  public void testDeleteAllForRepositoryWithoutLimit() {
    GENERATED_DATA.forEach(searchDAO::save);
    searchDAO.deleteAllForRepository(repository.contentRepositoryId(), FORMAT, 0);

    SqlSearchRequest countRequest = SqlSearchRequest.builder()
        .limit(10)
        .offset(0)
        .build();

    Long count = searchDAO.count(countRequest);
    assertThat(count, is(0L));
  }

  @Test
  public void testSaveBatch() {
    searchDAO.saveBatch(GENERATED_DATA);

    SqlSearchRequest countRequest = SqlSearchRequest.builder()
        .limit(10)
        .offset(0)
        .build();

    long count = searchDAO.count(countRequest);
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

  @Test
  public void testTagsTsVectorLowercase() {
    searchDAO.saveBatch(GENERATED_DATA);

    // SqlSearchQueryCondition will actually do this for us, but we do it here also to be future proof
    String tagNameLower = TAG_NAME.toLowerCase();

    SqlSearchQueryCondition queryCondition =
        conditionBuilder
            .build(new ExpressionGroup(new SqlPredicate(EQ, TAGS, new ExactTerm(tagNameLower)), null))
            .getComponentCondition();

    Collection<?> results = searchDAO.searchComponents(SqlSearchRequest.builder()
        .searchFilter(queryCondition.getSqlFilter())
        .searchFilterValues(queryCondition.getParameters())
        .build());

    assertThat(results, hasSize(GENERATED_DATA.size()));
  }

  @Test
  public void testBatchSavedFormatForPaths() {
    Iterator<SearchRecordData> iterator = GENERATED_DATA.iterator();
    int count = 0;
    while (iterator.hasNext()) {
      SearchRecordData searchRecordData = iterator.next();
      searchRecordData.addPath(String.format("/org/sonatype/test/file_%s.jar", count));
      count++;
    }
    searchDAO.saveBatch(GENERATED_DATA);
    session.getTransaction().commit();
    List<String> savedPaths = getSavedPaths();
    assertThat(savedPaths, hasSize(GENERATED_DATA.size()));

    // Verify the format of saved paths based on database type
    for (int i = 0; i < savedPaths.size(); i++) {
      String expectedPath = String.format("/org/sonatype/test/file_%s.jar", i);
      if (isOnH2()) {
        // H2: paths are stored as array of strings (JSON)
        assertThat(savedPaths.get(i), is("[\"" + expectedPath + "\"]"));
      }
      else {
        // PostgreSQL: paths are stored wrapped in curly braces (VARCHAR)
        assertThat(savedPaths.get(i), is("{'" + expectedPath + "'}"));
      }
    }
  }

  @Test
  public void testSavedFormatForPaths() {
    SearchRecordData searchRecordData = GENERATED_DATA.getFirst();
    searchRecordData.addPath("/org/sonatype/test/file_1.jar");
    searchDAO.save(searchRecordData);
    session.getTransaction().commit();
    List<String> savedPaths = getSavedPaths();
    assertThat(savedPaths, hasSize(1));

    // Verify the format of saved paths based on database type
    if (isOnH2()) {
      // H2: paths are stored as array of strings (JSON)
      assertThat(savedPaths, contains("[\"/org/sonatype/test/file_1.jar\"]"));
    }
    else {
      // PostgreSQL: paths are stored wrapped in curly braces (VARCHAR)
      assertThat(savedPaths, contains("{'/org/sonatype/test/file_1.jar'}"));
    }
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

  private void queueUpdate(final SearchRecordData searchTableData) {
    try {
      beginWork.await();
      searchDAO.save(searchTableData);
      workDone.countDown();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private SearchConditionFactory createSearchConditionFactory() {
    return new H2SearchConditionFactory(new H2SearchDB());
  }

  private static SqlSearchRequest prepareSearchRequest(
      final int limit,
      final int offset,
      final SqlSearchQueryCondition condition)
  {
    SqlSearchQueryCondition componentFilterQuery = null;

    if (Objects.nonNull(condition)) {
      componentFilterQuery = condition;
    }
    String filterFormat = null;
    Map<String, Object> formatValues = null;
    if (Objects.nonNull(componentFilterQuery)) {
      filterFormat = componentFilterQuery.getSqlFilter();
      formatValues = componentFilterQuery.getParameters();
    }

    // Regular search request
    return SqlSearchRequest
        .builder()
        .limit(limit)
        .offset(offset)
        .searchFilter(filterFormat)
        .searchFilterValues(formatValues)
        .build();
  }

  static boolean isPostgres() {
    return Boolean.valueOf(System.getProperty("test.postgres", "false"));
  }

  private boolean isOnH2() {
    return conditionBuilder instanceof H2SearchConditionFactory;
  }

  private List<String> getSavedPaths() {
    List<String> paths = new ArrayList<>();
    try (Connection conn = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT paths FROM search_components")) {
      while (resultSet.next()) {
        String pathValue = resultSet.getString("paths");
        if (pathValue != null) {
          paths.add(pathValue);
        }
      }
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to query paths from search_components", e);
    }
    return paths;
  }
}
