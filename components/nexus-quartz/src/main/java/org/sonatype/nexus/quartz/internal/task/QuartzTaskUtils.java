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
package org.sonatype.nexus.quartz.internal.task;

import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.quartz.TriggerKey.triggerKey;

/**
 * Utilities for adapting the scheduling API to Quartz.
 *
 * @since 3.19
 */
public class QuartzTaskUtils {
  private static final Logger LOG = LoggerFactory.getLogger(QuartzTaskUtils.class);

  private QuartzTaskUtils() {
    // static class
  }

  /**
   * Returns the trigger associated with NX Task wrapping job.
   *
   * The trigger executing this Job does NOT have to be THAT trigger, think about "runNow"!
   * So, this method returns the associated trigger, while the trigger in context might be something
   * completely different.
   *
   * If not found, returns {@code null}.
   */
  @Nullable
  public static Trigger getJobTrigger(final JobExecutionContext context) {
    try {
      final JobKey jobKey = context.getJobDetail().getKey();
      return context.getScheduler().getTrigger(triggerKey(jobKey.getName(), jobKey.getGroup()));
    }
    catch (SchedulerException e) {
      LOG.debug("Unable to get job trigger for context: {}", context, e);
      return null;
    }
  }

  /**
   * Returns the current trigger for currently executing NX Task.
   *
   * That is either its associated trigger loaded up by key using {@link #getJobTrigger(JobExecutionContext)},
   * or if not found (can happen when invoked from
   * {@link JobListener#jobWasExecuted(JobExecutionContext, JobExecutionException)}
   * method for a canceled/removed job, the "current" trigger from context is returned.
   *
   * Never returns {@code null}, as Quartz context always contains a trigger.
   */
  public static Trigger getCurrentTrigger(final JobExecutionContext context) {
    final Trigger jobTrigger = getJobTrigger(context);
    return jobTrigger != null ? jobTrigger : context.getTrigger();
  }

  /**
   * Extracts {@link TaskConfiguration} from given {@link JobDetail}.
   *
   * Only copies string values from job-data map.
   */
  public static TaskConfiguration configurationOf(final JobDetail jobDetail) {
    checkNotNull(jobDetail);

    return configurationOf(jobDetail.getJobDataMap());
  }

  /**
   * Extracts {@link TaskConfiguration} from given {@link JobDetail}.
   *
   * Only copies string values from job-data map.
   */
  public static TaskConfiguration configurationOf(final JobDataMap jobDataMap) {
    checkNotNull(jobDataMap);
    TaskConfiguration config = new TaskConfiguration();
    for (Entry<String, Object> entry : jobDataMap.entrySet()) {
      if (entry.getValue() instanceof String) {
        config.setString(entry.getKey(), (String) entry.getValue());
      }
    }
    return config;
  }

  /**
   * Saves {@link TaskConfiguration} back to the given {@link JobDetail}.
   */
  public static void updateJobData(final JobDetail jobDetail, final TaskConfiguration taskConfiguration) {
    JobDataMap jobDataMap = jobDetail.getJobDataMap();
    taskConfiguration.asMap().forEach((key, value) -> {
      if (TaskConfiguration.REMOVE_ATTRIBUTE_MARKER.equals(value)) {
        jobDataMap.remove(key);
      }
      else if (!value.equals(jobDataMap.get(key))) {
        jobDataMap.put(key, value); // only touch jobDataMap if value actually changed
      }
    });
  }
}
