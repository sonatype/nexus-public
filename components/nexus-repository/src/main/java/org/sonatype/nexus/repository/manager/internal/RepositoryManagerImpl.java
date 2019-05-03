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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventConsumer;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.internal.ConfigurationCreatedEvent;
import org.sonatype.nexus.repository.config.internal.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.config.internal.ConfigurationEvent;
import org.sonatype.nexus.repository.config.internal.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationUpdatedEvent;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryLoadedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryMetadataUpdatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryRestoredEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.storage.internal.BucketUpdatedEvent;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STORAGE;

/**
 * Default {@link RepositoryManager} implementation.
 *
 * @since 3.0
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
@ManagedObject(
    domain = "org.sonatype.nexus.repository.manager",
    typeClass = RepositoryManager.class,
    description = "Repository manager"
)
public class RepositoryManagerImpl
    extends StateGuardLifecycleSupport
    implements RepositoryManager, EventAware
{
  public static final String CLEANUP_ATTRIBUTES_KEY = "cleanup";

  public static final String CLEANUP_NAME_KEY = "policyName";

  private final DatabaseFreezeService databaseFreezeService;

  private final EventManager eventManager;

  private final ConfigurationStore store;

  private final Map<String, Recipe> recipes;

  private final RepositoryFactory factory;

  private final Provider<ConfigurationFacet> configFacet;

  private final RepositoryAdminSecurityContributor securityContributor;

  private final List<DefaultRepositoriesContributor> defaultRepositoriesContributors;

  private final Map<String, Repository> repositories = Maps.newConcurrentMap();

  private final boolean skipDefaultRepositories;

  private final BlobStoreManager blobStoreManager;

  private final GroupMemberMappingCache groupMemberMappingCache;

  @Inject
  public RepositoryManagerImpl(final EventManager eventManager,
                               final ConfigurationStore store,
                               final RepositoryFactory factory,
                               final Provider<ConfigurationFacet> configFacet,
                               final Map<String, Recipe> recipes,
                               final RepositoryAdminSecurityContributor securityContributor,
                               final List<DefaultRepositoriesContributor> defaultRepositoriesContributors,
                               final DatabaseFreezeService databaseFreezeService,
                               @Named("${nexus.skipDefaultRepositories:-false}") final boolean skipDefaultRepositories,
                               final BlobStoreManager blobStoreManager,
                               final GroupMemberMappingCache groupMemberMappingCache)
  {
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    this.factory = checkNotNull(factory);
    this.configFacet = checkNotNull(configFacet);
    this.recipes = checkNotNull(recipes);
    this.securityContributor = checkNotNull(securityContributor);
    this.defaultRepositoriesContributors = checkNotNull(defaultRepositoriesContributors);
    this.databaseFreezeService = checkNotNull(databaseFreezeService);
    this.skipDefaultRepositories = skipDefaultRepositories;
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.groupMemberMappingCache = checkNotNull(groupMemberMappingCache);
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
    securityContributor.add(repository);

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
    securityContributor.remove(repository);
  }

  // TODO: Generally need to consider exception handling to ensure proper state is maintained always

  @Override
  protected void doStart() throws Exception {
    blobStoreManager.start();

    groupMemberMappingCache.init(this);

    List<Configuration> configurations = store.list();

    // attempt to provision default repositories if allowed
    if (configurations.isEmpty()) {
      if (skipDefaultRepositories || !blobStoreManager.exists(DEFAULT_BLOBSTORE_NAME)) {
        log.debug("Skipping provisioning of default repositories");
        return;
      }

      // attempt to discover default repository configurations
      log.debug("No repositories configured; provisioning default repositories");
      provisionDefaultRepositories();
      configurations = store.list();

      // if we still have no repository configurations, complain and stop
      if (configurations.isEmpty()) {
        log.debug("No default repositories to provision");
        return;
      }
    }

    restoreRepositories(configurations);

    startRepositories();
  }

  private void provisionDefaultRepositories() {
    for (DefaultRepositoriesContributor contributor : defaultRepositoriesContributors) {
      for (Configuration configuration : contributor.getRepositoryConfigurations()) {
        log.debug("Provisioning default repository: {}", configuration);
        store.create(configuration);
      }
    }
  }

  private void restoreRepositories(final List<Configuration> configurations) throws Exception {
    log.debug("Restoring {} repositories", configurations.size());
    for (Configuration configuration : configurations) {
      log.debug("Restoring repository: {}", configuration);
      Repository repository = newRepository(configuration);
      track(repository);

      eventManager.post(new RepositoryLoadedEvent(repository));
    }
  }

  private void startRepositories() throws Exception {
    log.debug("Starting {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Starting repository: {}", repository);
      repository.start();

      eventManager.post(new RepositoryRestoredEvent(repository));
    }
  }

  @Override
  protected void doStop() throws Exception {

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

    blobStoreManager.stop();
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<Repository> browse() {
    return ImmutableList.copyOf(repositories.values());
  }

  @Override
  @Guarded(by = STARTED)
  public Iterable<Repository> browseForBlobStore(String blobStoreId) {
    return stream(browse().spliterator(), true)
        .filter(r -> blobStoreId.equals(r.getConfiguration().attributes(STORAGE).get(BLOB_STORE_NAME)))
        ::iterator;
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

    log.info("Creating repository: {} -> {}", repositoryName, configuration);

    Repository repository = newRepository(configuration);

    if (!EventHelper.isReplicating()) {
      store.create(configuration);
    }

    track(repository);

    repository.start();

    eventManager.post(new RepositoryCreatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository update(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    String repositoryName = checkNotNull(configuration.getRepositoryName());

    log.info("Updating repository: {} -> {}", repositoryName, configuration);

    Repository repository = repository(repositoryName);

    // ensure configuration sanity
    repository.validate(configuration);

    if (!EventHelper.isReplicating()) {
      store.update(configuration);
    }

    repository.stop();
    repository.update(configuration);
    repository.start();

    eventManager.post(new RepositoryUpdatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);
    databaseFreezeService.checkUnfrozen("Unable to delete repository when database is frozen.");

    log.info("Deleting repository: {}", name);

    Repository repository = repository(name);
    Configuration configuration = repository.getConfiguration();

    removeRepositoryFromAllGroups(repository);

    repository.stop();
    repository.delete();
    repository.destroy();

    if (!EventHelper.isReplicating()) {
      store.delete(configuration);
    }

    untrack(repository);

    eventManager.post(new RepositoryDeletedEvent(repository));
  }

  /**
   * Retrieve a list of all groups that contain the desired repository, either directly or transitively through
   * another group.
   *
   * @since 3.14
   *
   * @param repositoryName
   * @return List of group(s) that contain the supplied repository.  Ordered from closest to the repo to farthest away
   * i.e. if group A contains group B which contains repo C the returned list would be ordered B,A
   */
  @Override
  @Guarded(by = STARTED)
  public List<String> findContainingGroups(final String repositoryName) {
    return groupMemberMappingCache.getGroups(repositoryName);
  }

  private void removeRepositoryFromAllGroups(final Repository repositoryToRemove) throws Exception {
    for (Repository group : repositories.values()) {
        Optional<GroupFacet> groupFacet = group.optionalFacet(GroupFacet.class);
        if (groupFacet.isPresent() && groupFacet.get().member(repositoryToRemove)) {
          removeRepositoryFromGroup(repositoryToRemove, group);
      }
    }
  }

  private void removeRepositoryFromGroup(final Repository repositoryToRemove, final Repository group) throws Exception {
    NestedAttributesMap groupAttributes = group.getConfiguration().attributes("group");
    groupAttributes.get("memberNames", Collection.class).remove(repositoryToRemove.getName());
    update(group.getConfiguration());
  }

  private Stream<Object> blobstoreUsageStream(final String blobStoreName) {
    return stream(browse().spliterator(), false)
      .map(Repository::getConfiguration)
      .map(Configuration::getAttributes)
      .map(a -> a.get(STORAGE))
      .map(s -> s.get(BLOB_STORE_NAME))
      .filter(blobStoreName::equals);
  }

  @Override
  public boolean isBlobstoreUsed(final String blobStoreName) {
    return blobstoreUsageStream(blobStoreName).findAny().isPresent() ||
        blobStoreManager.blobStoreUsageCount(blobStoreName) > 0;
  }

  @Override
  public long blobstoreUsageCount(final String blobStoreName) {
    return blobstoreUsageStream(blobStoreName).count();
  }

  @Override
  public Stream<Repository> browseForCleanupPolicy(final String cleanupPolicyName) {
    return stream(browse().spliterator(), false)
        .filter(repository -> repositoryHasCleanupPolicy(repository, cleanupPolicyName));
  }

  @Override
  public Collection<Recipe> getAllSupportedRecipes() {
    return recipes.values();
  }

  @Subscribe
  public void on(final ConfigurationCreatedEvent event) {
    handleReplication(event, e -> create(e.getConfiguration()));
  }

  @Subscribe
  public void on(final ConfigurationUpdatedEvent event) {
    handleReplication(event, e -> update(e.getConfiguration()));
  }

  @Subscribe
  public void on(final ConfigurationDeletedEvent event) {
    handleReplication(event, e -> delete(e.getRepositoryName()));
  }

  private void handleReplication(final ConfigurationEvent event, final EventConsumer<ConfigurationEvent> consumer) {
    if (!event.isLocal()) {
      try {
        consumer.accept(event);
      }
      catch (Exception e) {
        log.error("Failed to replicate: {}", event, e);
      }
    }
  }

  @Subscribe
  public void onBucketUpdated(final BucketUpdatedEvent event) {
    Repository repository = repositories.get(event.getRepositoryName());
    if (repository != null) {
      eventManager.post(new RepositoryMetadataUpdatedEvent(repository));
    }
    else {
      log.debug("Not posting metadata update event for deleted repository {}", event.getRepositoryName());
    }
  }

  private boolean repositoryHasCleanupPolicy(final Repository repository, final String cleanupPolicyName) {
    return ofNullable(repository.getConfiguration())
        .map(Configuration::getAttributes)
        .map(attributes -> attributes.get(CLEANUP_ATTRIBUTES_KEY))
        .filter(Objects::nonNull)
        .map(cleanupPolicyMap -> cleanupPolicyMap.get(CLEANUP_NAME_KEY))
        .filter(cleanupPolicyName::equals)
        .isPresent();
  }
}
