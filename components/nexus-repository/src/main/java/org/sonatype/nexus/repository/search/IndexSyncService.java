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
package org.sonatype.nexus.repository.search;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.orient.entity.EntityLog;
import org.sonatype.nexus.orient.entity.EntityLog.UnknownDeltaException;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.COMPONENT;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_REPOSITORY_NAME;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;

/**
 * Service that keeps the Elasticsearch index in-sync with off-process database changes.
 *
 * @since 3.4
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class IndexSyncService
    extends LifecycleSupport
{
  public static final String INDEX_UPGRADE_MARKER = "INDEX_UPGRADE_MARKER";

  private final Provider<DatabaseInstance> componentDatabase;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final NodeAccess nodeAccess;

  private final IndexRequestProcessor indexRequestProcessor;

  private final IndexRebuildManager indexRebuildManager;

  private final EntityLog entityLog;

  private final File checkpointFile;

  @Inject
  public IndexSyncService(@Named(COMPONENT) final Provider<DatabaseInstance> componentDatabase,
                          final ComponentEntityAdapter componentEntityAdapter,
                          final AssetEntityAdapter assetEntityAdapter,
                          final ApplicationDirectories directories,
                          final NodeAccess nodeAccess,
                          final IndexRequestProcessor indexRequestProcessor,
                          final IndexRebuildManager indexRebuildManager)
  {
    this.componentDatabase = checkNotNull(componentDatabase);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.indexRequestProcessor = checkNotNull(indexRequestProcessor);
    this.indexRebuildManager = checkNotNull(indexRebuildManager);

    this.entityLog = new EntityLog(componentDatabase, componentEntityAdapter, assetEntityAdapter);
    this.checkpointFile = new File(directories.getWorkDirectory("elasticsearch"), "nexus.lsn");
  }

  @Override
  protected void doStart() throws Exception {
    indexRequestProcessor.start();

    try {
      Map<ORID, EntityAdapter> changes = entityLog.since(loadCheckpoint());
      if (!changes.isEmpty()) {
        log.info("Applying {} incremental search updates", changes.size());
        syncIndex(changes);
      }
    }
    catch (UnknownDeltaException e) {
      logReason("Rebuilding search indexes because database has diverged", e);
      indexRebuildManager.rebuildAllIndexes();
    }
    catch (FileNotFoundException e) {
      if (!nodeAccess.isOldestNode()) {
        logReason("Rebuilding search indexes to match joining cluster", e);
        indexRebuildManager.rebuildAllIndexes();
      }
    }
    catch (Exception e) {
      log.warn("Unexpected error, skipping index sync", e);
    }
  }

  @Override
  protected void doStop() throws Exception {
    try {
      saveCheckpoint(entityLog.mark());
    }
    catch (IOException e) {
      log.warn("Problem saving {}", checkpointFile, e);
    }

    indexRequestProcessor.stop();
  }

  private void logReason(final String message, final Throwable cause) {
    if (log.isDebugEnabled()) {
      log.info(message, cause);
    }
    else {
      log.info(message);
    }
  }

  private void syncIndex(final Map<ORID, EntityAdapter> changes) {
    IndexBatchRequest batch = new IndexBatchRequest();
    try (ODatabaseDocumentTx db = componentDatabase.get().acquire()) {
      changes.forEach((rid, adapter) -> {
        ODocument document = db.load(rid);
        if (document != null) {
          EntityId componentId = findComponentId(document);
          if (componentId != null) {
            batch.update(findRepositoryName(document), componentId);
          }
        }
        else if (adapter instanceof ComponentEntityAdapter) {
          batch.delete(null, componentId(rid));
        }
      });
    }
    indexRequestProcessor.process(batch);
  }

  private String findRepositoryName(final ODocument document) {
    return ((ODocument) document.field(P_BUCKET)).field(P_REPOSITORY_NAME);
  }

  @Nullable
  private EntityId findComponentId(final ODocument document) {
    if (document.containsField(P_COMPONENT)) {
      // get the owning component from the asset, might be null if asset is standalone
      return componentId(document.field(P_COMPONENT, ORID.class));
    }
    else {
      // not an asset, must be a component itself
      return componentId(document.getIdentity());
    }
  }

  @Nullable
  private EntityId componentId(@Nullable final ORID rid) {
    return rid != null ? new AttachedEntityId(componentEntityAdapter, rid) : null;
  }

  @VisibleForTesting
  OLogSequenceNumber loadCheckpoint() throws IOException {
    try (InputStream in = new FileInputStream(checkpointFile)) {
      byte[] nexusLsnBytes = ByteStreams.toByteArray(in);
      if (Arrays.equals(nexusLsnBytes, INDEX_UPGRADE_MARKER.getBytes(StandardCharsets.UTF_8))) {
        throw new UnknownDeltaException("Index upgrade indicator found in Elasticsearch marker file");
      }
      return new OLogSequenceNumber(ByteStreams.newDataInput(nexusLsnBytes));
    }
  }

  private void saveCheckpoint(final OLogSequenceNumber checkpoint) throws IOException {
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(checkpointFile))) {
      checkpoint.toStream(out);
    }
  }
}
