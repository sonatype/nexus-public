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
package org.sonatype.nexus.repository.search.elasticsearch;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.internal.resources.TokenEncoder;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.index.ElasticSearchIndexService;
import org.sonatype.nexus.repository.search.query.ElasticSearchQueryService;
import org.sonatype.nexus.repository.search.query.ElasticSearchUtils;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.sonatype.nexus.repository.search.index.SearchConstants.*;

/**
 * Implementation of {@link SearchService} to be used with orient/elasticsearch
 *
 * @since 3.38
 */
@Named
@Singleton
public class ElasticSearchServiceImpl
    extends ComponentSupport
    implements SearchService
{
  private static final DateTimeFormatter DATE_TIME_FORMATTER = ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSZ");

  private final ElasticSearchQueryService elasticSearchQueryService;

  private final ElasticSearchIndexService elasticSearchIndexService;

  private final ElasticSearchUtils elasticSearchUtils;

  private final TokenEncoder tokenEncoder;

  private final Set<ElasticSearchExtension> decorators;

  @Inject
  public ElasticSearchServiceImpl(
      final ElasticSearchQueryService elasticSearchQueryService,
      final ElasticSearchIndexService elasticSearchIndexService,
      final ElasticSearchUtils elasticSearchUtils,
      final TokenEncoder tokenEncoder,
      final Set<ElasticSearchExtension> decorators)
  {
    this.elasticSearchQueryService = checkNotNull(elasticSearchQueryService);
    this.elasticSearchIndexService = checkNotNull(elasticSearchIndexService);
    this.elasticSearchUtils = checkNotNull(elasticSearchUtils);
    this.tokenEncoder = checkNotNull(tokenEncoder);
    this.decorators = checkNotNull(decorators);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    QueryBuilder queryBuilder = elasticSearchUtils.buildQuery(searchRequest);

    int from = Optional.ofNullable(searchRequest.getContinuationToken())
        .filter(Strings2::notBlank)
        .map(__ -> decodeFrom(searchRequest, queryBuilder))
        .orElseGet(() -> Optional.ofNullable(searchRequest.getOffset()).orElse(0));

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

  @Override
  public void waitForCalm() {
    elasticSearchIndexService.waitForCalm();
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
    searchResponse.setTotalHits(esResponse.getHits().getTotalHits());

    return searchResponse;
  }

  @SuppressWarnings("unchecked")
  private ComponentSearchResult toComponentSearchResult(final SearchHit componentHit) {
    Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
    Repository repository = elasticSearchUtils.getReadableRepository((String) componentMap.get(REPOSITORY_NAME));

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
    componentSearchResult.setId(componentHit.getId());
    componentSearchResult.setRepositoryName(repository.getName());
    componentSearchResult.setFormat(repository.getFormat().getValue());
    componentSearchResult.setLastDownloaded(calculateOffsetDateTime(componentMap, LAST_DOWNLOADED_KEY));
    componentSearchResult.setLastModified(calculateOffsetDateTime(componentMap, LAST_BLOB_UPDATED_KEY));

    decorators.forEach(extension -> extension.updateComponent(componentSearchResult, componentHit));

    return componentSearchResult;
  }

  @SuppressWarnings("unchecked")
  private AssetSearchResult toAssetSearchResult(final Map<String, Object> assetMap, final Repository repository) {

    AssetSearchResult assetSearchResult = new AssetSearchResult();

    assetSearchResult.setAttributes((Map<String, Object>) assetMap.getOrDefault(ATTRIBUTES, Collections.emptyMap()));
    assetSearchResult.setPath((String) assetMap.get(NAME));
    assetSearchResult.setRepository(repository.getName());
    assetSearchResult.setId(String.valueOf(assetMap.get(ID)));
    assetSearchResult.setChecksum((Map<String, String>) assetSearchResult.getAttributes().get(CHECKSUM));
    assetSearchResult.setFormat(repository.getFormat().getValue());
    assetSearchResult.setContentType((String) assetMap.get(CONTENT_TYPE));
    assetSearchResult.setLastModified(calculateLastModified(assetSearchResult.getAttributes()));
    assetSearchResult.setUploader((String) assetMap.get(UPLOADER));
    assetSearchResult.setUploaderIp((String) assetMap.get(UPLOADER_IP));
    assetSearchResult.setLastDownloaded(calculateLastDownloaded(assetMap));
    assetSearchResult.setFileSize(getFileSize(assetMap));

    return assetSearchResult;
  }

  private Long getFileSize(final Map<String, Object> attributes) {
    return Optional.ofNullable(attributes.get(FILE_SIZE))
        .map(Number.class::cast)
        .map(Number::longValue)
        .orElse(null);
  }

  private OffsetDateTime calculateOffsetDateTime(final Map<String, Object> attributes, final String field) {
    try {
      return Optional.ofNullable(attributes.get(field))
          .map(String.class::cast)
          .map(DATE_TIME_FORMATTER::parse)
          .map(Instant::from)
          .map(instant -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC))
          .orElse(null);
    }
    catch (Exception ignored) {
      log.debug("Unable to retrieve {}", field, ignored);
      // Nothing we can do here for invalid data. It shouldn't happen but date parsing will blow out the results.
      return null;
    }
  }

  private Date calculateLastDownloaded(final Map<String, Object> attributes) {
    try {
      return Optional.ofNullable(attributes.get(LAST_DOWNLOADED_KEY))
          .map(String.class::cast)
          .map(DATE_TIME_FORMATTER::parse)
          .map(Instant::from)
          .map(Date::from)
          .orElse(null);
    }
    catch (Exception ignored) {
      log.debug("Unable to retrieve last_downloaded", ignored);
      return null;
    }
  }

  private Date calculateLastModified(final Map<String, Object> attributes) {
    try {
      return Optional.ofNullable(attributes.get("content"))
          .map(Map.class::cast)
          .map(content -> content.get("last_modified"))
          .map(Object::toString) // Sometimes last_modified is a string and sometimes a long for unknown reasons
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
