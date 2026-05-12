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
package org.sonatype.nexus.blobstore.common;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.scheduling.ParallelTaskSupport;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for tasks that applies to blobstores
 */
public abstract class BlobStoreParallelTaskSupport
    extends ParallelTaskSupport
{
  public static final String BLOBSTORE_NAME_FIELD_ID = "blobstoreName";

  public static final String ALL = "(All Blob Stores)";

  protected BlobStoreManager blobStoreManager;

  /**
   * @param concurrencyLimit the number of concurrent threads processing the queue allowed.
   */
  protected BlobStoreParallelTaskSupport(final int concurrencyLimit) {
    super(concurrencyLimit);
  }

  /**
   * @param taskLoggingEnabled whether task logging should be enabled
   * @param concurrencyLimit the number of concurrent threads processing the queue allowed.
   */
  protected BlobStoreParallelTaskSupport(
      final boolean taskLoggingEnabled,
      final int concurrencyLimit)
  {
    super(taskLoggingEnabled, concurrencyLimit);
  }

  @Autowired
  public void install(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  protected Stream<Runnable> jobStream(final ProgressLogIntervalHelper progress) {
    return findBlobStores()
        .flatMap(blobStore -> jobStream(progress, blobStore));
  }

  /**
   * Create a stream of runnables derived from the blobstore.
   */
  protected abstract Stream<Runnable> jobStream(ProgressLogIntervalHelper progress, final BlobStore blobStore);

  private Stream<BlobStore> findBlobStores() {
    final String blobStoreName = getBlobStoreField();
    checkArgument(!Strings.isNullOrEmpty(blobStoreName));

    Set<String> blobStoreNames = Set.of(blobStoreName.split(","));

    if (blobStoreNames.contains(ALL)) {
      return StreamSupport.stream(blobStoreManager.browse().spliterator(), false)
          .filter(this::appliesTo);
    }
    return blobStoreNames.stream()
        .map(blobStoreManager::get)
        .filter(Objects::nonNull)
        .filter(this::appliesTo);
  }

  /**
   * Return true if the task should be run against the specified blobstore.
   */
  protected abstract boolean appliesTo(final BlobStore blobStore);

  /**
   * Extract blobstore field out of configuration.
   */
  protected String getBlobStoreField() {
    return getConfiguration().getString(BLOBSTORE_NAME_FIELD_ID);
  }
}
