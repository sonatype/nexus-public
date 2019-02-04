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

import java.util.concurrent.atomic.AtomicBoolean

import javax.inject.Provider

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.DefaultComponent
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.scheduling.CancelableHelper
import org.sonatype.nexus.scheduling.TaskInterruptedException
import org.sonatype.nexus.security.SecurityHelper

import com.google.common.base.Function
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.elasticsearch.action.ListenableActionFuture
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.AdminClient
import org.elasticsearch.client.Client
import org.elasticsearch.client.IndicesAdminClient
import org.elasticsearch.common.settings.Settings
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.arrayWithSize
import static org.hamcrest.Matchers.contains
import static org.junit.Assert.fail
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1

class SearchServiceImplTest
    extends TestSupport
{
  private static final Function<Component, String> NEVER_CALLED_ID_PRODUCER = null

  private static final Function<Component, String> NEVER_CALLED_JSON_DOC_PRODUCER = null

  @Mock
  Provider<Client> clientProvider

  @Mock
  Client client

  @Mock
  AdminClient adminClient

  @Mock
  IndicesAdminClient indicesAdminClient

  @Mock
  IndicesExistsRequestBuilder indicesExistsRequestBuilder

  @Mock
  ListenableActionFuture<IndicesExistsResponse> actionFuture

  @Mock
  IndicesExistsResponse indicesExistsResponse

  @Mock
  RepositoryManager repositoryManager

  @Mock
  SecurityHelper securityHelper

  @Mock
  SearchSubjectHelper searchSubjectHelper

  @Mock
  List<IndexSettingsContributor> indexSettingsContributors

  @Mock
  EventManager eventManager

  @Mock
  BulkProcessor bulkProcessor

  AtomicBoolean cancelled = new AtomicBoolean(false)

  Settings settings = Settings.EMPTY

  SearchServiceImpl searchService

  @Before
  public void setup() {
    CancelableHelper.set(cancelled)
    when(clientProvider.get()).thenReturn(client)
    when(client.admin()).thenReturn(adminClient)
    when(adminClient.indices()).thenReturn(indicesAdminClient)
    when(client.settings()).thenReturn(settings)

    searchService = new SearchServiceImpl(clientProvider, repositoryManager, securityHelper, searchSubjectHelper,
        indexSettingsContributors, eventManager, false, 1000, 0, 0, 3000)
    searchService.bulkProcessor = bulkProcessor
  }

  @After
  void tearDown() {
    CancelableHelper.remove()
  }

  @Test
  public void testCreateIndexAlreadyExists() throws Exception {
    ArgumentCaptor<String> varArgs = captureRepoNameArg()

    searchService.createIndex(repository('test'))

    assertThat(varArgs.getAllValues(), contains(SHA1.function().hashUnencodedChars('test').toString()))
  }

  /**
   * Search indices identifiers are {@link Repository#getName()} passed thru SHA1 hasher to normalize them and
   * make them suit ES index name requirements (lower case, max len 255, etc).
   */
  @Test
  public void testCreateIndexRepositoryNameMapping() throws Exception {
    ArgumentCaptor<String> varArgs = captureRepoNameArg()

    searchService.createIndex(repository('UPPERCASE'))

    assertThat(varArgs.getAllValues(), contains(SHA1.function().hashUnencodedChars('UPPERCASE').toString()))
  }

  /**
   * Verify successful execution path for {@link SearchServiceImpl#bulkPut(Repository, Iterable, Function, Function)}.
   */
  @Test
  void testBulkPut() {
    int requestCount = 50
    BiMap<String, Component> components = HashBiMap.create()
    for (int i = 0; i < requestCount; i++) {
      components.put(UUID.randomUUID().toString(), new DefaultComponent())
    }

    BiMap<Component, String> inverse = components.inverse()
    String json = '{ "a": "b" }'

    Repository repository = repository(SearchServiceImpl.TYPE)
    ArgumentCaptor<String> indexName = captureRepoNameArg()
    searchService.createIndex(repository)

    components.entrySet().forEach({ entry ->
      IndexRequestBuilder builder = mock(IndexRequestBuilder.class)
      when(
          client.prepareIndex(indexName.capture(), eq(SearchServiceImpl.TYPE), eq(entry.getKey()))
      ).thenReturn(builder)
      when(builder.setSource(json)).thenReturn(builder)
      org.elasticsearch.action.index.IndexRequest request = mock(org.elasticsearch.action.index.IndexRequest.class)
      when(builder.request()).thenReturn(request)
    })

    searchService.bulkPut(repository,
        components.values(),
        { component -> inverse.get(component) },
        { component -> json }
    )

    // Note: can't do a 'verify(bulkProcessor, times(requestCount)).add(any())' here,
    // because BulkProcessor#add is overloaded, each variant taking a single argument of the same supertype
    // mockito is unable to resolve the invoked method (fails with 'wanted, but not invoked error')
    verify(bulkProcessor).flush()
  }

  @Test
  void testBulkPutCancellation() {
    def components = [new DefaultComponent()]
    Repository repository = repository(SearchServiceImpl.TYPE)
    captureRepoNameArg()
    searchService.createIndex(repository)

    cancelled.set(true)

    try {
      searchService.bulkPut(repository, components, NEVER_CALLED_ID_PRODUCER, NEVER_CALLED_JSON_DOC_PRODUCER)
      fail('Expected exception')
    }
    catch (TaskInterruptedException expected) {
    }

    verify(bulkProcessor, never()).flush()
  }

  @Test
  void testGetSearchableIndexes() {
    ArgumentCaptor<String> captureA = captureRepoNameArg()
    def repositoryA = repository('a')
    searchService.createIndex(repositoryA)
    def encodedA = captureA.allValues[0]

    ArgumentCaptor<String> captureB = captureRepoNameArg()
    def repositoryB = repository('b')
    searchService.createIndex(repositoryB)
    def encodedB = captureB.allValues[0]

    when(repositoryManager.browse()).thenReturn([repositoryA, repositoryB])
    def searchable = searchService.getSearchableIndexes(true, ['a'])
    assertThat(searchable as List, contains(encodedA))
    assertThat(searchable, arrayWithSize(1))

    when(repositoryManager.browse()).thenReturn([repositoryA, repositoryB])
    searchable = searchService.getSearchableIndexes(true, ['a', 'b'])
    assertThat(searchable as List, contains(encodedA, encodedB))
    assertThat(searchable, arrayWithSize(2))
  }

  protected Repository repository(String name) {
    Repository repository = new RepositoryImpl(eventManager, new HostedType(), new TestFormat('test'))
    repository.name = name
    def configuration = new Configuration()
    configuration.online = true
    repository.configuration = configuration
    SearchFacet searchFacet = mock(SearchFacet)
    repository.attach(searchFacet)
    return repository
  }

  private ArgumentCaptor<String> captureRepoNameArg() {
    ArgumentCaptor<String> varArgs = ArgumentCaptor.forClass(String.class)
    when(indicesAdminClient.prepareExists(varArgs.capture())).thenReturn(indicesExistsRequestBuilder)
    when(indicesExistsRequestBuilder.execute()).thenReturn(actionFuture)
    when(actionFuture.actionGet()).thenReturn(indicesExistsResponse)
    when(indicesExistsResponse.isExists()).thenReturn(true)
    varArgs
  }

  class TestFormat
      extends Format
  {

    TestFormat(final String value) {
      super(value)
    }
  }
}
