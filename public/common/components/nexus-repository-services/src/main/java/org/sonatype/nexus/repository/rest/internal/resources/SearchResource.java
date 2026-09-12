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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
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
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;

import com.google.common.annotations.VisibleForTesting;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.sonatype.nexus.repository.search.SearchUtils.CONTINUATION_TOKEN;
import static org.sonatype.nexus.repository.search.SearchUtils.SORT_DIRECTION;
import static org.sonatype.nexus.repository.search.SearchUtils.SORT_FIELD;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import static org.sonatype.nexus.security.internal.uploadermetadata.UploaderMetadataSecurityContributor.UPLOADER_METADATA_READ_PERMISSION;

/**
 * @since 3.4
 */
@Component
@Path(SearchResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SearchResource
    implements Resource, SearchResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String RESOURCE_URI = V1_API_PREFIX + "/search";

  public static final String SEARCH_ASSET_URI = "/assets";

  public static final String SEARCH_AND_DOWNLOAD_URI = "/assets/download";

  /**
   * Safety cap on continuation-token paging of the component-wide count query. 20 * 50 = 1000 rows,
   * larger than any real component's cross-repository/version footprint, small enough to keep
   * request latency bounded even on a runaway continuation-token chain.
   */
  private static final int COUNT_QUERY_PAGE_CAP = 20;

  private final SearchUtils searchUtils;

  private final SearchResultFilterUtils searchResultFilterUtils;

  private final SearchService searchService;

  private final ComponentXOFactory componentXOFactory;

  private final Set<SearchResourceExtension> searchResourceExtensions;

  private final Map<String, AssetXODescriptor> assetDescriptors;

  private final EventManager eventManager;

  private final RepositoryManager repositoryManager;

  private final SecurityHelper securityHelper;

  private int pageSize = 50;

  @Autowired
  public SearchResource(
      final SearchUtils searchUtils,
      final SearchResultFilterUtils searchResultFilterUtils,
      final SearchService searchService,
      final ComponentXOFactory componentXOFactory,
      final Set<SearchResourceExtension> searchResourceExtensions,
      final EventManager eventManager,
      final RepositoryManager repositoryManager,
      final SecurityHelper securityHelper,
      @Nullable final List<AssetXODescriptor> assetDescriptorsList)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.searchResultFilterUtils = checkNotNull(searchResultFilterUtils);
    this.searchService = checkNotNull(searchService);
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.searchResourceExtensions = checkNotNull(searchResourceExtensions);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.securityHelper = checkNotNull(securityHelper);
    this.assetDescriptors =
        assetDescriptorsList != null ? QualifierUtil.buildQualifierBeanMap(assetDescriptorsList) : null;
    this.eventManager = checkNotNull(eventManager);
  }

  @RequiresUser
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
    List<SearchFilter> searchFilters = searchUtils.getComponentSearchFilters(uriInfo);
    Function<String, String> repositoryNames = tryExtractRepositoryFromSearch(searchFilters);

    // Get asset parameters to filter assets within components
    MultivaluedMap<String, String> assetParams = getAssetParams(uriInfo);

    boolean uploaderVisible = SecurityUtils.getSubject().isPermitted(UPLOADER_METADATA_READ_PERMISSION);
    List<ComponentXO> componentXOs = response.getSearchResults()
        .stream()
        .map(componentHit -> this.toComponent(componentHit, repositoryNames, assetParams, uploaderVisible))
        .filter(Objects::nonNull)
        // Filter out components with no assets when asset parameters are specified
        .filter(component -> assetParams.isEmpty() ||
            component.getAssets() != null && !component.getAssets().isEmpty())
        .collect(toList());

    return new Page<>(componentXOs, response.getContinuationToken());
  }

  /**
   * @since 3.6.1
   */
  @RequiresUser
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
  @RequiresUser
  @GET
  @Path(SEARCH_AND_DOWNLOAD_URI)
  @Override
  public Response searchAndDownloadAssets(
      @QueryParam(SORT_FIELD) final String sort,
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

    Function<String, String> repositoryNames =
        tryExtractRepositoryFromSearch(searchUtils.getComponentSearchFilters(uriInfo));

    // Filter Assets by the criteria
    boolean uploaderVisible = SecurityUtils.getSubject().isPermitted(UPLOADER_METADATA_READ_PERMISSION);
    List<AssetXO> assets = response.getSearchResults()
        .stream()
        .flatMap(component -> searchResultFilterUtils.filterComponentAssets(component, assetParams))
        .flatMap(asset -> {
          try {
            Repository repo = searchUtils.getRepository(repositoryNames.apply(asset.getRepository()));
            return Stream.of(AssetXO.from(asset, repo, assetDescriptors, uploaderVisible));
          }
          catch (NotFoundException e) {
            log.debug("Skipping asset '{}' — repository '{}' no longer exists",
                asset.getPath(), asset.getRepository());
            return Stream.empty();
          }
        })
        .collect(toList());

    return new Page<>(assets, response.getContinuationToken());
  }

  @VisibleForTesting
  SearchResponse doSearch(
      final String continuationToken,
      final String sort,
      final String direction,
      final Integer seconds,
      final UriInfo uriInfo)
  {
    // All search params (component + asset filters) are included in the SQL search for more accurate results,
    // the search records include asset specific fields such as asset checksums, that can be used for filtering at the
    // component level.
    // A second round of asset-level filtering can be performed later via
    // SearchResultFilterUtils.filterComponentAssets()
    // if the desire is to filter at asset level only.
    List<SearchFilter> searchFilters = searchUtils.getSearchFilters(uriInfo);

    fireSearchEvent(searchFilters);

    SearchRequest request = SearchRequest.builder()
        .searchFilters(searchFilters)
        .continuationToken(continuationToken)
        .limit(getPageSize())
        .sortField(sort)
        .sortDirection(Optional.ofNullable(direction)
            .filter(dir -> !dir.isBlank())
            .map(String::toUpperCase)
            .map(this::parseSortDirection)
            .orElse(null))
        .includeAssets()
        .build();

    return searchService.search(request);
  }

  private SortDirection parseSortDirection(final String direction) {
    try {
      return SortDirection.valueOf(direction);
    }
    catch (IllegalArgumentException e) {
      throw new WebApplicationMessageException(Response.Status.BAD_REQUEST,
          format("\"%s\" is not a valid sort direction. Supported values are: ASC, DESC", direction),
          APPLICATION_JSON);
    }
  }

  // Returns null if the repository no longer exists (deleted after search index was built); callers must
  // filter(Objects::nonNull).
  @Nullable
  private ComponentXO toComponent(
      final ComponentSearchResult componentHit,
      final Function<String, String> repositoryAccessMap,
      final MultivaluedMap<String, String> assetParams,
      final boolean uploaderVisible)
  {
    ComponentXO componentXO = componentXOFactory.createComponentXO();
    Repository repository;
    try {
      repository = searchUtils.getRepository(repositoryAccessMap.apply(componentHit.getRepositoryName()));
    }
    catch (NotFoundException e) {
      log.debug("Skipping component '{}' — repository '{}' no longer exists",
          componentHit.getName(), componentHit.getRepositoryName());
      return null;
    }

    componentXO.setGroup(componentHit.getGroup());
    componentXO.setName(componentHit.getName());
    componentXO.setVersion(componentHit.getVersion());
    componentXO.setId(new RepositoryItemIDXO(componentHit.getRepositoryName(), componentHit.getId()).getValue());
    componentXO.setRepository(componentHit.getRepositoryName());
    componentXO.setFormat(componentHit.getFormat());

    List<AssetXO> assets = searchResultFilterUtils.filterComponentAssets(componentHit, assetParams)
        .map(asset -> AssetXO.from(asset, repository, assetDescriptors, uploaderVisible))
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
        .entrySet()
        .stream()
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

  /**
   * If we have specified a specific Repository for a Search, extract the name of that Repository.
   * This is necessary to ensure that downloadUrls for Group Repositories don't expose details of internal members.
   */
  @VisibleForTesting
  Function<String, String> tryExtractRepositoryFromSearch(final List<SearchFilter> searchFilters) {
    // Handle repositoryName="foo or bar"
    Map<String, String> nameMap = searchFilters
        .stream()
        .filter(filter -> filter.getProperty().equals("repository_name"))
        .findFirst()
        .map(SearchFilter::getValue)
        .map(filter -> Stream.of(filter.split(" ")))
        .orElseGet(Stream::empty)
        .map(repositoryManager::get)
        .filter(Objects::nonNull)
        .map(Repository::getName)
        .collect(Collectors.toMap(Function.identity(), Function.identity()));

    // If any of the specified repositories are groups then add leaf members missing from the map referencing the group
    for (String repositoryName : Set.copyOf(nameMap.keySet())) {
      Repository repository = repositoryManager.get(repositoryName);
      repository.optionalFacet(GroupFacet.class).ifPresent(groupFacet -> {
        groupFacet.leafMembers()
            .stream()
            .map(Repository::getName)
            .forEach(memberName -> nameMap.putIfAbsent(memberName, repositoryName));
      });
    }
    return repositoryName -> nameMap.getOrDefault(repositoryName, repositoryName);
  }

  @RequiresUser
  @GET
  @Path("/repositories")
  public RepositoriesForVersionResponse searchRepositoriesForVersion(
      @QueryParam("format") final String format,
      @QueryParam("namespace") final String namespace,
      @QueryParam("name") final String name,
      @QueryParam("version") final String version)
  {
    requireNonBlank(format, "format");
    requireNonBlank(name, "name");
    // version must be present but may be blank: '' is the version raw and every other versionless
    // format actually stores, and an exact `version = ''` predicate matches precisely those rows.
    // Blank is a value to query for, not a value to reject. Absent (null) is still a caller bug.
    requirePresent(version, "version");
    // namespace intentionally allowed blank — raw and other namespace-less formats
    String ns = namespace == null ? "" : namespace;

    // 1. Row set: repositories containing (format, namespace, name, version).
    SearchRequest versionScoped = SearchRequest.builder()
        .searchFilters(List.of(
            new SearchFilter("format", format),
            new SearchFilter("namespace", ns),
            new SearchFilter("name", name),
            new SearchFilter("version", version)))
        .limit(getPageSize())
        .build();
    SearchResponse versionRows = searchService.search(versionScoped);

    // 2. Component-wide per-repository distinct-version counts — paged.
    Map<String, Set<String>> perRepoVersions = new HashMap<>();
    String continuationToken = null;
    int page = 0;
    do {
      SearchRequest componentScoped = SearchRequest.builder()
          .searchFilters(List.of(
              new SearchFilter("format", format),
              new SearchFilter("namespace", ns),
              new SearchFilter("name", name)))
          .continuationToken(continuationToken)
          .limit(getPageSize())
          .build();
      SearchResponse allVersionRows = searchService.search(componentScoped);
      for (ComponentSearchResult r : resultsOrEmpty(allVersionRows)) {
        perRepoVersions.computeIfAbsent(r.getRepositoryName(), k -> new HashSet<>())
            .add(r.getVersion());
      }
      continuationToken = allVersionRows.getContinuationToken();
      page++;
    }
    while (continuationToken != null && page < COUNT_QUERY_PAGE_CAP);
    if (page >= COUNT_QUERY_PAGE_CAP && continuationToken != null) {
      log.warn("Version-count paging hit safety cap of {} pages for {}:{}:{}",
          COUNT_QUERY_PAGE_CAP, forLog(format), forLog(ns), forLog(name));
    }
    Map<String, Long> perRepoVersionCounts = perRepoVersions.entrySet()
        .stream()
        .collect(toMap(Entry::getKey, e -> (long) e.getValue().size()));

    // 3. Assemble: dedup by repositoryName, resolve type via RepositoryManager,
    // filter out any repository that no longer exists.
    List<RepositoryForVersion> items = resultsOrEmpty(versionRows)
        .stream()
        .map(ComponentSearchResult::getRepositoryName)
        .distinct()
        .map(repoName -> {
          Repository repo = repositoryManager.get(repoName);
          if (repo == null) {
            return null;
          }
          if (!securityHelper.anyPermitted(
              new RepositoryViewPermission(repo.getFormat().getValue(), repo.getName(), BreadActions.BROWSE))) {
            return null;
          }
          return new RepositoryForVersion(
              repoName,
              repo.getType().getValue(),
              perRepoVersionCounts.getOrDefault(repoName, 0L));
        })
        .filter(Objects::nonNull)
        .sorted(comparing(RepositoryForVersion::repositoryName))
        .collect(toList());

    return new RepositoriesForVersionResponse(items, items.size());
  }

  private static List<ComponentSearchResult> resultsOrEmpty(final SearchResponse response) {
    List<ComponentSearchResult> results = response.getSearchResults();
    return results != null ? results : List.of();
  }

  private static void requireNonBlank(final String value, final String paramName) {
    if (value == null || value.isBlank()) {
      throw new WebApplicationMessageException(Response.Status.BAD_REQUEST,
          format("Missing required parameter: %s", paramName),
          APPLICATION_JSON);
    }
  }

  /**
   * Like {@link #requireNonBlank} but accepts the empty string, for parameters whose blank value is
   * meaningful rather than missing. Deliberately not Guava's {@code checkNotNull} — that raises an
   * NPE, which surfaces to the caller as a 500 for what is a malformed request.
   */
  private static void requirePresent(final String value, final String paramName) {
    if (value == null) {
      throw new WebApplicationMessageException(Response.Status.BAD_REQUEST,
          format("Missing required parameter: %s", paramName),
          APPLICATION_JSON);
    }
  }

  private static String forLog(final String value) {
    return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
  }
}
