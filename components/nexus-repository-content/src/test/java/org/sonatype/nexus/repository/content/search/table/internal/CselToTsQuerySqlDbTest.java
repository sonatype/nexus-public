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
package org.sonatype.nexus.repository.content.search.table.internal;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.content.testsuite.groups.PostgresTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;
import org.sonatype.nexus.repository.content.search.table.SearchTableDAO;
import org.sonatype.nexus.repository.content.search.table.SearchTableData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.normalize.VersionNumberExpander;
import org.sonatype.nexus.repository.search.table.SelectorTsQuerySqlBuilder;
import org.sonatype.nexus.selector.JexlEngine;

import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;

/**
 * Demonstrates regex matching of paths stored in the component_search.paths
 * column as {/path1/baz} {/path2/bar} {/path3/foo}
 *
 * @see SearchTableDAO
 */
@Category(PostgresTestGroup.class)
public class CselToTsQuerySqlDbTest
    extends ExampleContentTestSupport
{
  private static final String FORMAT = "test";

  private static final int TABLE_RECORDS_TO_GENERATE = 5;

  private static final List<SearchTableData> GENERATED_DATA = new ArrayList<>(TABLE_RECORDS_TO_GENERATE);

  private DataSession<?> session;

  private SearchTableDAO searchDAO;

  private final JexlEngine jexlEngine = new JexlEngine();

  private Map<Integer, SearchTableData> componentIdToSearchTableData;

  private Map<Integer, List<String>> componentIdToPaths;

  private SelectorTsQuerySqlBuilder builder;

  private CselToTsQuerySql underTest;

  public CselToTsQuerySqlDbTest() {
    super(SearchTableDAO.class);
  }

  @Before
  public void setup() {
    builder = new SelectorTsQuerySqlBuilder();
    builder.propertyAlias("a", "a_alias");
    builder.propertyAlias("b", "b_alias");
    builder.propertyAlias("path", "paths");
    builder.parameterPrefix("#{filterParams.");
    builder.parameterSuffix("}");
    underTest = new CselToTsQuerySql();
  }

  @After
  public void destroyContent() {
    if (session != null) {
      GENERATED_DATA.clear();
      session.close();
    }
  }

  @Test
  public void shouldImplicitlyMatchFullPathWithinString() {
    setupContent();

    //Test matching first token in a string. ^ and $ are implied
    parseExpression("path=~\"/org/assertj/assertj-core/3.14.0/assertj-core-3.14.0.jar\"");

    assertResults(searchDAO.searchComponents(searchRequest()),
        "/org/assertj/assertj-core/3.14.0/assertj-core-3.14.0.jar");

    //Test matching second token in a string. ^ and $ are implied
    resetBuilder();
    parseExpression("path=~\"/some/other/path/for/ruby/gem.rz\"");

    assertResults(searchDAO.searchComponents(searchRequest()),
        "/some/other/path/for/ruby/gem.rz");

    //Test matching last token in a string. ^ and $ are implied
    resetBuilder();
    parseExpression("path=~\"/quick/Marshal.4.8/empty_cucumber-0.0.4.gemspec.rz\"");

    assertResults(searchDAO.searchComponents(searchRequest()),
        "/quick/Marshal.4.8/empty_cucumber-0.0.4.gemspec.rz");

    resetBuilder();
    //always match the full path
    parseExpression("path=~\"yet-another-awesome-3.14.0.awe$\"");

    assertThat(searchDAO.searchComponents(searchRequest()).isEmpty(), is(true));
  }

  @Test
  public void shouldExplicitlyMatchFullPathWithinString() {
    setupContent();

    parseExpression("path=~\"^/org/apache/commons/1.10.0/commons-text-1.10.0.md5$\"");

    Collection<SearchResult> searchResults = searchDAO.searchComponents(searchRequest());

    assertResults(searchResults, "/org/apache/commons/1.10.0/commons-text-1.10.0.md5");
  }

  @Test
  public void shouldNotMatchWhenNotFullPath() {
    setupContent();
    parseExpression("path=~\"assertj-core-3.14.0.jar\"");

    assertThat(searchDAO.searchComponents(searchRequest()).isEmpty(), is(true));

    resetBuilder();
    parseExpression("path=~\"/org/assertj/\"");

    assertThat(searchDAO.searchComponents(searchRequest()).isEmpty(), is(true));
  }

  @Test
  public void shouldMatchWhenNotFullPathAndHasWildcard() {
    setupContent();
    parseExpression("path=~\".*/3.14.0/assertj-core-3.14.0.jar\"");

    assertResults(searchDAO.searchComponents(searchRequest()),
        "/org/assertj/assertj-core/3.14.0/assertj-core-3.14.0.jar");

    resetBuilder();
    parseExpression("path=~\"/org/apache/.*/commons-lang.*.md5\"");

    assertResults(searchDAO.searchComponents(searchRequest()),
        "/org/apache/commons/3.12.0/commons-lang3-3.12.0.md5");
  }

  @Test
  public void shouldMatchStartOfPath() {
    setupContent();
    //match the start of a path that is actually at the start of the string
    parseExpression("path=~\"^/gems/empty_cucumber\"");

    assertResults(searchDAO.searchComponents(searchRequest()),
        "/gems/empty_cucumber-0.0.4.gem");

    //match the start of a path that is in the middle of the string
    resetBuilder();
    parseExpression("path=~\"^/some/other/path\"");
    assertResults(searchDAO.searchComponents(searchRequest()),
        "/some/other/path/for/ruby/gem.rz");
  }

  @Test
  public void shouldMatchFullPathOrStartOfPath() {
    setupContent();
    parseExpression("path=~\"^/quick/Marshal.4.8|/org/apache/commons/1.10.0/commons-text-1.10.0.jar\"");

    Collection<SearchResult> searchResults = searchDAO.searchComponents(searchRequest());

    assertThat(searchResults.size(), is(3));

    searchResults.forEach(searchResult ->
        assertComponent(searchResult, componentIdToSearchTableData.get(searchResult.componentId())));
  }

  private void assertResults(
      final Collection<SearchResult> searchResults,
      final String expectedPath)
  {
    assertThat(searchResults.size(), is(1));
    searchResults.forEach(result -> assertResult(result, expectedPath));
  }

  private void assertResult(final SearchResult searchResult, final String expectedPath) {
    SearchTableData expected = componentIdToSearchTableData.get(searchResult.componentId());
    assertComponent(searchResult, expected);
    assertThat(expected.getPaths(), hasItem(expectedPath));
  }

  private void assertComponent(final SearchResult searchResult, final SearchTableData expected) {
    assertThat(searchResult.namespace(), is(expected.getNamespace()));
    assertThat(searchResult.componentName(), is(expected.getComponentName()));
    assertThat(searchResult.version(), is(expected.getVersion()));
    assertThat(searchResult.repositoryName(), is(expected.getRepositoryName()));
  }

  private void parseExpression(final String expression) {
    ASTJexlScript script = jexlEngine.parseExpression(expression);
    script.childrenAccept(underTest, builder);
  }

  private void saveData() {
    GENERATED_DATA.forEach(searchDAO::save);
    long count = searchDAO.count(null, null);
    assertThat(count, is((long) TABLE_RECORDS_TO_GENERATE));
  }

  private SqlSearchRequest searchRequest() {
    return SqlSearchRequest.builder()
        .limit(10)
        .sortDirection(SortDirection.ASC.name())
        .searchFilter(builder.getQueryString())
        .searchFilterValues(builder.getQueryParameters())
        .build();
  }

  private void setupContent() {
    generateRandomNamespaces(5);
    generateRandomNames(5);
    generateRandomVersions(10);
    Map<Integer, List<String>> paths = generatePaths();
    generateConfiguration();
    componentIdToSearchTableData = new HashMap<>();
    componentIdToPaths = new HashMap<>();

    ConfigurationData configuration = generatedConfigurations().get(0);
    generateSingleRepository(UUID.fromString(configuration.getRepositoryId().getValue()));
    ContentRepositoryData repository = generatedRepositories().get(0);

    generateContent(TABLE_RECORDS_TO_GENERATE, true);

    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    TestComponentDAO componentDAO = session.access(TestComponentDAO.class);

    for (int i = 0; i < TABLE_RECORDS_TO_GENERATE; i++) {
      Component component = generatedComponents().get(i);
      AssetBlob blob = generatedAssetBlobs().get(i);

      SearchTableData tableData = new SearchTableData();
      //PK
      tableData.setRepositoryId(repository.contentRepositoryId());
      tableData.setComponentId(InternalIds.internalComponentId(component));
      tableData.setFormat(FORMAT);
      //tableData component
      tableData.setNamespace(component.namespace() + "_" + i);
      tableData.setComponentName(component.name() + "_" + i);
      tableData.addAliasComponentName(component.name() + "_" + i);
      tableData.setComponentKind(component.kind() + "_" + i);
      tableData.setVersion(component.version() + "_" + i);
      tableData.setNormalisedVersion(VersionNumberExpander.expand(component.version()));
      tableData.setRepositoryName(configuration.getRepositoryName() + "_" + i);
      tableData.setLastModified(OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
      //tableData blob
      tableData.addKeyword(blob.contentType());
      paths.get(i).forEach(tableData::addPath);
      componentIdToSearchTableData.put(tableData.getComponentId(), tableData);
      componentIdToPaths.put(tableData.getComponentId(), paths.get(i));

      componentDAO.readComponent(internalComponentId(component))
          .map(Component::entityVersion).ifPresent(tableData::setEntityVersion);

      GENERATED_DATA.add(tableData);
    }
    searchDAO = session.access(SearchTableDAO.class);
    saveData();
  }

  private Map<Integer, List<String>> generatePaths() {
    Map<Integer, List<String>> paths = new HashMap<>();

    paths.put(0, asList("/quick/Marshal.4.8/rspec-junit-0.1.2.gemspec.rz",
        "/some/other/path/for/ruby/gem.rz",
        "/gems/rspec-junit-0.1.2.gem"));

    paths.put(1, asList("/gems/empty_cucumber-0.0.4.gem",
        "/quick/Marshal.4.8/empty_cucumber-0.0.4.gemspec.rz"));

    paths.put(2, asList("/org/apache/commons/3.12.0/commons-lang3-3.12.0.jar",
        "/org/apache/commons/3.12.0/commons-lang3-3.12.0.md5",
        "/org/apache/commons/3.12.0/commons-lang3-3.12.0.sha1"));

    paths.put(3, asList("/org/apache/commons/1.10.0/commons-text-1.10.0.jar",
        "/org/apache/commons/1.10.0/commons-text-1.10.0.md5",
        "/org/apache/commons/1.10.0/commons-text-1.10.0.sha1"));

    paths.put(4, asList("/org/assertj/assertj-core/3.14.0/assertj-core-3.14.0.jar",
        "/org/assertj/assertj-core/3.14.0/some-other-file-3.14.0.pom",
        "/org/assertj/assertj-core/3.14.0/yet-another-awesome-3.14.0.awe"));
    return paths;
  }

  private void resetBuilder() {
    builder.clearQueryString();
    builder.getQueryParameters().clear();
  }
}
