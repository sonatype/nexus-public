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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

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
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.rest.SearchResourceExtension;
import org.sonatype.nexus.repository.rest.SearchUtils;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.SearchResourceDoc;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.rest.SearchUtils.CONTINUATION_TOKEN;
import static org.sonatype.nexus.repository.rest.SearchUtils.SORT_DIRECTION;
import static org.sonatype.nexus.repository.rest.SearchUtils.SORT_FIELD;
import static org.sonatype.nexus.repository.rest.api.AssetXO.fromAsset;
import static org.sonatype.nexus.repository.rest.api.AssetXO.fromElasticSearchMap;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;
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

  private final BrowseService browseService;

  private final SearchService searchService;

  private final TokenEncoder tokenEncoder;

  private final ComponentXOFactory componentXOFactory;

  private final Set<SearchResourceExtension> searchResourceExtensions;

  private int pageSize = 50;

  @Inject
  public SearchResource(final SearchUtils searchUtils,
                        final AssetMapUtils assetMapUtils,
                        final BrowseService browseService,
                        final SearchService searchService,
                        final TokenEncoder tokenEncoder,
                        final ComponentXOFactory componentXOFactory,
                        final Set<SearchResourceExtension> searchResourceExtensions)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.assetMapUtils = checkNotNull(assetMapUtils);
    this.browseService = checkNotNull(browseService);
    this.searchService = checkNotNull(searchService);
    this.tokenEncoder = checkNotNull(tokenEncoder);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.searchResourceExtensions = checkNotNull(searchResourceExtensions);
  }

  @GET
  public Page<ComponentXO> search(
      @QueryParam(CONTINUATION_TOKEN) final String continuationToken,
      @QueryParam(SORT_FIELD) final String sort,
      @QueryParam(SORT_DIRECTION) final String direction,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = searchUtils.buildQuery(uriInfo);

    int from = tokenEncoder.decode(continuationToken, query);

    SearchResponse response = searchService
        .search(query, searchUtils.getSortBuilders(sort, direction, false), from, getPageSize());

    List<ComponentXO> componentXOs = Arrays.stream(response.getHits().hits())
        .map(this::toComponent)
        .collect(toList());

    return new Page<>(componentXOs, componentXOs.size() == getPageSize() ?
        tokenEncoder.encode(from, getPageSize(), query) : null);
  }

  private ComponentXO toComponent(final SearchHit hit) {
    Map<String, Object> source = checkNotNull(hit.getSource());
    Repository repository = searchUtils.getRepository((String) source.get(REPOSITORY_NAME));

    ComponentXO componentXO = componentXOFactory.createComponentXO();

    componentXO
        .setAssets(browseService.browseComponentAssets(repository, hit.getId())
            .getResults()
            .stream()
            .map(asset -> fromAsset(asset, repository))
            .collect(toList()));

    componentXO.setGroup((String) source.get(GROUP));
    componentXO.setName((String) source.get(NAME));
    componentXO.setVersion((String) source.get(VERSION));
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), hit.getId()).getValue());
    componentXO.setRepository(repository.getName());
    componentXO.setFormat(repository.getFormat().getValue());

    for (SearchResourceExtension searchResourceExtension : searchResourceExtensions) {
      componentXO = searchResourceExtension.updateComponentXO(componentXO, hit);
    }

    return componentXO;
  }

  /**
   * @since 3.6.1
   */
  @GET
  @Path(SEARCH_ASSET_URI)
  public Page<AssetXO> searchAssets(
      @QueryParam(CONTINUATION_TOKEN) final String continuationToken,
      @QueryParam(SORT_FIELD) final String sort,
      @QueryParam(SORT_DIRECTION) final String direction,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = searchUtils.buildQuery(uriInfo);

    int from = tokenEncoder.decode(continuationToken, query);

    SearchResponse componentResponse = searchService
        .search(query, searchUtils.getSortBuilders(sort, direction, false), from, getPageSize());

    List<AssetXO> assetXOs = retrieveAssets(componentResponse, uriInfo);
    return new Page<>(assetXOs, componentResponse.getHits().hits().length == getPageSize() ?
        tokenEncoder.encode(from, getPageSize(), query) : null);
  }

  /**
   * @since 3.7
   */
  @GET
  @Path(SEARCH_AND_DOWNLOAD_URI)
  public Response searchAndDownloadAssets(@QueryParam(SORT_FIELD) final String sort,
                                          @QueryParam(SORT_DIRECTION) final String direction,
                                          @Context final UriInfo uriInfo)
  {
    QueryBuilder query = searchUtils.buildQuery(uriInfo);

    List<AssetXO> assetXOs = retrieveAssets(query, sort, direction, uriInfo);

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
                                       final int from)
  {
    return this.retrieveAssets(
        searchService.search(query, searchUtils.getSortBuilders(sort, direction, false), from, getPageSize()), uriInfo);
  }

  private List<AssetXO> retrieveAssets(final QueryBuilder query,
                                       final String sort,
                                       final String direction,
                                       final UriInfo uriInfo)
  {
    return this.retrieveAssets(query, sort, direction, uriInfo, 0);
  }

  @SuppressWarnings("unchecked")
  private Stream<AssetXO> extractAssets(final SearchHit componentHit,
                                        final MultivaluedMap<String, String> assetParams)
  {
    Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
    Repository repository = searchUtils.getRepository((String) componentMap.get(REPOSITORY_NAME));

    List<Map<String, Object>> assets = (List<Map<String, Object>>) componentMap.get("assets");
    if (assets == null) {
      return Stream.empty();
    }

    return assets.stream()
        .filter(assetMap -> assetMapUtils.filterAsset(assetMap, assetParams))
        .map(asset -> fromElasticSearchMap(asset, repository));
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
