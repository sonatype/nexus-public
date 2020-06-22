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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.index.SearchIndexService;
import org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder;
import org.sonatype.nexus.repository.search.query.SearchQueryService;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.Duration.ofSeconds;
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.repositoryQuery;

/**
 * Default {@link SearchService} implementation. It does not expects that {@link Repository} have storage facet
 * attached.
 *
 * @since 3.0
 */
@Named("legacy")
@Singleton
public class SearchServiceImpl
    extends ComponentSupport
    implements SearchService
{
  private final SearchIndexService searchIndexService;

  private final SearchQueryService searchQueryService;

  /**
   * @param searchIndexService the search index service
   * @param searchQueryService the search query service
   */
  @Inject
  public SearchServiceImpl(
      final SearchIndexService searchIndexService,
      final SearchQueryService searchQueryService)
  {
    this.searchIndexService = checkNotNull(searchIndexService);
    this.searchQueryService = checkNotNull(searchQueryService);
  }

  @Override
  public void createIndex(final Repository repository) {
    searchIndexService.createIndex(repository);
  }

  @Override
  public void deleteIndex(final Repository repository) {
    searchIndexService.deleteIndex(repository);
  }

  @Override
  public void rebuildIndex(final Repository repository) {
    searchIndexService.rebuildIndex(repository);
  }

  @Override
  public void put(final Repository repository, final String identifier, final String json) {
    searchIndexService.put(repository, identifier, json);
  }

  @Override
  public <T> List<Future<Void>> bulkPut(final Repository repository,
                                        final Iterable<T> components,
                                        final Function<T, String> identifierProducer,
                                        final Function<T, String> jsonDocumentProducer)
  {
    return searchIndexService.bulkPut(repository, components, identifierProducer, jsonDocumentProducer);
  }

  @Override
  public void delete(final Repository repository, final String identifier) {
    searchIndexService.delete(repository, identifier);
  }

  @Override
  public void bulkDelete(@Nullable final Repository repository, final Iterable<String> identifiers) {
    searchIndexService.bulkDelete(repository, identifiers);
  }

  @Override
  public void flush(final boolean fsync) {
    searchIndexService.flush(fsync);
  }

  @Override
  public long getUpdateCount() {
    return searchIndexService.getUpdateCount();
  }

  @Override
  public boolean isCalmPeriod() {
    return searchIndexService.isCalmPeriod();
  }

  @Override
  public void waitForCalm() {
    searchIndexService.waitForCalm();
  }

  /**
   * Use this method with caution. It makes use of the scroll API in ElasticSearch which is not thread safe. If two
   * matching queries are received from different threads within the configured 1 minute it is possible that scrolling 
   * through the data will return different pages of the same result set to each of the threads.
   *
   * For additional context see: https://issues.sonatype.org/browse/NEXUS-18847
   * 
   * @param query
   * @return an Iterable wrapping the scroll context which times out if not used within 1 minute
   */
  @Override
  public Iterable<SearchHit> browseUnrestricted(final QueryBuilder query) {
    return browseUnrestrictedInRepos(query, null);
  }

  /**
   * Use this method with caution. It makes use of the scroll API in ElasticSearch which is not thread safe. If two
   * matching queries are received from different threads within the configured 1 minute it is possible that scrolling 
   * through the data will return different pages of the same result set to each of the threads.
   *
   * For additional context see: https://issues.sonatype.org/browse/NEXUS-18847
   * 
   * @param query
   * @param repoNames
   * @return an Iterable wrapping the scroll context which times out if not used within 1 minute
   */
  @Override
  public Iterable<SearchHit> browseUnrestrictedInRepos(final QueryBuilder query,
                                                       @Nullable final Collection<String> repoNames)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query);

    if (repoNames != null) {
      repoQuery = repoQuery.inRepositories(repoNames);
    }

    return searchQueryService.browse(repoQuery.unrestricted());
  }

  /**
   * Use this method with caution. It makes use of the scroll API in ElasticSearch which is not thread safe. If two
   * matching queries are received from different threads within the configured 1 minute it is possible that scrolling 
   * through the data will return different pages of the same result set to each of the threads.
   *
   * For additional context see: https://issues.sonatype.org/browse/NEXUS-18847
   * 
   * @param query
   * @return an Iterable wrapping the scroll context which times out if not used within 1 minute
   */
  @Override
  public Iterable<SearchHit> browse(final QueryBuilder query) {
    return searchQueryService.browse(query);
  }

  @Override
  public SearchResponse searchUnrestrictedInRepos(final QueryBuilder query,
                                                  @Nullable final List<SortBuilder> sort,
                                                  final int from,
                                                  final int size,
                                                  final Collection<String> repoNames)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query).inRepositories(repoNames);

    if (sort != null) {
      repoQuery = repoQuery.sortBy(sort);
    }

    return searchQueryService.search(repoQuery.unrestricted(), from, size);
  }

  @Override
  public SearchResponse searchUnrestricted(final QueryBuilder query,
                                           @Nullable final List<SortBuilder> sort,
                                           final int from,
                                           final int size)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query);

    if (sort != null) {
      repoQuery = repoQuery.sortBy(sort);
    }

    return searchQueryService.search(repoQuery.unrestricted(), from, size);
  }

  @Override
  public SearchResponse search(final QueryBuilder query,
                               @Nullable final List<SortBuilder> sort,
                               final int from,
                               final int size,
                               @Nullable final Integer seconds)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query);

    if (sort != null) {
      repoQuery = repoQuery.sortBy(sort);
    }

    if (seconds != null) {
      repoQuery = repoQuery.timeout(ofSeconds(seconds));
    }

    return searchQueryService.search(repoQuery, from, size);
  }

  @Override
  public SearchResponse searchInReposWithAggregations(final QueryBuilder query,
                                                      final List<AggregationBuilder> aggregations,
                                                      final Collection<String> repoNames)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query).inRepositories(repoNames);

    return searchQueryService.search(repoQuery, aggregations);
  }

  @Override
  public SearchResponse searchUnrestrictedInReposWithAggregations(final QueryBuilder query,
                                                                  final List<AggregationBuilder> aggregations,
                                                                  @Nullable final List<SortBuilder> sort,
                                                                  final Collection<String> repoNames)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query).inRepositories(repoNames);

    if (sort != null) {
      repoQuery = repoQuery.sortBy(sort);
    }

    return searchQueryService.search(repoQuery.unrestricted(), aggregations);
  }

  @Override
  public long countUnrestricted(final QueryBuilder query) {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query);

    return searchQueryService.count(repoQuery.unrestricted());
  }
}
