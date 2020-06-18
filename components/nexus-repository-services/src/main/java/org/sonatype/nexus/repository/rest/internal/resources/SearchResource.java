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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.SearchResourceExtension;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.SearchResourceDoc;
import org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder;
import org.sonatype.nexus.repository.search.query.SearchQueryService;
import org.sonatype.nexus.repository.search.query.SearchUtils;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.search.index.SearchConstants.ASSETS;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.VERSION;
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.repositoryQuery;
import static org.sonatype.nexus.repository.search.query.SearchUtils.CONTINUATION_TOKEN;
import static org.sonatype.nexus.repository.search.query.SearchUtils.SORT_DIRECTION;
import static org.sonatype.nexus.repository.search.query.SearchUtils.SORT_FIELD;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.4
 */
@Named
@Singleton
@Path(SearchResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SearchResource
    extends ComponentSupport
    implements Resource, SearchResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/search";

  public static final String SEARCH_ASSET_URI = "/assets";

  public static final String SEARCH_AND_DOWNLOAD_URI = "/assets/download";

  private final SearchUtils searchUtils;

  private final AssetMapUtils assetMapUtils;

  private final SearchQueryService searchQueryService;

  private final TokenEncoder tokenEncoder;

  private final ComponentXOFactory componentXOFactory;

  private final Set<SearchResourceExtension> searchResourceExtensions;

  private int pageSize = 50;

  @Inject
  public SearchResource(final SearchUtils searchUtils,
                        final AssetMapUtils assetMapUtils,
                        final SearchQueryService searchQueryService,
                        final TokenEncoder tokenEncoder,
                        final ComponentXOFactory componentXOFactory,
                        final Set<SearchResourceExtension> searchResourceExtensions)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.assetMapUtils = checkNotNull(assetMapUtils);
    this.searchQueryService = checkNotNull(searchQueryService);
    this.tokenEncoder = checkNotNull(tokenEncoder);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.searchResourceExtensions = checkNotNull(searchResourceExtensions);
  }

  @GET
  @Override
  public Page<ComponentXO> search(
      @QueryParam(CONTINUATION_TOKEN) final String continuationToken,
      @QueryParam(SORT_FIELD) final String sort,
      @QueryParam(SORT_DIRECTION) final String direction,
      @Nullable @QueryParam("timeout") final Integer seconds,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = searchUtils.buildQuery(uriInfo);

    int from = tokenEncoder.decode(continuationToken, query);

    RepositoryQueryBuilder repoQuery = repositoryQuery(query);
    repoQuery.sortBy(searchUtils.getSortBuilders(sort, direction, false));
    if (seconds != null) {
      repoQuery.timeout(Duration.ofSeconds(seconds));
    }

    SearchResponse response = searchQueryService.search(query, from, getPageSize());

    List<ComponentXO> componentXOs = Arrays.stream(response.getHits().hits())
        .map(this::toComponent)
        .collect(toList());

    return new Page<>(componentXOs, componentXOs.size() == getPageSize() ?
        tokenEncoder.encode(from, getPageSize(), query) : null);
  }

  @SuppressWarnings("unchecked")
  private ComponentXO toComponent(final SearchHit componentHit) {
    Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
    Repository repository = searchUtils.getRepository((String) componentMap.get(REPOSITORY_NAME));

    ComponentXO componentXO = componentXOFactory.createComponentXO();

    List<Map<String, Object>> assets = (List<Map<String, Object>>) componentMap.get(ASSETS);
    if (assets != null) {
      componentXO.setAssets(assets.stream()
          .map(assetMap -> AssetXO.fromElasticSearchMap(assetMap, repository))
          .collect(toList()));
    }
    else {
      componentXO.setAssets(ImmutableList.of());
    }

    componentXO.setGroup((String) componentMap.get(GROUP));
    componentXO.setName((String) componentMap.get(NAME));
    componentXO.setVersion((String) componentMap.get(VERSION));
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), componentHit.getId()).getValue());
    componentXO.setRepository(repository.getName());
    componentXO.setFormat(repository.getFormat().getValue());

    for (SearchResourceExtension searchResourceExtension : searchResourceExtensions) {
      componentXO = searchResourceExtension.updateComponentXO(componentXO, componentHit);
    }

    return componentXO;
  }

  /**
   * @since 3.6.1
   */
  @GET
  @Path(SEARCH_ASSET_URI)
  @Override
  public Page<AssetXO> searchAssets(
      @QueryParam(CONTINUATION_TOKEN) final String continuationToken,
      @QueryParam(SORT_FIELD) final String sort,
      @QueryParam(SORT_DIRECTION) final String direction,
      @Nullable @QueryParam("timeout") final Integer seconds,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = searchUtils.buildQuery(uriInfo);

    int from = tokenEncoder.decode(continuationToken, query);

    RepositoryQueryBuilder repoQuery = repositoryQuery(query);
    repoQuery.sortBy(searchUtils.getSortBuilders(sort, direction, false));
    if (seconds != null) {
      repoQuery.timeout(Duration.ofSeconds(seconds));
    }

    SearchResponse componentResponse = searchQueryService.search(query, from, getPageSize());

    List<AssetXO> assetXOs = retrieveAssets(componentResponse, uriInfo);
    return new Page<>(assetXOs, componentResponse.getHits().hits().length == getPageSize() ?
        tokenEncoder.encode(from, getPageSize(), query) : null);
  }

  /**
   * @since 3.7
   */
  @GET
  @Path(SEARCH_AND_DOWNLOAD_URI)
  @Override
  public Response searchAndDownloadAssets(@QueryParam(SORT_FIELD) final String sort,
                                          @QueryParam(SORT_DIRECTION) final String direction,
                                          @QueryParam("timeout") Integer seconds,
                                          @Context final UriInfo uriInfo)
  {
    QueryBuilder query = searchUtils.buildQuery(uriInfo);

    List<AssetXO> assetXOs = retrieveAssets(query, sort, direction, seconds, uriInfo);

    return new AssetDownloadResponseProcessor(assetXOs, !Strings2.isEmpty(sort)).process();
  }

  private List<AssetXO> retrieveAssets(final SearchResponse response, final UriInfo uriInfo) {
    // get the asset specific parameters
    MultivaluedMap<String, String> assetParams = getAssetParams(uriInfo);

    return Arrays.stream(response.getHits().hits())
        .flatMap(hit -> extractAssets(hit, assetParams))
        .collect(toList());
  }

  private List<AssetXO> retrieveAssets(final QueryBuilder query,
                                       final String sort,
                                       final String direction,
                                       final UriInfo uriInfo,
                                       final int from,
                                       @Nullable final Integer seconds)
  {
    RepositoryQueryBuilder repoQuery = repositoryQuery(query);
    repoQuery.sortBy(searchUtils.getSortBuilders(sort, direction, false));
    if (seconds != null) {
      repoQuery.timeout(Duration.ofSeconds(seconds));
    }

    return this.retrieveAssets(searchQueryService.search(query, from, getPageSize()), uriInfo);
  }

  private List<AssetXO> retrieveAssets(final QueryBuilder query,
                                       final String sort,
                                       final String direction,
                                       final Integer seconds,
                                       final UriInfo uriInfo)
  {
    return this.retrieveAssets(query, sort, direction, uriInfo, 0, seconds);
  }

  @SuppressWarnings("unchecked")
  private Stream<AssetXO> extractAssets(final SearchHit componentHit,
                                        final MultivaluedMap<String, String> assetParams)
  {
    Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
    Repository repository = searchUtils.getRepository((String) componentMap.get(REPOSITORY_NAME));

    List<Map<String, Object>> assets = (List<Map<String, Object>>) componentMap.get(ASSETS);
    if (assets == null) {
      return Stream.empty();
    }

    return assets.stream()
        .filter(assetMap -> assetMapUtils.filterAsset(assetMap, assetParams))
        .map(assetMap -> AssetXO.fromElasticSearchMap(assetMap, repository));
  }

  @VisibleForTesting
  MultivaluedMap<String, String> getAssetParams(final UriInfo uriInfo) {
    return uriInfo.getQueryParameters()
        .entrySet().stream()
        .filter(t -> searchUtils.isAssetSearchParam(t.getKey()))
        .collect(toMap(Entry::getKey, Entry::getValue, (u, v) -> {
          throw new IllegalStateException(format("Duplicate key %s", u));
        }, MultivaluedHashMap::new));
  }

  private int getPageSize() {
    return pageSize;
  }

  @VisibleForTesting
  void setPageSize(final int pageSize) {
    this.pageSize = pageSize;
  }
}
