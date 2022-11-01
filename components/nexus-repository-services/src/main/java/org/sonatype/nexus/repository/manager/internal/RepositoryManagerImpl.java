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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventConsumer;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.distributed.event.service.api.EventType;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryConfigurationEvent;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryRemoteConnectionStatusEvent;
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
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType;
import org.sonatype.nexus.repository.manager.ConfigurationValidator;
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
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.distributed.event.service.api.EventType.CREATED;
import static org.sonatype.nexus.distributed.event.service.api.EventType.DELETED;
import static org.sonatype.nexus.distributed.event.service.api.EventType.UPDATED;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.validation.ConstraintViolations.maybeAdd;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

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

  private final FreezeService freezeService;

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

  private final List<ConfigurationValidator> configurationValidators;

  @Inject
  public RepositoryManagerImpl(
      final EventManager eventManager,
      final ConfigurationStore store,
      final RepositoryFactory factory,
      final Provider<ConfigurationFacet> configFacet,
      final Map<String, Recipe> recipes,
      final RepositoryAdminSecurityContributor securityContributor,
      final List<DefaultRepositoriesContributor> defaultRepositoriesContributors,
      final FreezeService freezeService,
      @Named("${nexus.skipDefaultRepositories:-false}") final boolean skipDefaultRepositories,
      final BlobStoreManager blobStoreManager,
      final GroupMemberMappingCache groupMemberMappingCache,
      final List<ConfigurationValidator> configurationValidators)
  {
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    this.factory = checkNotNull(factory);
    this.configFacet = checkNotNull(configFacet);
    this.recipes = checkNotNull(recipes);
    this.securityContributor = checkNotNull(securityContributor);
    this.defaultRepositoriesContributors = checkNotNull(defaultRepositoriesContributors);
    this.freezeService = checkNotNull(freezeService);
    this.skipDefaultRepositories = skipDefaultRepositories;
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.groupMemberMappingCache = checkNotNull(groupMemberMappingCache);
    this.configurationValidators = checkNotNull(configurationValidators);
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
    Repository repository = repositories.get(name.toLowerCase());
    if (repository == null) {
      throw new ValidationException("Missing repository: " + name);
    }
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

    Repository value = repositories.putIfAbsent(repository.getName().toLowerCase(), repository);

    if (value == null) {
      log.debug("Tracking: {}", repository);
    }
    else {
      log.debug("An existing repository with the same name is already tracked {}", value);
    }
  }

  /**
   * Untrack repository.
   */
  private void untrack(final Repository repository) {
    log.debug("Untracking: {}", repository);
    repositories.remove(repository.getName().toLowerCase());

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
  public Iterable<Repository> browseForBlobStore(final String blobStoreId) {
    Iterable<Repository> browseResult = browse();

    if (browseResult != null && browseResult.iterator().hasNext()) {
      return stream(browseResult.spliterator(), true)
          .filter(Repository::isStarted)
          .filter(r -> blobStoreId.equals(r.getConfiguration().attributes(STORAGE).get(BLOB_STORE_NAME)))
          ::iterator;
    }
    else {
      return Collections.emptyList();
    }
  }

  @Override
  public boolean exists(final String name) {
    return isRepositoryLoaded(name) || store.exists(name);
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Repository get(final String name) {
    checkNotNull(name);
    Repository repository = repositories.get(name.toLowerCase());

    if (repository == null) {
      Collection<Configuration> configurations = store.readByNames(Collections.singleton(name));

      if (configurations.isEmpty()) {
        return null;
      }

      try {
        return loadRepositoryIntoMemory(configurations.stream().findFirst().get());
      }
      catch (Exception e) {
        log.error("Exception loading repository: {} into memory", name, e);
        return null;
      }
    }

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository create(final Configuration configuration) throws Exception {
    checkNotNull(configuration);

    if (isRepositoryLoaded(configuration.getRepositoryName())) {
      throw new ValidationException("Repository has created already!");
    }

    Repository repository = loadRepositoryIntoMemory(configuration);
    if (!EventHelper.isReplicating()) {
      store.create(configuration);
    }
    repository.start();

    eventManager.post(new RepositoryCreatedEvent(repository));
    distributeRepositoryConfigurationEvent(repository.getName(), CREATED);

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository update(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    validateConfiguration(configuration);

    // load old configuration before update
    Configuration oldConfiguration = repository(configuration.getRepositoryName()).getConfiguration().copy();
    String repositoryName = checkNotNull(configuration.getRepositoryName());
    validateRepositoryConfiguration(repositoryName, configuration);

    // the configuration must be updated before repository restart
    if (!EventHelper.isReplicating()) {
      store.update(configuration);
    }
    Repository repository = updateRepositoryInMemory(configuration);
    eventManager.post(new RepositoryUpdatedEvent(repository, oldConfiguration));
    distributeRepositoryConfigurationEvent(repository.getName(), UPDATED);

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    checkNotNull(name);

    Repository repository = deleteRepositoryFromMemory(name);

    if (!EventHelper.isReplicating()) {
      Optional<Configuration> configuration = repositoryConfiguration(name);
      configuration.ifPresent(store::delete);
    }

    eventManager.post(new RepositoryDeletedEvent(repository));
    distributeRepositoryConfigurationEvent(name, DELETED);

    log.info("Deleted repository: {}", name);
  }

  private Repository deleteRepositoryFromMemory(final String name) throws Exception {
    checkNotNull(name);
    freezeService.checkWritable("Unable to delete repository when database is frozen.");

    log.info("Deleting repository from memory: {}", name);

    Repository repository = repository(name);

    removeRepositoryFromAllGroups(repository);

    repository.stop();
    repository.delete();
    repository.destroy();

    untrack(repository);

    return repository;
  }

  private Repository loadRepositoryIntoMemory(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    String repositoryName = checkNotNull(configuration.getRepositoryName());

    log.info("Creating repository in memory: {} -> {}", repositoryName, configuration);

    validateConfiguration(configuration);

    Repository repository = newRepository(configuration);
    track(repository);

    return repository;
  }

  private Repository updateRepositoryInMemory(final Configuration configuration) throws Exception {
    checkNotNull(configuration);

    String repositoryName = checkNotNull(configuration.getRepositoryName());
    log.info("Updating repository in memory: {} -> {}", repositoryName, configuration);

    Repository repository = repository(repositoryName);

    repository.stop();
    repository.update(configuration);
    repository.start();

    return repository;
  }

  /**
   * Retrieve a list of all groups that contain the desired repository, either directly or transitively through another
   * group.
   *
   * @param repositoryName
   * @return List of group(s) that contain the supplied repository.  Ordered from closest to the repo to farthest away
   * i.e. if group A contains group B which contains repo C the returned list would be ordered B,A
   * @since 3.14
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
    Configuration configuration = group.getConfiguration().copy();
    NestedAttributesMap groupAttributes = configuration.attributes("group");
    groupAttributes.get("memberNames", Collection.class).remove(repositoryToRemove.getName());
    update(configuration);
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
  public boolean isDataStoreUsed(final String dataStoreName) {
    return stream(browse().spliterator(), false)
        .map(Repository::getConfiguration)
        .map(Configuration::getAttributes)
        .map(a -> a.get(STORAGE))
        .map(s -> s.get(DATA_STORE_NAME))
        .anyMatch(dataStoreName::equals);
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

  @Override
  public Configuration newConfiguration() {
    return store.newConfiguration();
  }

  @Override
  @Guarded(by = STARTED)
  public int count() {
    return repositories.size();
  }

  @Subscribe
  public void on(final RepositoryConfigurationEvent event) {
    if (!EventHelper.isReplicating()) {
      return;
    }
    String repositoryName = event.getRepositoryName();
    EventType eventType = event.getEventType();

    log.debug("Consume distributed RepositoryConfigurationEvent: repository={}, type={}", repositoryName, eventType);

    switch (eventType) {
      case CREATED:
        handleRepositoryCreated(repositoryName);
        break;
      case UPDATED:
        handleRepositoryUpdated(repositoryName);
        break;
      case DELETED:
        handleRepositoryDeleted(repositoryName);
        break;
      default:
        log.error("Unknown event type {}", eventType);
    }
  }

  @Subscribe
  public void on(final RepositoryRemoteConnectionStatusEvent event) {
    if (!EventHelper.isReplicating()) {
      return;
    }
    String repositoryName = event.getRepositoryName();

    if (isRepositoryLoaded(repositoryName)) {
      //Event shouldn't be propagated if repository isn't propagated yet to the current node
      RemoteConnectionStatusType statusType =
          RemoteConnectionStatusType.values()[event.getRemoteConnectionStatusTypeOrdinal()];
      // restore RemoteConnectionStatus from event
      log.debug("Consume distributed RepositoryRemoteConnectionStatusEvent: repository={}, type={}",
          repositoryName, statusType);
      RemoteConnectionStatus status = new RemoteConnectionStatus(statusType, event.getReason())
          .setBlockedUntil(new DateTime(event.getBlockedUntilMillis()))
          .setRequestUrl(event.getRequestUrl());

      repository(repositoryName).facet(HttpClientFacet.class).setStatus(status);
    }
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

  private boolean repositoryHasCleanupPolicy(final Repository repository, final String cleanupPolicyName) {
    return ofNullable(repository.getConfiguration())
        .map(Configuration::getAttributes)
        .map(attributes -> attributes.get(CLEANUP_ATTRIBUTES_KEY))
        .filter(Objects::nonNull)
        .map(cleanupPolicyMap -> (Collection) cleanupPolicyMap.get(CLEANUP_NAME_KEY))
        .filter(Objects::nonNull)
        .filter(cleanupPolicies -> cleanupPolicies.contains(cleanupPolicyName))
        .isPresent();
  }

  private void validateConfiguration(final Configuration configuration) {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    configurationValidators.forEach(
        validator -> maybeAdd(violations, validator.validate(configuration))
    );
    maybePropagate(violations, log);
  }

  private Optional<Configuration> repositoryConfiguration(final String repositoryName) {
    checkNotNull(repositoryName);

    List<Configuration> configurations = store.list();
    if (!configurations.isEmpty()) {
      return configurations.stream()
          .filter(config -> config.getRepositoryName().equals(repositoryName))
          .findFirst();
    }
    return Optional.empty();
  }

  private void handleRepositoryCreated(final String repositoryName) {
    if (isRepositoryLoaded(repositoryName)) {
      // repository already presented on the current node UI
      return;
    }
    // try to the repository on current the node UI based on its configuration
    Optional<Configuration> configuration = repositoryConfiguration(repositoryName);
    configuration.ifPresent(config -> {
      try {
        Repository repository = loadRepositoryIntoMemory(config);
        repository.start();
        eventManager.post(new RepositoryCreatedEvent(repository));
      }
      catch (Exception e) {
        log.error("Error updating repository on the current node UI.", e);
      }
    });
  }

  private void handleRepositoryUpdated(final String repositoryName) {
    if (!isRepositoryLoaded(repositoryName)) {
      log.debug("Can not find repository configuration {}", repositoryName);
      return;
    }

    Optional<Configuration> configuration = repositoryConfiguration(repositoryName);
    configuration.ifPresent(config -> {
      try {
        validateConfiguration(config);
        Configuration oldConfiguration = repository(repositoryName).getConfiguration().copy();
        Repository repository = repository(repositoryName);
        updateRepositoryInMemory(config);
        eventManager.post(new RepositoryUpdatedEvent(repository, oldConfiguration));
      }
      catch (Exception e) {
        log.error("Error updating repository configuration on UI", e);
      }
    });
  }

  private void handleRepositoryDeleted(final String repositoryName) {
    if (!isRepositoryLoaded(repositoryName)) {
      log.debug("Repository {} already deleted on the current node", repositoryName);
      return;
    }

    try {
      Repository repository = deleteRepositoryFromMemory(repositoryName);
      eventManager.post(new RepositoryDeletedEvent(repository));
    }
    catch (Exception e) {
      log.error("Error deleting repository on the current node.", e);
    }
  }

  private void distributeRepositoryConfigurationEvent(final String repositoryName, final EventType eventType) {
    log.debug("Distribute repository configuration event: repository={}:{}", repositoryName, eventType);

    RepositoryConfigurationEvent configEvent = new RepositoryConfigurationEvent(repositoryName, eventType);
    eventManager.post(configEvent);
  }

  private void validateRepositoryConfiguration(final String repositoryName, final Configuration configuration) throws Exception {
    Repository repository = repository(repositoryName);
    // ensure configuration sanity
    repository.validate(configuration);
  }

  private boolean isRepositoryLoaded(final String repositoryName) {
    return repositories.containsKey(repositoryName.toLowerCase());
  }
}
