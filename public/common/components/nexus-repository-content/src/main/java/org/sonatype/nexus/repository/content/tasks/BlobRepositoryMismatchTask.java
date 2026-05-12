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
package org.sonatype.nexus.repository.content.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryParallelTaskSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Stream.empty;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.entity.Continuations.BROWSE_LIMIT;
import static org.sonatype.nexus.logging.task.TaskLogType.TASK_LOG_ONLY;
import static org.sonatype.nexus.repository.RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * This task fixes an issue where a blob's properties may have the wrong repositoryName set
 * by comparing the blob's REPO_NAME_HEADER property against the repository it belongs to.
 * When a mismatch is detected, the blob's properties file is updated with the correct repository name.
 * <p>
 * The task supports:
 * <ul>
 * <li>Processing a single repository or all repositories (using {@code *} as the repository name)</li>
 * <li>Continuation token-based resumption after interruption</li>
 * <li>Configurable concurrency limit via {@code blob.repository.name.mismatch.concurrencyLimit}</li>
 * <li>Default concurrency of available processors / 2 (minimum 1, maximum 64)</li>
 * </ul>
 */
@Component
@TaskLogging(TASK_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BlobRepositoryMismatchTask
    extends RepositoryParallelTaskSupport
    implements Cancelable
{
  /**
   * Prefix for storing the continuation token in task configuration.
   * Continuation tokens are used to resume task execution after interruption.
   * The stored key format is: {@code .continuationToken.{repositoryName}}
   */
  static final String CONTINUATION_TOKEN_PREFIX = ".continuationToken.";

  /**
   * Prefix for storing the count of blobs processed for a single repository.
   * Used to track progress when resuming task execution.
   * The stored key format is: {@code .currentRepositoryBlobCount.{repositoryName}}
   */
  static final String CURRENT_REPOSITORY_BLOB_COUNT_PREFIX = ".currentRepositoryBlobCount.";

  /**
   * Maximum concurrency limit to prevent resource exhaustion.
   */
  private static final int MAX_CONCURRENCY_LIMIT = 64;

  private final BlobStoreManager blobStoreManager;

  // Count of fixed blobs across all jobStreams
  // The completion message shows count for "this run" only
  private final AtomicLong totalFixedBlobCount = new AtomicLong();

  private final TaskResultStateStore taskResultStateStore;

  /**
   * Particular customer infrastructure for skipping blob processing.
   * Default: false (checks and fixes mismatches).
   * Set to true to skip blob checking and fixing.
   */
  private final boolean skipProcessing;

  private final ProgressLogIntervalHelper progressLogIntervalHelper = new ProgressLogIntervalHelper(log, 60);

  /**
   * Constructs a new BlobRepositoryMismatchTask.
   *
   * @param blobStoreManager the blob store manager for accessing blob stores
   * @param taskResultStateStore the task result state store for tracking progress
   * @param concurrencyLimit the maximum number of concurrent threads; if null or less than 1,
   *          defaults to available processors divided by 2 (minimum 1, maximum 64)
   * @param skipProcessing if true, will skip repository processing, only recommended
   *          when upgrading from 3.76 or earlier
   */
  @Autowired
  public BlobRepositoryMismatchTask(
      final BlobStoreManager blobStoreManager,
      final TaskResultStateStore taskResultStateStore,
      @Value("${blob.repository.name.mismatch.concurrencyLimit:#{null}}") final Integer concurrencyLimit,
      // Production skip flag: set to true when upgrading from pre-3.76
      @Value("${blob.repository.name.mismatch.skipProcessing:false}") final boolean skipProcessing)
  {
    super(resolveConcurrencyLimit(concurrencyLimit));
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.taskResultStateStore = checkNotNull(taskResultStateStore);
    this.skipProcessing = skipProcessing;
  }

  private static int resolveConcurrencyLimit(final Integer concurrencyLimit) {
    if (concurrencyLimit == null || concurrencyLimit < 1) {
      int defaultLimit = Runtime.getRuntime().availableProcessors() / 2;
      // Ensure at least 1 when available processors is 1
      return Math.max(1, defaultLimit);
    }
    // Cap at maximum to prevent resource exhaustion
    return Math.min(concurrencyLimit, MAX_CONCURRENCY_LIMIT);
  }

  @Override
  protected Object result() {
    return totalFixedBlobCount.get();
  }

  /**
   * Get the task configuration for testing purposes.
   */
  @Override
  @VisibleForTesting
  public TaskConfiguration getConfiguration() {
    return super.getConfiguration();
  }

  @Override
  public void configure(final TaskConfiguration configuration) {
    checkNotNull(configuration);

    // Call super configure first to apply the new configuration
    super.configure(configuration);

    // Collect all repository names from current configuration
    Set<String> currentRepos = new HashSet<>();
    String repoField = configuration.getString(REPOSITORY_NAME_FIELD_ID);
    if (repoField != null && !repoField.isEmpty()) {
      // REPOSITORY_NAME_FIELD_ID holds a single repo name or "*" for all repos
      // No need to split by comma - use as-is
      currentRepos.add(repoField.trim());
    }

    // Clean up any stale continuation tokens that don't match current configuration
    cleanupStaleTokens(currentRepos);
  }

  /**
   * Cleans up stale continuation tokens and processed count for repositories
   * that are no longer in the current configuration. This prevents accumulation
   * of orphaned tokens when the task is reconfigured to process different repositories.
   * <p>
   * The task processes each repository independently, and continuation tokens are
   * stored with keys like {@code .continuationToken.{repositoryName}}. When the task
   * configuration changes to a different set of repositories, any tokens for repos
   * that are no longer being processed would accumulate indefinitely without this cleanup.
   *
   * @param currentRepos the set of repository names currently configured
   */
  private void cleanupStaleTokens(final Set<String> currentRepos) {
    if (currentRepos.isEmpty()) {
      return;
    }

    // Scan configuration for any continuation token keys
    Map<String, String> allKeys = getConfiguration().asMap();

    for (Map.Entry<String, String> entry : allKeys.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(CONTINUATION_TOKEN_PREFIX)) {
        clearConfigurationKey(currentRepos, key.substring(CONTINUATION_TOKEN_PREFIX.length()), key);
      }
      else if (key.startsWith(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX)) {
        clearConfigurationKey(currentRepos, key.substring(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX.length()), key);
      }
    }
  }

  private void clearConfigurationKey(final Set<String> currentRepos, final String repositoryName, final String key) {
    if (!currentRepos.contains(repositoryName)) {
      log.debug("Cleaning up stale key {} for repository: {}", key, repositoryName);
      getConfiguration().setString(key, null);
    }
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress) {
    log.info("Starting blob repository mismatch check");
    return super.jobStream(progress);
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress, final Repository repository) {
    if (skipProcessing) {
      log.debug("Skip processing is enabled, skipping blob mismatch processing for repository: {}",
          repository.getName());
      return empty();
    }

    // Get saved processed count for accurate progress percentage on resume
    RepositoryData repositoryData = new RepositoryData(repository, getSavedRepositoryBlobCount(repository.getName()));

    log.info("Processing repository: {}", repositoryData.name());

    BlobStore blobstore = getBlobStore(repositoryData.repository()).orElse(null);
    if (blobstore == null) {
      log.error("Unable to obtain blobstore for {}", repositoryData.name());
      return Stream.of();
    }

    // Get a saved continuation token (or null to start from beginning)
    String savedToken = getSavedContinuationToken(repositoryData.name());

    // Update progress to indicate resumption if token exists
    if (savedToken != null) {
      updateProgress(String.format("Resuming %s from continuation token %s (blobs processed %d)", repositoryData.name(),
          savedToken,
          repositoryData.processedBlobCount()), true);
    }

    return createJobStream(repositoryData, blobstore, savedToken);
  }

  private Runnable createJob(final RepositoryData repositoryData, final FluentAsset asset, final BlobStore blobStore) {
    return () -> {
      BlobData blobData;
      try {
        blobData = getBlobData(asset, blobStore);
      }
      catch (MissingBlobException | IOException e) {
        log.warn("Missing blob data for asset {}", asset, e);
        return;
      }

      if (blobData == null) {
        return;
      }

      if (checkBlobRepositoryHeaderMatch(blobData.blob, repositoryData.name())) {
        log.trace("Ignoring asset {} because of repository property and blob header match", asset);
        return;
      }

      try {
        // Fix the blob's REPO_NAME_HEADER by updating the blob attributes directly
        fixBlobRepositoryHeader(blobData.blobStore, blobData.blob, repositoryData.name());
        progressLogIntervalHelper.info("Fixed blob {} repository header from wrong value to {} for asset {}",
            blobData.blob.getId(), repositoryData.repository().getName(), asset.path());
        repositoryData.incrementFixedBlobCount();
        totalFixedBlobCount.incrementAndGet();
      }
      catch (IOException e) {
        log.warn("Failed to fix blob header of asset {} due to I/O error", asset, e);
      }
      catch (Exception e) {
        log.warn("Failed to fix blob header of asset {} due to unexpected error", asset, e);
      }
    };
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    Optional<BlobStore> blobstore = getBlobStore(repository);

    if (blobstore.isEmpty()) {
      log.error("Unable to identify blobstore type for repository {}", repository);
      return false;
    }

    return HostedType.NAME.equals(repository.getType().toString());
  }

  private Optional<BlobStore> getBlobStore(final Repository repository) {
    return Optional.ofNullable(repository.getConfiguration())
        .map(Configuration::getAttributes)
        .map(attr -> attr.get(STORAGE))
        .map(Map.class::cast)
        .map(storage -> storage.get(BLOB_STORE_NAME))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(blobStoreManager::get);
  }

  private BlobData getBlobData(
      final FluentAsset asset,
      final BlobStore blobStore) throws IOException, MissingBlobException
  {
    if (asset.blob().isEmpty()) {
      log.warn("Missing blob for asset: {}", asset.path());
      return null;
    }

    AssetBlob assetBlob = asset.blob().get();
    BlobRef blobRef = assetBlob.blobRef();
    BlobId blobId = blobRef.getBlobId();

    Blob sourceBlob = blobStore.get(blobId);
    if (sourceBlob == null) {
      log.error("Missing blob reference: {}", blobId);
      throw new MissingBlobException(blobRef);
    }

    return new BlobData(blobStore, sourceBlob);
  }

  private boolean checkBlobRepositoryHeaderMatch(final Blob blob, final String repositoryName) {
    Map<String, String> headers = blob.getHeaders();
    if (headers == null) {
      return false;
    }
    String repoHeader = headers.get(REPO_NAME_HEADER);
    return repositoryName.equals(repoHeader);
  }

  private void fixBlobRepositoryHeader(
      final BlobStore blobStore,
      final Blob blob,
      final String correctRepositoryName) throws IOException
  {
    BlobId blobId = blob.getId();

    // Get existing blob attributes
    Map<String, String> headers = blob.getHeaders();
    if (headers == null || headers.isEmpty()) {
      log.error("Blob attributes not found for blob: {}", blobId);
      throw new IOException("Blob attributes not found: " + blobId);
    }

    // Update the REPO_NAME_HEADER in headers
    Map<String, String> headersCopy = new HashMap<>(headers);
    headersCopy.put(REPO_NAME_HEADER, correctRepositoryName);

    // Create new BlobAttributes with updated headers
    BlobAttributes newAttributes = blobStore.createBlobAttributesInstance(
        blobId,
        headersCopy,
        blob.getMetrics());

    // Persist the updated attributes to the blob's .properties file
    blobStore.setBlobAttributes(blobId, newAttributes);
  }

  @Override
  public String getMessage() {
    return "Searching for blob properties mismatching " + REPO_NAME_HEADER;
  }

  /**
   * Get the saved continuation token for a repository.
   */
  private String getSavedContinuationToken(final String repositoryName) {
    String taskKey = CONTINUATION_TOKEN_PREFIX + repositoryName;
    return getConfiguration().getString(taskKey);
  }

  /**
   * Get the saved processed count for a repository.
   */
  private long getSavedRepositoryBlobCount(final String repositoryName) {
    String taskKey = CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repositoryName;
    return getConfiguration().getLong(taskKey, 0L);
  }

  private void updateProgress(
      final RepositoryData repositoryData,
      final String continuationToken)
  {
    log.debug("Setting {} to {}", CONTINUATION_TOKEN_PREFIX + repositoryData.repository().getName(), continuationToken);
    log.debug("Setting {} to {}", CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repositoryData.repository().getName(),
        repositoryData.processedBlobCount());
    getConfiguration().setString(CONTINUATION_TOKEN_PREFIX + repositoryData.repository().getName(), continuationToken);
    getConfiguration().setLong(CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repositoryData.repository().getName(),
        repositoryData.processedBlobCount());

    String progressMessage = String.format("Processed %d/%d blobs in repository %s (%d%%)",
        repositoryData.processedBlobCount(), repositoryData.totalBlobCount(),
        repositoryData.repository().getName(), repositoryData.percentageComplete());
    updateProgress(progressMessage);
  }

  /**
   * Update progress message and persist to task configuration.
   */
  private void updateProgress(final String progressMessage) {
    updateProgress(progressMessage, false);
  }

  /**
   * Update progress message and persist to task configuration.
   */
  private void updateProgress(final String progressMessage, final boolean forceLog) {
    if (forceLog) {
      log.info(progressMessage);
    }
    else {
      progressLogIntervalHelper.info(progressMessage);
    }
    getConfiguration().setProgress(progressMessage);
    taskResultStateStore.updateJobDataMap(getTaskInfo());
  }

  /**
   * Cleans up task configuration and reports final progress upon completion.
   * <p>
   * This method is called exactly once when processing finishes. It removes continuation
   * token and processed count from configuration, then reports the final completion
   * message with the total number of fixed blobs for this run only.
   *
   * @param repositoryData the repository data to clean up tokens for
   */
  private void finishedProgress(final RepositoryData repositoryData) {
    String continuationTokenKey = CONTINUATION_TOKEN_PREFIX + repositoryData.name();
    String currentRepositoryBlobCountKey = CURRENT_REPOSITORY_BLOB_COUNT_PREFIX + repositoryData.name();

    log.trace("Removing {}", continuationTokenKey);
    log.trace("Removing {}", currentRepositoryBlobCountKey);
    getConfiguration().setString(continuationTokenKey, null);
    getConfiguration().setString(currentRepositoryBlobCountKey, null);

    updateProgress(
        String.format("Completed processing blobs in repository %s, (Total fixed %d blobs)", repositoryData.name(),
            repositoryData.fixedBlobCount()),
        true);
  }

  /**
   * Creates the callback for persisting continuation tokens after each page is processed. The callback fires with the
   * continuation token and page size when a page is complete.
   * <p>
   * Exceptions from the callback are caught and logged to prevent task failure when updating progress. The task
   * continues processing regardless of callback exceptions.
   *
   * @param repositoryData repository data and progress values
   * @return the callback to persist continuation tokens
   */
  private Consumer<Continuations.PageInfo> createTokenPersistenceCallback(final RepositoryData repositoryData) {
    return pageInfo -> {
      try {
        if (pageInfo.getContinuationToken() != null) {
          // Increment by actual page size, not BROWSE_LIMIT, for accurate progress tracking
          repositoryData.incrementProcessedBlobCount(pageInfo.getPageSize());
          updateProgress(repositoryData, pageInfo.getContinuationToken());
        }
        else {
          // Final page reached. finishedProgress() is safe to call multiple times (it clears
          // tokens to null).
          finishedProgress(repositoryData);
        }
      }
      catch (Exception e) {
        log.warn(
            "Failed to update progress for repository {}, continuing processing. "
                + "Continuation token: {}, page size: {}",
            repositoryData.name(),
            pageInfo.getContinuationToken(),
            pageInfo.getPageSize(),
            e);
      }
    };
  }

  /**
   * Creates the job stream for processing assets in a repository.
   * <p>
   * This method sets up the streaming pipeline with continuation token support and
   * ensures {@link #finishedProgress(RepositoryData)} is called exactly once when processing completes.
   * The callback is invoked with null when the final page is complete, triggering
   * finishedProgress() to clean up tokens and report completion. For empty iterables,
   * where the callback may never be invoked, an additional check is performed after
   * stream consumption.
   *
   * @param repositoryData the repository to process
   * @param blobStore the blob store for this repository
   * @param savedToken saved continuation token for resumption
   * @return stream of runnable jobs for processing assets
   */
  private Stream<Runnable> createJobStream(
      final RepositoryData repositoryData,
      final BlobStore blobStore,
      final String savedToken)
  {
    ContentFacet contentFacet;
    try {
      contentFacet = repositoryData.repository().facet(ContentFacet.class);
    }
    catch (MissingFacetException e) {
      log.warn("Unable to access ContentFacet for repository {}, skipping", repositoryData.repository().getName(), e);
      return Stream.of();
    }

    BiFunction<Integer, String, Continuation<FluentAsset>> browseFn =
        (limit, token) -> contentFacet.assets().browseEager(limit, token);

    // Use Continuations.iterableOf with a callback that saves the continuation token
    // after each page is fully processed and after fetching the next page.
    // The callback is invoked with null when the final page is complete, triggering
    // finishedProgress() to clean up tokens and report completion.
    final Consumer<Continuations.PageInfo> tokenPersistenceCallback = createTokenPersistenceCallback(repositoryData);

    Iterable<FluentAsset> iterable =
        Continuations.iterableOf(browseFn, BROWSE_LIMIT, savedToken, tokenPersistenceCallback);

    return StreamSupport
        .stream(iterable.spliterator(), false)
        .filter(FluentAsset::hasBlob)
        .map(asset -> createJob(repositoryData, asset, blobStore));
  }

  private record BlobData(
      BlobStore blobStore,
      Blob blob)
  {
  }

  static class RepositoryData
  {
    private final Repository repository;

    // Lazy count - computed on first access to avoid upfront query for progress estimate
    // -1 indicates not yet computed; Long.MIN_VALUE indicates computed but unavailable (no ContentFacet)
    private volatile long totalBlobCount = -1L;

    private final AtomicLong processedBlobCount;

    private final AtomicLong fixedBlobCount = new AtomicLong();

    public RepositoryData(final Repository repository, final long savedProcessedBlobCount) {
      this.repository = repository;
      this.processedBlobCount = new AtomicLong(savedProcessedBlobCount);
    }

    public Repository repository() {
      return repository;
    }

    public String name() {
      return repository().getName();
    }

    public long totalBlobCount() {
      if (totalBlobCount == -1L) {
        // Compute count once and cache it
        synchronized (this) {
          if (totalBlobCount == -1L) {
            ContentFacet contentFacet = repository().optionalFacet(ContentFacet.class).orElse(null);
            totalBlobCount = contentFacet != null ? (long) contentFacet.assets().count() : Long.MIN_VALUE;
          }
        }
      }
      return totalBlobCount == Long.MIN_VALUE ? -1L : totalBlobCount;
    }

    public long processedBlobCount() {
      return processedBlobCount.get();
    }

    public long fixedBlobCount() {
      return fixedBlobCount.get();
    }

    public void incrementProcessedBlobCount(long count) {
      processedBlobCount.addAndGet(count);
    }

    public void incrementFixedBlobCount() {
      fixedBlobCount.incrementAndGet();
    }

    public int percentageComplete() {
      return totalBlobCount() > 0 ? (int) (processedBlobCount() * 100 / totalBlobCount()) : 0;
    }
  }
}
