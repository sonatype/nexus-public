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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreConfiguration;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreStore;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.scheduling.TaskUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.DRY_RUN;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.INTEGRITY_CHECK;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.RESTORE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.SINCE_DAYS;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.UNDELETE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.datastore.DefaultIntegrityCheckStrategy.DEFAULT_NAME;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * @since 3.29
 */
@Named
public class RestoreMetadataTask
    extends TaskSupport
    implements Cancelable
{
  private final BlobStoreManager blobStoreManager;

  private final Optional<ChangeRepositoryBlobStoreStore> changeBlobstoreStore;

  private final RepositoryManager repositoryManager;

  private final Map<String, RestoreBlobStrategy> restoreBlobStrategies;

  private final Map<String, IntegrityCheckStrategy> integrityCheckStrategies;

  private final BlobStoreUsageChecker blobStoreUsageChecker;

  private final DryRunPrefix dryRunPrefix;

  private final IntegrityCheckStrategy defaultIntegrityCheckStrategy;

  private final MaintenanceService maintenanceService;

  private final AssetBlobRefFormatCheck assetBlobRefFormatCheck;

  private final TaskUtils taskUtils;

  private final Map<String, Boolean> formatAssetBlobRefMigrated;

  @Inject
  public RestoreMetadataTask(
      final BlobStoreManager blobStoreManager,
      @Nullable final ChangeRepositoryBlobStoreStore changeBlobstoreStore,
      final RepositoryManager repositoryManager,
      final Map<String, RestoreBlobStrategy> restoreBlobStrategies,
      final BlobStoreUsageChecker blobStoreUsageChecker,
      final DryRunPrefix dryRunPrefix,
      final Map<String, IntegrityCheckStrategy> integrityCheckStrategies,
      final MaintenanceService maintenanceService,
      final AssetBlobRefFormatCheck assetBlobRefFormatCheck,
      final TaskUtils taskUtils)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.changeBlobstoreStore = Optional.ofNullable(changeBlobstoreStore);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.restoreBlobStrategies = checkNotNull(restoreBlobStrategies);
    this.blobStoreUsageChecker = checkNotNull(blobStoreUsageChecker);
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
    this.defaultIntegrityCheckStrategy = checkNotNull(integrityCheckStrategies.get(DEFAULT_NAME));
    this.integrityCheckStrategies = checkNotNull(integrityCheckStrategies);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.assetBlobRefFormatCheck = checkNotNull(assetBlobRefFormatCheck);
    this.taskUtils = checkNotNull(taskUtils);
    formatAssetBlobRefMigrated = new HashMap<>();
  }

  @Override
  public String getMessage() {
    return "Uses blobs in a blobstore to restore assets to a repository";
  }

  @VisibleForTesting
  void checkForConflicts() {
    String blobStoreName = checkNotNull(getConfiguration().getString(BLOB_STORE_NAME_FIELD_ID));

    taskUtils.checkForConflictingTasks(getId(), getName(), asList("repository.move"), ImmutableMap
        .of("moveInitialBlobstore", asList(blobStoreName), "moveTargetBlobstore", asList(blobStoreName)));

    checkForUnfinishedMoveTask(blobStoreName);
  }

  private void checkForUnfinishedMoveTask(String blobStoreName) {
    List<ChangeRepositoryBlobStoreConfiguration> existingMoves = changeBlobstoreStore
        .map(store -> store.findByBlobStoreName(blobStoreName))
        .orElseGet(Collections::emptyList);

    if (!existingMoves.isEmpty()) {
      log.info(TASK_LOG_ONLY, "found {} unfinished move tasks using blobstore '{}', unable to run task '{}'",
          existingMoves.size(), blobStoreName, getName());

      throw new IllegalStateException(
          format("found unfinished move task using blobstore '%s', task can't be executed", blobStoreName));
    }
  }

  @Override
  protected Void execute() throws Exception {
    checkForConflicts();

    String blobStoreId = checkNotNull(getConfiguration().getString(BLOB_STORE_NAME_FIELD_ID));
    boolean dryRun = getConfiguration().getBoolean(DRY_RUN, false);
    boolean restoreBlobs = getConfiguration().getBoolean(RESTORE_BLOBS, false);
    boolean undeleteBlobs = getConfiguration().getBoolean(UNDELETE_BLOBS, false);
    boolean integrityCheck = getConfiguration().getBoolean(INTEGRITY_CHECK, false);
    Integer sinceDays = getConfiguration().getInteger(SINCE_DAYS, -1);

    BlobStore blobStore = blobStoreManager.get(blobStoreId);

    restore(blobStore, restoreBlobs, undeleteBlobs, dryRun, sinceDays);

    blobStoreIntegrityCheck(integrityCheck, blobStoreId, dryRun, sinceDays);

    return null;
  }

  private void restore(
      final BlobStore blobStore,
      final boolean restore,
      final boolean undelete,
      final boolean dryRun,
      final Integer sinceDays) // NOSONAR
  {
    if (!restore && !undelete) {
      log.warn("No repair/restore operations selected");
      return;
    }

    String logPrefix = dryRun ? dryRunPrefix.get() : "";
    long processed = 0;
    long undeleted = 0;
    boolean updateAssets = !dryRun && restore;
    Set<Repository> touchedRepositories = new HashSet<>();

    if (dryRun) {
      log.info("{}Actions will be logged, but no changes will be made.", logPrefix);
    }

    formatAssetBlobRefMigrated.clear();
    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      for (BlobId blobId : getBlobIdStream(blobStore, sinceDays)) {
        try {
          if (isCanceled()) {
            log.info("Restore metadata task for {} was canceled", blobStore.getBlobStoreConfiguration().getName());
            break;
          }

          Optional<Context> optionalContext = buildContext(blobStore, blobId);
          if (optionalContext.isPresent()) {
            Context context = optionalContext.get();

            if (isAssetBlobRefNotMigrated(context.repository)) {
              continue;
            }

            if (restore && context.restoreBlobStrategy != null && !context.blobAttributes.isDeleted()) {
              context.restoreBlobStrategy.restore(context.properties, context.blob, context.blobStore, dryRun);
            }
            if (undelete &&
                blobStore.undelete(blobStoreUsageChecker, context.blobId, context.blobAttributes, dryRun)) {
              undeleted++;
            }

            if (updateAssets) {
              touchedRepositories.add(context.repository);
            }
          }

          processed++;

          progressLogger
              .info("{}Elapsed time: {}, processed: {}, un-deleted: {}", logPrefix, progressLogger.getElapsed(),
                  processed, undeleted);
        }
        catch (Exception e) {
          log.error("Error restoring blob {}", blobId, e);
        }
      }

      updateAssets(touchedRepositories, updateAssets);
    }
  }

  private boolean isAssetBlobRefNotMigrated(final Repository repository) {
    try {
      return formatAssetBlobRefMigrated.computeIfAbsent(repository.getFormat().getValue(),
          __ -> assetBlobRefFormatCheck.isAssetBlobRefNotMigrated(repository));
    }
    catch (Exception e) {
      log.error("Error occurred determining asset blob ref migration status. " +
          "Will assume asset blob refs are not migrated.", e);
      return true;
    }
  }

  private Iterable<BlobId> getBlobIdStream(final BlobStore store, final Integer sinceDays) {
    if (isNull(sinceDays) || sinceDays < 0) {
      log.info("Will process all blobs");
      return store.getBlobIdStream()::iterator;
    }
    return store.getBlobIdUpdatedSinceStream(Duration.ofDays(sinceDays))::iterator;
  }

  private void updateAssets(final Set<Repository> repositories, final boolean updateAssets) {
    for (Repository repository : repositories) {
      if (isCanceled()) {
        break;
      }

      ofNullable(restoreBlobStrategies.get(repository.getFormat().getValue()))
          .ifPresent(strategy -> strategy.after(updateAssets, repository));
    }
  }

  private void blobStoreIntegrityCheck(
      final boolean integrityCheck,
      final String blobStoreId,
      final boolean dryRun,
      final int sinceDays)
  {
    if (!integrityCheck) {
      log.warn("Integrity check operation not selected");
      return;
    }

    BlobStore blobStore = blobStoreManager.get(blobStoreId);

    if (blobStore == null) {
      log.error("Unable to find blob store '{}' in the blob store manager", blobStoreId);
      return;
    }

    Iterable<Repository> repositories = repositoryManager.browseForBlobStore(blobStoreId);

    List<Repository> syncList = Collections.synchronizedList(StreamSupport.stream(repositories.spliterator(), false)
        .collect(Collectors.toList()));

    syncList.stream()
        .filter(Objects::nonNull)
        .filter(repository -> !(repository.getType() instanceof GroupType))
        .filter(Repository::isStarted)
        .forEach(repository ->
            integrityCheckStrategies
                .getOrDefault(repository.getFormat().getValue(), defaultIntegrityCheckStrategy)
                .check(repository, blobStore, this::isCanceled, sinceDays,
                    a -> this.integrityCheckFailedHandler(repository, a, dryRun))
        );
  }

  protected void integrityCheckFailedHandler(final Repository repository, final Asset asset, final boolean isDryRun) {
    log.info("{}Removing asset {} from repository {}, blob integrity check failed", isDryRun ? dryRunPrefix.get() : "",
        asset.path(), repository.getName());

    if (!isDryRun) {
      maintenanceService.deleteAsset(repository, asset);
    }
  }

  private Optional<Context> buildContext(final BlobStore blobStore, final BlobId blobId)
  {
    return Optional.of(new Context(blobStore, blobId))
        .map(c -> c.blob(c.blobStore.get(c.blobId, true)))
        .map(c -> c.blobAttributes(c.blobStore.getBlobAttributes(c.blobId)))
        .map(c -> c.properties(c.blobAttributes.getProperties()))
        .map(c -> c.repositoryName(c.properties.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)))
        .map(c -> c.repository(repositoryManager.get(c.repositoryName)))
        .map(c -> c.restoreBlobStrategy(restoreBlobStrategies.get(c.repository.getFormat().getValue())));
  }

  private static class Context
  {
    final BlobStore blobStore;

    final BlobId blobId;

    Blob blob;

    BlobAttributes blobAttributes;

    Properties properties;

    String repositoryName;

    Repository repository;

    RestoreBlobStrategy restoreBlobStrategy;

    Context(final BlobStore blobStore, final BlobId blobId) {
      this.blobStore = checkNotNull(blobStore);
      this.blobId = checkNotNull(blobId);
    }

    Context blob(final Blob blob) {
      if (blob == null) {
        return null;
      }
      else {
        this.blob = blob;
        return this;
      }
    }

    Context blobAttributes(final BlobAttributes blobAttributes) {
      if (blobAttributes == null) {
        return null;
      }
      else {
        this.blobAttributes = blobAttributes;
        return this;
      }
    }

    Context properties(final Properties properties) {
      if (properties == null) {
        return null;
      }
      else {
        this.properties = properties;
        return this;
      }
    }

    Context repositoryName(final String repositoryName) {
      if (repositoryName == null) {
        return null;
      }
      else {
        this.repositoryName = repositoryName;
        return this;
      }
    }

    Context repository(final Repository repository) {
      if (repository == null) {
        return null;
      }
      else {
        this.repository = repository;
        return this;
      }
    }

    Context restoreBlobStrategy(final RestoreBlobStrategy restoreBlobStrategy) {
      this.restoreBlobStrategy = restoreBlobStrategy;
      return this;
    }
  }
}
