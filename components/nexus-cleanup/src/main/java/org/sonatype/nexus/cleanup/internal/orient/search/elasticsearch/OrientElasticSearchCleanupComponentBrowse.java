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
package org.sonatype.nexus.cleanup.internal.orient.search.elasticsearch;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.internal.search.elasticsearch.AbstractSearchCleanupComponentBrowse;
import org.sonatype.nexus.cleanup.internal.search.elasticsearch.CriteriaAppender;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.search.query.ElasticSearchQueryService;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.codahale.metrics.MetricRegistry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * Finds components for cleanup using Elastic Search.
 *
 * @since 3.14
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientElasticSearchCleanupComponentBrowse
    extends AbstractSearchCleanupComponentBrowse
    implements OrientCleanupComponentBrowse
{
  @Inject
  public OrientElasticSearchCleanupComponentBrowse(
      final Map<String, CriteriaAppender> criteriaAppenders,
      final ElasticSearchQueryService elasticSearchQueryService,
      final MetricRegistry metricRegistry)
  {
    super(criteriaAppenders, elasticSearchQueryService, metricRegistry);
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

    SearchResponse searchResponse = invokeSearchByPage(policy, repository, options, query);

    List<Component> components = stream(searchResponse.getHits().spliterator(), false)
        .map(searchHit -> tx.findComponent(new DetachedEntityId(searchHit.getId())))
        .filter(Objects::nonNull)
        .collect(toList());

    return new PagedResponse<>(searchResponse.getHits().getTotalHits(), components);
  }
}
