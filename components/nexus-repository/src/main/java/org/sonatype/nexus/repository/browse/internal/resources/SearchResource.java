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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.api.AssetXO;
import org.sonatype.nexus.repository.browse.api.ComponentXO;
import org.sonatype.nexus.repository.browse.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.browse.internal.resources.doc.SearchResourceDoc;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.search.SearchMapping;
import org.sonatype.nexus.repository.search.SearchMappings;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.shiro.util.CollectionUtils.isEmpty;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonatype.nexus.repository.browse.api.AssetXO.fromAsset;
import static org.sonatype.nexus.repository.browse.api.AssetXO.fromElasicSearchMap;
import static org.sonatype.nexus.repository.browse.internal.resources.AssetMapUtils.getValueFromAssetMap;
import static org.sonatype.nexus.repository.browse.internal.resources.SearchResource.RESOURCE_URI;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @since 3.4
 */
@Named
@Singleton
@Path(RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SearchResource
    extends ComponentSupport
    implements Resource, SearchResourceDoc
{
  public static final String RESOURCE_URI = BETA_API_PREFIX + "/search";

  public static final String SEARCH_ASSET_URI = "/assets";

  public static final String SEARCH_AND_DOWNLOAD_URI = "/assets/download";

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final BrowseService browseService;

  private final SearchService searchService;

  private static final int PAGE_SIZE = 50;

  private final Type groupType;

  private final TokenEncoder tokenEncoder;

  private final Map<String, String> searchParams;

  private final Map<String, String> assetSearchParams;

  @Inject
  public SearchResource(final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
                        final BrowseService browseService,
                        final SearchService searchService,
                        @Named(GroupType.NAME) final Type groupType,
                        final TokenEncoder tokenEncoder,
                        final Map<String, SearchMappings> searchMappings)
  {
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.browseService = checkNotNull(browseService);
    this.searchService = checkNotNull(searchService);
    this.groupType = checkNotNull(groupType);
    this.tokenEncoder = checkNotNull(tokenEncoder);
    this.searchParams = checkNotNull(searchMappings).entrySet().stream()
        .flatMap(e -> stream(e.getValue().get().spliterator(), true))
        .collect(toMap(SearchMapping::getAlias, SearchMapping::getAttribute));
    this.assetSearchParams = searchParams.entrySet().stream()
        .filter(e -> e.getValue().startsWith("assets."))
        .collect(toMap(Entry::getKey, Entry::getValue));
  }

  Map<String, String> getSearchParams() {
    return searchParams;
  }

  Map<String, String> getAssetSearchParams() {
    return assetSearchParams;
  }

  @GET
  public Page<ComponentXO> search(
      @QueryParam("continuationToken") final String continuationToken,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = buildQuery(uriInfo);

    int from = tokenEncoder.decode(continuationToken, query);
    SearchResponse response = searchService.search(query, emptyList(), from, PAGE_SIZE);

    List<ComponentXO> componentXOs = Arrays.stream(response.getHits().hits())
        .map(this::toComponent)
        .collect(toList());

    return new Page<>(componentXOs, componentXOs.size() == PAGE_SIZE ?
        tokenEncoder.encode(from, PAGE_SIZE, query) : null);
  }

  private ComponentXO toComponent(final SearchHit hit) {
    Map<String, Object> source = checkNotNull(hit.getSource());
    Repository repository = repositoryManagerRESTAdapter.getRepository((String) source.get(REPOSITORY_NAME));
    ComponentXO componentXO = new ComponentXO();

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

    return componentXO;
  }

  /**
   * @since 3.6.1
   */
  @GET
  @Path(SEARCH_ASSET_URI)
  public Page<AssetXO> searchAssets(
      @QueryParam("continuationToken") final String continuationToken,
      @Context final UriInfo uriInfo)
  {
    QueryBuilder query = buildQuery(uriInfo);

    int from = tokenEncoder.decode(continuationToken, query);

    List<AssetXO> assetXOs = retrieveAssets(query, uriInfo, from);
    return new Page<>(assetXOs, assetXOs.size() == PAGE_SIZE ?
        tokenEncoder.encode(from, PAGE_SIZE, query) : null);
  }

  /**
   * @since 3.7
   */
  @GET
  @Path(SEARCH_AND_DOWNLOAD_URI)
  public Response searchAndDownloadAssets(@Context final UriInfo uriInfo)
  {
    QueryBuilder query = buildQuery(uriInfo);

    List<AssetXO> assetXOs = retrieveAssets(query, uriInfo);

    return new AssetDownloadResponseProcessor(assetXOs).process();
  }

  /**
   * Builds a {@link QueryBuilder} based on configured search parameters.
   *
   * @param uriInfo {@link UriInfo} to extract query parameters from
   */
  @VisibleForTesting
  QueryBuilder buildQuery(final UriInfo uriInfo) {
    BoolQueryBuilder query = boolQuery();

    MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

    if (queryParams.containsKey("q")) {
      query.must(queryStringQuery(queryParams.getFirst("q")));
    }

    queryParams.forEach((key, value) -> {
      if (value.isEmpty() || value.get(0).isEmpty()) {
        // no value sent
        return;
      }
      if ("q".equals(key)) {
        // skip the keyword search
        return;
      }
      if ("continuationToken".equals(key)) {
        // skip the continuation token
        return;
      }
      if ("repository".equals(key)) {
        Repository repository = repositoryManagerRESTAdapter.getRepository(value.get(0));
        if (groupType.equals(repository.getType())) {
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

  private List<AssetXO> retrieveAssets(final QueryBuilder query, final UriInfo uriInfo, final int from) {
    SearchResponse response = searchService.search(query, emptyList(), from, PAGE_SIZE);

    // get the asset specific parameters
    MultivaluedMap<String, String> assetParams = getAssetParams(uriInfo);

    return Arrays.stream(response.getHits().hits())
        .flatMap(hit -> extractAssets(hit, assetParams))
        .collect(toList());
  }

  private List<AssetXO> retrieveAssets(final QueryBuilder query, final UriInfo uriInfo) {
    return this.retrieveAssets(query, uriInfo, 0);
  }

  @SuppressWarnings("unchecked")
  private Stream<AssetXO> extractAssets(final SearchHit componentHit,
                                        final MultivaluedMap<String, String> assetParams)
  {
    Map<String, Object> componentMap = checkNotNull(componentHit.getSource());
    Repository repository = repositoryManagerRESTAdapter.getRepository((String) componentMap.get(REPOSITORY_NAME));

    List<Map<String, Object>> assets = (List<Map<String, Object>>) componentMap.get("assets");
    if (assets == null) {
      return Stream.empty();
    }

    return assets.stream()
        .filter(assetMap -> filterAsset(assetMap, assetParams))
        .map(asset -> fromElasicSearchMap(asset, repository));
  }

  @VisibleForTesting
  boolean filterAsset(final Map<String, Object> assetMap, final MultivaluedMap<String, String> assetParams) {
    // if no asset parameters were sent, we'll count that as return all assets
    if (isEmpty(assetParams)) {
      return true;
    }

    AtomicBoolean assetValueFound = new AtomicBoolean(false);

    // loop each asset specific http query parameter to filter out assets that do not apply
    assetParams.forEach((key, values) -> {
      String assetParam = assetSearchParams.get(key);

      // does the assetMap contain the requested value?
      getValueFromAssetMap(assetMap, assetParam).ifPresent(assetValue -> {
        if (assetValue.equals(values.get(0))) {
          assetValueFound.set(true);
        }
      });
    });

    return assetValueFound.get();
  }

  @VisibleForTesting
  MultivaluedMap<String, String> getAssetParams(final UriInfo uriInfo) {
    return uriInfo.getQueryParameters()
        .entrySet().stream()
        .filter(t -> assetSearchParams.containsKey(t.getKey()))
        .collect(toMap(Entry::getKey, Entry::getValue, (u, v) -> {
          throw new IllegalStateException(format("Duplicate key %s", u));
        }, MultivaluedHashMap::new));
  }

}
