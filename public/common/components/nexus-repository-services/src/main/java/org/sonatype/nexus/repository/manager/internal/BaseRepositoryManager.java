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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;
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
    this.eventManager = checkNotNull(eventManager);
    this.store = checkNotNull(store);
    this.factory = checkNotNull(factory);
    this.configFacet = checkNotNull(configFacet);
    this.recipes = QualifierUtil.buildQualifierBeanMap(checkNotNull(recipesList));
    this.securityContributor = checkNotNull(securityContributor);
    this.defaultRepositoriesContributors = checkNotNull(defaultRepositoriesContributors);
    this.skipDefaultRepositories = skipDefaultRepositories;
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.groupMemberMappingCache = checkNotNull(groupMemberMappingCache);
    this.configurationValidators = checkNotNull(configurationValidators);
    this.httpAuthenticationSecretEncoder = checkNotNull(httpAuthenticationSecretEncoder);
    this.failedRepositoryTracker = checkNotNull(failedRepositoryTracker);
    this.ecrSecretEncoder = ecrSecretEncoder;
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
        log.debug("Provisioning default repository: {}", configuration);
        store.create(configuration);
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
    checkNotNull(name);
    return repositories.get(name.toLowerCase());
  }

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Repository softGet(final String name) {
    checkNotNull(name);

    String lcName = name.toLowerCase();
    return repositories.get(lcName);
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
    checkNotNull(name);
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
    checkNotNull(configuration);
    String repositoryName = checkNotNull(configuration.getRepositoryName());

    log.info("Creating repository: {} -> {}", repositoryName, configuration);

    Repository repository;

    try {
      if (!EventHelper.isReplicating()) {
        httpAuthenticationSecretEncoder.encodeHttpAuthPassword(configuration.getAttributes());
        encodeEcrSecretsIfNeeded(configuration.getAttributes());
      }
      validateConfiguration(configuration);

      repository = newRepository(configuration);

      if (!EventHelper.isReplicating()) {
        store.create(configuration);
      }
    }
    catch (Exception e) {
      httpAuthenticationSecretEncoder.removeSecret(configuration.getAttributes());
      removeEcrSecretsIfNeeded(configuration.getAttributes());
      throw e;
    }
    repository.start();

    track(repository);

    eventManager.post(new RepositoryCreatedEvent(repository));

    return repository;
  }

  @Override
  @Guarded(by = STARTED)
  public Repository update(final Configuration configuration) throws Exception {
    checkNotNull(configuration);
    String repositoryName = checkNotNull(configuration.getRepositoryName());

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
    if (existingFirewall == null || existingFirewall.isEmpty()) {
      // Common case for repositories that have never had firewall configured: the existing
      // firewall map is either absent or an empty map planted by Configuration.attributes(key)
      // lazy-init via FirewallConfigurationHelper.getFirewallMode(...) /
      // FirewallAttributes.fromConfiguration(...). Either way there is nothing meaningful to
      // carry forward, so leave target alone (don't synthesise an empty firewall block).
      return;
    }
    log.debug(
        "Preserving existing firewall configuration on '{}' update because the request did not redefine it: {}",
        repositoryName, existingFirewall);
    target.attributes(FIREWALL).backing().putAll(existingFirewall);
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
    checkNotNull(name);

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
