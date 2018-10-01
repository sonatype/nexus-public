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
package org.sonatype.nexus.cleanup.internal.search.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.service.CleanupComponentBrowse;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * Finds components for cleanup using Elastic Search.
 *
 * @since 3.14
 */
@Named
@Singleton
public class ElasticSearchCleanupComponentBrowse
    extends ComponentSupport
    implements CleanupComponentBrowse
{
  private static final String NAME = "name";

  private static final String VERSION = "version";

  private static final String GROUP = "group";

  private final SearchService searchService;

  private final Map<String, CriteriaAppender> criteriaAppenders;

  @Inject
  public ElasticSearchCleanupComponentBrowse(final Map<String, CriteriaAppender> criteriaAppenders,
                                             final SearchService searchService)
  {
    this.criteriaAppenders = checkNotNull(criteriaAppenders);
    this.searchService = checkNotNull(searchService);
  }

  @Override
  public Iterable<EntityId> browse(final CleanupPolicy policy, final Repository repository) {
    if (policy.getCriteria().isEmpty()) {
      return emptyList();
    }
    
    QueryBuilder query = convertPolicyToQuery(policy);

    log.debug("Searching for components to cleanup using policy {}", policy);
    
    return transform(searchService.browseUnrestrictedInRepos(query, ImmutableList.of(repository.getName())),
        searchHit -> new DetachedEntityId(searchHit.getId()));
  }

  @Override
  public PagedResponse<Component> browseByPage(final CleanupPolicy policy,
                                               final Repository repository,
                                               final QueryOptions options)
  {
    checkNotNull(options.getStart());
    checkNotNull(options.getLimit());

    StorageTx tx = UnitOfWork.currentTx();

    QueryBuilder query = convertPolicyToQuery(policy, options);

    log.debug("Searching for components to cleanup using policy {}", policy);

    SearchResponse searchResponse = searchService.searchUnrestrictedInRepos(query,
        getSort(options.getSortProperty(), options.getSortDirection()),
        options.getStart(),
        options.getLimit(),
        ImmutableList.of(repository.getName()));

    List<Component> components = stream(searchResponse.getHits().spliterator(), false)
        .map(searchHit -> tx.findComponent(new DetachedEntityId(searchHit.getId())))
        .filter(Objects::nonNull)
        .collect(toList());

    return new PagedResponse<>(searchResponse.getHits().getTotalHits(), components);
  }

  private QueryBuilder convertPolicyToQuery(final CleanupPolicy policy, final QueryOptions options) {
    BoolQueryBuilder queryBuilder = convertPolicyToQuery(policy);

    if(isNullOrEmpty(options.getFilter())) {
      return queryBuilder;
    }

    QueryStringQueryBuilder stringQueryBuilder =
        QueryBuilders.queryStringQuery(addWildcard(options.getFilter()))
            .field(NAME).field(GROUP).field(VERSION);
    return queryBuilder.must(stringQueryBuilder);
  }

  private String addWildcard(final String filter) {
    return filter + "*";
  }

  private BoolQueryBuilder convertPolicyToQuery(final CleanupPolicy policy) {
    BoolQueryBuilder query = boolQuery().must(matchAllQuery());

    for (Entry<String, String> criteria : policy.getCriteria().entrySet()) {
      addCriteria(query, criteria.getKey(), criteria.getValue());
    }

    return query;
  }

  private void addCriteria(final BoolQueryBuilder query, final String key, final String value) {
    if (!criteriaAppenders.containsKey(key)) {
      throw new UnsupportedOperationException("Criteria of type " + key + " is not supported");
    }

    criteriaAppenders.get(key).append(query, value);
  }

  private List<SortBuilder> getSort(final String sortProperty, final String sortDirection) {
    return ImmutableList.of(
        SortBuilders.fieldSort(sortProperty).order(SortOrder.valueOf(sortDirection.toUpperCase()))
    );
  }
}
