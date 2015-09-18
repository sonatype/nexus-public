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
package org.sonatype.nexus.quartz;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;

/**
 * Component managing Quartz {@link Scheduler}.
 *
 * @since 3.0
 */
public interface QuartzSupport
{
  /**
   * Returns the Quartz {@link Scheduler} instance.
   */
  Scheduler getScheduler();

  /**
   * Helper method to ease "one time" execution of a {@link Job}.
   */
  <T extends Job> JobKey execute(final Class<T> clazz);

  /**
   * Helper method to ease scheduling of a {@link Job}.
   */
  <T extends Job> JobKey scheduleJob(final Class<T> clazz, final Trigger trigger);

  /**
   * Returns {@code true} if job with given key exists and is currently running.
   */
  boolean isRunning(JobKey jobKey);

  /**
   * Cancels (if running) the job. Returns {@code true} if job with given key existed and was canceled. To job support
   * cancellation, it should implement the {@link InterruptableJob} interface. If the job under given key is not
   * implementing it, this method will never return {@code true}.
   */
  boolean cancelJob(JobKey jobKey);

  /**
   * Cancels (if running) and removes the job. Returns {@code true} if job with given key
   * existed and was removed.
   */
  boolean removeJob(JobKey jobKey);
}
