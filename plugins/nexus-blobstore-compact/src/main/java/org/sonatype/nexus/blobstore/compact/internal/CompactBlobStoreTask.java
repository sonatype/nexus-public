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

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.common.log.MarkedLogger;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.storage.ComponentDatabase;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Preconditions.checkNotNull;
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
{
  private static final String ANY_NODE = "%";

  private final OSQLSynchQuery<ODocument> assetBlobRefQuery = new OSQLSynchQuery<>(
      "SELECT FROM asset WHERE name = ? AND blob_ref LIKE ? LIMIT 1"
  );

  private Timer inUseCheckQueryTimer = SharedMetricRegistries.getOrCreate("nexus").timer(
      name(CompactBlobStoreTask.class, "in-use-check-query")
  );

  private final BlobStoreManager blobStoreManager;

  private final Supplier<ODatabaseDocumentTx> txSupplier;

  @Inject
  public CompactBlobStoreTask(final BlobStoreManager blobStoreManager,
                              @Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstanceProvider)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.txSupplier = () -> databaseInstanceProvider.get().acquire();
  }

  @Override
  protected Object execute() throws Exception {
    BlobStore blobStore = blobStoreManager.get(getBlobStoreField());
    if (blobStore != null) {
      blobStore.compact(inUseChecker(), new MarkedLogger(log, TASK_LOG_ONLY));
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

  private BlobStoreUsageChecker inUseChecker() {
    return (BlobStore blobStore, BlobId blobId, String blobName) -> {
      Timer.Context time = inUseCheckQueryTimer.time();
      BlobRef blobRef = new BlobRef(ANY_NODE, blobStore.getBlobStoreConfiguration().getName(), blobId.asUniqueString());
      try (ODatabaseDocumentTx tx = txSupplier.get()) {
        tx.begin();
        List<ODocument> results = tx.command(assetBlobRefQuery).execute(blobName, blobRef.toString());
        return !results.isEmpty();
      }
      finally {
        time.stop();
      }
    };
  }
}
