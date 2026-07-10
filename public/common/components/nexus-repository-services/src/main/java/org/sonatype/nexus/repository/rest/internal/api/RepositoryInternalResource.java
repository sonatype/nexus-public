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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import java.util.HashMap;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonatype.nexus.security.BreadActions.READ;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsDTO;

/**
 * @since 3.29
 */
@Component
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RepositoryInternalResource.RESOURCE_PATH)
public class RepositoryInternalResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String RESOURCE_PATH = "internal/ui/repositories";

  static final RepositoryXO ALL_REFERENCE = new RepositoryXO(
      RepositorySelector.all().toSelector(),
      "(All Repositories)");

  static final String ALL_FORMATS = "*";

  private static final Set<String> ALLOWED_FACET_PACKAGES = Set.of("org.sonatype.nexus.", "com.sonatype.nexus.");

  private final List<Format> formats;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final ProxyType proxyType;

  private final List<Recipe> recipes;

  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final Map<String, ApiRepositoryAdapter> convertersByFormat;

  private final ApiRepositoryAdapter defaultAdapter;

  /*
   * Optional because RepositoryMetricsServiceImpl lives in nexus-pro-datastore-plugin and is not
   * present in OSS distributions (e.g. nexus-repository-core). Mirrors the @Nullable pattern used
   * by RepositoryManagerRESTAdapterImpl and RepositoryUiService for the same dependency. Without
   * this, the OSS core distribution fails to start because Spring cannot find a bean of type
   * RepositoryMetricsService.
   */
  private final Optional<RepositoryMetricsService> repositoryMetricsService;

  @Autowired
  public RepositoryInternalResource(
      final List<Format> formats,
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final ProxyType proxyType,
      final List<Recipe> recipes,
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final List<ApiRepositoryAdapter> convertersByFormatList,
      @Qualifier("default") final ApiRepositoryAdapter defaultAdapter,
      @Nullable final RepositoryMetricsService repositoryMetricsService)
  {
    this.formats = checkNotNull(formats);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.proxyType = checkNotNull(proxyType);
    this.recipes = checkNotNull(recipes);
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.convertersByFormat = QualifierUtil.buildQualifierBeanMap(checkNotNull(convertersByFormatList));
    this.defaultAdapter = checkNotNull(defaultAdapter);
    this.repositoryMetricsService = Optional.ofNullable(repositoryMetricsService);
  }

  @GET
  @RequiresAuthentication
  public List<RepositoryXO> getRepositories(
      @QueryParam("type") final String type,
      @QueryParam("withAll") final boolean withAll,
      @QueryParam("withFormats") final boolean withFormats,
      @QueryParam("format") final String formatParam,
      @QueryParam("facets") final String facetsParam,
      @QueryParam("versionPolicies") final String versionPoliciesParam)
  {
    // Parse facets filter (comma-separated fully-qualified class names)
    List<Class<? extends Facet>> facetClasses = parseFacets(facetsParam);
    // Parse type filter (comma-separated, with ! prefix for exclusions)
    List<String> typeIncludes = parseIncludes(type);
    List<String> typeExcludes = parseExcludes(type);
    // Parse format filter (comma-separated, with ! prefix for exclusions)
    // ALL_FORMATS ("*") means no format filtering
    final boolean allFormats = formatParam != null && formatParam.equals(ALL_FORMATS);
    List<String> formatIncludes = allFormats ? Collections.emptyList() : parseIncludes(formatParam);
    List<String> formatExcludes = allFormats ? Collections.emptyList() : parseExcludes(formatParam);
    // Parse version-policies filter (Maven-only attribute; non-Maven repos have a null value).
    List<String> versionPolicyIncludes = parseIncludes(versionPoliciesParam);
    List<String> versionPolicyExcludes = parseExcludes(versionPoliciesParam);

    List<RepositoryXO> repositories = repositoryPermissionChecker.userCanBrowseRepositories(repositoryManager.browse())
        .stream()
        .filter(repository -> matchesFilter(repository.getType().getValue(), typeIncludes, typeExcludes))
        .filter(repository -> matchesFilter(repository.getFormat().getValue(), formatIncludes, formatExcludes))
        .filter(repository -> facetClasses.isEmpty() || hasAnyFacet(repository, facetClasses))
        .filter(repository -> matchesNullableFilter(getVersionPolicy(repository), versionPolicyIncludes,
            versionPolicyExcludes))
        .map(repository -> new RepositoryXO(repository.getName(), repository.getName()))
        .sorted(Comparator.comparing(RepositoryXO::getName))
        .collect(toList());

    List<RepositoryXO> result = new ArrayList<>();
    if (withAll) {
      result.add(ALL_REFERENCE);
    }
    if (withFormats) {
      formats.stream()
          .map(format -> new RepositoryXO(
              RepositorySelector.allOfFormat(format.getValue()).toSelector(),
              "(All " + format.getValue() + " Repositories)"))
          .sorted(Comparator.comparing(RepositoryXO::getName))
          .forEach(result::add);
    }
    result.addAll(repositories);

    return result;
  }

  /**
   * Parse comma-separated facet class names into Class objects.
   */
  @SuppressWarnings("unchecked")
  private List<Class<? extends Facet>> parseFacets(final String facetsParam) {
    if (isBlank(facetsParam)) {
      return Collections.emptyList();
    }
    List<Class<? extends Facet>> result = new ArrayList<>();
    for (String facetName : facetsParam.split(",")) {
      String trimmed = facetName.trim();
      if (!trimmed.isEmpty()) {
        if (ALLOWED_FACET_PACKAGES.stream().noneMatch(trimmed::startsWith)) {
          log.warn("Facet class name '{}' is not in an allowed package", trimmed);
          continue;
        }
        try {
          Class<?> clazz = Class.forName(trimmed);
          if (Facet.class.isAssignableFrom(clazz)) {
            result.add((Class<? extends Facet>) clazz);
          }
          else {
            log.warn("Class {} is not a Facet", trimmed);
          }
        }
        catch (ClassNotFoundException e) {
          log.warn("Facet class not found: {}", trimmed);
        }
      }
    }
    return result;
  }

  /**
   * Check if a repository has any of the specified facets.
   */
  private boolean hasAnyFacet(final Repository repository, final List<Class<? extends Facet>> facetClasses) {
    for (Class<? extends Facet> facetClass : facetClasses) {
      try {
        repository.facet(facetClass);
        return true;
      }
      catch (MissingFacetException e) {
        // Facet not present, try next
      }
    }
    return false;
  }

  @GET
  @Path("/repository/{repositoryName}")
  @RequiresAuthentication
  public AbstractApiRepository getRepository(@PathParam("repositoryName") final String repositoryName) {
    return authorizingRepositoryManager.getRepositoryWithAdmin(repositoryName)
        .map(repository -> convertersByFormat.getOrDefault(repository.getFormat().getValue(), defaultAdapter)
            .adaptDecorated(repository))
        .get();
  }

  /**
   * Returns the signing passphrase for a repository (admin-only, internal endpoint).
   * Used by the admin UI to populate password fields on edit without exposing secrets via public API.
   */
  @GET
  @Path("/repository/{repositoryName}/signing-passphrase")
  @RequiresAuthentication
  public Map<String, String> getSigningPassphrase(@PathParam("repositoryName") final String repositoryName) {
    Repository repository = authorizingRepositoryManager.getRepositoryWithAdmin(repositoryName).get();
    String format = repository.getFormat().getValue();
    Map<String, String> result = new HashMap<>();

    if ("apt".equals(format)) {
      NestedAttributesMap attrs = repository.getConfiguration().attributes("aptSigning");
      String passphrase = attrs.get("passphrase", String.class);
      result.put("passphrase", passphrase);
    }
    else if ("yum".equals(format)) {
      NestedAttributesMap attrs = repository.getConfiguration().attributes("yumSigning");
      String passphrase = attrs.get("passphrase", String.class);
      result.put("passphrase", passphrase);
    }

    return result;
  }

  @GET
  @Path("/details")
  public List<RepositoryDetailXO> getRepositoryDetails() {
    Map<String, RepositoryMetricsDTO> metricsByName = repositoryMetricsService
        .map(svc -> svc.list().stream().collect(Collectors.toMap(RepositoryMetricsDTO::getName, m -> m)))
        .orElseGet(Map::of);

    return stream(repositoryManager.browse())
        .filter(repository -> repositoryPermissionChecker.userHasRepositoryAdminPermission(repository, READ))
        .map(repository -> asRepositoryDetail(repository, metricsByName.get(repository.getName())))
        .collect(toList());
  }

  /**
   * Get repository details with server-side filtering, sorting, and pagination.
   * This endpoint is optimized for enterprise-scale deployments with many repositories.
   *
   * @param formats Comma-separated list of formats to filter by (e.g., "maven2,npm")
   * @param types Comma-separated list of types to filter by (e.g., "hosted,proxy")
   * @param statuses Comma-separated list of statuses to filter by ("online" or "offline")
   * @param nameFilter Text to filter repository names (case-insensitive substring match)
   * @param sortField Field to sort by: "name", "format", "type", or "status"
   * @param sortDirection Sort direction: "asc" or "desc"
   * @param page Page number (1-indexed)
   * @param pageSize Number of items per page (default 50, max 200)
   * @return Paginated list of repository details with total count
   */
  @GET
  @Path("/details/filtered")
  public RepositoryDetailPageXO getRepositoryDetailsFiltered(
      @QueryParam("formats") final String formats,
      @QueryParam("types") final String types,
      @QueryParam("statuses") final String statuses,
      @QueryParam("nameFilter") final String nameFilter,
      @QueryParam("sortField") final String sortField,
      @QueryParam("sortDirection") final String sortDirection,
      @QueryParam("page") final Integer page,
      @QueryParam("pageSize") final Integer pageSize)
  {
    // Parse filter parameters
    List<String> formatList = parseCommaSeparated(formats);
    List<String> typeList = parseCommaSeparated(types);
    List<String> statusList = parseCommaSeparated(statuses);

    Map<String, RepositoryMetricsDTO> metricsByName = repositoryMetricsService
        .map(svc -> svc.list().stream().collect(Collectors.toMap(RepositoryMetricsDTO::getName, m -> m)))
        .orElseGet(Map::of);

    // Build filtered stream - use userCanBrowseRepositories to allow anonymous access
    List<RepositoryDetailXO> allRepos =
        repositoryPermissionChecker.userCanBrowseRepositories(repositoryManager.browse())
            .stream()
            .map(repo -> asRepositoryDetail(repo, metricsByName.get(repo.getName())))
            .filter(repo -> filterByFormats(repo, formatList))
            .filter(repo -> filterByTypes(repo, typeList))
            .filter(repo -> filterByStatuses(repo, statusList))
            .filter(repo -> filterByName(repo, nameFilter))
            .collect(toList());

    // Sort
    Comparator<RepositoryDetailXO> comparator = getComparator(sortField, sortDirection);
    allRepos.sort(comparator);

    // Calculate pagination
    int totalCount = allRepos.size();
    int actualPage = (page != null && page > 0) ? page : 1;
    int actualPageSize = Math.min((pageSize != null && pageSize > 0) ? pageSize : 50, 200);
    int fromIndex = (actualPage - 1) * actualPageSize;
    int toIndex = Math.min(fromIndex + actualPageSize, totalCount);

    // Extract page
    List<RepositoryDetailXO> pageData = (fromIndex < totalCount)
        ? allRepos.subList(fromIndex, toIndex)
        : new ArrayList<>();

    return new RepositoryDetailPageXO(pageData, totalCount, actualPage, actualPageSize);
  }

  private List<String> parseCommaSeparated(String value) {
    if (isBlank(value)) {
      return new ArrayList<>();
    }
    List<String> result = new ArrayList<>();
    for (String part : value.split(",")) {
      String trimmed = part.trim().toLowerCase();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
  }

  private List<String> parseIncludes(String value) {
    if (isBlank(value)) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (String part : value.split(",")) {
      String trimmed = part.trim().toLowerCase();
      if (!trimmed.isEmpty() && !trimmed.startsWith("!")) {
        result.add(trimmed);
      }
    }
    return result;
  }

  private List<String> parseExcludes(String value) {
    if (isBlank(value)) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (String part : value.split(",")) {
      String trimmed = part.trim().toLowerCase();
      if (trimmed.startsWith("!") && trimmed.length() > 1) {
        result.add(trimmed.substring(1));
      }
    }
    return result;
  }

  private boolean matchesFilter(String value, List<String> includes, List<String> excludes) {
    String lowerValue = value.toLowerCase();
    if (!excludes.isEmpty() && excludes.contains(lowerValue)) {
      return false;
    }
    if (!includes.isEmpty()) {
      return includes.contains(lowerValue);
    }
    return true;
  }

  /**
   * Same as {@link #matchesFilter} but tolerates a null value. A null value passes
   * exclude-only filters (matching the classic UI's RepositoryUiService.filterIn semantics
   * for non-Maven repos when filtering by versionPolicy) and fails include filters.
   */
  private boolean matchesNullableFilter(String value, List<String> includes, List<String> excludes) {
    String lowerValue = value == null ? null : value.toLowerCase();
    if (lowerValue != null && !excludes.isEmpty() && excludes.contains(lowerValue)) {
      return false;
    }
    if (!includes.isEmpty()) {
      return lowerValue != null && includes.contains(lowerValue);
    }
    return true;
  }

  /**
   * Extract the Maven version policy from a repository's configuration. Returns null for
   * non-Maven repositories or when the attribute is missing.
   */
  private static String getVersionPolicy(final Repository repository) {
    Map<String, Map<String, Object>> attrs = repository.getConfiguration().getAttributes();
    if (attrs == null) {
      return null;
    }
    Map<String, Object> mavenAttrs = attrs.get("maven");
    if (mavenAttrs == null) {
      return null;
    }
    Object policy = mavenAttrs.get("versionPolicy");
    return policy instanceof String ? (String) policy : null;
  }

  private boolean filterByFormats(RepositoryDetailXO repo, List<String> formats) {
    if (formats.isEmpty()) {
      return true;
    }
    return formats.contains(repo.getFormat().toLowerCase());
  }

  private boolean filterByTypes(RepositoryDetailXO repo, List<String> types) {
    if (types.isEmpty()) {
      return true;
    }
    return types.contains(repo.getType().toLowerCase());
  }

  private boolean filterByStatuses(RepositoryDetailXO repo, List<String> statuses) {
    if (statuses.isEmpty()) {
      return true;
    }
    boolean isOnline = repo.getStatus() != null && repo.getStatus().isOnline();
    String statusStr = isOnline ? "online" : "offline";
    return statuses.contains(statusStr);
  }

  private boolean filterByName(RepositoryDetailXO repo, String nameFilter) {
    if (isBlank(nameFilter)) {
      return true;
    }
    return repo.getName().toLowerCase().contains(nameFilter.toLowerCase().trim());
  }

  private Comparator<RepositoryDetailXO> getComparator(String sortField, String sortDirection) {
    boolean ascending = !"desc".equalsIgnoreCase(sortDirection);

    Comparator<RepositoryDetailXO> comparator;
    if ("format".equalsIgnoreCase(sortField)) {
      comparator = Comparator.comparing(RepositoryDetailXO::getFormat, String.CASE_INSENSITIVE_ORDER);
    }
    else if ("type".equalsIgnoreCase(sortField)) {
      comparator = Comparator.comparing(RepositoryDetailXO::getType, String.CASE_INSENSITIVE_ORDER);
    }
    else if ("status".equalsIgnoreCase(sortField)) {
      comparator =
          Comparator.comparing(repo -> repo.getStatus() != null && repo.getStatus().isOnline() ? "online" : "offline");
    }
    else {
      // Default: sort by name
      comparator = Comparator.comparing(RepositoryDetailXO::getName, String.CASE_INSENSITIVE_ORDER);
    }

    return ascending ? comparator : comparator.reversed();
  }

  @GET
  @Path("/recipes")
  public List<RecipeXO> getRecipes() {
    return recipes.stream()
        .filter(Recipe::isFeatureEnabled)
        .map(RecipeXO::new)
        .collect(toList());
  }

  private RepositoryDetailXO asRepositoryDetail(
      final Repository repository,
      @Nullable final RepositoryMetricsDTO metrics)
  {
    String name = repository.getName();
    String type = repository.getType().toString();
    String format = repository.getFormat().toString();
    String url = repository.getUrl();
    RepositoryStatusXO statusXO = getStatusXO(repository);

    RepositoryDetailXO detailXO = format.equals("nuget")
        ? asNugetRepository(repository)
        : new RepositoryDetailXO(name, type, format, url, statusXO);

    if (metrics != null) {
      detailXO.setSize(metrics.totalSize);
      detailXO.setAssetCount(metrics.blobCount);
    }

    // Set blob store name from storage configuration
    String blobStoreName = repository.getConfiguration()
        .attributes("storage")
        .get("blobStoreName", String.class);
    detailXO.setBlobStoreName(blobStoreName);

    return detailXO;
  }

  @SuppressWarnings("unchecked")
  private RepositoryDetailXO asNugetRepository(Repository repository) {
    String name = repository.getName();
    String type = repository.getType().getValue();
    String format = repository.getFormat().getValue();
    String url = repository.getUrl();
    RepositoryStatusXO statusXO = getStatusXO(repository);

    String nugetVersion = null;
    Collection<String> memberNames = null;

    if (type.equals(ProxyType.NAME)) {
      nugetVersion = (String) repository
          .getConfiguration()
          .attributes("nugetProxy")
          .get("nugetVersion");
    }
    else if (type.equals(GroupType.NAME)) {
      memberNames = (Collection<String>) repository
          .getConfiguration()
          .attributes("group")
          .get("memberNames");
    }
    else {
      RepositoryDetailXO detailXO = new RepositoryDetailXO(name, type, format, url, statusXO);
      detailXO.setBlobStoreName(repository.getConfiguration()
          .attributes("storage")
          .get("blobStoreName", String.class));
      return detailXO;
    }

    RepositoryNugetXO nugetXO = new RepositoryNugetXO(name, type, format, url, statusXO, nugetVersion, memberNames);
    nugetXO.setBlobStoreName(repository.getConfiguration()
        .attributes("storage")
        .get("blobStoreName", String.class));
    return nugetXO;
  }

  private RemoteConnectionStatus getStatus(final Repository repository) {
    return repository.facet(HttpClientFacet.class).getStatus();
  }

  private String getStatusDescription(final Repository repository) {
    String description = null;
    if (proxyType.equals(repository.getType())) {
      description = getStatus(repository).getDescription();
    }
    return description;
  }

  private String getStatusReason(final Repository repository) {
    String reason = null;
    if (proxyType.equals(repository.getType())) {
      reason = getStatus(repository).getReason();
    }
    return reason;
  }

  private RepositoryStatusXO getStatusXO(final Repository repository) {
    boolean online = repository.getConfiguration().isOnline();
    String description = getStatusDescription(repository);
    String reason = getStatusReason(repository);
    return new RepositoryStatusXO(online, description, reason);
  }
}
