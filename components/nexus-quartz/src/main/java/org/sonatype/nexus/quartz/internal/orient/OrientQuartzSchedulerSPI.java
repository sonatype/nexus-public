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
package org.sonatype.nexus.quartz.internal.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.quartz.internal.QuartzSchedulerSPI;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;
import org.sonatype.nexus.thread.DatabaseStatusDelayedExecutor;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.JobStore;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * Quartz {@link SchedulerSPI}.
 *
 * @since 3.0
 */
@Named
@ManagedLifecycle(phase = SERVICES)
@Singleton
public class OrientQuartzSchedulerSPI
    extends QuartzSchedulerSPI
{
  @SuppressWarnings("squid:S00107") //suppress constructor parameter count
  @Inject
  public OrientQuartzSchedulerSPI(final EventManager eventManager,
                            final NodeAccess nodeAccess,
                            final Provider<JobStore> jobStoreProvider,
                            final Provider<Scheduler> schedulerProvider,
                            final LastShutdownTimeService lastShutdownTimeService,
                            final DatabaseStatusDelayedExecutor delayedExecutor,
                            @Named("${nexus.quartz.recoverInterruptedJobs:-true}") final boolean recoverInterruptedJobs)
  {
    super(eventManager, nodeAccess, jobStoreProvider, schedulerProvider, lastShutdownTimeService, delayedExecutor,
        recoverInterruptedJobs);
  }

  public void remoteJobCreated(final JobDetail jobDetail) {
    // simulate signals Quartz would have sent
    quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
    quartzScheduler.notifySchedulerListenersJobAdded(jobDetail);
  }

  public void remoteJobUpdated(final JobDetail jobDetail) throws SchedulerException {
    updateJobListener(jobDetail);

    // simulate signals Quartz would have sent
    quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
    quartzScheduler.notifySchedulerListenersJobAdded(jobDetail);
  }

  public void remoteJobDeleted(final JobDetail jobDetail) throws SchedulerException {
    // simulate signals Quartz would have sent
    quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
    quartzScheduler.notifySchedulerListenersJobDeleted(jobDetail.getKey());

    removeJobListener(jobDetail.getKey());
  }

  public void remoteTriggerCreated(final Trigger trigger) throws SchedulerException {
    if (!isRunNow(trigger)) {

      attachJobListener(jobStoreProvider.get().retrieveJob(trigger.getJobKey()), trigger);

      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(getNextFireMillis(trigger));
      quartzScheduler.notifySchedulerListenersSchduled(trigger);
    }
    else if (isLimitedToThisNode(trigger)) {
      // special "run-now" task which was created on a different node to where it will run
      // when this happens we ping the scheduler to make sure it runs as soon as possible
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersSchduled(trigger);
    }
  }

  public void remoteTriggerUpdated(final Trigger trigger) throws SchedulerException {
    if (!isRunNow(trigger)) {

      updateJobListener(trigger);

      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(getNextFireMillis(trigger));
      quartzScheduler.notifySchedulerListenersUnscheduled(trigger.getKey());
      quartzScheduler.notifySchedulerListenersSchduled(trigger);
    }
  }

  public void remoteTriggerDeleted(final Trigger trigger) throws SchedulerException {
    if (!isRunNow(trigger)) {

      // simulate signals Quartz would have sent
      quartzScheduler.getSchedulerSignaler().signalSchedulingChange(0L);
      quartzScheduler.notifySchedulerListenersUnscheduled(trigger.getKey());

      removeJobListener(trigger.getJobKey());
    }
  }
}
