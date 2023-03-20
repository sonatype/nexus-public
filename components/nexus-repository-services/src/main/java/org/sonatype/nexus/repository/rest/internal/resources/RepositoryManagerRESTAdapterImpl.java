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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.rest.api.RepositoryXO;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.ProxyType;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.sonatype.nexus.repository.http.HttpStatus.FORBIDDEN;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;

/**
 * An implementation of the {@link RepositoryManagerRESTAdapter}
 *
 * @since 3.4
 */
@Named
public class RepositoryManagerRESTAdapterImpl
    extends ComponentSupport
    implements RepositoryManagerRESTAdapter
{
  private final RepositoryManager repositoryManager;

  private final ConfigurationStore configurationStore;

  private final Map<String, Recipe> recipes;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  @Inject
  public RepositoryManagerRESTAdapterImpl(
      final RepositoryManager repositoryManager,
      final ConfigurationStore configurationStore,
      final Map<String, Recipe> recipes,
      final RepositoryPermissionChecker repositoryPermissionChecker)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.configurationStore = checkNotNull(configurationStore);
    this.recipes = checkNotNull(recipes);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
  }

  @Override
  public Repository toRepository(final String repositoryId) {
    if (repositoryId == null) {
      throw new WebApplicationException("repositoryId is required.", UNPROCESSABLE_ENTITY);
    }
    return ofNullable(repositoryManager.get(repositoryId))
        .orElseThrow(() -> new NotFoundException("Unable to locate repository with id " + repositoryId));
  }

  @Override
  public Repository getRepository(final String repositoryId) {
    Repository repository = toRepository(repositoryId);

    if (repositoryPermissionChecker.userCanReadOrBrowse(repository)) {
      //browse or read implies complete access to the repository.
      return repository;
    }
    else {
      //repository exists but user does not have the appropriate permission to browse, return a 403
      throw new WebApplicationException(FORBIDDEN);
    }
  }

  @Override
  public Repository getReadableRepository(String repositoryId) {
    if (repositoryId == null) {
      throw new WebApplicationException("repositoryId is required.", UNPROCESSABLE_ENTITY);
    }
    Repository repository = ofNullable(repositoryManager.get(repositoryId))
        .orElseThrow(() -> new NotFoundException("Unable to locate repository with id " + repositoryId));

    if (!userCanReadOrBrowseOrGroupPermissions(repository)) {
      // user does not have the appropriate permission to browse, return a 403
      throw new WebApplicationException(FORBIDDEN);
    }

    //browse or read implies complete access to the repository.
    return repository;
  }

  private boolean userCanReadOrBrowseOrGroupPermissions(final Repository repository) {
    //  Given - repository = raw-hosted
    //  nx-repository-view-raw-raw-hosted-read - allowed
    //  nx-repository-view-raw-raw-group-read(raw-group contains raw-hosted as a member) - allowed
    List<String> repositories = new ArrayList<>(repositoryManager.findContainingGroups(repository.getName()));
    repositories.add(repository.getName());

    return repositories
        .stream()
        .map(repositoryManager::get)
        .filter(Objects::nonNull)
        .anyMatch(repositoryPermissionChecker::userCanReadOrBrowse);
  }

  @Override
  public List<RepositoryXO> getRepositories() {
    Configuration[] configurations = configurationStore.list().toArray(new Configuration[0]);
    return repositoryPermissionChecker.userCanBrowseRepositories(configurations).stream()
        .map(this::asRepository)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> findContainingGroups(final String repositoryName) {
    return repositoryManager.findContainingGroups(repositoryName);
  }

  private Type getType(final Configuration configuration) {
    Recipe recipe = recipes.get(configuration.getRecipeName());
    return recipe.getType();
  }

  private Format getFormat(final Configuration configuration) {
    Recipe recipe = recipes.get(configuration.getRecipeName());
    return recipe.getFormat();
  }

  private static String getUrl(final String repositoryName) {
    return BaseUrlHolder.get() + "/repository/" + repositoryName;
  }

  private Map<String, Object> attributes(final Configuration configuration) {
    if (getType(configuration) instanceof ProxyType) {
      NestedAttributesMap attrs = configuration.attributes("proxy");
      if (attrs != null) {
        Object remoteUrl = attrs.get("remoteUrl", EMPTY);
        return singletonMap("proxy", singletonMap("remoteUrl", remoteUrl));
      }
    }

    return emptyMap();
  }

  private RepositoryXO asRepository(final Configuration configuration) {
    RepositoryXO xo = new RepositoryXO();
    String repositoryName = configuration.getRepositoryName();
    Type type = getType(configuration);

    xo.setName(repositoryName);
    xo.setType(type.getValue());
    xo.setFormat(getFormat(configuration).getValue());
    xo.setUrl(getUrl(repositoryName));
    xo.setAttributes(attributes(configuration));

    return xo;
  }
}
