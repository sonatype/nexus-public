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

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ValidationException;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.IncompatibleRepositoryException;
import org.sonatype.nexus.repository.rest.api.RepositoryNotFoundException;
import org.sonatype.nexus.repository.search.index.RebuildIndexTask;
import org.sonatype.nexus.repository.search.index.RebuildIndexTaskDescriptor;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.security.BreadActions.ADD;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.security.BreadActions.EDIT;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * A repository manager which limits access to repositories based on the current user's permissions.
 *
 * @since 3.20
 */
@Named
@Singleton
public class AuthorizingRepositoryManagerImpl
    implements AuthorizingRepositoryManager
{
  private final RepositoryManager repositoryManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final TaskScheduler taskScheduler;

  private final RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Inject
  public AuthorizingRepositoryManagerImpl(
      final RepositoryManager repositoryManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final TaskScheduler taskScheduler,
      final RepositoryCacheInvalidationService repositoryCacheInvalidationService)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.repositoryCacheInvalidationService = checkNotNull(repositoryCacheInvalidationService);
  }

  public void create(@Nonnull final Configuration configuration) throws Exception {
    String format = configuration.getRecipeName().split("-")[0];
    repositoryPermissionChecker.ensureUserCanAdmin(ADD, format, configuration.getRepositoryName());
    repositoryManager.create(configuration);
  }

  public boolean update(@Nonnull final Configuration configuration) throws Exception {
    Repository repository = repositoryManager.get(configuration.getRepositoryName());
    if (repository != null) {
      repositoryPermissionChecker.ensureUserCanAdmin(EDIT, repository);
      Configuration updatedConfig = repository.getConfiguration().copy();
      updatedConfig.setRoutingRuleId(configuration.getRoutingRuleId());
      updatedConfig.setOnline(configuration.isOnline());
      updatedConfig.setAttributes(configuration.getAttributes());
      repositoryManager.update(updatedConfig);
      return true;
    }
    return false;
  }

  public boolean delete(@Nonnull final String name) throws Exception {
    Repository repository = repositoryManager.get(name);
    if (repository != null) {
      repositoryPermissionChecker.ensureUserCanAdmin(DELETE, repository);
      try {
        repositoryManager.delete(repository.getName());
      }
      catch (ValidationException e) {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Returns the repositories which the user has an administrative read privilege.
   */
  public List<Repository> getRepositoriesWithAdmin() {
    return repositoryPermissionChecker.userHasRepositoryAdminPermission(repositoryManager.browse(), READ);
  }

  @Override
  public Optional<Repository> getRepositoryWithAdmin(final String repositoryName) {
    return Optional.ofNullable(repositoryManager.get(repositoryName))
        .flatMap(repo -> repositoryPermissionChecker.userHasRepositoryAdminPermission(singletonList(repo), READ).stream()
            .findFirst());
  }

  private void ensureHostedOrProxy(final Repository repository) throws IncompatibleRepositoryException {
    String type = repository.getType().getValue();
    if (!type.equals(HostedType.NAME) && !type.equals(ProxyType.NAME)) {
      throw new IncompatibleRepositoryException("You can rebuild search index of hosted or proxy repository only");
    }
  }

  /**
   * Trigger rebuild index task for given repository.
   *
   * @throws RepositoryNotFoundException     if repository does not exists
   * @throws IncompatibleRepositoryException if is not hosted or proxy type
   */
  @SuppressWarnings("squid:S1160") // suppress warning about two checked exceptions
  public void rebuildSearchIndex(@Nonnull final String name)
      throws RepositoryNotFoundException, IncompatibleRepositoryException
  {
    Repository repository = getEditableRepositoryOrThrow(name);
    ensureHostedOrProxy(repository);
    TaskConfiguration taskConfiguration =
        taskScheduler.createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID);
    taskConfiguration.setString(RebuildIndexTask.REPOSITORY_NAME_FIELD_ID, name);
    taskScheduler.submit(taskConfiguration);
  }

  /**
   * Invalidate cache of a given repository.
   *
   * @throws RepositoryNotFoundException if repository does not exists
   * @throws IllegalStateException       if is not proxy or group type
   */
  @SuppressWarnings("squid:S1160") // suppress warning about two checked exceptions
  public void invalidateCache(@Nonnull final String name)
      throws RepositoryNotFoundException, IncompatibleRepositoryException
  {
    Repository repository = getEditableRepositoryOrThrow(name);
    ensureProxyOrGroup(repository);
    repositoryCacheInvalidationService.processCachesInvalidation(repository);
  }

  private void ensureProxyOrGroup(final Repository repository) throws IncompatibleRepositoryException {
    String type = repository.getType().getValue();
    if (!type.equals(ProxyType.NAME) && !type.equals(GroupType.NAME)) {
      throw new IncompatibleRepositoryException("You can invalidate cache of proxy or group repository only");
    }
  }

  private Repository getEditableRepositoryOrThrow(@Nonnull final String name)
      throws RepositoryNotFoundException
  {
    Repository repository = repositoryManager.get(name);
    if (repository == null) {
      throw new RepositoryNotFoundException();
    }
    repositoryPermissionChecker.ensureUserCanAdmin(EDIT, repository);
    return repository;
  }
}
