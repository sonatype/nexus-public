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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

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
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.SearchResourceExtension;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.SearchResourceDoc;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SearchUtils;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.event.SearchEvent;
import org.sonatype.nexus.repository.search.event.SearchEventSource;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.search.SearchUtils.CONTINUATION_TOKEN;
import static org.sonatype.nexus.repository.search.SearchUtils.SORT_DIRECTION;
import static org.sonatype.nexus.repository.search.SearchUtils.SORT_FIELD;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.4
 */
@Named
@Singleton
@Path(SearchResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@FeatureFlag(name = FeatureFlags.ELASTIC_SEARCH_ENABLED, enabledByDefault = true)
public class SearchResource
    extends ComponentSupport
    implements Resource, SearchResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/search";

  public static final String SEARCH_ASSET_URI = "/assets";

  public static final String SEARCH_AND_DOWNLOAD_URI = "/assets/download";

  private final SearchUtils searchUtils;

  private final SearchResultFilterUtils searchResultFilterUtils;

  private final SearchService searchService;

  private final ComponentXOFactory componentXOFactory;

  private final Set<SearchResourceExtension> searchResourceExtensions;

  private final Map<String, AssetXODescriptor> assetDescriptors;

  private final EventManager eventManager;

  private int pageSize = 50;

  @Inject
  public SearchResource(
      final SearchUtils searchUtils,
      final SearchResultFilterUtils searchResultFilterUtils,
      final SearchService searchService,
      final ComponentXOFactory componentXOFactory,
      final Set<SearchResourceExtension> searchResourceExtensions,
      final EventManager eventManager,
      @Nullable final Map<String, AssetXODescriptor> assetDescriptors)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.searchResultFilterUtils = checkNotNull(searchResultFilterUtils);
    this.searchService = checkNotNull(searchService);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.searchResourceExtensions = checkNotNull(searchResourceExtensions);
    this.assetDescriptors = assetDescriptors;
    this.eventManager = checkNotNull(eventManager);
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
    SearchResponse response = doSearch(continuationToken, sort, direction, seconds, uriInfo);

    List<ComponentXO> componentXOs = response.getSearchResults().stream()
        .map(this::toComponent)
        .collect(toList());

    return new Page<>(componentXOs, response.getContinuationToken());
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
    return assetSearch(continuationToken, sort, direction, seconds, uriInfo);
  }

  /**
   * @since 3.7
   */
  @GET
  @Path(SEARCH_AND_DOWNLOAD_URI)
  @Override
  public Response searchAndDownloadAssets(@QueryParam(SORT_FIELD) final String sort,
                                          @QueryParam(SORT_DIRECTION) final String direction,
                                          @QueryParam("timeout") final Integer seconds,
                                          @Context final UriInfo uriInfo)
  {
    Page<AssetXO> assets = assetSearch(null, sort, direction, seconds, uriInfo);

    return new AssetDownloadResponseProcessor(assets.getItems(), !Strings2.isEmpty(sort)).process();
  }

  private Page<AssetXO> assetSearch(
      final String continuationToken,
      final String sort,
      final String direction,
      final Integer seconds,
      final UriInfo uriInfo)
  {
    SearchResponse response = doSearch(continuationToken, sort, direction, seconds, uriInfo);

    if (response.getSearchResults().isEmpty()) {
      return new Page<>(Collections.emptyList(), null);
    }

    MultivaluedMap<String, String> assetParams = getAssetParams(uriInfo);

    // Filter Assets by the criteria
    List<AssetXO> assets = response.getSearchResults().stream()
        .flatMap(component -> searchResultFilterUtils.filterComponentAssets(component, assetParams))
        .map(asset -> AssetXO.from(asset, searchUtils.getRepository(asset.getRepository()), assetDescriptors))
        .collect(toList());

    return new Page<>(assets, response.getContinuationToken());
  }

  private SearchResponse doSearch(
      final String continuationToken,
      final String sort,
      final String direction,
      final Integer seconds,
      final UriInfo uriInfo)
  {
    List<SearchFilter> searchFilters = searchUtils.getSearchFilters(uriInfo);
    fireSearchEvent(searchFilters);

    SearchRequest request = SearchRequest.builder()
        .searchFilters(searchFilters)
        .continuationToken(continuationToken)
        .limit(getPageSize())
        .sortField(sort)
        .sortDirection(Optional.ofNullable(direction)
            .map(String::toUpperCase)
            .map(SortDirection::valueOf)
            .orElse(null))
        .includeAssets()
        .build();

    return searchService.search(request);
  }

  private ComponentXO toComponent(final ComponentSearchResult componentHit) {
    ComponentXO componentXO = componentXOFactory.createComponentXO();
    Repository repository = searchUtils.getRepository(componentHit.getRepositoryName());

    componentXO.setGroup(componentHit.getGroup());
    componentXO.setName(componentHit.getName());
    componentXO.setVersion(componentHit.getVersion());
    componentXO.setId(new RepositoryItemIDXO(repository.getName(), componentHit.getId()).getValue());
    componentXO.setRepository(componentHit.getRepositoryName());
    componentXO.setFormat(componentHit.getFormat());

    List<AssetXO> assets = componentHit.getAssets().stream()
       .map(asset -> AssetXO.from(asset, repository, assetDescriptors))
       .collect(toList());
    componentXO.setAssets(assets);
    for (SearchResourceExtension searchResourceExtension : searchResourceExtensions) {
      componentXO = searchResourceExtension.updateComponentXO(componentXO, componentHit);
    }

    return componentXO;
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

  private void fireSearchEvent(final Collection<SearchFilter> searchFilters) {
    eventManager.post(new SearchEvent(searchFilters, SearchEventSource.REST));
  }
}
