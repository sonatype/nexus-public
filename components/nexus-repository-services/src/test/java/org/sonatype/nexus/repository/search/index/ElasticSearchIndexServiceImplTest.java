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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.search.query.ElasticSearchQueryServiceImpl;
import org.sonatype.nexus.repository.search.query.SearchSubjectHelper;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.security.SecurityHelper;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testcontainers.shaded.com.google.common.collect.BiMap;
import org.testcontainers.shaded.com.google.common.collect.HashBiMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.search.index.SearchConstants.TYPE;

public class ElasticSearchIndexServiceImplTest
    extends TestSupport
{
  private static final Function<Map, String> NEVER_CALLED_ID_PRODUCER = null;

  private static final Function<Map, String> NEVER_CALLED_JSON_DOC_PRODUCER = null;

  @Mock
  Provider<Client> clientProvider;

  @Mock
  Client client;

  @Mock
  AdminClient adminClient;

  @Mock
  IndicesAdminClient indicesAdminClient;

  @Mock
  IndicesExistsRequestBuilder indicesExistsRequestBuilder;

  @Mock
  ListenableActionFuture<IndicesExistsResponse> actionFuture;

  @Mock
  IndicesExistsResponse indicesExistsResponse;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  SecurityHelper securityHelper;

  @Mock
  SearchSubjectHelper searchSubjectHelper;

  @Mock
  List<IndexSettingsContributor> indexSettingsContributors;

  @Mock
  EventManager eventManager;

  @Mock
  BulkProcessor bulkProcessor;

  ExecutorService executorService = Executors.newSingleThreadExecutor();

  AtomicBoolean cancelled = new AtomicBoolean(false);

  Settings settings = Settings.EMPTY;

  ElasticSearchIndexServiceImpl searchIndexService;

  ElasticSearchQueryServiceImpl searchQueryService;

  @Before
  public void setup() {
    CancelableHelper.set(cancelled);
    when(clientProvider.get()).thenReturn(client);
    when(client.admin()).thenReturn(adminClient);
    when(adminClient.indices()).thenReturn(indicesAdminClient);
    when(client.settings()).thenReturn(settings);

    IndexNamingPolicy indexNamingPolicy = new HashedNamingPolicy();

    searchIndexService = new ElasticSearchIndexServiceImpl(clientProvider,
        indexNamingPolicy, indexSettingsContributors, eventManager, 1000, 0, 0, 3000, 1);

    searchQueryService = new ElasticSearchQueryServiceImpl(clientProvider,
        repositoryManager, securityHelper, searchSubjectHelper, indexNamingPolicy, false);

    Map<Integer, Entry<BulkProcessor, ExecutorService>> bulkProcessorToExecutors = new HashMap<>();
    bulkProcessorToExecutors.put(0, new SimpleImmutableEntry<>(bulkProcessor, executorService));

    searchIndexService.setBulkProcessorToExecutors(bulkProcessorToExecutors);
  }

  @After
  public void tearDown() {
    CancelableHelper.remove();
    executorService.shutdown();
  }

  @Test
  public void testCreateIndexAlreadyExists() throws Exception {
    ArgumentCaptor<String> varArgs = captureRepoNameArg();

    searchIndexService.createIndex(repository("test"));

    assertThat(varArgs.getAllValues(), contains(SHA1.function().hashUnencodedChars("test").toString()));
  }

  @Test
  public void testCreateIndexRepositoryNameMapping() throws Exception {
    ArgumentCaptor<String> varArgs = captureRepoNameArg();

    searchIndexService.createIndex(repository("UPPERCASE"));

    assertThat(varArgs.getAllValues(), contains(SHA1.function().hashUnencodedChars("UPPERCASE").toString()));
  }

  @Test
  public void testBulkPut() throws Exception {
    int requestCount = 50;
    BiMap<String, Map> components = HashBiMap.create();
    for (int i = 0; i < requestCount; i++) {
      String id = UUID.randomUUID().toString();
      components.put(id, Collections.singletonMap("id", id));
    }

    BiMap<Map, String> inverse = components.inverse();
    String json = "{ \"a\": \"b\" }";

    Repository repository = repository("test-repo");
    ArgumentCaptor<String> indexName = captureRepoNameArg();
    searchIndexService.createIndex(repository);

    components.entrySet().forEach(entry -> {
      IndexRequestBuilder builder = mock(IndexRequestBuilder.class);
      when(client.prepareIndex(indexName.capture(), eq(TYPE), eq(entry.getKey()))).thenReturn(builder);
      when(builder.setSource(json)).thenReturn(builder);
      org.elasticsearch.action.index.IndexRequest request = mock(org.elasticsearch.action.index.IndexRequest.class);
      when(builder.request()).thenReturn(request);
    });

    List<Future<Void>> futures = searchIndexService.bulkPut(repository,
        components.values(),
        component -> inverse.get(component),
        component -> json);

    futures.forEach(future -> {
      try {
        future.get();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    verify(bulkProcessor).flush();
  }

  @Test
  public void testBulkPutCancellation() throws Exception {
    List<Map> components = Collections.singletonList(Collections.emptyMap());
    Repository repository = repository("test-repo");
    captureRepoNameArg();
    searchIndexService.createIndex(repository);

    cancelled.set(true);

    assertThrows(TaskInterruptedException.class,
        () -> searchIndexService.bulkPut(repository, components, NEVER_CALLED_ID_PRODUCER,
            NEVER_CALLED_JSON_DOC_PRODUCER));

    verify(bulkProcessor, never()).flush();
  }

  protected Repository repository(String name) throws Exception {
    TestRepository repository = new TestRepository(eventManager, new HostedType(), new TestFormat("test"));
    repository.setName(name);
    Configuration configuration = mock(Configuration.class);
    when(configuration.isOnline()).thenReturn(true);
    repository.setConfiguration(configuration);
    SearchIndexFacet searchFacet = mock(SearchIndexFacet.class);
    repository.attach(searchFacet);
    return repository;
  }

  private ArgumentCaptor<String> captureRepoNameArg() {
    ArgumentCaptor<String> varArgs = ArgumentCaptor.forClass(String.class);
    when(indicesAdminClient.prepareExists(varArgs.capture())).thenReturn(indicesExistsRequestBuilder);
    when(indicesExistsRequestBuilder.execute()).thenReturn(actionFuture);
    when(actionFuture.actionGet()).thenReturn(indicesExistsResponse);
    when(indicesExistsResponse.isExists()).thenReturn(true);
    return varArgs;
  }

  class TestFormat
      extends Format
  {
    TestFormat(final String value) {
      super(value);
    }
  }

  class TestRepository
      extends RepositoryImpl
  {
    private String name;

    private Configuration configuration;

    public TestRepository(EventManager eventManager, Type type, Format format) {
      super(eventManager, type, format);
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setConfiguration(Configuration configuration) {
      this.configuration = configuration;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Configuration getConfiguration() {
      return configuration;
    }
  }
}
