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
package org.sonatype.nexus.coreui.internal.metrics;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.internal.upgrade.datastore.UpgradeTaskStore;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * State contributor to indicate if blob store metrics-related upgrade tasks are currently being calculated.
 * This allows the UI to display "Calculating..." instead of "0" for metrics during database migration.
 *
 * @since 3.87
 */
@Component
@Singleton
public class BlobStoreMetricsStateContributor
    implements StateContributor
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String BLOBSTORE_METRICS_CALCULATING = "nexus.datastore.blobstore.metrics.calculating";

  private static final Set<String> METRIC_TASK_IDS = Set.of(
      "nexus.blobstore.metrics.migration.task",
      "component.normalize.version");

  private static final String RECALCULATE_TASK_TYPE_ID = "blobstore.metrics.reconcile";

  private final UpgradeTaskStore upgradeTaskStore;

  private final TaskScheduler taskScheduler;

  @Inject
  public BlobStoreMetricsStateContributor(
      final UpgradeTaskStore upgradeTaskStore,
      final TaskScheduler taskScheduler)
  {
    this.upgradeTaskStore = requireNonNull(upgradeTaskStore);
    this.taskScheduler = requireNonNull(taskScheduler);
  }

  @Override
  public Map<String, Object> getState() {
    boolean isCalculating = isUpgradeTaskRunning() || isRecalculateTaskRunning();

    log.debug("Blob store metrics calculating status: {}", isCalculating);

    return ImmutableMap.of(BLOBSTORE_METRICS_CALCULATING, isCalculating);
  }

  private boolean isUpgradeTaskRunning() {
    return upgradeTaskStore.browse()
        .anyMatch(task -> METRIC_TASK_IDS.contains(task.getTaskId()));
  }

  private boolean isRecalculateTaskRunning() {
    return taskScheduler.listsTasks()
        .stream()
        .filter(task -> RECALCULATE_TASK_TYPE_ID.equals(task.getTypeId()))
        .anyMatch(task -> task.getCurrentState().getState() == TaskState.RUNNING);
  }
}
