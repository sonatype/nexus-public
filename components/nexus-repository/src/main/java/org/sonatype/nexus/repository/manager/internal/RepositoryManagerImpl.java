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
package org.sonatype.nexus.repository.manager.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationCreatedEvent;
import org.sonatype.nexus.repository.config.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.config.ConfigurationEvent;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.ConfigurationUpdatedEvent;
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryLoadedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryRestoredEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link RepositoryManager} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedObject(
    domain = "org.sonatype.nexus.repository.manager",
    typeClass = RepositoryManager.class,
    description = "Repository manager"
)
public class RepositoryManagerImpl
    extends StateGuardLifecycleSupport
    implements RepositoryManager, EventAware
{
  private final EventBus eventBus;

  private final ConfigurationStore store;

  private final Map<String, Recipe> recipes;

  private final RepositoryFactory factory;

  private final Provider<ConfigurationFacet> configFacet;

  private final RepositoryAdminSecurityConfigurationResource securityResource;

  private final List<DefaultRepositoriesContributor> defaultRepositoriesContributors;

  private final Map<String, Repository> repositories = Maps.newHashMap();

  private final boolean skipDefaultRepositories;

  @Inject
  public RepositoryManagerImpl(final EventBus eventBus,
                               final ConfigurationStore store,
                               final RepositoryFactory factory,
                               final Provider<ConfigurationFacet> configFacet,
                               final Map<String, Recipe> recipes,
                               final RepositoryAdminSecurityConfigurationResource securityResource,
                               final List<DefaultRepositoriesContributor> defaultRepositoriesContributors,
                               @Named("${nexus.skipDefaultRepositories:-false}") final boolean skipDefaultRepositories)
  {
    this.eventBus = checkNotNull(eventBus);
    this.store = checkNotNull(store);
    this.factory = checkNotNull(factory);
    this.configFacet = checkNotNull(configFacet);
    this.recipes = checkNotNull(recipes);
    this.securityResource = checkNotNull(securityResource);
    this.defaultRepositoriesContributors = checkNotNull(defaultRepositoriesContributors);
    this.skipDefaultRepositories = skipDefaultRepositories;
  }

  /**
   * Lookup a recipe by name.
   */
  private Recipe recipe(final String name) {
    Recipe recipe = recipes.get(name);
    checkState(recipe != null, "Missing recipe: %s", name);
    return recipe;
  }

  /**
   * Lookup a repository by name.
   */
  private Repository repository(final String name) {
    Repository repository = repositories.get(name);
    checkState(repository != null, "Missing repository: %s", name);
    return repository;
  }

  /**
   * Construct a new repository from configuration.
   */
  private Repository newRepository(final Configuration configuration) throws Exception {
    String recipeName = configuration.getRecipeName();
    Recipe recipe = recipe(recipeName);
    log.debug("Using recipe: [{}] {}", recipeName, recipe);

    Repository repository = factory.create(recipe.getType(), recipe.getFormat());

    // attach mandatory facets
    repository.attach(configFacet.get());

    // apply recipe to repository
    recipe.apply(repository);

    // verify required facets
    repository.facet(ViewFacet.class);

    // ensure configuration sanity, once all facets are attached
    repository.validate(configuration);

    // initialize repository
    repository.init(configuration);

    return repository;
  }

  /**
   * Track repository.
   */
  private void track(final Repository repository) {
    // configure security
    securityResource.add(repository);

    log.debug("Tracking: {}", repository);
    repositories.put(repository.getName(), repository);
  }

  /**
   * Untrack repository.
   */
  private void untrack(final Repository repository) {
    log.debug("Untracking: {}", repository);
    repositories.remove(repository.getName());

    // tear down security
    securityResource.remove(repository);
  }

  // TODO: Generally need to consider exception handling to ensure proper state is maintained always

  @Override
  protected void doStart() throws Exception {
    List<Configuration> configurations = store.list();

    // attempt to provision default repositories if allowed
    if (configurations.isEmpty()) {
      if (skipDefaultRepositories) {
        log.debug("Skipping provisioning of default repositories");
        return;
      }

      // attempt to discover default repository configurations
      log.debug("No repositories configured; provisioning default repositories");
      for (DefaultRepositoriesContributor contributor : defaultRepositoriesContributors) {
        for (Configuration configuration : contributor.getRepositoryConfigurations()) {
          log.debug("Provisioning default repository: {}", configuration);
          store.create(configuration);
        }
      }
      configurations = store.list();

      // if we still have no repository configurations, complain and stop
      if (configurations.isEmpty()) {
        log.debug("No default repositories to provision");
        return;
      }
    }

    log.debug("Restoring {} repositories", configurations.size());
    for (Configuration configuration : configurations) {
      log.debug("Restoring repository: {}", configuration);
      Repository repository = newRepository(configuration);
      track(repository);

      eventBus.post(new RepositoryLoadedEvent(repository));
    }

    log.debug("Starting {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Starting repository: {}", repository);
      repository.start();

      eventBus.post(new RepositoryRestoredEvent(repository));
    }
  }

  @Override
  protected void doStop() throws Exception {
    if (repositories.isEmpty()) {
      log.debug("No repositories defined");
      return;
    }

    log.debug("Stopping {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Stopping repository: {}", repository);
      repository.stop();
    }

    log.debug("Destroying {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Destroying repository: {}", repository);
      repository.destroy();
    }

    repositories.clear();
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<Repository> browse() {
    return ImmutableList.copyOf(repositories.values());
  }

  @Override
  public boolean exists(final String name) {
    return stream(browse().spliterator(), false).anyMatch(repository -> repository.getName().equalsIgnoreCase(name));
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Repository get(final String name) {
    checkNotNull(name);

    return repositories.get(name);
  }

  @Override
  @Guarded(by = STARTED)
  public Repository create(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    String repositoryName = checkNotNull(configuration.getRepositoryName());

    log.debug("Creating repository: {} -> {}", repositoryName, configuration);

    Repository repository = newRepository(configuration);
    store.create(configuration);
    track(repository);

    repository.start();

    eventBus.post(new RepositoryCreatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository update(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    String repositoryName = checkNotNull(configuration.getRepositoryName());

    log.debug("Updating repository: {} -> {}", repositoryName, configuration);

    Repository repository = repository(repositoryName);

    // ensure configuration sanity
    repository.validate(configuration);
    store.update(configuration);

    repository.stop();
    repository.update(configuration);
    repository.start();

    eventBus.post(new RepositoryUpdatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);

    log.debug("Deleting repository: {}", name);

    Repository repository = repository(name);
    Configuration configuration = repository.getConfiguration();
    repository.stop();
    repository.delete();
    repository.destroy();
    store.delete(configuration);
    untrack(repository);

    eventBus.post(new RepositoryDeletedEvent(repository));
  }

  @Override
  public boolean isBlobstoreUsed(String blobStoreName) {
    return stream(browse().spliterator(), false)
      .map(Repository::getConfiguration)
      .map(Configuration::getAttributes)
      .map(a -> a.get("storage"))
      .map(s -> s.get("blobStoreName"))
      .filter(blobStoreName::equals)
      .findAny()
      .isPresent();
  }

  @Override
  public long blobstoreUsageCount(final String blobStoreName) {
    return stream(browse().spliterator(), false)
        .map(Repository::getConfiguration)
        .map(Configuration::getAttributes)
        .map(a -> a.get("storage"))
        .map(s -> s.get("blobStoreName"))
        .filter(blobStoreName::equals)
        .count();
  }

  @Subscribe
  public void on(final ConfigurationDeletedEvent event) {
    handleRemoteOnly(event, repositoryName -> {
      // only delete if the repository is tracked
      if (repositories.containsKey(repositoryName)) {
        try {
          log.trace("delete: {}", repositoryName);
          Repository repository = repository(repositoryName);
          repository.destroy();
          untrack(repository);
        }
        catch (Exception e) {
          log.warn("delete failed: {}", repositoryName, e);
        }
      }
    });
  }

  @Subscribe
  public void on(final ConfigurationUpdatedEvent event) {
    handleRemoteOnly(event, repositoryName -> {
      // only update if the repository is tracked
      if (repositories.containsKey(repositoryName)) {
        withConfiguration(repositoryName, c -> {
          try {
            log.trace("update: {} -- {}", repositoryName, c);
            Repository repository = repository(repositoryName);
            repository.stop();
            repository.update(c);
            repository.start();
          }
          catch (Exception e) {
            log.warn("update failed: {}", repositoryName, e);
          }
        });
      }
    });
  }

  @Subscribe
  public void on(final ConfigurationCreatedEvent event) {
    handleRemoteOnly(event, repositoryName -> {
      // only create if the repository is not tracked
      if (!repositories.containsKey(repositoryName)) {
        withConfiguration(repositoryName, c -> {
          try {
            log.trace("create: {} -- {}", repositoryName, c);
            Repository repository = newRepository(c);
            track(repository);
            repository.start();
          }
          catch (Exception e) {
            log.warn("create failed: {}", repositoryName, e);
          }
        });
      }
    });
  }

  private void handleRemoteOnly(final ConfigurationEvent event, final Consumer<String> consumer) {
    log.trace("handling: {}", event);
    // skip local events
    if (!event.isLocal()) {
      String repositoryName = event.getRepositoryName();
      consumer.accept(repositoryName);
    }
  }

  private void withConfiguration(final String repositoryName, final Consumer<Configuration> consumer) {
    store.list().stream()
        .filter(c -> c.getRepositoryName().equals(repositoryName))
        .findFirst()
        .ifPresent(consumer);
  }
}
