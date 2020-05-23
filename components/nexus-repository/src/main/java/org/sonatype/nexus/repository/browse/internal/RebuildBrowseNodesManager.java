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
package org.sonatype.nexus.repository.browse.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentDatabase;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;
import static org.sonatype.nexus.orient.entity.AttachedEntityHelper.id;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_PENDING_DELETION;

/**
 * @since 3.6
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class RebuildBrowseNodesManager
    extends StateGuardLifecycleSupport
{
  @VisibleForTesting
  static final String SELECT_ANY_ASSET_BY_BUCKET = "select @rid from asset where bucket = :bucket limit 1";

  @VisibleForTesting
  static final String SELECT_ANY_BROWSE_NODE_BY_BUCKET = "select @rid from browse_node where repository_name = :repositoryName limit 1";

  private final Provider<DatabaseInstance> componentDatabaseInstanceProvider;

  private final TaskScheduler taskScheduler;

  private final boolean automaticRebuildEnabled;

  private final BucketEntityAdapter bucketEntityAdapter;

  @Inject
  public RebuildBrowseNodesManager(@Named(ComponentDatabase.NAME)
                                   final Provider<DatabaseInstance> componentDatabaseInstanceProvider,
                                   final TaskScheduler taskScheduler,
                                   final BrowseNodeConfiguration configuration,
                                   final BucketEntityAdapter bucketEntityAdapter)
  {
    this.componentDatabaseInstanceProvider = checkNotNull(componentDatabaseInstanceProvider);
    this.taskScheduler = checkNotNull(taskScheduler);
    this.automaticRebuildEnabled = checkNotNull(configuration).isAutomaticRebuildEnabled();
    this.bucketEntityAdapter = bucketEntityAdapter;
  }

  @Override
  protected void doStart() { // NOSONAR
    if (!automaticRebuildEnabled) {
      return;
    }

    Stopwatch sw = Stopwatch.createStarted();
    try {
      Collection<Bucket> buckets = inTx(componentDatabaseInstanceProvider).call(db -> {
        return stream(bucketEntityAdapter.browse(db)).filter(bucket -> {
          if (bucket.attributes().contains(P_PENDING_DELETION)) {
            log.debug("browse_node table won't be rebuilt for bucket={} as it is marked for deletion", id(bucket));
            return false;
          }
          boolean hasAssets = !execute(db, SELECT_ANY_ASSET_BY_BUCKET, singletonMap("bucket", id(bucket)))
              .isEmpty();
          boolean hasBrowseNodes = !execute(db, SELECT_ANY_BROWSE_NODE_BY_BUCKET,
              singletonMap("repositoryName", bucket.getRepositoryName()))
              .isEmpty();

          if (hasAssets ^ hasBrowseNodes) {
            log.debug("browse_node table will be rebuilt for bucket={}", id(bucket));
            return true;
          }
          else if (!hasAssets) {
            log.debug("browse_node table won't be populated as there are no assets for bucketId={}", id(bucket));
          }
          else {
            log.debug("browse_node table already populated for bucketId={}", id(bucket));
          }
          return false;
        }).collect(toList());
      });

      String repositoryNames = buckets.stream().map(Bucket::getRepositoryName).collect(Collectors.joining(","));

      if (!Strings2.isEmpty(repositoryNames)) {
        boolean existingTask = taskScheduler.findAndSubmit(RebuildBrowseNodesTaskDescriptor.TYPE_ID,
            ImmutableMap.of(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, repositoryNames));
        if (!existingTask) {
          launchNewTask(repositoryNames);
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to determine if the browse nodes need to be rebuilt for any repositories", e);
    }
    log.debug("scheduling rebuild browse nodes tasks took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
  }

  private void launchNewTask(final String repositoryNames) {
    TaskConfiguration configuration = taskScheduler
        .createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    configuration.setString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, repositoryNames);
    configuration.setName("Rebuild repository browse tree - (" + repositoryNames + ")");
    taskScheduler.submit(configuration);
  }

  private List<ODocument> execute(final ODatabaseDocumentTx db, // NOSONAR
                                  final String query,
                                  final Map<String, Object> parameters)
  {
    return db.command(new OCommandSQL(query)).execute(parameters);
  }
}
