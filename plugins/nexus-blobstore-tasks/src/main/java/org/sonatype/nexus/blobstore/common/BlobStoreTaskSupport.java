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

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.scheduling.TaskInterruptedException;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.collect.Iterables;

/**
 * Support for tasks that apply changes to a set of blob stores
 */
public abstract class BlobStoreTaskSupport
    extends TaskSupport
{
  public static final String BLOBSTORE_NAME_FIELD_ID = "blobstoreName";

  public static final String ALL = "(All Blob Stores)";

  protected final BlobStoreManager blobStoreManager;

  protected BlobStoreTaskSupport(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = blobStoreManager;
  }

  protected BlobStoreTaskSupport(final boolean taskLoggingEnabled, final BlobStoreManager blobStoreManager) {
    super(taskLoggingEnabled);
    this.blobStoreManager = blobStoreManager;
  }

  @Override
  protected Object execute() throws Exception {
    int processedBlobStores = 0;
    MultipleFailures failures = new MultipleFailures();

    for (BlobStore blobStore : findBlobStores()) {
      if (isCanceled()) {
        break;
      }

      String blobstoreName = blobStore.getBlobStoreConfiguration().getName();

      try {
        log.info("processing blob store '{}'", blobstoreName);
        execute(blobStore);
        log.info("successfully processed blob store '{}'", blobstoreName);
        processedBlobStores++;
      }
      catch (TaskInterruptedException e) {
        throw e;
      }
      catch (Exception e) {
        log.error("Failure processing blobstore '{}'", blobstoreName, e);
        failures.add(e);
      }
    }

    log.info("finished task '{}' - processed blob stores : {}", getMessage(), processedBlobStores);
    failures.maybePropagate(String.format("Failure running task '%s", getMessage()));

    return null;
  }

  private Iterable<BlobStore> findBlobStores() {
    final String blobStoreField = getBlobStoreField();

    String[] names = blobStoreField.split(",");

    if (Arrays.asList(names).contains(ALL)) {
      return Iterables.filter(blobStoreManager.browse(), this::appliesTo);
    }

    return Arrays.stream(names)
        .map(blobStoreManager::get)
        .filter(Objects::nonNull)
        .filter(this::appliesTo)
        .collect(Collectors.toSet());
  }

  protected String getBlobStoreField() {
    return getConfiguration().getString(BLOBSTORE_NAME_FIELD_ID);
  }

  /**
   * Identifies if a blobstore applicable to process
   *
   * @param blobStore the blobstore to be evaluated
   * @return a boolean variable indicating if the given blob store is applicable
   */
  protected abstract boolean appliesTo(final BlobStore blobStore);

  /**
   * Processes a single blob store
   *
   * @param blobStore the blob store to be processed
   */
  protected abstract void execute(final BlobStore blobStore);
}
