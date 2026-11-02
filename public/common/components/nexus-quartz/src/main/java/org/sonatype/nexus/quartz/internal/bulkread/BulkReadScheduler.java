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
package org.sonatype.nexus.quartz.internal.bulkread;

import java.util.Map;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Extended Scheduler interface with optimized bulk read methods.
 * This avoids the N+1 query problem when fetching multiple jobs with triggers.
 */
public interface BulkReadScheduler
    extends Scheduler
{
  /**
   * Retrieves all job details for a specific group using a single optimized query.
   *
   * @param groupName the job group name
   * @return map of job keys to their associated job details
   * @throws SchedulerException if there is an error retrieving the job details
   */
  Map<JobKey, JobDetail> getJobDetailsForGroup(String groupName) throws SchedulerException;

  /**
   * Retrieves all triggers for a specific group using optimized bulk queries.
   * This method executes 3 queries (QRTZ_TRIGGERS, QRTZ_CRON_TRIGGERS, QRTZ_SIMPLE_TRIGGERS).
   *
   * @param groupName the trigger group name
   * @return map of trigger keys to their associated triggers
   * @throws SchedulerException if there is an error retrieving the triggers
   */
  Map<TriggerKey, Trigger> getTriggersForGroup(String groupName) throws SchedulerException;

  /**
   * Retrieves the live scheduler instance executing each job in a group, using a single query.
   * <p>
   * Jobs merely acquired are omitted, as are records left behind by an instance that is no longer
   * checking in. In a cluster the result describes work genuinely in flight on any node.
   *
   * @param groupName the job group name
   * @return map of job keys to the id of the live scheduler instance executing them
   * @throws SchedulerException if there is an error retrieving the executing jobs
   */
  Map<JobKey, String> getExecutingInstanceIdsForGroup(String groupName) throws SchedulerException;

  /**
   * Returns the underlying delegate Scheduler.
   *
   * @return the delegate Scheduler
   */
  Scheduler getDelegate();
}
