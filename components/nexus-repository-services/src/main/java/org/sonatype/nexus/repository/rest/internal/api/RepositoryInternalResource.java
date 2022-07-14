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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.goodies.common.ComponentSupport;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * @since 3.29
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RepositoryInternalResource.RESOURCE_PATH)
public class RepositoryInternalResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/repositories";

  static final RepositoryXO ALL_REFERENCE = new RepositoryXO(
      RepositorySelector.all().toSelector(),
      "(All Repositories)"
  );

  static final String ALL_FORMATS = "*";

  private final List<Format> formats;

  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final ProxyType proxyType;

  private final List<Recipe> recipes;

  private final AuthorizingRepositoryManager authorizingRepositoryManager;

  private final Map<String, ApiRepositoryAdapter> convertersByFormat;

  private final ApiRepositoryAdapter defaultAdapter;

  @Inject
  public RepositoryInternalResource(
      final List<Format> formats,
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final ProxyType proxyType,
      final List<Recipe> recipes,
      final AuthorizingRepositoryManager authorizingRepositoryManager,
      final Map<String, ApiRepositoryAdapter> convertersByFormat,
      @Named("default") final ApiRepositoryAdapter defaultAdapter)
  {
    this.formats = checkNotNull(formats);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.proxyType = checkNotNull(proxyType);
    this.recipes = checkNotNull(recipes);
    this.authorizingRepositoryManager = checkNotNull(authorizingRepositoryManager);
    this.convertersByFormat = checkNotNull(convertersByFormat);
    this.defaultAdapter = checkNotNull(defaultAdapter);
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
              "(All " + format.getValue() + " Repositories)"
          ))
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
    return authorizingRepositoryManager.getRepositoryWithAdmin(repositoryName).map(repository ->
        convertersByFormat.getOrDefault(repository.getFormat().getValue(), defaultAdapter).adapt(repository)).get();
  }

  @GET
  @Path("/details")
  public List<RepositoryDetailXO> getRepositoryDetails()
  {
    return stream(repositoryManager.browse())
        .filter(repository -> repositoryPermissionChecker.userHasRepositoryAdminPermission(repository, READ))
        .map(this::asRepositoryDetail)
        .collect(toList());
  }

  @GET
  @Path("/recipes")
  public List<RecipeXO> getRecipes()
  {
    return recipes.stream()
        .filter(Recipe::isFeatureEnabled)
        .map(RecipeXO::new)
        .collect(toList());
  }

  private RepositoryDetailXO asRepositoryDetail(final Repository repository) {
    String name = repository.getName();
    String type = repository.getType().toString();
    String format = repository.getFormat().toString();
    String url = repository.getUrl();
    RepositoryStatusXO statusXO = getStatusXO(repository);

    return format.equals("nuget")
            ? asNugetRepository(repository)
            : new RepositoryDetailXO(name, type, format, url, statusXO);
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
    } else if (type.equals(GroupType.NAME)) {
      memberNames = (Collection<String>) repository
              .getConfiguration()
              .attributes("group")
              .get("memberNames");
    } else {
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

