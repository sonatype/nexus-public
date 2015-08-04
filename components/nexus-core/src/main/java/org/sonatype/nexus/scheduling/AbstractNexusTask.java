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
package org.sonatype.nexus.scheduling;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.sonatype.nexus.scheduling.events.NexusTaskEventStarted;
import org.sonatype.nexus.scheduling.events.NexusTaskEventStoppedCanceled;
import org.sonatype.nexus.scheduling.events.NexusTaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.NexusTaskEventStoppedFailed;
import org.sonatype.nexus.web.internal.BaseUrlDetector;
import org.sonatype.scheduling.AbstractSchedulerTask;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.TaskInterruptedException;
import org.sonatype.scheduling.TaskState;
import org.sonatype.scheduling.TaskUtil;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Throwables;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DurationFormatUtils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base class for all Nexus tasks.
 */
public abstract class AbstractNexusTask<T>
    extends AbstractSchedulerTask<T>
    implements NexusTask<T>
{
  public static final long A_DAY = 24L * 60L * 60L * 1000L;

  private EventBus eventBus;

  private BaseUrlDetector baseUrlDetector;

  protected AbstractNexusTask() {
    this(null);
  }

  protected AbstractNexusTask(final String name) {
    if (name == null || name.trim().length() == 0) {
      TaskUtils.setName(this, getClass().getSimpleName());
    }
    else {
      TaskUtils.setName(this, name);
    }
  }

  protected AbstractNexusTask(final EventBus eventBus, final String name) {
    this(name);
    this.eventBus = eventBus;
  }

  protected EventBus getEventBus() {
    checkState(eventBus != null);
    return eventBus;
  }

  @Inject
  public void setEventBus(final EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Inject
  public void setBaseUrlDetector(final BaseUrlDetector baseUrlDetector) {
    this.baseUrlDetector = baseUrlDetector;
  }

  protected final void notifyEventListeners(final Object event) {
    eventBus.post(event);
  }

  public boolean isExposed() {
    // override to hide it
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    return getParameter(ID_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return getParameter(NAME_KEY);
  }

  /**
   * {@inheritDoc}
   */
  public boolean shouldSendAlertEmail() {
    final String alertEmail = getAlertEmail();
    return alertEmail != null && alertEmail.trim().length() > 0;
  }

  /**
   * {@inheritDoc}
   */
  public String getAlertEmail() {
    return getParameter(ALERT_EMAIL_KEY);
  }

  public boolean allowConcurrentSubmission(Map<String, List<ScheduledTask<?>>> activeTasks) {
    // concurrent execution will stop us if needed, but user may freely submit
    return true;
  }

  public boolean allowConcurrentExecution(Map<String, List<ScheduledTask<?>>> activeTasks) {
    // most basic check: simply not allowing multiple execution of instances of this class
    // override if needed
    if (activeTasks.containsKey(this.getClass().getSimpleName())) {
      for (ScheduledTask<?> task : activeTasks.get(this.getClass().getSimpleName())) {
        if (TaskState.RUNNING.equals(task.getTaskState())) {
          return false;
        }
      }
      return true;
    }
    else {
      return true;
    }
  }

  public final T call()
      throws Exception
  {
    getLogger().info(getLoggedMessage("started"));
    final long started = System.currentTimeMillis();

    // attempt to set the base-url for the current thread
    if (baseUrlDetector != null) {
      baseUrlDetector.set();
    }

    // fire event
    final NexusTaskEventStarted<T> startedEvent = new NexusTaskEventStarted<T>(this);
    getEventBus().post(startedEvent);

    T result = null;

    try {
      beforeRun();

      result = doRun();

      if (TaskUtil.getCurrentProgressListener().isCanceled()) {
        getLogger().info(getLoggedMessage("canceled", started));

        getEventBus().post(new NexusTaskEventStoppedCanceled<T>(this, startedEvent));
      }
      else {
        getLogger().info(getLoggedMessage("finished", started));

        getEventBus().post(new NexusTaskEventStoppedDone<T>(this, startedEvent));
      }

      afterRun();

      return result;
    }
    catch (final Throwable e) {
      // this if below is to catch TaskInterruptedException in tasks that does not handle it
      // and let it propagate.
      if (e instanceof TaskInterruptedException) {
        getLogger().info(getLoggedMessage("canceled", started));

        // just return, nothing happened just task cancelled
        getEventBus().post(new NexusTaskEventStoppedCanceled<T>(this, startedEvent));

        return null;
      }
      else {
        getLogger().warn(getLoggedMessage("failed", started), e);

        // notify that there was a failure
        getEventBus().post(new NexusTaskEventStoppedFailed<T>(this, startedEvent, e));

        Throwables.propagateIfInstanceOf(e, Exception.class);
        throw Throwables.propagate(e);
      }
    }
  }

  protected String getLoggedMessage(final String action) {
    return String.format("Scheduled task (%s) %s :: %s", getName(), action, getMessage());
  }

  protected String getLoggedMessage(final String action, final long started) {
    final String startedStr = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(started);
    final String durationStr = DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - started);

    return String.format("%s (started %s, runtime %s)", getLoggedMessage(action), startedStr, durationStr);
  }

  protected void beforeRun()
      throws Exception
  {
    // override if needed
  }

  protected abstract T doRun()
      throws Exception;

  protected void afterRun()
      throws Exception
  {
    // override if needed
  }

  protected abstract String getAction();

  protected abstract String getMessage();

}
