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
package org.sonatype.nexus.repository.search.sql.query.h2;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.datastore.mybatis.handlers.ExternalMetadataTypeHandler;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.security.AssetPermissionChecker;
import org.sonatype.nexus.repository.content.store.BlobRefTypeHandler;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.index.SearchRecordProducer;
import org.sonatype.nexus.repository.search.sql.query.DatabaseTypeDetector;
import org.sonatype.nexus.repository.search.sql.query.DefaultSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.query.ExpressionBuilder;
import org.sonatype.nexus.repository.search.sql.query.KeywordSqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.sql.query.SearchRequestModifier;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchService;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchSortUtil;
import org.sonatype.nexus.repository.search.sql.query.security.SqlSearchPermissionBuilder;
import org.sonatype.nexus.repository.search.sql.store.SearchRecordData;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;
import org.sonatype.nexus.repository.search.sql.store.SearchTableDAO;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.CaptureLogsFor;
import org.sonatype.nexus.testcommon.extensions.LoggingExtension.TestLogAccessor;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * This class contains tests of common search patterns
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith({DatabaseExtension.class, LoggingExtension.class})
class H2SearchTest
{
  private static final String MAVEN_SLF4J_PROVIDER = "maven-slf4j-provider";

  private static final String ORG_APACHE_MAVEN = "org.apache.maven";

  @CaptureLogsFor(value = SearchTableDAO.class, level = Level.TRACE)
  TestLogAccessor accessor;

  @DataSessionConfiguration(
      daos = {SearchTableDAO.class, TestContentRepositoryDAO.class, TestComponentDAO.class, TestAssetBlobDAO.class,
          TestAssetDAO.class, ConfigurationDAO.class},
      typeHandlers = {BlobRefTypeHandler.class, ExternalMetadataTypeHandler.class}, h2 = true, postgresql = false)
  protected TestDataSessionSupplier sessionRule;

  @Mock
  SqlSearchPermissionBuilder permissionBuilder;

  @Mock
  AssetPermissionChecker permissionChecker;

  private int repositoryId;

  @BeforeEach
  void setup() {
    ContentRepositoryData repository = new ContentRepositoryData();
    repository.setConfigRepositoryId(new DetachedEntityId(UUID.randomUUID().toString()));
    sessionRule.callDAO(TestContentRepositoryDAO.class, dao -> dao.createContentRepository(repository));
    repositoryId = InternalIds.contentRepositoryId(repository);

    when(permissionBuilder.build(any())).thenReturn(Optional.empty());
    lenient().when(permissionChecker.findPermittedAssets(any(), any(), any())).thenAnswer(i -> i.getArgument(0));
  }

  @DatabaseTest(h2 = true)
  void testExact() {
    createComponent(c -> {
      c.setNamespace(ORG_APACHE_MAVEN);
      c.setName(MAVEN_SLF4J_PROVIDER);
      c.setVersion("4.0.0");
    });

    // Exact
    assertThat(underTest().search(nameRequest(MAVEN_SLF4J_PROVIDER)).getSearchResults(), hasSize(1));
    assertThat(underTest().search(namespaceRequest(ORG_APACHE_MAVEN)).getSearchResults(), hasSize(1));

    // maven colon-separator
    assertThat(underTest().search(keywordRequest("org.apache.maven:maven-slf4j-provider")).getSearchResults(),
        hasSize(1));
    assertThat(underTest().search(keywordRequest("org.apache.maven:maven-slf4j-provider:4.0.0")).getSearchResults(),
        hasSize(1));
  }

  @DatabaseTest(h2 = true)
  void testTokenization() {
    createComponent(c -> {
      c.setNamespace(ORG_APACHE_MAVEN);
      c.setName(MAVEN_SLF4J_PROVIDER);
    });

    // spaces
    assertThat(underTest().search(keywordRequest("maven apache")).getSearchResults(), hasSize(1));
    assertThat(underTest().search(keywordRequest("maven provider")).getSearchResults(), hasSize(1));
    assertThat(underTest().search(keywordRequest(ORG_APACHE_MAVEN)).getSearchResults(), hasSize(1));
    assertThat(underTest().search(keywordRequest(MAVEN_SLF4J_PROVIDER)).getSearchResults(), hasSize(1));
    assertThat(underTest().search(keywordRequest("maven*provider apache")).getSearchResults(), hasSize(1));
  }

