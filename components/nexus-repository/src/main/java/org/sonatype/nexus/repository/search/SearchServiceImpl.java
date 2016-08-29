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
package org.sonatype.nexus.repository.search;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchSubjectHelper.SubjectRegistration;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.internal.ContentAuthPluginScriptFactory;
import org.sonatype.nexus.security.SecurityHelper;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.elasticsearch.action.admin.indices.validate.query.QueryExplanation;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.profile.ProfileShardResult;
import org.elasticsearch.search.sort.SortBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Default {@link SearchService} implementation. It does not expects that {@link Repository} have storage facet
 * attached.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SearchServiceImpl
    extends ComponentSupport
    implements SearchService
{
  private static final String TYPE = "component";

  /**
   * Resource name of ElasticSearch mapping configuration.
   */
  public static final String MAPPING_JSON = "elasticsearch-mapping.json";

  private static final SearchResponse EMPTY_SEARCH_RESPONSE = new SearchResponse(InternalSearchResponse.empty(), null, 0,
      0, 0, new ShardSearchFailure[]{});

  private final Provider<Client> client;

  private final RepositoryManager repositoryManager;

  private final SecurityHelper securityHelper;

  private final SearchSubjectHelper searchSubjectHelper;

  private final List<IndexSettingsContributor> indexSettingsContributors;

  private final ConcurrentMap<String, String> repositoryNameMapping;

  private final boolean profile;

  @Inject
  public SearchServiceImpl(final Provider<Client> client,
                           final RepositoryManager repositoryManager,
                           final SecurityHelper securityHelper,
                           final SearchSubjectHelper searchSubjectHelper,
                           final List<IndexSettingsContributor> indexSettingsContributors,
                           @Named("${nexus.elasticsearch.profile:-false}") final boolean profile)
  {
    this.client = checkNotNull(client);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.securityHelper = checkNotNull(securityHelper);
    this.searchSubjectHelper = checkNotNull(searchSubjectHelper);
    this.indexSettingsContributors = checkNotNull(indexSettingsContributors);
    this.repositoryNameMapping = Maps.newConcurrentMap();
    this.profile = checkNotNull(profile);
  }

  @Override
  public void createIndex(final Repository repository) {
    checkNotNull(repository);
    final String safeIndexName = SHA1.function().hashUnencodedChars(repository.getName()).toString();
    log.debug("Creating index for {}", repository);
    createIndex(repository, safeIndexName);
  }

  private void createIndex(final Repository repository, final String indexName) {
    // TODO we should calculate the checksum of index settings and compare it with a value stored in index _meta tags
    // in case that they not match (settings changed) we should drop the index, recreate it and re-index all components
    IndicesAdminClient indices = indicesAdminClient();
    if (!indices.prepareExists(indexName).execute().actionGet().isExists()) {
      // determine list of mapping configuration urls
      List<URL> urls = Lists.newArrayListWithExpectedSize(indexSettingsContributors.size() + 1);
      urls.add(Resources.getResource(getClass(), MAPPING_JSON)); // core mapping
      for (IndexSettingsContributor contributor : indexSettingsContributors) {
        URL url = contributor.getIndexSettings(repository);
        if (url != null) {
          urls.add(url);
        }
      }

      try {
        // merge all mapping configuration
        String source = "{}";
        for (URL url : urls) {
          log.debug("Merging ElasticSearch mapping: {}", url);
          String contributed = Resources.toString(url, Charsets.UTF_8);
          log.trace("Contributed ElasticSearch mapping: {}", contributed);
          source = JsonUtils.merge(source, contributed);
        }
        // update runtime configuration
        log.trace("ElasticSearch mapping: {}", source);
        indices.prepareCreate(indexName)
            .setSource(source)
            .execute()
            .actionGet();
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    repositoryNameMapping.put(repository.getName(), indexName);
  }

  @Override
  public void deleteIndex(final Repository repository) {
    checkNotNull(repository);
    String indexName = repositoryNameMapping.remove(repository.getName());
    if (indexName != null) {
      log.debug("Removing index of {}", repository);
      deleteIndex(indexName);
    }
  }

  private void deleteIndex(final String indexName) {
    IndicesAdminClient indices = indicesAdminClient();
    if (indices.prepareExists(indexName).execute().actionGet().isExists()) {
      indices.prepareDelete(indexName).execute().actionGet();
    }
  }

  public void rebuildIndex(final Repository repository) {
    checkNotNull(repository);
    String indexName = repositoryNameMapping.remove(repository.getName());
    if (indexName != null) {
      log.debug("Rebuilding index for {}", repository);
      deleteIndex(indexName);
      createIndex(repository, indexName);
    }
  }

  @Override
  public void put(final Repository repository, final String identifier, final String json) {
    checkNotNull(repository);
    checkNotNull(identifier);
    checkNotNull(json);
    String indexName = repositoryNameMapping.get(repository.getName());
    if (indexName == null) {
      return;
    }
    log.debug("Adding to index document {} from {}: {}", identifier, repository, json);
    client.get().prepareIndex(indexName, TYPE, identifier).setSource(json).execute();
  }

  @Override
  public void delete(final Repository repository, final String identifier) {
    checkNotNull(repository);
    checkNotNull(identifier);
    String indexName = repositoryNameMapping.get(repository.getName());
    if (indexName == null) {
      return;
    }
    log.debug("Removing from index document {} from {}", identifier, repository);
    client.get().prepareDelete(indexName, TYPE, identifier).execute();
  }

  @Override
  public Iterable<SearchHit> browseUnrestricted(final QueryBuilder query) {
    checkNotNull(query);
    try {
      if (!indicesAdminClient().prepareValidateQuery().setQuery(query).execute().actionGet().isValid()) {
        throw new IllegalArgumentException("Invalid query");
      }
    }
    catch (IndexNotFoundException e) {
      // no repositories were created yet, so there is no point in searching
      return null;
    }
    final String[] searchableIndexes = getSearchableIndexes(false);
    if (searchableIndexes.length == 0) {
      return Collections.emptyList();
    }
    return new Iterable<SearchHit>()
    {
      @Override
      public Iterator<SearchHit> iterator() {
        return new Iterator<SearchHit>()
        {
          private SearchResponse response;

          private Iterator<SearchHit> iterator;

          private boolean noMoreHits = false;

          @Override
          public boolean hasNext() {
            if (noMoreHits) {
              return false;
            }
            if (response == null) {
              response = client.get().prepareSearch(searchableIndexes)
                  .setTypes(TYPE)
                  .setQuery(query)
                  .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                  .setSize(100)
                  .execute()
                  .actionGet();
              iterator = Arrays.asList(response.getHits().getHits()).iterator();
              noMoreHits = !iterator.hasNext();
            }
            else if (!iterator.hasNext()) {
              response = client.get().prepareSearchScroll(response.getScrollId())
                  .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                  .execute()
                  .actionGet();
              iterator = Arrays.asList(response.getHits().getHits()).iterator();
              noMoreHits = !iterator.hasNext();
            }
            return iterator.hasNext();
          }

          @Override
          public SearchHit next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return iterator.next();
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  @Override
  public Iterable<SearchHit> browseUnrestricted(final QueryBuilder query, final int from, final int size) {
    SearchResponse response = searchUnrestricted(query, null, from, size);
    return response.getHits();
  }

  @Override
  public SearchResponse searchUnrestricted(final QueryBuilder query,
                                           @Nullable final List<SortBuilder> sort,
                                           final int from,
                                           final int size)
  {
    if (!validateQuery(query)) {
      return EMPTY_SEARCH_RESPONSE;
    }
    final String[] searchableIndexes = getSearchableIndexes(false);
    if (searchableIndexes.length == 0) {
      return EMPTY_SEARCH_RESPONSE;
    }

    return executeSearch(query, searchableIndexes, from, size, sort, null);
  }

  @Override
  public SearchResponse search(final QueryBuilder query,
                               @Nullable final List<SortBuilder> sort,
                               final int from,
                               final int size)
  {
    if (!validateQuery(query)) {
      return EMPTY_SEARCH_RESPONSE;
    }
    final String[] searchableIndexes = getSearchableIndexes(true);
    if (searchableIndexes.length == 0) {
      return EMPTY_SEARCH_RESPONSE;
    }

    try (SubjectRegistration registration = searchSubjectHelper.register(securityHelper.subject())) {
      return executeSearch(query, searchableIndexes, from, size, sort,
          QueryBuilders.scriptQuery(ContentAuthPluginScriptFactory.newScript(registration.getId())));
    }
  }

  private SearchResponse executeSearch(final QueryBuilder query,
                                       final String[] searchableIndexes,
                                       final int from,
                                       final int size,
                                       @Nullable final List<SortBuilder> sort,
                                       @Nullable final QueryBuilder postFilter)
  {
    checkNotNull(query);
    checkNotNull(searchableIndexes);
    SearchRequestBuilder searchRequestBuilder = client.get().prepareSearch(searchableIndexes)
        .setTypes(TYPE)
        .setQuery(query)
        .setFrom(from)
        .setSize(size)
        .setProfile(profile);
    if (postFilter != null) {
      searchRequestBuilder.setPostFilter(postFilter);
    }
    if (sort != null) {
      for (SortBuilder entry : sort) {
        searchRequestBuilder.addSort(entry);
      }
    }
    SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

    if (profile) {
      logProfileResults(searchResponse);
    }

    return searchResponse;
  }

  @Override
  public long countUnrestricted(final QueryBuilder query) {
    if(!validateQuery(query)) {
      return 0;
    }
    final String[] searchableIndexes = getSearchableIndexes(false);
    if (searchableIndexes.length == 0) {
      return 0;
    }
    SearchRequestBuilder count = client.get().prepareSearch(searchableIndexes).setQuery(query).setSize(0);
    SearchResponse response = count.execute().actionGet();
    return response.getHits().totalHits();
  }

  private boolean validateQuery(QueryBuilder query) {
    checkNotNull(query);
    try {
      ValidateQueryResponse validateQueryResponse = indicesAdminClient().prepareValidateQuery()
          .setQuery(query).setExplain(true).execute().actionGet();
      if (!validateQueryResponse.isValid()) {
        if (log.isDebugEnabled()) {
          Collection<String> explanations = Collections2.transform(validateQueryResponse.getQueryExplanation(),
              new Function<QueryExplanation, String>()
              {
                @Nullable
                @Override
                public String apply(final QueryExplanation input) {
                  return input.getExplanation() != null ? input.getExplanation() : input.getError();
                }
              });
          log.debug("Invalid query explanation: {}", explanations);
        }
        throw new IllegalArgumentException("Invalid query");
      }
    }
    catch (IndexNotFoundException e) {
      // no repositories were created yet, so there is no point in searching
      return false;
    }
    return true;
  }

  private String[] getSearchableIndexes(final boolean skipPermissionCheck) {
    List<String> indexes = Lists.newArrayList();
    for (Repository repository : repositoryManager.browse()) {
      // check if search facet is available so avoid searching repositories without an index
      repository.optionalFacet(SearchFacet.class).ifPresent((searchFacet) -> {
        String indexName = repositoryNameMapping.get(repository.getName());
        if (indexName != null
            && repository.getConfiguration().isOnline()
            && (skipPermissionCheck || securityHelper.allPermitted(new RepositoryViewPermission(repository, BROWSE)))) {
          indexes.add(indexName);
        }
      });
    }
    return indexes.toArray(new String[indexes.size()]);
  }

  /**
   * Returns the indixes admin client.
   */
  private IndicesAdminClient indicesAdminClient() {
    return client.get().admin().indices();
  }

  private void logProfileResults(final SearchResponse searchResponse) {
    for (Entry<String, List<ProfileShardResult>> entry : searchResponse.getProfileResults().entrySet()) {
      for (ProfileShardResult profileShardResult : entry.getValue()) {
        try {
          XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
          builder.startObject();
          profileShardResult.toXContent(builder, ToXContent.EMPTY_PARAMS);
          builder.endObject();
          log.info("Elasticsearch profile for {} is: {}", entry.getKey(), builder.string());
        }
        catch (IOException e) {
          log.error("Error writing elasticsearch profile result", e);
        }
      }
    }
  }
}
