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
package org.sonatype.nexus.internal.backup;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.elasticsearch.common.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Task to backup DBs.
 *
 * @since 3.2
 */
@Named
public class DatabaseBackupTask
    extends TaskSupport
{

  private static final int MAX_CONCURRENT_BACKUPS = 32;

  private static final int MAX_QUEUED_BACKUPS = 2;

  private String location;

  private String nodeId;

  private final NodeAccess nodeAccess;

  private final DatabaseBackup databaseBackup;

  @Inject
  public DatabaseBackupTask(final NodeAccess nodeAccess, final DatabaseBackup databaseBackup) {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.databaseBackup = checkNotNull(databaseBackup);
  }

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Database backup")
    String message();
  }

  private static final Messages messages = I18N.create(Messages.class);

  @Override
  public String getMessage() {
    return messages.message();
  }

  @Override
  public void configure(final TaskConfiguration configuration) {
    super.configure(configuration);
    this.location = configuration.getString(DatabaseBackupTaskDescriptor.BACKUP_LOCATION);
    this.nodeId = configuration.getString(DatabaseBackupTaskDescriptor.BACKUP_NODE_ID);
  }

  @Override
  protected Object execute() throws Exception {
    List<Callable<Void>> jobs = Lists.newArrayList();
    log.info("task named '{}' database backup to location {} on node {}", getName(), location,
        nodeId == null ? "NOT-CLUSTERED" : nodeId);
    MultipleFailures failures = new MultipleFailures();
    if (runOnMe()) {
      for (String dbName : databaseBackup.dbNames()) {
        try {
          log.info("database backup of {} starting", dbName);
          Callable<Void> job = databaseBackup.fullBackup(location, dbName);
          jobs.add(job);
        }
        catch (Exception e) {
          failures.add(new RuntimeException(String.format(
              "database backup of %s to location: %s please check filesystem permissions and that the location exists",
              dbName, location), e));
        }
      }
    }
    monitorBackupResults(jobs, failures);
    failures.maybePropagate();
    return null;
  }

  private void monitorBackupResults(final List<Callable<Void>> jobs, final MultipleFailures failures)
      throws InterruptedException {
    ExecutorService executorService = makeExecutorService();
    List<Future<Void>> futures = executorService.invokeAll(jobs);
    executorService.shutdown();
    for (Future<Void> future : futures) {
      try {
        future.get();
      }
      catch (ExecutionException e) {
        if (e.getCause() != null) {
          failures.add(e.getCause()); // when cause is present, unwrapping to reduce log noise
        }
        else {
          failures.add(e);
        }
      }
    }
  }

  private ExecutorService makeExecutorService() {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_QUEUED_BACKUPS);
    ThreadFactory factory = new NexusThreadFactory("dbbackup", "dbbackup");
    ThreadPoolExecutor backing =
        new ThreadPoolExecutor(MAX_CONCURRENT_BACKUPS, MAX_CONCURRENT_BACKUPS, 1, TimeUnit.NANOSECONDS, queue, factory);
    backing.allowCoreThreadTimeOut(true);
    return NexusExecutorService.forFixedSubject(backing, FakeAlmightySubject.TASK_SUBJECT);
  }

  @VisibleForTesting
  boolean runOnMe() {
    // not running in HA - only node, task should run
    if (!nodeAccess.isClustered()){
      return true;
    }
    // running in HA - but node was never specified (most likely that task was created before HA was configured)
    if (nodeAccess.isClustered() && Strings.isNullOrEmpty(nodeId)) {
      throw new IllegalStateException("backup task failed because it was not configured to run in a cluster");
    }
    // running in HA - this is the correct node to execute the task
    if (nodeAccess.isClustered() && Objects.equal(nodeAccess.getId(), nodeId)) {
      return true;
    }
    // running in HA - but the node that executes this task is no longer in the cluster, better inform admins
    if (nodeAccess.isClustered() && !nodeAccess.getMemberIds().contains(nodeId)) {
      throw new IllegalStateException(
          String.format("backup task failed because node with nodeId %s was not found in the cluster", nodeId)
      );
    }
    // only run if one of the correct cases above were met
    return false;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(final String nodeId) {
    this.nodeId = nodeId;
  }

}
