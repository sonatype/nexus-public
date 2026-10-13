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

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryRemoteConnectionStatusEvent;
import org.sonatype.nexus.repository.MissingRepositoryException;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
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
import org.sonatype.nexus.repository.search.index.SearchIndexFacet;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Provider;
import jakarta.validation.ConstraintViolation;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.blobstore.api.BlobStoreManager.DEFAULT_BLOBSTORE_NAME;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.FIREWALL;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.validation.ConstraintViolations.maybeAdd;
import static org.sonatype.nexus.validation.ConstraintViolations.maybePropagate;

/**
 * Default {@link RepositoryManager} implementation.
 *
 * @since 3.0
 */
public abstract class BaseRepositoryManager<BSM extends BlobStoreManager>
    extends StateGuardLifecycleSupport
    implements RepositoryManager, EventAware
{
  private static final Logger log = LoggerFactory.getLogger(BaseRepositoryManager.class);

  public static final String CLEANUP_ATTRIBUTES_KEY = "cleanup";

  public static final String CLEANUP_NAME_KEY = "policyName";

  protected final EventManager eventManager;

  protected final ConfigurationStore store;

  protected final Map<String, Recipe> recipes;

  protected final RepositoryFactory factory;

  protected final Provider<ConfigurationFacet> configFacet;

  protected final RepositoryAdminSecurityContributor securityContributor;

  private final List<DefaultRepositoriesContributor> defaultRepositoriesContributors;

  protected final Map<String, Repository> repositories = Maps.newConcurrentMap();

  private final boolean skipDefaultRepositories;

  protected final BSM blobStoreManager;

  protected final GroupMemberMappingCache groupMemberMappingCache;

  protected final List<ConfigurationValidator> configurationValidators;

  protected final HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder;

  protected final FailedRepositoryTracker failedRepositoryTracker;

  /**
   * Lock map for retry operations to prevent concurrent retries for the same repository.
   */
  private final Map<String, Object> retryLocks = new ConcurrentHashMap<>();

  /**
   * Deduplicates concurrent materialization of the same repository: an origin {@link #create}, a replicated
   * create replayed by the configuration event handler, and a {@link #get} read-miss all funnel through
   * {@link #create}. The lead thread builds, starts and tracks the repository once; cooperating threads receive
   * its result instead of racing to materialize (and then tear down) a duplicate copy. Local (single-node)
   * concurrency controls only -- cross-node convergence is handled by the shared configuration store.
   */
  private final Cooperation2 configureCooperation = new DefaultCooperation2Factory().configure()
      .majorTimeout(Duration.ofSeconds(60))
      .minorTimeout(Duration.ofSeconds(30))
      .threadsPerKey(100)
      .build(BaseRepositoryManager.class);

  /**
   * Repositories currently being configured on the calling thread, mapped to their in-progress instance (null
   * until built). A facet/subscriber that fires while a repository is being built/started (before it is tracked)
   * can resolve {@link #get} for that same repository on this thread; the read-miss returns the in-progress
   * instance rather than re-entering {@link #create}, which would make the thread wait on its own in-flight
   * {@link #configureCooperation} future. Guards {@link #loadFromStoreIfPresent} against that self-wait.
   */
  private final ThreadLocal<Map<String, Repository>> configuringOnThread = ThreadLocal.withInitial(HashMap::new);

  @Nullable
  private final SecretEncoder ecrSecretEncoder;

  protected BaseRepositoryManager(
      final EventManager eventManager,
      final ConfigurationStore store,
      final RepositoryFactory factory,
      final Provider<ConfigurationFacet> configFacet,
      final List<Recipe> recipesList,
      final RepositoryAdminSecurityContributor securityContributor,
      final List<DefaultRepositoriesContributor> defaultRepositoriesContributors,
      final boolean skipDefaultRepositories,
      final BSM blobStoreManager,
      final GroupMemberMappingCache groupMemberMappingCache,
      final List<ConfigurationValidator> configurationValidators,
      final HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder,
      final FailedRepositoryTracker failedRepositoryTracker)
  {
    this(eventManager, store, factory, configFacet, recipesList, securityContributor,
        defaultRepositoriesContributors, skipDefaultRepositories, blobStoreManager, groupMemberMappingCache,
        configurationValidators, httpAuthenticationSecretEncoder, failedRepositoryTracker, null);
  }

  protected BaseRepositoryManager(
      final EventManager eventManager,
      final ConfigurationStore store,
      final RepositoryFactory factory,
      final Provider<ConfigurationFacet> configFacet,
      final List<Recipe> recipesList,
      final RepositoryAdminSecurityContributor securityContributor,
      final List<DefaultRepositoriesContributor> defaultRepositoriesContributors,
      final boolean skipDefaultRepositories,
      final BSM blobStoreManager,
      final GroupMemberMappingCache groupMemberMappingCache,
      final List<ConfigurationValidator> configurationValidators,
      final HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder,
      final FailedRepositoryTracker failedRepositoryTracker,
      @Nullable final SecretEncoder ecrSecretEncoder)
  {
    this.eventManager = requireNonNull(eventManager);
    this.store = requireNonNull(store);
    this.factory = requireNonNull(factory);
    this.configFacet = requireNonNull(configFacet);
    this.recipes = QualifierUtil.buildQualifierBeanMap(requireNonNull(recipesList));
    this.securityContributor = requireNonNull(securityContributor);
    this.defaultRepositoriesContributors = requireNonNull(defaultRepositoriesContributors);
    this.skipDefaultRepositories = skipDefaultRepositories;
    this.blobStoreManager = requireNonNull(blobStoreManager);
    this.groupMemberMappingCache = requireNonNull(groupMemberMappingCache);
    this.configurationValidators = requireNonNull(configurationValidators);
    this.httpAuthenticationSecretEncoder = requireNonNull(httpAuthenticationSecretEncoder);
    this.failedRepositoryTracker = requireNonNull(failedRepositoryTracker);
    this.ecrSecretEncoder = ecrSecretEncoder;
  }

  // Setter-injected (post-construction) and non-null once Spring has wired it; the null-tolerant check in
  // isClustered() is pure defense for any call that could occur before wiring completes.
  private NodeAccess nodeAccess;

  /**
   * Setter-injected (like other post-construction dependencies) so the concrete subclasses' constructors do
   * not need to change. Used to gate {@link #get}'s read-through materialization to clustered nodes only.
   */
  @Autowired
  public void setNodeAccess(final NodeAccess nodeAccess) {
    this.nodeAccess = requireNonNull(nodeAccess);
  }

  /**
   * Encode ECR secrets if encoder is available.
   */
  private void encodeEcrSecretsIfNeeded(final Map<String, Map<String, Object>> attributes) {
    if (ecrSecretEncoder != null) {
      ecrSecretEncoder.encodeSecrets(attributes);
    }
  }

  /**
   * Encode ECR secrets with old/new comparison if encoder is available.
   */
  private void encodeEcrSecretsIfNeeded(
      final Map<String, Map<String, Object>> oldAttributes,
      final Map<String, Map<String, Object>> newAttributes)
  {
    if (ecrSecretEncoder != null) {
      ecrSecretEncoder.encodeSecrets(oldAttributes, newAttributes);
    }
  }

  /**
   * Remove ECR secrets if encoder is available.
   */
  private void removeEcrSecretsIfNeeded(final Map<String, Map<String, Object>> attributes) {
    if (ecrSecretEncoder != null) {
      ecrSecretEncoder.removeSecret(attributes);
    }
  }

  /**
   * Remove ECR secrets with old/new comparison if encoder is available.
   */
  private void removeEcrSecretsIfNeeded(
      final Map<String, Map<String, Object>> oldAttributes,
      final Map<String, Map<String, Object>> newAttributes)
  {
    if (ecrSecretEncoder != null) {
      ecrSecretEncoder.removeSecret(oldAttributes, newAttributes);
    }
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
   * Check if a recipe is available by name.
   */
  private boolean isRecipeAvailable(final String name) {
    return recipes.containsKey(name);
  }

  /**
   * Lookup a repository by name.
   */
  private Repository repository(final String name) {
    Repository repository = repositories.get(name.toLowerCase());
    if (repository == null) {
      throw new MissingRepositoryException(name);
    }
    return repository;
  }

  /**
   * Construct a new repository from configuration.
   */
  protected Repository newRepository(final Configuration configuration) throws Exception {
    String recipeName = configuration.getRecipeName();
    Recipe recipe = recipe(recipeName);
    log.debug("Using recipe: [{}] {}", recipeName, recipe);

    Repository repository = factory.create(recipe.getType(), recipe.getFormat());

    // attach mandatory facets
    repository.attach(configFacet.get());

    // apply recipe to repository
    recipe.apply(repository, configuration);

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
  protected void track(final Repository repository) {
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
    List<Configuration> configurations = store.list();

    // attempt to provision default repositories if allowed
    if (configurations.isEmpty()) {
      if (skipDefaultRepositories || !blobStoreManager.exists(DEFAULT_BLOBSTORE_NAME)) {
        log.debug("Skipping provisioning of default repositories");
      }
      else {
        // attempt to discover default repository configurations
        log.debug("No repositories configured; provisioning default repositories");
        provisionDefaultRepositories();
        configurations = store.list();

        // if we still have no repository configurations, complain and stop
        if (configurations.isEmpty()) {
          log.debug("No default repositories to provision");
        }
      }
    }

    // Restore and start all configured repositories (both provisioned defaults and existing)
    restoreRepositories(configurations);

    startRepositories();

    groupMemberMappingCache.init(this);

    log.info("Completed initialization");
  }

  private void provisionDefaultRepositories() {
    for (DefaultRepositoriesContributor contributor : defaultRepositoriesContributors) {
      for (Configuration configuration : contributor.getRepositoryConfigurations()) {
        String repositoryName = configuration.getRepositoryName();
        log.debug("Provisioning default repository: {}", configuration);
        try {
          // Run ConfigurationValidators on the contributor-supplied config so
          // edition-specific guards (e.g. OciCloudEditionConfigurationValidator)
          // apply to default-repo seeding, not just the REST API write paths.
          // Per-repo failures are tracked rather than aborting the whole
          // provisioning loop — a single misconfigured contributor must not
          // prevent other defaults (and the rest of the manager) from booting.
          validateConfiguration(configuration);
          store.create(configuration);
          failedRepositoryTracker.clearFailure(repositoryName);
        }
        catch (Exception e) {
          log.error("Failed to provision default repository '{}' from contributor '{}'. "
              + "Repository will not be available; other defaults will continue to provision.",
              repositoryName, contributor.getClass().getSimpleName(), e);
          failedRepositoryTracker.recordFailure(repositoryName, e);
        }
      }
    }
  }

  private void restoreRepositories(final List<Configuration> configurations) throws Exception {
    log.info("Restoring {} repositories", configurations.size());
    for (Configuration configuration : configurations) {
      try {
        log.debug("Restoring repository: {}", configuration);
        Stopwatch stopwatch = Stopwatch.createStarted();
        String recipeName = configuration.getRecipeName();
        if (!isRecipeAvailable(recipeName)) {
          log.warn("Skipping repository '{}' because recipe '{}' is not available (possibly disabled by feature flag)",
              configuration.getRepositoryName(), recipeName);
          continue;
        }
        // Re-run ConfigurationValidators on restore so a backup taken on a
        // self-hosted node cannot smuggle a now-illegal config (e.g. an OCI
        // repo with forceBasicAuth=false) onto a cloud node. Per-repo
        // failures are caught below and tracked, so other repositories still
        // start up.
        validateConfiguration(configuration);
        Repository repository = newRepository(configuration);
        track(repository);
        failedRepositoryTracker.clearFailure(configuration.getRepositoryName());
        log.debug("Repository {} restored in {}", repository.getName(), stopwatch.stop());
        eventManager.post(new RepositoryLoadedEvent(repository));
      }
      catch (Exception e) {
        log.error("Failed to restore repository: {}. Repository will not be available.",
            configuration.getRepositoryName(), e);
        failedRepositoryTracker.recordFailure(configuration.getRepositoryName(), e);
        // Continue with other repositories - don't let one failure stop the entire system
      }
    }
  }

  private void startRepositories() throws Exception {
    log.info("Starting {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      try {
        log.debug("Starting repository: {}", repository);
        Stopwatch stopwatch = Stopwatch.createStarted();
        repository.start();
        failedRepositoryTracker.clearFailure(repository.getName());
        log.debug("Repository {} started in {}", repository.getName(), stopwatch.stop());
        eventManager.post(new RepositoryRestoredEvent(repository));
      }
      catch (Exception e) {
        log.error("Failed to start repository: {}. Repository will remain in stopped/failed state.",
            repository.getName(), e);
        failedRepositoryTracker.recordFailure(repository.getName(), e);
        // Continue with other repositories - don't let one failure stop the entire system
      }
    }
  }

  @Override
  protected void doStop() throws Exception {

    log.debug("Stopping {} repositories", repositories.size());
    for (Repository repository : repositories.values()) {
      log.debug("Stopping repository: {}", repository);
      repository.stopSafe();
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
  @Guarded(by = STARTED)
  public Iterable<Repository> browseForBlobStore(final String blobStoreId) {
    Iterable<Repository> browseResult = browse();

    if (browseResult != null && browseResult.iterator().hasNext()) {
      return stream(browseResult.spliterator(), true)
          .filter(Repository::isStarted)
          .filter(r -> blobStoreId.equals(r.getConfiguration().attributes(STORAGE).get(BLOB_STORE_NAME)))::iterator;
    }
    else {
      return Collections.emptyList();
    }

  }

  @Override
  public boolean exists(final String name) {
    return isRepositoryLoaded(name) || store.exists(name);
  }

  @Override
  public Stream<Configuration> getConfigurations() {
    return repositories.values()
        .stream()
        .map(Repository::getConfiguration);
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Repository get(final String name) {
    requireNonNull(name);
    String lcName = name.toLowerCase();
    Repository repository = repositories.get(lcName);
    if (repository == null && isClustered()) {
      // Read-through on a cache miss (clustered nodes only): the repository's configuration may be committed
      // to the shared store but not yet materialized on this node (e.g. it was just created on another node --
      // the create->use window). Materialize it from the store on demand and cache it so the miss self-heals.
      // Heals *creates* only; updates and deletes to an already-cached repository are propagated separately.
      // On a single node a miss is authoritative, so we skip the store round-trip entirely.
      repository = loadFromStoreIfPresent(lcName).orElse(null);
    }
    return repository;
  }

  private boolean isClustered() {
    // nodeAccess is setter-injected after construction; get() is @Guarded(by = STARTED) so it is always set by
    // the time this is reached, but guard defensively regardless.
    return nodeAccess != null && nodeAccess.isClustered();
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Repository softGet(final String name) {
    requireNonNull(name);

    String lcName = name.toLowerCase();
    return repositories.get(lcName);
  }

  /**
   * Materializes a repository from the shared configuration store on a {@link #get(String)} read-miss, so a
   * repository created on another node becomes usable here without waiting for the change to be replicated.
   * Delegates to {@link #create} under a replicating context -- exactly what the configuration event handler
   * does -- so the repository is built, started and tracked and its {@link RepositoryCreatedEvent} fires for
   * local subscribers, while the store write and the re-published distributed event are both suppressed. The
   * shared cooperation point in {@link #create} guarantees that a concurrent replicated create for the same
   * name materializes it exactly once. A single exact-match {@link ConfigurationStore#readByName} lookup keeps a
   * miss for a genuinely absent name (e.g. a 404) off the materialization path -- an empty result is
   * authoritative. Never throws.
   */
  private Optional<Repository> loadFromStoreIfPresent(final String lcName) {
    Map<String, Repository> configuring = configuringOnThread.get();
    if (configuring.containsKey(lcName)) {
      // Re-entrant read-miss for a repository already being configured on this thread (e.g. a facet/subscriber
      // resolving get(self) while the repository is starting, before it is tracked). Return the in-progress
      // instance rather than re-entering create() -- cooperation would make this thread wait on its own
      // in-flight materialization future (a self-wait that times out after ~60s+30s). The value is null only
      // during early validation, before the instance is built, where an empty result (a plain miss) is correct.
      return Optional.ofNullable(configuring.get(lcName));
    }
    // Read the persisted configuration directly; an empty result is an authoritative miss (a genuinely absent
    // repository, or one deleted between the map miss and here). Repository names are stored lowercased, so the
    // already-lowercased lcName matches.
    Optional<Configuration> config = store.readByName(lcName);
    if (config.isEmpty()) {
      return Optional.empty();
    }
    try {
      log.info("Materializing repository '{}' from store on read-miss", lcName);
      Supplier<Repository> materialize = () -> {
        try {
          return create(config.get());
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      };
      // A read-miss is a local, non-authoritative materialization of an already-persisted repository: run it as
      // a replication so create() skips the store write and does not re-publish the create back over DES, but
      // still builds, starts and tracks the repository and fires its event for local subscribers.
      Repository repository =
          EventHelper.isReplicating() ? materialize.get() : EventHelper.asReplicating(materialize);
      // The row can be deleted on a peer while we materialize (the delete event has not drained locally yet).
      // If it vanished, treat this as a miss rather than handing back a repository that no longer exists; the
      // tracked instance is reaped when the replicated delete event arrives (imminent under immediate
      // consistency). See NEXUS-54229.
      if (repository != null && store.readByName(lcName).isEmpty()) {
        log.debug("Repository '{}' was deleted during read-miss materialization; treating as a miss", lcName);
        return Optional.empty();
      }
      return Optional.ofNullable(repository);
    }
    catch (RuntimeException e) {
      log.error("Failed to materialize repository '{}' from store on read-miss: {}", lcName, e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Attempts to load a repository that previously failed to initialize or start.
   * This method is synchronized per-repository to prevent concurrent retry attempts.
   *
   * @param name the repository name
   * @return the loaded repository if successful, empty if still failing
   */
  @Guarded(by = STARTED)
  public Optional<Repository> retryFailedRepository(final String name) {
    requireNonNull(name);
    String lcName = name.toLowerCase();

    // If already loaded, just return it
    Repository existing = repositories.get(lcName);
    if (existing != null) {
      failedRepositoryTracker.clearFailure(name);
      return Optional.of(existing);
    }

    // Only retry if there's a recorded failure
    if (!failedRepositoryTracker.hasFailed(name)) {
      log.debug("Repository '{}' has no recorded failure, nothing to retry", name);
      return Optional.empty();
    }

    // Synchronize per-repository to prevent concurrent retries
    Object lock = retryLocks.computeIfAbsent(lcName, k -> new Object());
    synchronized (lock) {
      // Double-check after acquiring lock
      existing = repositories.get(lcName);
      if (existing != null) {
        failedRepositoryTracker.clearFailure(name);
        return Optional.of(existing);
      }

      // Guard: failure may have been cleared by another thread between outer check and lock acquisition
      if (!failedRepositoryTracker.hasFailed(lcName)) {
        log.debug("Retry skipped for '{}': failure was cleared by another thread", name);
        return Optional.empty();
      }

      return retrieveConfigurationByName(lcName)
          .flatMap(config -> {
            try {
              log.info("Retrying failed repository: {}", name);
              Repository repository = newRepository(config);
              track(repository);
              repository.start();
              failedRepositoryTracker.clearFailure(name);
              log.info("Repository '{}' successfully recovered", name);
              eventManager.post(new RepositoryRestoredEvent(repository));
              return Optional.of(repository);
            }
            catch (Exception e) {
              log.error("Retry failed for repository '{}': {}", name, e.getMessage(), e);
              failedRepositoryTracker.recordFailure(name, e);
              return Optional.empty();
            }
          });
    }
  }

  /**
   * Gets the tracker for failed repositories.
   *
   * @return the failed repository tracker
   */
  public FailedRepositoryTracker getFailedRepositoryTracker() {
    return failedRepositoryTracker;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository create(final Configuration configuration) throws Exception {
    requireNonNull(configuration);
    String repositoryName = requireNonNull(configuration.getRepositoryName());
    String lcName = repositoryName.toLowerCase();

    log.info("Creating repository: {} -> {}", repositoryName, configuration);

    // Origin creates, replicated creates (replayed by the configuration event handler) and get() read-miss
    // materializations all funnel through this one cooperation point keyed by name, so the repository is
    // built/started/tracked exactly once even when two of those paths fire concurrently on the same node.
    Map<String, Repository> configuring = configuringOnThread.get();
    if (configuring.containsKey(lcName)) {
      // Re-entrant create for a repository already being configured on this thread (e.g. a facet/subscriber
      // that resolves this same repository during its own construction/start). Never re-enter the cooperation
      // point -- that would make this thread wait on its own in-flight materialization future (a ~60s+30s
      // self-wait). Return the in-progress instance; it is null only before newRepository() has built it, in
      // which case the caller sees a plain miss rather than a stall.
      return configuring.get(lcName);
    }
    configuring.put(lcName, null);
    try {
      return configureCooperation.on(() -> doCreate(configuration))
          .checkFunction(() -> Optional.ofNullable(repositories.get(lcName)))
          .cooperate("create", lcName);
    }
    catch (IOException e) {
      // doCreate wraps a non-IO checked failure as IOException; restore the original cause for callers.
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw e;
    }
    finally {
      configuring.remove(lcName);
    }
  }

  /**
   * Builds, persists (origin only), starts and tracks a repository, then fires {@link RepositoryCreatedEvent}.
   * This is the single "configure a repository" path shared by an origin {@link #create}, a replicated create
   * replayed by the configuration event handler, and a {@link #get} read-miss. Under a replicating context (the
   * latter two) the store write is skipped -- the configuration already exists -- and re-publishing the create
   * back over DES is suppressed, but the repository is still materialized locally and {@link RepositoryCreatedEvent}
   * still fires for local subscribers. Invoked as the {@link #configureCooperation} work function, so checked
   * non-IO failures are wrapped as {@link IOException} (unwrapped by {@link #create}); unchecked failures
   * propagate unchanged.
   */
  private Repository doCreate(final Configuration configuration) throws IOException {
    if (EventHelper.isReplicating()) {
      // A replicated create or read-miss materialization is idempotent: if the repository was already
      // materialized (e.g. by the event handler between a read-miss check and here) adopt it instead of
      // building a duplicate that would leak and, for path-routed formats, deregister the live instance.
      Repository existing = repositories.get(configuration.getRepositoryName().toLowerCase());
      if (existing != null) {
        return existing;
      }
    }

    Repository repository;
    try {
      if (!EventHelper.isReplicating()) {
        httpAuthenticationSecretEncoder.encodeHttpAuthPassword(configuration.getAttributes());
        encodeEcrSecretsIfNeeded(configuration.getAttributes());
      }
      validateConfiguration(configuration);

      repository = newRepository(configuration);
      // Publish the in-progress instance so a re-entrant get(self) on this thread (a facet/subscriber resolving
      // the repository while it starts) returns it instead of re-entering create() and self-waiting.
      configuringOnThread.get().put(configuration.getRepositoryName().toLowerCase(), repository);

      if (!EventHelper.isReplicating()) {
        store.create(configuration);
      }
    }
    catch (RuntimeException e) {
      httpAuthenticationSecretEncoder.removeSecret(configuration.getAttributes());
      removeEcrSecretsIfNeeded(configuration.getAttributes());
      throw e;
    }
    catch (Exception e) {
      httpAuthenticationSecretEncoder.removeSecret(configuration.getAttributes());
      removeEcrSecretsIfNeeded(configuration.getAttributes());
      throw new IOException(e);
    }

    try {
      repository.start();
    }
    // No secret cleanup on a start() failure: on the origin path store.create() has already committed the
    // configuration row (secrets included), so the repository exists and is tended by normal lifecycle / delete;
    // on the replicating path no secrets were encoded here. Secret cleanup applies only to a pre-persist failure
    // in the block above.
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new IOException(e);
    }

    track(repository);

    eventManager.post(new RepositoryCreatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository update(final Configuration configuration) throws Exception {
    requireNonNull(configuration);
    String repositoryName = requireNonNull(configuration.getRepositoryName());

    log.info("Updating repository: {} -> {}", repositoryName, configuration);

    validateConfiguration(configuration);

    Repository repository = repository(repositoryName);

    final Configuration oldConfiguration = repository.getConfiguration().copy();

    // NEXUS-53393: preserve omitted firewall block from old config — see helper Javadoc.
    // Replication should faithfully apply the originator's authoritative state, not
    // heuristically merge — so skip preservation on the replicating thread.
    //
    // Ordering note (load-bearing): this runs AFTER validateConfiguration(...) above. The
    // preserved firewall mode therefore is not re-checked by FirewallConfigurationValidator,
    // which is the right trade-off — the mode was already valid when stored, and re-validating
    // could spuriously reject a no-change PUT when CLM/IQ is temporarily unavailable. Do not
    // move this above validateConfiguration.
    if (!EventHelper.isReplicating()) {
      preserveFirewallFromExistingIfAbsent(oldConfiguration, configuration, repositoryName);
    }

    try {
      if (!EventHelper.isReplicating()) {
        httpAuthenticationSecretEncoder.encodeHttpAuthPassword(oldConfiguration.getAttributes(),
            configuration.getAttributes());
        encodeEcrSecretsIfNeeded(oldConfiguration.getAttributes(), configuration.getAttributes());
      }

      // ensure configuration sanity
      repository.validate(configuration);

      repository.stopSafe();

      if (!EventHelper.isReplicating()) {
        store.update(configuration);
        httpAuthenticationSecretEncoder.removeSecret(oldConfiguration.getAttributes(), configuration.getAttributes());
        removeEcrSecretsIfNeeded(oldConfiguration.getAttributes(), configuration.getAttributes());
      }
    }
    catch (Exception e) {
      httpAuthenticationSecretEncoder.removeSecret(configuration.getAttributes(), oldConfiguration.getAttributes());
      removeEcrSecretsIfNeeded(configuration.getAttributes(), oldConfiguration.getAttributes());
      throw e;
    }

    repository.update(configuration);
    repository.start();

    eventManager.post(new RepositoryUpdatedEvent(repository, oldConfiguration));

    return repository;
  }

  /**
   * NEXUS-53393: carry the persisted {@code firewall} sub-block from {@code existing} onto
   * {@code target} when {@code target} does not already carry one.
   * <p>
   * The typed REST API request-to-Configuration converters (see
   * {@code ProxyRepositoryApiRequestToConfigurationConverter#convertFirewall}) only write the
   * {@code firewall} block when the request body supplies a non-null {@code mode}. As a result,
   * an omitted {@code firewall} field, {@code firewall: null}, {@code firewall: \{\}} and
   * {@code firewall: \{"mode": null\}} are all indistinguishable in the converted Configuration
   * — they all surface as "no firewall key in the attributes map". Without this helper, every
   * such partial update would silently wipe the persisted {@code firewall.mode}, dropping
   * firewall protection on the repository.
   * <p>
   * The contract intentionally accepts that ambiguity: a caller can clear firewall only by
   * sending an explicit {@code mode} value (canonically {@link
   * org.sonatype.nexus.repository.firewall.FirewallMode#DISABLED}). All other shapes are
   * treated as "do not change".
   * <p>
   * Defensive copy: subsequent code in {@link #update(Configuration)} (validation, secret
   * encoding on the {@code httpclient} sub-block, the rollback path) operates on the new
   * Configuration and may iterate or mutate the attributes map. The defensive {@code putAll}
   * keeps the firewall sub-map independent of the existing Configuration's reference, so
   * mutations to either side after this call cannot bleed across.
   * <p>
   * The same omission-clears semantic affects other optional sub-sections (cleanup,
   * negativeCache, httpClient, storage, format-specific blocks); they are out of scope for
   * NEXUS-53393 and tracked separately.
   */
  @VisibleForTesting
  static void preserveFirewallFromExistingIfAbsent(
      final Configuration existing,
      final Configuration target,
      final String repositoryName)
  {
    Map<String, Map<String, Object>> targetAttrs = target.getAttributes();
    if (targetAttrs != null && targetAttrs.containsKey(FIREWALL)) {
      // Target already carries a firewall block. The typed REST converter only writes one when
      // the request body supplies a non-null mode (so PCCS/AUDIT/QUARANTINE/DISABLED all land
      // here, including the canonical clear via mode=DISABLED). The same guard also handles
      // any other code path that may have planted a firewall sub-map on target before this
      // helper ran (e.g. validation or pre-processing that called attributes(FIREWALL) and
      // triggered the lazy-init computeIfAbsent in ConfigurationData). In every case, leave
      // target's firewall block untouched — explicit caller intent wins.
      return;
    }
    Map<String, Map<String, Object>> existingAttrs = existing.getAttributes();
    if (existingAttrs == null) {
      return;
    }
    Map<String, Object> existingFirewall = existingAttrs.get(FIREWALL);
    if (existingFirewall == null || existingFirewall.isEmpty()
        || existingFirewall.values().stream().allMatch(Objects::isNull)) {
      // Common case for repositories that have never had firewall configured: the existing
      // firewall map is either absent, empty, or contains only null-valued entries (e.g.
      // {mode=null} planted by the UI before a firewall mode is assigned). ConcurrentHashMap
      // (which backs target's attribute sub-maps) does not allow null values and would NPE on
      // putAll; a null-only map carries no actionable configuration (NEXUS-48685). Either way
      // there is nothing meaningful to carry forward, so leave target alone.
      return;
    }
    log.debug(
        "Preserving existing firewall configuration on '{}' update because the request did not redefine it: {}",
        repositoryName, existingFirewall);
    // ConcurrentHashMap (backing the target attribute sub-map) does not permit null values.
    // Filter them before copying so that a mixed map like {mode=QUARANTINE, someKey=null}
    // still preserves its non-null entries (NEXUS-48685).
    Map<String, Object> firewallBacking = target.attributes(FIREWALL).backing();
    existingFirewall.forEach((k, v) -> {
      if (v != null) {
        firewallBacking.put(k, v);
      }
    });
  }

  protected void validateConfiguration(final Configuration configuration) {
    Set<ConstraintViolation<?>> violations = new HashSet<>();
    configurationValidators.forEach(
        validator -> maybeAdd(violations, validator.validate(configuration)));

    maybePropagate(violations, log);
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final String name) throws Exception {
    requireNonNull(name);

    log.info("Deleting repository: {}", name);

    // Handle already-deleted repositories during event replication
    Repository repository = repositories.get(name.toLowerCase());
    if (repository == null) {
      if (EventHelper.isReplicating()) {
        log.debug("Repository '{}' already deleted during event replication, skipping", name);
        return; // already deleted is OK during replication
      }

      // Check if this is a failed repository that needs cleanup
      if (failedRepositoryTracker.hasFailed(name.toLowerCase())) {
        log.info("Deleting failed repository: {}", name);
        Configuration configuration = store.list()
            .stream()
            .filter(c -> c.getRepositoryName().equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new MissingRepositoryException(name));
        store.delete(configuration);
        failedRepositoryTracker.clearFailure(name);
        log.info("Deleted failed repository: {}", name);
        return;
      }

      throw new MissingRepositoryException(name);
    }

    Configuration configuration = repository.getConfiguration().copy();

    // Only remove from groups on the originating node to prevent HA race conditions
    // Remote nodes skip this since groups are already updated by originating node
    if (!EventHelper.isReplicating()) {
      removeRepositoryFromAllGroups(repository);
    }

    repository.stopSafe();
    repository.delete();
    repository.destroy();

    if (!EventHelper.isReplicating()) {
      httpAuthenticationSecretEncoder.removeSecret(configuration.getAttributes());
      removeEcrSecretsIfNeeded(configuration.getAttributes());
      store.delete(configuration);
    }

    untrack(repository);

    eventManager.post(new RepositoryDeletedEvent(repository));

    log.info("Deleted repository: {}", name);
  }

  @Override
  @Guarded(by = STARTED)
  public void softDelete(final String name, final String deletedBy) throws Exception {
    requireNonNull(name);
    requireNonNull(deletedBy);

    log.info("Soft-deleting repository: {}", name);

    Repository repository = repositories.get(name.toLowerCase());
    if (repository == null) {
      throw new MissingRepositoryException(name);
    }

    if (!EventHelper.isReplicating()) {
      removeRepositoryFromAllGroups(repository);
    }

    repository.stopSafe();

    repository.optionalFacet(SearchIndexFacet.class).ifPresent(searchFacet -> {
      try {
        log.debug("Cleaning search index for soft-deleted repository: {}", name);
        searchFacet.deleteIndex();
      }
      catch (Exception e) {
        log.warn("Failed to clean search index for soft-deleted repository: {}", name, e);
      }
    });

    repository.destroy();

    untrack(repository);

    if (!EventHelper.isReplicating()) {
      store.softDelete(name, deletedBy);
    }

    eventManager.post(new RepositoryDeletedEvent(repository));

    log.info("Soft-deleted repository: {}", name);
  }

  @Override
  @Guarded(by = STARTED)
  public Repository restoreFromSoftDelete(final String name) throws Exception {
    requireNonNull(name);

    log.info("Restoring soft-deleted repository: {}", name);

    store.undoSoftDelete(name);

    Configuration configuration = store.readByNames(Collections.singleton(name))
        .stream()
        .findFirst()
        .orElseThrow(() -> new MissingRepositoryException(name));

    Repository repository = newRepository(configuration);
    track(repository);
    repository.start();

    repository.optionalFacet(SearchIndexFacet.class).ifPresent(searchFacet -> {
      try {
        log.debug("Rebuilding search index for restored repository: {}", name);
        searchFacet.rebuildIndex();
      }
      catch (Exception e) {
        log.warn("Failed to rebuild search index for restored repository: {}", name, e);
      }
    });

    eventManager.post(new RepositoryCreatedEvent(repository));

    log.info("Restored soft-deleted repository: {}", name);
    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public void hardDelete(final String name) throws Exception {
    requireNonNull(name);

    log.info("Hard-deleting soft-deleted repository: {}", name);

    Configuration configuration = store.readDeleted(name);
    if (configuration == null) {
      throw new IllegalStateException("Repository '" + name + "' is not in soft-deleted state");
    }

    // Re-instantiate temporarily to run facet content cleanup
    Repository repository = newRepository(configuration);
    try {
      repository.start();
      repository.stop();
      repository.delete();
      repository.destroy();
    }
    catch (Exception e) {
      log.warn("Error during content cleanup for hard-delete of '{}'; proceeding with config removal", name, e);
      try {
        repository.destroy();
      }
      catch (Exception ignored) { // NOSONAR
      }
    }

    store.hardDelete(name);
    httpAuthenticationSecretEncoder.removeSecret(configuration.getAttributes());

    log.info("Hard-deleted repository: {}", name);
  }

  /**
   * Retrieve a list of all groups that contain the desired repository, either directly or transitively through
   * another group.
   *
   * @since 3.14
   *
   * @param repositoryName
   * @return Set of group(s) that contain the supplied repository
   */
  @Override
  @Guarded(by = STARTED)
  public Set<String> findContainingGroups(final String repositoryName) {
    return groupMemberMappingCache.getGroups(repositoryName);
  }

  private void removeRepositoryFromAllGroups(final Repository repositoryToRemove) {
    for (Repository group : repositories.values()) {
      try {
        Optional<GroupFacet> groupFacet = group.optionalFacet(GroupFacet.class);
        if (groupFacet.isPresent() && groupFacet.get().member(repositoryToRemove)) {
          removeRepositoryFromGroup(repositoryToRemove, group);
        }
      }
      catch (Exception e) {
        // A concurrent delete/update may transiently stop this group, so the @Guarded(by = STARTED)
        // member() check (or the subsequent update) can throw InvalidStateException / other lifecycle
        // errors. Skip this group and continue so one in-flight group does not abort the whole delete
        // (NEXUS-53816). If the group was only briefly stopped, the stale member self-heals on its next
        // member change or config update.
        log.warn("Failed to remove repository '{}' from group '{}'", repositoryToRemove.getName(), group.getName(),
            e);
      }
    }
  }

  private void removeRepositoryFromGroup(final Repository repositoryToRemove, final Repository group) throws Exception {
    Configuration configuration = group.getConfiguration().copy();

    // IMPORTANT: Configuration.copy() creates a shallow copy that loses the repository ID.
    // Restore the ID from the original configuration to ensure the update operation targets
    // the correct repository. Without this, the update may fail or target the wrong entity.
    if (configuration instanceof ConfigurationData) {
      ((ConfigurationData) configuration).setId(group.getConfiguration().getRepositoryId());
    }

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
    // Query the shared configuration store rather than this node's in-memory repository cache: under HA the
    // cache can lag a peer node's repository create/delete, producing a false "in use" (blocking a legitimate
    // blob store delete -- the S3BlobStoreIT teardown 400) or a false "not used" (allowing deletion of a blob
    // store still referenced by a not-yet-materialized repository). The store reflects the committed cross-node
    // truth. See NEXUS-54229. The second clause still covers group blob stores that embed this member.
    return store.countByBlobStore(blobStoreName) > 0 ||
        blobStoreManager.blobStoreUsageCount(blobStoreName) > 0;
  }

  @Override
  public Set<String> blobstoresInUse() {
    // Reads all repository configurations from the shared store in one query (store.list() -> full rows) and
    // extracts the distinct storage.blobStoreName values in memory -- one round-trip for the whole blob store
    // list UI instead of an isBlobstoreUsed() query per blob store. The repository table is small, so the full
    // read is cheap; the countByBlobStore expression index backs the per-name isBlobstoreUsed() path instead.
    // See NEXUS-54229.
    return store.list()
        .stream()
        .map(Configuration::getAttributes)
        .filter(Objects::nonNull)
        .map(attributes -> attributes.get(STORAGE))
        .filter(Objects::nonNull)
        .map(storage -> storage.get(BLOB_STORE_NAME))
        .filter(Objects::nonNull)
        .map(String.class::cast)
        .collect(toSet());
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
  public void on(final RepositoryRemoteConnectionStatusEvent event) {
    if (event.isLocal()) {
      return;
    }
    String repositoryName = event.getRepositoryName();

    if (isRepositoryLoaded(repositoryName)) {
      // Event shouldn't be propagated if repository isn't propagated yet to the current node
      RemoteConnectionStatusType statusType =
          RemoteConnectionStatusType.values()[event.getRemoteConnectionStatusTypeOrdinal()];
      // restore RemoteConnectionStatus from event
      log.debug("Consume distributed RepositoryRemoteConnectionStatusEvent: repository={}, type={}",
          repositoryName, statusType);
      RemoteConnectionStatus status = new RemoteConnectionStatus(statusType, event.getReason())
          .setBlockedUntil(new DateTime(event.getBlockedUntilMillis()))
          .setRequestUrl(event.getRequestUrl());

      Repository repository = repository(repositoryName);
      if (repository.isStarted()) {
        repository.facet(HttpClientFacet.class).setStatus(status);
      }
    }
  }

  /*
   * Use the ConfigurationStore to find a Configuration by name if it exists.
   */
  @Override
  public Optional<Configuration> retrieveConfigurationByName(final String name) {
    String lcName = name.toLowerCase();
    return store.list()
        .stream()
        .filter(candidate -> lcName.equals(candidate.getRepositoryName().toLowerCase()))
        .findAny();
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

  private boolean isRepositoryLoaded(final String repositoryName) {
    return repositories.containsKey(repositoryName.toLowerCase());
  }
}
