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
package org.sonatype.nexus.scheduling.internal;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;

import com.codahale.metrics.health.HealthCheck;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Scheduler health check that reports tasks with auto-recovered triggers.
 *
 * This check identifies two scenarios:
 * - Jobs that have no trigger at all in the database (should not occur after startup, as triggers are
 * auto-created during startup)
 * - Triggers that were automatically recovered during system startup and marked with a recovery flag,
 * indicating the task schedule needs to be reconfigured by an administrator
 *
 * The alert message provides clear instructions for administrators to navigate to the Tasks UI and
 * reconfigure affected task schedules. This addresses NEXUS-44603 by making the alert actionable.
 *
 * @since 3.17
 */
@Component
@Qualifier("Scheduler")
public class SchedulerHealthCheck
    extends HealthCheck
{
  private final Provider<SchedulerSPI> scheduler;

  @Autowired
  public SchedulerHealthCheck(final Provider<SchedulerSPI> scheduler) {
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  protected Result check() {
    List<String> missingTaskDescriptions = scheduler.get().getMissingTriggerDescriptions();
    return missingTaskDescriptions.isEmpty() ? Result.healthy() : Result.unhealthy(reason(missingTaskDescriptions));
  }

  private String reason(final List<String> missingTaskDescriptions) {
    String taskDescriptions = String.join(", ", missingTaskDescriptions);
    return format("Missing triggers were auto-recovered for %s task%s: %s. " +
        "Review and update each task's schedule (Administration > System > Tasks).",
        missingTaskDescriptions.size(),
        missingTaskDescriptions.size() == 1 ? "" : "s",
        taskDescriptions);
  }
}
