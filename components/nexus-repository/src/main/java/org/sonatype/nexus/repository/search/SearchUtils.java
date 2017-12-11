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

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.internal.resources.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.types.GroupType;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static jline.internal.Preconditions.checkNotNull;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * @since 3.next
 */
@Named
@Singleton
public class SearchUtils
    extends ComponentSupport
{
  private static final String Q = "q";

  private static final String CONTINUATION_TOKEN = "continuationToken";

  private static final String ASSET_PREFIX = "assets.";

  private final RepositoryManagerRESTAdapter repoAdapter;

  private final Map<String, String> searchParams;

  private final Map<String, String> assetSearchParams;

  @Inject
  public SearchUtils(final RepositoryManagerRESTAdapter repoAdapter,
                     final Map<String, SearchMappings> searchMappings)
  {
    this.repoAdapter = checkNotNull(repoAdapter);
    this.searchParams = checkNotNull(searchMappings).entrySet().stream()
        .flatMap(e -> stream(e.getValue().get().spliterator(), true))
        .collect(toMap(SearchMapping::getAlias, SearchMapping::getAttribute));
    this.assetSearchParams = searchParams.entrySet().stream()
        .filter(e -> e.getValue().startsWith(ASSET_PREFIX))
        .collect(toMap(Entry::getKey, Entry::getValue));
  }

  public Map<String, String> getSearchParameters() {
    return searchParams;
  }

  public Map<String, String> getAssetSearchParameters() {
    return assetSearchParams;
  }

  public Repository getRepository(final String repository) {
    return repoAdapter.getRepository(repository);
  }

  /**
   * Builds a {@link QueryBuilder} based on configured search parameters.
   *
   * @param uriInfo {@link UriInfo} to extract query parameters from
   */
  public QueryBuilder buildQuery(final UriInfo uriInfo) { // NOSONAR
    BoolQueryBuilder query = boolQuery();

    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

    if (queryParams.containsKey(Q)) {
      query.must(queryStringQuery(queryParams.getFirst(Q)));
    }

    queryParams.forEach((key, value) -> {
      if (value.isEmpty() || value.get(0).isEmpty()) {
        // no value sent
        return;
      }
      if (Q.equals(key)) {
        // skip the keyword search
        return;
      }
      if (CONTINUATION_TOKEN.equals(key)) {
        // skip the continuation token
        return;
      }
      if ("repository".equals(key)) {
        Repository repository = repoAdapter.getRepository(value.get(0));
        if (isGroup(repository)) {
          repository.facet(GroupFacet.class).leafMembers().forEach(r ->
              query.should(termQuery(searchParams.get(key), r.getName())));
          query.minimumNumberShouldMatch(1);
          return;
        }
      }
      query.filter(termQuery(searchParams.getOrDefault(key, key), value.get(0)));
    });

    log.debug("Query: {}", query);
    return query;
  }

  private boolean isGroup(final Repository repository) {
    return GroupType.NAME.equals(repository.getType().getValue());
  }

  public boolean isAssetSearchParam(final String assetSearchParam) {
    return assetSearchParams.containsKey(assetSearchParam) || isFullAssetAttributeName(assetSearchParam);
  }

  public boolean isFullAssetAttributeName(final String assetSearchParam) {
    return assetSearchParam.startsWith(ASSET_PREFIX);
  }
}
