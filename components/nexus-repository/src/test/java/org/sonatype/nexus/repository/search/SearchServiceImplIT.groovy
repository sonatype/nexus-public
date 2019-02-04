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
package org.sonatype.nexus.repository.search

import java.nio.file.Paths
import java.security.SecureRandom

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.goodies.testsupport.junit.TestDataRule
import org.sonatype.nexus.common.app.ApplicationDirectories
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.elasticsearch.internal.ClientProvider
import org.sonatype.nexus.elasticsearch.internal.NodeProvider
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.DefaultComponent
import org.sonatype.nexus.security.SecurityHelper

import com.google.common.collect.ContiguousSet
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.common.collect.Range
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock

import static com.google.common.collect.DiscreteDomain.integers
import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.MINUTES
import static org.elasticsearch.index.query.QueryBuilders.boolQuery
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
import static org.mockito.Mockito.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class SearchServiceImplIT
    extends TestSupport
{
  static final String BASEDIR = new File(System.getProperty('basedir', '')).absolutePath

  private static final int CALM_TIMEOUT = 3000

  @Rule
  public TestDataRule testData = new TestDataRule(Paths.get(BASEDIR, 'src/test/it-resources').toFile())

  static final int TEST_REPOSITORY_COUNT = 10

  static final int TEST_COMPONENT_COUNT = 3000

  @Mock
  ApplicationDirectories directories

  @Mock
  NodeAccess nodeAccess

  @Mock
  RepositoryManager repositoryManager

  @Mock
  Configuration repositoryConfig

  @Mock
  SearchFacet searchFacet

  @Mock
  Format testFormat

  @Mock
  SecurityHelper securityHelper

  @Mock
  SearchSubjectHelper searchSubjectHelper

  @Mock
  EventManager eventManager

  SearchServiceImpl searchService

  def repositories = []

  def components = []

  Multimap<Repository, Component> componentsByRepository = Multimaps.newListMultimap([:], {[]})

  BoolQueryBuilder exampleQuery = boolQuery().must(queryStringQuery('example'))

  @Before
  public void setup() {
    when(directories.getConfigDirectory('fabric')).thenReturn(testData.resolveFile('fabric'))
    when(nodeAccess.getId()).thenReturn('test-node')

    System.setProperty('testdir', new File(BASEDIR, 'target/test-node').path)

    NodeProvider nodeProvider = new NodeProvider(directories, nodeAccess, null, null)
    ClientProvider clientProvider = new ClientProvider(nodeProvider)

    searchService = new SearchServiceImpl(clientProvider, repositoryManager, securityHelper, searchSubjectHelper,
        ImmutableList.of(), eventManager, false, 1000, 1, 0, CALM_TIMEOUT)

    when(repositoryConfig.isOnline()).thenReturn(true)
    when(testFormat.getValue()).thenReturn('test-format')

    for (int i = 0; i < TEST_REPOSITORY_COUNT; i++) {
      Repository repository = mock(Repository.class)
      when(repository.getName()).thenReturn("test-$i" as String)
      when(repository.getConfiguration()).thenReturn(repositoryConfig)
      when(repository.optionalFacet(SearchFacet.class)).thenReturn(Optional.of(searchFacet))
      when(repository.getFormat()).thenReturn(testFormat)
      searchService.createIndex(repository)
      repositories.add(repository)
    }

    when(repositoryManager.browse()).thenReturn(repositories as List)
    when(securityHelper.allPermitted(any())).thenReturn(true)

    for (int i = 0; i < TEST_COMPONENT_COUNT; i++) {
      Component component = new DefaultComponent()
      component.format('test-format')
      component.group('example')
      component.name("$i")
      component.version('1.0')
      components.add(component)
    }
  }

  @After
  public void teardown() {
    repositories.forEach(searchService.&deleteIndex)
  }

  @Test
  public void testBulkDelete() throws Exception {

    seedComponentIndex()

    // attempt to bulk-delete indexed documents under each repository
    repositories.forEach({ repo ->
        searchService.bulkDelete(repo, componentsByRepository.get(repo)*.name()) })

    // wait for all documents to be removed
    await().atMost(1, MINUTES).until({
        assertThat(Iterables.size(searchService.browseUnrestricted(exampleQuery)), is(0)) })
  }

  @Test
  public void testBulkDeleteByIdentifierOnly() throws Exception {

    seedComponentIndex()

    // attempt to bulk-delete indexed documents by identifier only, without knowing the owning repository
    searchService.bulkDelete(null, ContiguousSet.create(Range.closed(0, TEST_COMPONENT_COUNT), integers()).asList())

    // wait for all documents to be removed
    await().atMost(1, MINUTES).until({
        assertThat(Iterables.size(searchService.browseUnrestricted(exampleQuery)), is(0)) })
  }

  @Test
  public void searchResultsArePaged() throws Exception {
    seedComponentIndex()

    def query = boolQuery()
        .must(matchAllQuery())

    def repos = repositories.stream().map { it.name }.collect()
    def searchResponse = searchService.searchUnrestrictedInRepos(query, null, 0, 2, repos)

    assert searchResponse.hits.size() == 2

    def secondPage = searchService.searchUnrestrictedInRepos(query, null, 2, 4, repos)

    assert secondPage.hits.size() == 4
    assert !searchResponse.hits.contains(secondPage.hits[0])
    assert !searchResponse.hits.contains(secondPage.hits[1])
  }

  private seedComponentIndex() {
    Random random = new SecureRandom()

    // distribute components across repositories
    components.forEach({ component ->
        componentsByRepository.put(repositories[random.nextInt(TEST_REPOSITORY_COUNT)], component) })

    // populate each index using bulk operations
    componentsByRepository.keySet().forEach({ repository ->
      searchService.bulkPut(
          repository,
          componentsByRepository.get(repository),
          { component -> component.name() },
          { component ->
            """
              { "format":"${component.format()}",
                "group":"${component.group()}",
                "name":"${component.name()}",
                "version":"${component.version()}"
              }
              """ as String
          })
    })

    // wait for all documents to be indexed
    await().atMost(1, MINUTES).until({
        assertThat(Iterables.size(searchService.browseUnrestricted(exampleQuery)), is(TEST_COMPONENT_COUNT)) })
  }
}
