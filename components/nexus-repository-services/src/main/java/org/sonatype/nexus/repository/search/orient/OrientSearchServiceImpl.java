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
package org.sonatype.nexus.repository.search.orient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.internal.resources.TokenEncoder;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.ElasticSearchQueryService;
import org.sonatype.nexus.repository.search.query.ElasticSearchUtils;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ASSETS;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ATTRIBUTES;
import static org.sonatype.nexus.repository.search.index.SearchConstants.CHECKSUM;
import static org.sonatype.nexus.repository.search.index.SearchConstants.CONTENT_TYPE;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ID;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.VERSION;

/**
 * Implementation of {@link SearchService} to be used with orient/elasticsearch
 *
 * @since 3.next
 */
@Named
@Singleton
public class OrientSearchServiceImpl
    extends ComponentSupport
    implements SearchService
{
  private final ElasticSearchQueryService elasticSearchQueryService;

  private final ElasticSearchUtils elasticSearchUtils;

  private final TokenEncoder tokenEncoder;

  private final Set<OrientSearchExtension> decorators;

  @Inject
  public OrientSearchServiceImpl(
      final ElasticSearchQueryService elasticSearchQueryService,
      final ElasticSearchUtils elasticSearchUtils,
      final TokenEncoder tokenEncoder,
      final Set<OrientSearchExtension> decorators)
  {
    this.elasticSearchQueryService = checkNotNull(elasticSearchQueryService);
    this.elasticSearchUtils = checkNotNull(elasticSearchUtils);
    this.tokenEncoder = checkNotNull(tokenEncoder);
    this.decorators = checkNotNull(decorators);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    QueryBuilder queryBuilder = elasticSearchUtils.buildQuery(searchRequest);

    int from = 0;

    if (StringUtils.isNotEmpty(searchRequest.getContinuationToken())) {
      from = decodeFrom(searchRequest, queryBuilder);
    }

    org.elasticsearch.action.search.SearchResponse searchResponse =
        elasticSearchQueryService.search(queryBuilder, from, searchRequest.getLimit());
    return convertSearchResponse(searchResponse, continuationToken(searchRequest, queryBuilder, searchResponse));
  }

  @Override
  public Iterable<ComponentSearchResult> browse(final SearchRequest searchRequest) {
    Iterable<SearchHit> browse = elasticSearchQueryService.browse(elasticSearchUtils.buildQuery(searchRequest));
    return () -> new SearchResultIterator(browse.iterator());
  }

  @Override
  public long count(final SearchRequest searchRequest) {
    return elasticSearchQueryService.count(elasticSearchUtils.buildQuery(searchRequest));
  }

  private class SearchResultIterator
      implements Iterator<ComponentSearchResult>
  {
    private final Iterator<SearchHit> searchHitIterator;

    public SearchResultIterator(final Iterator<SearchHit> searchHitIterator) {
      this.searchHitIterator = searchHitIterator;
    }

    @Override
    public boolean hasNext() {
      return searchHitIterator.hasNext();
    }

    @Override
    public ComponentSearchResult next() {
      return toComponentSearchResult(searchHitIterator.next());
    }
  }

  private String continuationToken(
      final SearchRequest request,
      final QueryBuilder query,
      final org.elasticsearch.action.search.SearchResponse searchResponse)
  {
    if (request.getLimit() != searchResponse.getHits().hits().length) {
      return null;
    }

    int from = decodeFrom(request, query);
    return tokenEncoder.encode(from, request.getLimit(), query);
  }

  private int decodeFrom(final SearchRequest request, final QueryBuilder query) {
    return tokenEncoder.decode(request.getContinuationToken(), query);
  }

  private SearchResponse convertSearchResponse(
      final org.elasticsearch.action.search.SearchResponse esResponse,
      final String continuationToken)
  {
    SearchResponse searchResponse = new SearchResponse();
    List<ComponentSearchResult> componentSearchResults = new ArrayList<>();

    for (SearchHit hit : esResponse.getHits()) {
      componentSearchResults.add(toComponentSearchResult(hit));
    }

    searchResponse.setSearchResults(componentSearchResults);

    searchResponse.setContinuationToken(continuationToken);
    return searchResponse;
  }

  @SuppressWarnings("unchecked")
  private ComponentSearchResult toComponentSearchResult(final SearchHit componentHit) {
    Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
    Repository repository = elasticSearchUtils.getRepository((String) componentMap.get(REPOSITORY_NAME));

    ComponentSearchResult componentSearchResult = new ComponentSearchResult();

    List<Map<String, Object>> assets = (List<Map<String, Object>>) componentMap.get(ASSETS);
    if (assets != null) {
      componentSearchResult.setAssets(assets.stream()
          .map(assetMap -> toAssetSearchResult(assetMap, repository))
          .collect(Collectors.toList()));
    }
    else {
      componentSearchResult.setAssets(ImmutableList.of());
    }

    componentSearchResult.setGroup((String) componentMap.get(GROUP));
    componentSearchResult.setName((String) componentMap.get(NAME));
    componentSearchResult.setVersion((String) componentMap.get(VERSION));
    componentSearchResult.setId(new RepositoryItemIDXO(repository.getName(), componentHit.getId()).getValue());
    componentSearchResult.setRepositoryName(repository.getName());
    componentSearchResult.setFormat(repository.getFormat().getValue());

    decorators.forEach(extension -> extension.updateComponent(componentSearchResult, componentHit));

    return componentSearchResult;
  }

  @SuppressWarnings("unchecked")
  private AssetSearchResult toAssetSearchResult(final Map<String, Object> assetMap, final Repository repository) {

    AssetSearchResult assetSearchResult = new AssetSearchResult();

    assetSearchResult.setAttributes((Map<String, Object>) assetMap.getOrDefault(ATTRIBUTES, Collections.emptyMap()));
    assetSearchResult.setPath((String) assetMap.get(NAME));
    assetSearchResult.setRepository(repository.getName());
    assetSearchResult.setId(new RepositoryItemIDXO(repository.getName(), String.valueOf(assetMap.get(ID))).getValue());
    assetSearchResult.setChecksum((Map<String, String>) assetSearchResult.getAttributes().get(CHECKSUM));
    assetSearchResult.setFormat(repository.getFormat().getValue());
    assetSearchResult.setContentType((String) assetMap.get(CONTENT_TYPE));
    assetSearchResult.setLastModified(calculateLastModified(assetMap));

    return assetSearchResult;
  }

  private Date calculateLastModified(final Map<String, Object> attributes) {
    try {
      return Optional.ofNullable(attributes.get("content"))
          .map(Map.class::cast)
          .map(content -> content.get("last_modified"))
          .map(String.class::cast)
          .map(Long::parseLong)
          .map(Date::new)
          .orElse(null);
    }
    catch (Exception ignored) {
      log.debug("Unable to retrieve last_modified", ignored);
      // Nothing we can do here for invalid data. It shouldn't happen but date parsing will blow out the results.
      return null;
    }
  }
}
