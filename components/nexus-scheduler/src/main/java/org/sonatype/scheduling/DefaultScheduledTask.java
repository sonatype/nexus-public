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
package org.sonatype.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.sonatype.scheduling.iterators.NoopSchedulerIterator;
import org.sonatype.scheduling.iterators.RunNowSchedulerIterator;
import org.sonatype.scheduling.iterators.SchedulerIterator;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.sisu.goodies.common.Loggers;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class DefaultScheduledTask<T>
    implements ScheduledTask<T>, Callable<T>
{
  private final Logger logger = Loggers.getLogger(getClass());

  private final String id;

  private String name;

  private final String type;

  private final DefaultScheduler scheduler;

  private final Callable<T> callable;

  private TaskState taskState;

  private Date scheduledAt;

  private Future<T> future;

  private Throwable brokenCause;

  private boolean enabled;

  private Date lastRun;

  private Date nextRun;

  private List<T> results;

  private Schedule schedule;

  private SchedulerIterator scheduleIterator;

  private volatile ProgressListener progressListener;

  boolean manualRun;

  private long duration;

  private TaskState lastStatus;

  private boolean toBeRemoved = false;

  public DefaultScheduledTask(String id, String name, String type, DefaultScheduler scheduler, Callable<T> callable,
                              Schedule schedule)
  {
    super();

    this.id = id;

    this.name = name;

    this.type = type;

    this.scheduler = scheduler;

    this.callable = callable;

    this.taskState = TaskState.SUBMITTED;

    this.enabled = true;

    this.results = new ArrayList<T>();

    this.schedule = schedule;

    this.scheduleIterator = null;

    this.nextRun = null;

    this.manualRun = false;
  }

  public SchedulerTask<T> getSchedulerTask() {
    final Callable<T> task = getTask();

    if (task != null && task instanceof SchedulerTask<?>) {
      return (SchedulerTask<T>) task;
    }
    else {
      return null;
    }
  }

  public ProgressListener getProgressListener() {
    return progressListener;
  }

  public Callable<T> getTask() {
    return callable;
  }

  protected void start() {
    this.scheduledAt = new Date();

    reschedule(true);
  }

  protected Future<T> getFuture() {
    return future;
  }

  protected void setFuture(Future<T> future) {
    this.future = future;
  }

  protected DefaultScheduler getScheduler() {
    return scheduler;
  }

  protected void setTaskState(TaskState state) {
    if (!getTaskState().isEndingState()) {
      this.taskState = state;
    }
  }

  protected void setBrokenCause(Throwable e) {
    this.brokenCause = e;
  }

  protected Callable<T> getCallable() {
    return callable;
  }

  protected boolean isManualRunScheduled() {
    return ManualRunSchedule.class.isAssignableFrom(getSchedule().getClass());
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public TaskState getTaskState() {
    return taskState;
  }

  public Date getScheduledAt() {
    return scheduledAt;
  }

  public void cancelOnly() {
    cancel(false, getScheduleIterator().isFinished());
  }

  public void cancel() {
    cancel(false);
  }

  public void cancel(boolean interrupt) {
    cancel(interrupt, true);
  }

  public void cancel(boolean interrupt, boolean removeTask) {
    final ProgressListener progressListener = getProgressListener();
    TaskState originalState = getTaskState();

    // only go into cancelling state if task is actually doing something
    if (originalState.isExecuting() || originalState.equals(TaskState.SLEEPING)) {
      setTaskState(TaskState.CANCELLING);

      if (progressListener != null) {
        progressListener.cancel();
      }

      // to prevent starting it if not yet started
      if (getFuture() != null) {
        getFuture().cancel(interrupt);
      }

      if (originalState.equals(TaskState.SLEEPING)) {
        // NEXUS-4681 set last run to identify we tried to run this
        setLastRun(new Date());

        // remove one-shots
        if (getScheduleIterator().isFinished()) {
          removeTask = true;
        }
        else if (!removeTask) {
          // only reschedule/set new state if task is not to be removed
          // (maintain correct state transitions, e.g. SUBMITTED -> CANCELLED not allowed)
          reschedule(true);
          TaskState newState = isManualRunScheduled() ? TaskState.SUBMITTED : TaskState.WAITING;
          setTaskState(newState);
        }
      }
      setToBeRemoved(removeTask);
    }

    // if this task is not executing, it can be immediately removed from task map
    if (removeTask && !originalState.isExecuting()) {
      setTaskState(TaskState.CANCELLED);
      getScheduler().removeFromTasksMap(this);
    }
  }

  public void reset() {
    final ProgressListener progressListener = getProgressListener();

    if (progressListener != null) {
      progressListener.cancel();
    }

    // to prevent starting it if not yet started
    if (getFuture() != null) {
      getFuture().cancel(false);
    }

    setTaskState(TaskState.SUBMITTED);

    reschedule(true);
  }

  public Throwable getBrokenCause() {
    return brokenCause;
  }

  public T get()
      throws ExecutionException, InterruptedException
  {
    return getFuture().get();
  }

  public T getIfDone() {
    if (TaskState.FINISHED.equals(getTaskState())) {
      try {
        return getFuture().get();
      }
      catch (ExecutionException e) {
        return null;
      }
      catch (InterruptedException e) {
        return null;
      }
    }
    else {
      return null;
    }
  }

  public void setLastRun(Date lastRun) {
    this.lastRun = new Date(lastRun.getTime() + 20);
  }

  protected void setLastStatus(TaskState lastStatus) {
    this.lastStatus = lastStatus;
  }

  protected void setDuration(long duration) {
    this.duration = duration;
  }

  private Future<T> doSchedule(final long nextTime, final boolean inheritContext) {
    // FIXME: All this coz MDCUtils are in Core, while
    // scheduler is a module ahead. If scheduler would
    // be in core, this would not be needed, but instead
    // MDCUtils.markCurrentContextNonInheritable() call would do it
    if (!inheritContext) {
      MDC.put("non-inheritable", "non-inheritable");
    }
    try {
      return getScheduler().getScheduledExecutorService().schedule(this, Math.max(0, nextTime
          - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
    }
    finally {
      if (!inheritContext) {
        MDC.remove("non-inheritable");
      }
    }
  }

  protected long reschedule(boolean setFuture) {
    if (!isManualRunScheduled()) {
      SchedulerIterator iter = getScheduleIterator();

      if (iter != null && !iter.isFinished()) {
        nextRun = iter.next();

        long nextTime = 0;

        if (nextRun != null) {
          nextTime = nextRun.getTime();
        }
        else {
          nextTime = System.currentTimeMillis();
        }

        getScheduler().taskRescheduled(this);

        if (setFuture) {
          setFuture(doSchedule(nextTime, iter instanceof RunNowSchedulerIterator));
        }

        return nextTime;
      }
      else {
        nextRun = null;
      }
    }
    else {
      nextRun = null;
    }

    return -1;
  }

  public void runNow() {
    // if we are not RUNNING
    if (!TaskState.RUNNING.equals(getTaskState()) && !manualRun) {
      manualRun = true;

      if (getFuture() != null) {
        //as bentmann mentioned, there is potential for the future to have started running between the check on our if above and now
        //so we force interrupt it
        getFuture().cancel(true);
      }

      setFuture(doSchedule(-1, true));
    }
  }

  public T call()
      throws Exception
  {
    try {
      this.progressListener = new LoggingProgressListener(getCallable().getClass().getSimpleName());

      TaskUtil.setCurrent(this.progressListener);

      T result = null;

      // Check to see if the task is already running, and if so, if concurrent executions are allowed.
      // Should there concurrency not be allowed, the task will just sleep 10 secs and try again
      if (getCallable() instanceof SchedulerTask) {
        // check for execution
        if (!((SchedulerTask<?>) getCallable()).allowConcurrentExecution(getScheduler().getActiveTasks())) {
          if (nextRun != null) {
            // simply reschedule itself for 10sec
            nextRun = new Date(nextRun.getTime() + 10000);
          }

          setFuture(getScheduler().getScheduledExecutorService().schedule(this, 10000,
              TimeUnit.MILLISECONDS));

          setTaskState(TaskState.SLEEPING);

          return result;
        }
      }

      //nextFuture is gone, was too problematic having all these futures to setup and cancel, now we calculate next run time before
      //we run (to preserve that) and then run the task, then schedule the futures afterwards, the only gotcha is manual runs
      //as there will be a future for the already scheduled next run that we need to handle
      long nextMillis = -1;
      Date peekBefore = null;
      Date peekAfter = null;

      if ((isEnabled() || manualRun) && getTaskState().isRunnable()) {
        setTaskState(TaskState.RUNNING);

        Date startDate = new Date();

        try {
          //on a manual run we dont need to calculate the next scheduled time unless the task changes the schedule
          //we will just reuse the previous future at the end
          if (!manualRun) {
            nextMillis = reschedule(false);
          }
          //if manual run, we take the previously set nextRun time and use that as next time
          else if (nextRun != null) {
            nextMillis = nextRun.getTime();
          }

          setLastRun(startDate);

          // keep track of the next run times so that if they change during run, we can reschedule the next future
          // appropriately
          peekBefore = getScheduleIterator().peekNext();
          result = getCallable().call();
          peekAfter = getScheduleIterator().peekNext();

          if (result != null) {
            results.add(result);
          }
          setLastStatus(TaskState.FINISHED);
        }
        catch (Throwable e) {
          logger.warn("Exception in call method of scheduled task {}", getName(), e);

          //in case schedule changed, lets grab it, even in error state
          if (peekAfter == null) {
            peekAfter = getScheduleIterator().peekNext();
          }

          setBrokenCause(e);

          setLastStatus(TaskState.BROKEN);
          if (isToBeRemoved()) {
            setTaskState(TaskState.CANCELLED);
          }
          else {
            setTaskState(TaskState.BROKEN);
          }

          //if this is not a manual task, and no next scheduled time, dump it
          if ((!isManualRunScheduled() && nextMillis == -1 && isEnabled()) || isToBeRemoved()) {
            getScheduler().removeFromTasksMap(this);
          }
          //if the next scheduled time changed during task run, reschedule
          else if ((peekBefore == null && peekAfter != null)
              || (peekBefore != null && !peekBefore.equals(peekAfter))) {
            reschedule(true);
          }
          //now that we dont schedule beforehand, need to schedule on error case if necessary
          else if (nextMillis != -1) {
            setFuture(doSchedule(nextMillis, false));
          }

          if (e instanceof Exception) {
            // this is an exception, pass it further
            throw (Exception) e;
          }
          else {
            // this is a Throwable or Error instance, pack it into an exception and rethrow
            throw new TaskExecutionException(e);
          }
        }
        finally {
          setDuration(System.currentTimeMillis() - startDate.getTime());
        }
      }

      if (TaskState.BROKEN == getTaskState()) {
        // do nothing, let user fix or delete it
      }
      else if (isToBeRemoved()) {
        setTaskState(TaskState.CANCELLED);
      }
      //manual schedule just sticks on submitted til user triggers again
      else if (isManualRunScheduled()) {
        setTaskState(TaskState.SUBMITTED);
      }
      //if the next scheduled time changed during task run, reschedule
      else if ((peekBefore == null && peekAfter != null)
          || (peekBefore != null && !peekBefore.equals(peekAfter))) {
        setTaskState(TaskState.WAITING);
        reschedule(true);
      }
      //schedule as appropriate in most common case
      else if (nextMillis != -1) {
        setTaskState(TaskState.WAITING);

        setFuture(doSchedule(nextMillis, false));
      }
      // If disabled (and not manually run),
      // put to waiting and reschedule for next time
      // user may want to enable at some point,
      // so still seeing the next run time may be handy
      else if (!isEnabled()) {
        setTaskState(TaskState.WAITING);

        reschedule(true);
      }
      // this execution was the last execution (no other if-clause triggered)
      else if (TaskState.CANCELLING.equals(getTaskState())) {
        setTaskState(TaskState.CANCELLED);
      }
      else {
        setTaskState(TaskState.FINISHED);
      }

      if (getTaskState().isEndingState() /* FINISHED or CANCELLED */) {
        getScheduler().removeFromTasksMap(this);
      }

      return result;
    }
    finally {
      manualRun = false;

      this.progressListener = null;

      TaskUtil.setCurrent(null);
    }
  }

  // IteratingTask

  public Date getLastRun() {
    return lastRun;
  }

  public TaskState getLastStatus() {
    return lastStatus;
  }

  public Long getDuration() {
    return duration;
  }

  public Date getNextRun() {
    return nextRun;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<T> getResults() {
    return results;
  }

  // ScheduledTask

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;

    this.scheduleIterator = null;
  }

  public SchedulerIterator getScheduleIterator() {
    if (scheduleIterator == null && getSchedule() != null) {
      scheduleIterator = getSchedule().getIterator();
    }

    //this noop iterator will never return a next run time, but will cover users from having to check for null
    if (scheduleIterator == null) {
      return new NoopSchedulerIterator();
    }

    return scheduleIterator;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getTaskParams() {
    if (SchedulerTask.class.isAssignableFrom(getCallable().getClass())) {
      return ((SchedulerTask<?>) getCallable()).getParameters();
    }

    return Collections.emptyMap();
  }

  public boolean isToBeRemoved() {
    return toBeRemoved;
  }

  public void setToBeRemoved(boolean toBeRemoved) {
    this.toBeRemoved = toBeRemoved;
  }
}
