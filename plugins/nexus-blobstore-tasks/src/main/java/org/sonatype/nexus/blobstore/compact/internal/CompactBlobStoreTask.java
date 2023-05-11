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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreConfiguration;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreStore;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.scheduling.TaskUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.sonatype.nexus.blobstore.compact.internal.CompactBlobStoreTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.TASK_LOG_ONLY;

/**
 * Task to compact a given blob store.
 *
 * @since 3.0
 */
@Named
public class CompactBlobStoreTask
    extends TaskSupport
    implements Cancelable
{
  private final BlobStoreManager blobStoreManager;

  private final Optional<ChangeRepositoryBlobStoreStore> changeBlobstoreStore;

  private final BlobStoreUsageChecker blobStoreUsageChecker;

  private final TaskUtils taskUtils;


  @Inject
  public CompactBlobStoreTask(
      final BlobStoreManager blobStoreManager,
      @Nullable final ChangeRepositoryBlobStoreStore changeBlobstoreStore,
      final BlobStoreUsageChecker blobStoreUsageChecker,
      final TaskUtils taskUtils)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.changeBlobstoreStore = Optional.ofNullable(changeBlobstoreStore);
    this.blobStoreUsageChecker = checkNotNull(blobStoreUsageChecker);
    this.taskUtils = checkNotNull(taskUtils);
  }

  @VisibleForTesting
  void checkForConflicts() {
    String blobStoreName = checkNotNull(getBlobStoreField());

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
          format("found unfinished move task(s) using blobstore '%s', task can't be executed", blobStoreName));
    }
  }

  @Override
  protected Object execute() throws Exception {
    checkForConflicts();

    BlobStore blobStore = blobStoreManager.get(getBlobStoreField());
    if (blobStore != null) {
      blobStore.compact(blobStoreUsageChecker);
    }
    else {
      log.warn("Unable to find blob store: {}", getBlobStoreField());
    }
    return null;
  }

  @Override
  public String getMessage() {
    return "Compacting " + getBlobStoreField() + " blob store";
  }

  private String getBlobStoreField() {
    return getConfiguration().getString(BLOB_STORE_NAME_FIELD_ID);
  }
}
