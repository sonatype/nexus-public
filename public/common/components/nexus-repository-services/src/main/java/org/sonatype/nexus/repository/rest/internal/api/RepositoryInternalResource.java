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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.common.QualifierUtil;
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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
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
@Singleton
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

  private final List<Format> formats;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final ProxyType proxyType;

  private final List<Recipe> recipes;

  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final Map<String, ApiRepositoryAdapter> convertersByFormat;

  private final ApiRepositoryAdapter defaultAdapter;

  private final RepositoryMetricsService repositoryMetricsService;

  @Inject
  public RepositoryInternalResource(
      final List<Format> formats,
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final ProxyType proxyType,
      final List<Recipe> recipes,
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final List<ApiRepositoryAdapter> convertersByFormatList,
      @Qualifier("default") final ApiRepositoryAdapter defaultAdapter,
      final RepositoryMetricsService repositoryMetricsService)
  {
    this.formats = checkNotNull(formats);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.proxyType = checkNotNull(proxyType);
    this.recipes = checkNotNull(recipes);
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.convertersByFormat = QualifierUtil.buildQualifierBeanMap(checkNotNull(convertersByFormatList));
    this.defaultAdapter = checkNotNull(defaultAdapter);
    this.repositoryMetricsService = checkNotNull(repositoryMetricsService);
  }

  @GET
  @RequiresAuthentication
  public List<RepositoryXO> getRepositories(
      @QueryParam("type") final String type,
      @QueryParam("withAll") final boolean withAll,
      @QueryParam("withFormats") final boolean withFormats,
      @QueryParam("format") final String formatParam)
  {
    List<RepositoryXO> repositories = repositoryPermissionChecker.userCanBrowseRepositories(repositoryManager.browse())
        .stream()
        .filter(repository -> isBlank(type) || type.equals(repository.getType().getValue()))
        .filter(repository -> isBlank(formatParam)
            || formatParam.equals(ALL_FORMATS)
            || formatParam.equals(repository.getFormat().getValue()))
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

  @GET
  @Path("/repository/{repositoryName}")
  @RequiresAuthentication
  public AbstractApiRepository getRepository(@PathParam("repositoryName") final String repositoryName) {
    return authorizingRepositoryManager.getRepositoryWithAdmin(repositoryName)
        .map(repository -> convertersByFormat.getOrDefault(repository.getFormat().getValue(), defaultAdapter)
            .adapt(repository))
        .get();
  }

  @GET
  @Path("/details")
  public List<RepositoryDetailXO> getRepositoryDetails() {
    Map<String, RepositoryMetricsDTO> metricsByName = repositoryMetricsService.list()
        .stream()
        .collect(Collectors.toMap(RepositoryMetricsDTO::getName, m -> m));

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

    Map<String, RepositoryMetricsDTO> metricsByName = repositoryMetricsService.list()
        .stream()
        .collect(Collectors.toMap(RepositoryMetricsDTO::getName, m -> m));

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
      return new RepositoryDetailXO(name, type, format, url, statusXO);
    }

    return new RepositoryNugetXO(name, type, format, url, statusXO, nugetVersion, memberNames);
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