  @DatabaseTest(h2 = true)
  void testStartsWith() {
    createComponent(c -> {
      c.setNamespace(ORG_APACHE_MAVEN);
      c.setName(MAVEN_SLF4J_PROVIDER);
    });

    assertThat(underTest().search(nameRequest("maven*provider")).getSearchResults(), hasSize(1));
    assertThat(underTest().search(nameRequest("maven*slf4j*provider")).getSearchResults(), hasSize(1));
    assertThat(underTest().search(nameRequest("maven-slf4*")).getSearchResults(), hasSize(1));
    assertThat(underTest().search(namespaceRequest("org.apache*")).getSearchResults(), hasSize(1));
    assertThat(underTest().search(nameRequest("mav*-slf4j-provider")).getSearchResults(), hasSize(1));

    assertThat(underTest().search(nameRequest("slf4j-provider")).getSearchResults(), empty());
    assertThat(underTest().search(nameRequest("slf4j-provider*")).getSearchResults(), empty());
    assertThat(underTest().search(nameRequest("slf4j*")).getSearchResults(), empty());
  }

  private SqlSearchService underTest() {
    return new SqlSearchService(new SearchStore(sessionRule, 100, 100),
        new SqlSearchSortUtil(searchDatabase(), searchMappings()), List.of(), expressionBuilder(),
        new SearchRequestModifier(searchQueryContributions()), conditionFactory(), Set.of(), mock(), permissionChecker);
  }

  private void createComponent(final Consumer<ComponentData> mutator) {
    ComponentData component = new ComponentData();
    component.setName(UUID.randomUUID().toString());
    component.setNamespace("");
    component.setVersion("1");
    component.setRepositoryId(repositoryId);
    component.setKind("");

    mutator.accept(component);

    sessionRule.callDAO(TestComponentDAO.class, dao -> dao.createComponent(component, true));

    // Fake asset
    FluentAsset asset = mock();
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/foo");

    FluentComponent result = sessionRule
        .withDAO(TestComponentDAO.class, dao -> dao.readComponent(InternalIds.internalComponentId(component)))
        .map(c -> new FluentComponentImpl(mock(), c, List.of(asset)))
        .orElseThrow();

    Repository repository = mock();
    when(repository.getName()).thenReturn("fake");
    Format format = mock();
    when(format.getValue()).thenReturn("test");
    when(repository.getFormat()).thenReturn(format);
    ContentFacet facet = mock();
    when(repository.facet(ContentFacet.class)).thenReturn(facet);
    when(facet.contentRepositoryId()).thenReturn(repositoryId);

    SearchRecordData record = recordProducer().createSearchRecord(result, repository).orElseThrow();
    store().save(record);
  }

  private static SearchRecordProducer recordProducer() {
    DatabaseTypeDetector typeDetector = mock();
    lenient().when(typeDetector.isH2()).thenReturn(true);
    when(typeDetector.isPostgreSQL()).thenReturn(false);
    return new SearchRecordProducer(List.of(), new VersionNormalizerService(List.of()), List.of(), List.of(), Set.of(),
        typeDetector);
  }

  private SearchStore store() {
    return new SearchStore(sessionRule, 100, 100);
  }

  private ExpressionBuilder expressionBuilder() {
    return new ExpressionBuilder(permissionBuilder,
        searchQueryContributions(),
        searchMappings());
  }

  private static SearchRequest keywordRequest(final String keyword) {
    return SearchRequest.builder()
        .searchFilter("keyword", keyword)
        .build();
  }

  private static SearchRequest nameRequest(final String name) {
    return SearchRequest.builder()
        .searchFilter("name", name)
        .build();
  }

  private static SearchRequest namespaceRequest(final String namespace) {
    return SearchRequest.builder()
        .searchFilter("namespace", namespace)
        .build();
  }

  private static List<SqlSearchQueryContribution> searchQueryContributions() {
    SearchMappingService mapping = new SearchMappingService(searchMappings());
    DefaultSqlSearchQueryContribution def = new DefaultSqlSearchQueryContribution();
    def.init(mapping);

    KeywordSqlSearchQueryContribution keyword = new KeywordSqlSearchQueryContribution();
    keyword.init(mapping);

    return List.of(def, keyword);
  }

  private static List<SearchMappings> searchMappings() {
    return List.of(new DefaultSearchMappings());
  }

  private static H2SearchConditionFactory conditionFactory() {
    return new H2SearchConditionFactory(searchDatabase());
  }

  private static H2SearchDB searchDatabase() {
    return new H2SearchDB();
  }
}
