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
package org.sonatype.nexus.proxy.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.AbstractConfigurable;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventPostRemove;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Repository registry. It holds handles to registered repositories and sorts them properly. This class is used to get
 * a
 * grip on repositories.
 * <p>
 * Getting reposes from here and changing repo attributes like group, id and rank have no effect on repo registry! For
 * that kind of change, you have to: 1) get repository, 2) remove repository from registry, 3) change repo attributes
 * and 4) add repository.
 * <p>
 * ProximityEvents: this component just "concentrates" the repositiry events of all known repositories by it. It can be
 * used as single point to access all repository events. TODO this is not a good place to keep group repository
 * management code
 *
 * @author cstamas
 */
@Singleton
@Named
public class DefaultRepositoryRegistry
    extends ComponentSupport
    implements RepositoryRegistry
{
  private final EventBus eventBus;

  private final RepositoryTypeRegistry repositoryTypeRegistry;

  @Inject
  public DefaultRepositoryRegistry(final EventBus eventBus, final RepositoryTypeRegistry repositoryTypeRegistry) {
    this.eventBus = checkNotNull(eventBus);
    this.repositoryTypeRegistry = checkNotNull(repositoryTypeRegistry);
  }

  @Override
  public void addRepository(final Repository repository) {
    final RepositoryTypeDescriptor rtd =
        repositoryTypeRegistry.getRepositoryTypeDescriptor(repository.getProviderRole(),
            repository.getProviderHint());
    insertRepository(rtd, repository);
    log.info("Added repository {}", RepositoryStringUtils.getFullHumanizedNameString(repository));
  }

  @Override
  public void removeRepository(final String repoId)
      throws NoSuchRepositoryException
  {
    doRemoveRepository(repoId, false);
  }

  @Override
  public void removeRepositorySilently(final String repoId)
      throws NoSuchRepositoryException
  {
    doRemoveRepository(repoId, true);
  }

  @Override
  public List<Repository> getRepositories() {
    return Collections.unmodifiableList(new ArrayList<Repository>(getRepositoriesMap().values()));
  }

  @Override
  public <T> List<T> getRepositoriesWithFacet(final Class<T> f) {
    final List<Repository> repositories = getRepositories();

    final ArrayList<T> result = new ArrayList<T>();

    for (Repository repository : repositories) {
      if (repository.getRepositoryKind().isFacetAvailable(f)) {
        result.add(repository.adaptToFacet(f));
      }
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public Repository getRepository(final String repoId)
      throws NoSuchRepositoryException
  {
    final Map<String, Repository> repositories = getRepositoriesMap();

    if (repositories.containsKey(repoId)) {
      return repositories.get(repoId);
    }
    else {
      throw new NoSuchRepositoryException(repoId);
    }
  }

  @Override
  public <T> T getRepositoryWithFacet(final String repoId, final Class<T> f)
      throws NoSuchRepositoryException
  {
    final Repository r = getRepository(repoId);

    if (r.getRepositoryKind().isFacetAvailable(f)) {
      return r.adaptToFacet(f);
    }
    else {
      throw new NoSuchRepositoryException(repoId);
    }
  }

  @Override
  public boolean repositoryIdExists(final String repositoryId) {
    return getRepositoriesMap().containsKey(repositoryId);
  }

  @Override
  public List<String> getGroupsOfRepository(final String repositoryId) {
    final ArrayList<String> result = new ArrayList<String>();

    try {
      final Repository repository = getRepository(repositoryId);

      for (GroupRepository group : getGroupsOfRepository(repository)) {
        result.add(group.getId());
      }
    }
    catch (NoSuchRepositoryException e) {
      // ignore, just return empty collection
    }

    return result;
  }

  @Override
  public List<GroupRepository> getGroupsOfRepository(final Repository repository) {
    final ArrayList<GroupRepository> result = new ArrayList<GroupRepository>();

    for (Repository repo : getRepositories()) {
      if (!repo.getId().equals(repository.getId())
          && repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
        final GroupRepository group = repo.adaptToFacet(GroupRepository.class);

        members:
        for (Repository member : group.getMemberRepositories()) {
          if (repository.getId().equals(member.getId())) {
            result.add(group);
            break members;
          }
        }
      }
    }

    return result;
  }

  //
  // priv
  //

  /**
   * The repository registry map
   */
  private final Map<String, Repository> _repositories = Maps.newHashMap();

  /**
   * The repository registry RO "view"
   */
  private volatile Map<String, Repository> _repositoriesView;

  /**
   * Returns a copy of map with repositories. Is synchronized method, to allow consistent-read access. Methods
   * modifying this map are all also synchronized (see API Interface and above), while all the "reading" methods from
   * public API will boil down to this single method.
   */
  protected synchronized Map<String, Repository> getRepositoriesMap() {
    if (_repositoriesView == null) {
      _repositoriesView = Collections.unmodifiableMap(new HashMap<String, Repository>(_repositories));
    }

    return _repositoriesView;
  }

  protected synchronized void repositoriesMapPut(final Repository repository) {
    _repositories.put(repository.getId(), repository);
    _repositoriesView = Collections.unmodifiableMap(new HashMap<String, Repository>(_repositories));
  }

  protected synchronized void repositoriesMapRemove(final String repositoryId) {
    _repositories.remove(repositoryId);
    _repositoriesView = Collections.unmodifiableMap(new HashMap<String, Repository>(_repositories));
  }

  protected void doRemoveRepository(final String repoId, final boolean silently)
      throws NoSuchRepositoryException
  {
    Repository repository = getRepository(repoId);
    RepositoryTypeDescriptor rtd =
        repositoryTypeRegistry.getRepositoryTypeDescriptor(repository.getProviderRole(),
            repository.getProviderHint());
    deleteRepository(rtd, repository, silently);

    if (!silently) {
      log.info("Removed repository {}", RepositoryStringUtils.getFullHumanizedNameString(repository));
    }
  }

  private void insertRepository(final RepositoryTypeDescriptor rtd, final Repository repository) {
    synchronized (this) {
      repositoriesMapPut(repository);
      rtd.instanceRegistered(this);
    }

    eventBus.post(new RepositoryRegistryEventAdd(this, repository));
  }

  private void deleteRepository(final RepositoryTypeDescriptor rtd, final Repository repository,
                                final boolean silently)
  {
    if (!silently) {
      eventBus.post(new RepositoryRegistryEventRemove(this, repository));
    }

    // dump the event listeners, as once deleted doesn't care about config changes any longer
    if (repository instanceof AbstractConfigurable) {
      ((AbstractConfigurable) repository).unregisterFromEventBus();
    }

    synchronized (this) {
      rtd.instanceUnregistered(this);
      repositoriesMapRemove(repository.getId());
    }

    if (!silently) {
      eventBus.post(new RepositoryRegistryEventPostRemove(this, repository));
    }
  }
}
