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
package org.sonatype.nexus.repository.search.index;

import java.io.File;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.junit.TestDataRule;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.elasticsearch.internal.ClientProvider;
import org.sonatype.nexus.elasticsearch.internal.NodeProvider;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.query.ElasticSearchQueryServiceImpl;
import org.sonatype.nexus.repository.search.query.SearchSubjectHelper;
import org.sonatype.nexus.security.SecurityHelper;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Range.closed;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.await;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.unrestricted;

public class ElasticSearchIndexServiceImplIT
    extends TestSupport
{
  static final String BASEDIR = new File(System.getProperty("basedir", "")).getAbsolutePath();

  private static final int CALM_TIMEOUT = 3000;

  private static final int TEST_REPOSITORY_COUNT = 10;

  private static final int TEST_COMPONENT_COUNT = 3000;

  @Rule
  public TestDataRule testData = new TestDataRule(Paths.get(BASEDIR, "src/test/it-resources").toFile());

  @Mock
  ApplicationDirectories directories;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  Configuration repositoryConfig;

  @Mock
  SearchIndexFacet searchFacet;

  @Mock
  Format testFormat;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  SecurityHelper securityHelper;

  @Mock
  SearchSubjectHelper searchSubjectHelper;

  @Mock
  EventManager eventManager;

  ElasticSearchIndexServiceImpl searchIndexService;

  ElasticSearchQueryServiceImpl searchQueryService;

  List<Repository> repositories = new ArrayList<>();

  List<Map<String, String>> components = new ArrayList<>();

  Multimap<Repository, Map<String, String>> componentsByRepository =
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

  BoolQueryBuilder exampleQuery = boolQuery().must(queryStringQuery("example"));

  @Before
  public void setup() {
    when(directories.getConfigDirectory("fabric")).thenReturn(testData.resolveFile("fabric"));
    when(nodeAccess.getId()).thenReturn("test-node");

    System.setProperty("testdir", new File(BASEDIR, "target/test-node").getPath());

    NodeProvider nodeProvider = new NodeProvider(directories, nodeAccess, null, null);
    ClientProvider clientProvider = new ClientProvider(nodeProvider);

    IndexNamingPolicy indexNamingPolicy = new HashedNamingPolicy();

    searchIndexService = new ElasticSearchIndexServiceImpl(clientProvider,
        indexNamingPolicy, List.of(), eventManager, 1000, 1, 0, CALM_TIMEOUT, 1);

    searchQueryService = new ElasticSearchQueryServiceImpl(clientProvider,
        repositoryManager, securityHelper, searchSubjectHelper, indexNamingPolicy, false);

    when(repositoryConfig.isOnline()).thenReturn(true);
    when(testFormat.getValue()).thenReturn("test-format");

    for (int i = 0; i < TEST_REPOSITORY_COUNT; i++) {
      Repository repository = mock(Repository.class);
      String repoName = "test-" + i;
      when(repository.getName()).thenReturn(repoName);
      when(repository.getConfiguration()).thenReturn(repositoryConfig);
      when(repository.optionalFacet(SearchIndexFacet.class)).thenReturn(Optional.of(searchFacet));
      when(repository.getFormat()).thenReturn(testFormat);
      searchIndexService.createIndex(repository);
      repositories.add(repository);
    }

    when(repositoryManager.browse()).thenReturn(repositories);
    when(securityHelper.allPermitted(any())).thenReturn(true);

    for (int i = 0; i < TEST_COMPONENT_COUNT; i++) {
      Map<String, String> component = new HashMap<>();
      component.put("format", "test-format");
      component.put("group", "example");
      component.put("name", String.valueOf(i));
      component.put("version", "1.0");
      components.add(component);
    }
  }

  @After
  public void teardown() {
    repositories.forEach(searchIndexService::deleteIndex);
  }

  @Test
  public void testBulkDelete() {
    seedComponentIndex();

    repositories.forEach(repo -> searchIndexService.bulkDelete(repo,
        componentsByRepository.get(repo).stream().map(c -> c.get("name")).toList()));

    await().atMost(1, MINUTES)
        .untilAsserted(() -> assertThat(Iterables.size(searchQueryService.browse(unrestricted(exampleQuery))), is(0)));
  }

  @Test
  public void testBulkDeleteByIdentifierOnly() {
    seedComponentIndex();

    searchIndexService.bulkDelete(null,
        ContiguousSet.create(closed(0, TEST_COMPONENT_COUNT), DiscreteDomain.integers())
            .asList()
            .stream()
            .map(String::valueOf)
            .toList());

    await().atMost(1, MINUTES)
        .untilAsserted(() -> assertThat(Iterables.size(searchQueryService.browse(unrestricted(exampleQuery))), is(0)));
  }

  @Test
  public void searchResultsArePaged() {
    seedComponentIndex();

    BoolQueryBuilder query = boolQuery().must(matchAllQuery());

    List<String> repos = repositories.stream().map(Repository::getName).collect(Collectors.toList());
    SearchResponse searchResponse = searchQueryService.search(unrestricted(query).inRepositories(repos), 0, 2);

    assertThat(searchResponse.getHits().hits().length, is(2));

    SearchResponse secondPage = searchQueryService.search(unrestricted(query).inRepositories(repos), 2, 4);

    assertThat(secondPage.getHits().hits().length, is(4));
    assertThat(searchResponse.getHits(), not(hasItems(secondPage.getHits().hits()[0], secondPage.getHits().hits()[1])));
  }

  private void seedComponentIndex() {
    Random random = new SecureRandom();

    components.forEach(
        component -> componentsByRepository.put(repositories.get(random.nextInt(TEST_REPOSITORY_COUNT)), component));

    componentsByRepository.keySet()
        .forEach(repository -> searchIndexService.bulkPut(
            repository,
            componentsByRepository.get(repository),
            component -> component.get("name"),
            component -> String.format("{ \"format\":\"%s\", \"group\":\"%s\", \"name\":\"%s\", \"version\":\"%s\" }",
                component.get("format"), component.get("group"), component.get("name"), component.get("version"))));

    await().atMost(1, MINUTES)
        .untilAsserted(
            () -> assertThat(Iterables.size(searchQueryService.browse(unrestricted(exampleQuery))),
                is(TEST_COMPONENT_COUNT)));
  }
}
