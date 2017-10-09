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
package org.sonatype.nexus.blobstore.restore;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.file.FileBlobAttributes;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.DRY_RUN;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.RESTORE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.UNDELETE_BLOBS;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

/**
 * @since 3.4
 */
@Named
public class RestoreMetadataTask
    extends TaskSupport
    implements Cancelable
{
  private final BlobStoreManager blobStoreManager;

  private final RepositoryManager repositoryManager;

  private final Map<String, RestoreBlobStrategy> restoreBlobStrategies;

  private final BlobStoreUsageChecker blobStoreUsageChecker;

  private final DryRunPrefix dryRunPrefix;

  @Inject
  public RestoreMetadataTask(final BlobStoreManager blobStoreManager,
                             final RepositoryManager repositoryManager,
                             final Map<String, RestoreBlobStrategy> restoreBlobStrategies,
                             final BlobStoreUsageChecker blobStoreUsageChecker,
                             final DryRunPrefix dryRunPrefix)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.restoreBlobStrategies = checkNotNull(restoreBlobStrategies);
    this.blobStoreUsageChecker = checkNotNull(blobStoreUsageChecker);
    this.dryRunPrefix = checkNotNull(dryRunPrefix);
  }

  @Override
  public String getMessage() {
    return null;
  }

  @Override
  protected Void execute() throws Exception {
    String blobStoreId = checkNotNull(getConfiguration().getString(BLOB_STORE_NAME_FIELD_ID));
    boolean dryRun = getConfiguration().getBoolean(DRY_RUN, false);
    boolean restoreBlobs = getConfiguration().getBoolean(RESTORE_BLOBS, false);
    boolean undeleteBlobs = getConfiguration().getBoolean(UNDELETE_BLOBS, false);

    restore(blobStoreId, restoreBlobs, undeleteBlobs, dryRun);

    return null;
  }

  private void restore(final String blobStoreName, final boolean restore, final boolean undelete, final boolean dryRun) // NOSONAR
  {
    if (!restore && !undelete) {
      log.warn("No repair/restore operations selected");
      return;
    }

    String logPrefix = dryRun ? dryRunPrefix.get() : "";
    BlobStore store = blobStoreManager.get(blobStoreName);

    ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
    long processed = 0;
    long undeleted = 0;

    if (dryRun) {
      log.info("{}Actions will be logged, but no changes will be made.", logPrefix);
    }
    if (store instanceof FileBlobStore) {
      FileBlobStore fileBlobStore = (FileBlobStore) store;
      for (BlobId blobId : (Iterable<BlobId>)fileBlobStore.getBlobIdStream()::iterator) {
        Optional<Context> context = buildContext(blobStoreName, fileBlobStore, blobId);
        if (context.isPresent()) {
          Context c =  context.get();
          if (restore && c.restoreBlobStrategy != null && !c.blobAttributes.isDeleted()) {
            c.restoreBlobStrategy.restore(c.properties, c.blob, c.blobStoreName, dryRun);
          }
          if (undelete &&
              fileBlobStore
                  .maybeUndeleteBlob(blobStoreUsageChecker, c.blobId, (FileBlobAttributes) c.blobAttributes, dryRun))
          {
            undeleted++;
          }
        }

        processed++;

        progressLogger.info("{}Elapsed time: {}, processed: {}, un-deleted: {}", logPrefix, progressLogger.getElapsed(),
            processed, undeleted);

        if (isCanceled()) {
          break;
        }
      }

      progressLogger.flush();
    }
    else {
      log.error("Blob store does not support rebuild: {}", blobStoreName);
    }
  }

  private Optional<Context> buildContext(final String blobStoreName, final FileBlobStore fileBlobStore,
                                                                    final BlobId blobId)
  {
    return Optional.of(new Context(blobStoreName, fileBlobStore, blobId))
        .map(c -> c.blob(c.fileBlobStore.get(c.blobId, true)))
        .map(c -> c.blobAttributes(c.fileBlobStore.getBlobAttributes(c.blobId)))
        .map(c -> c.properties(c.blobAttributes.getProperties()))
        .map(c -> c.repositoryName(c.properties.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)))
        .map(c -> c.repository(repositoryManager.get(c.repositoryName)))
        .map(c -> c.restoreBlobStrategy(restoreBlobStrategies.get(c.repository.getFormat().getValue())));
  }

  private static class Context {
    final String blobStoreName;

    final FileBlobStore fileBlobStore;

    final BlobId blobId;

    Blob blob;

    BlobAttributes blobAttributes;

    Properties properties;

    String repositoryName;

    Repository repository;

    RestoreBlobStrategy restoreBlobStrategy;

    Context(final String blobStoreName, final FileBlobStore fileBlobStore, final BlobId blobId) {
      this.blobStoreName = checkNotNull(blobStoreName);
      this.fileBlobStore = checkNotNull(fileBlobStore);
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
