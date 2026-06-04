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
package org.sonatype.nexus.blobstore.compact.internal;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.common.BlobStoreParallelTaskSupport;
import org.sonatype.nexus.scheduling.RecoveryModeService;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;

import org.springframework.beans.factory.annotation.Value;

import static org.sonatype.nexus.blobstore.common.BlobStoreParallelTaskSupport.ALL;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreConfiguration;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreStore;
import org.sonatype.nexus.scheduling.CancelableByRecoveryMode;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.blobstore.compact.internal.CompactBlobStoreTaskDescriptor.BLOBS_OLDER_THAN_FIELD_ID;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Task to compact a given blob store.
 *
 * @since 3.0
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CompactBlobStoreTask
    extends BlobStoreParallelTaskSupport
    implements CancelableByRecoveryMode
{

  private final Optional<ChangeRepositoryBlobStoreStore> changeBlobstoreStore;

  private final BlobStoreUsageChecker blobStoreUsageChecker;

  private final TaskUtils taskUtils;

  private final RecoveryModeService recoveryModeService;

  private final AtomicInteger processed = new AtomicInteger();

  @Autowired
  public CompactBlobStoreTask(
      @Nullable final ChangeRepositoryBlobStoreStore changeBlobstoreStore,
      final BlobStoreUsageChecker blobStoreUsageChecker,
      final TaskUtils taskUtils,
      @Nullable final RecoveryModeService recoveryModeService,
      @Value("${nexus.compact.blobstore.concurrencyLimit:5}") final int concurrencyLimit)
  {
    super(concurrencyLimit);
    this.changeBlobstoreStore = Optional.ofNullable(changeBlobstoreStore);
    this.blobStoreUsageChecker = checkNotNull(blobStoreUsageChecker);
    this.taskUtils = checkNotNull(taskUtils);
    this.recoveryModeService = recoveryModeService;
  }

  @VisibleForTesting
  void checkForConflicts(final String blobStoreName) {
    if (recoveryModeService != null) {
      recoveryModeService.ensureNotInRecoveryMode(getName());
    }
    taskUtils.checkForConflictingTasks(getId(), getName(), asList("repository.move"), ImmutableMap
        .of("moveInitialBlobstore", asList(blobStoreName), "moveTargetBlobstore", asList(blobStoreName)));

    checkForUnfinishedMoveTask(blobStoreName);
  }

  private void checkForUnfinishedMoveTask(final String blobStoreName) {
    List<ChangeRepositoryBlobStoreConfiguration> existingMoves = changeBlobstoreStore
        .map(store -> store.findByBlobStoreName(blobStoreName))
        .orElseGet(Collections::emptyList);

    if (!existingMoves.isEmpty()) {
      log.info(TASK_LOG_ONLY, "found {} unfinished move tasks using blobstore '{}', unable to run task '{}'",
          existingMoves.size(), blobStoreName, getName());

      throw new IllegalStateException(
          format("found unfinished move task(s) using blobstore '%s', task can't be executed", blobStoreName));
    }
  }

  private Runnable compact(final ProgressLogIntervalHelper progress, final BlobStore blobStore) {
    return () -> {
      CancelableHelper.checkCancellation();

      if (blobStore != null) {
        String blobStoreName = blobStore.getBlobStoreConfiguration().getName();
        log.debug("Starting compaction of blob store '{}'", blobStoreName);

        try {
          checkForConflicts(blobStoreName);
          blobStore.compact(blobStoreUsageChecker, getBlobsOlderThanField());

          int count = processed.incrementAndGet();
          log.debug("Completed compaction of blob store '{}'", blobStoreName);
          progress.info("Compacted {} blob stores", count);
        }
        catch (Exception e) {
          log.error("Failed to compact blob store '{}'", blobStoreName, e);
        }
      }
      else {
        log.warn("Unable to find blob store: {}", getBlobStoreField());
      }
    };
  }

  @Override
  protected Object result() {
    log.info("Compaction completed for {} blob stores", processed.get());
    return processed.get();
  }

  @Override
  public String getMessage() {
    String blobStoreField = getBlobStoreField();
    if (ALL.equals(blobStoreField)) {
      return "Compacting all blob stores";
    }
    return format("Compacting [%s]", blobStoreField);
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress, final BlobStore blobStore) {
    return Stream.of(compact(progress, blobStore));
  }

  /** compact applies to all types of blob stores */
  @Override
  protected boolean appliesTo(final BlobStore blobStore) {
    return true;
  }

  private Duration getBlobsOlderThanField() {
    return Duration.ofDays(getConfiguration().getInteger(BLOBS_OLDER_THAN_FIELD_ID, 0));
  }
}
