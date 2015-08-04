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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CProps;
import org.sonatype.nexus.configuration.model.CScheduleConfig;
import org.sonatype.nexus.configuration.model.CScheduledTask;
import org.sonatype.nexus.configuration.model.CScheduledTaskCoreConfiguration;
import org.sonatype.nexus.scheduling.TaskUtils;
import org.sonatype.scheduling.DefaultScheduledTask;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.Scheduler;
import org.sonatype.scheduling.SchedulerTask;
import org.sonatype.scheduling.TaskConfigManager;
import org.sonatype.scheduling.schedules.CronSchedule;
import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.HourlySchedule;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.MonthlySchedule;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.scheduling.schedules.WeeklySchedule;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation of the Task Configuration manager.
 */
@Singleton
@Named
public class DefaultTaskConfigManager
    extends AbstractLastingConfigurable<List<CScheduledTask>>
    implements TaskConfigManager
{

  private final Map<String, Provider<SchedulerTask<?>>> tasks;

  // TODO: Nx configuration is used here, as it's used as monitor for synchronization!!!
  @Inject
  public DefaultTaskConfigManager(final EventBus eventBus, final NexusConfiguration nexusConfiguration,
                                  final Map<String, Provider<SchedulerTask<?>>> tasks)
  {
    super("Scheduled Tasks", eventBus, nexusConfiguration);
    this.tasks = checkNotNull(tasks);
  }

  // ==

  @Override
  public void initializeConfiguration() throws ConfigurationException {
    if (getApplicationConfiguration() != null && getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<List<CScheduledTask>> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CScheduledTaskCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  // ==

  @Override
  public void initializeTasks(Scheduler scheduler) {
    initializeTasks(scheduler, new ArrayList<CScheduledTask>(getCurrentConfiguration(false)));
  }

  void initializeTasks(Scheduler scheduler, List<CScheduledTask> tasks) {
    if (tasks != null) {
      List<CScheduledTask> tempList = new ArrayList<CScheduledTask>(tasks);

      log.info(tempList.size() + " task(s) to load.");

      for (CScheduledTask task : tempList) {
        log.info("Loading task - " + task.getName());

        try {
          SchedulerTask<?> nexusTask = createTaskInstance(task.getType());

          for (CProps prop : task.getProperties()) {
            nexusTask.addParameter(prop.getKey(), prop.getValue());
          }

          TaskUtils.setId(nexusTask, task.getId());
          TaskUtils.setName(nexusTask, task.getName());

          DefaultScheduledTask<?> scheduledTask = (DefaultScheduledTask<?>) scheduler.initialize(task.getId(),
              task.getName(), task.getType(), nexusTask,
              translateFrom(task.getSchedule(), new Date(task.getNextRun())), task.isEnabled());

          // since the default schedules task appends 20 ms to the last run time, we don't want
          // set the value if it is 0, otherwise will give appearance that task did run, since
          // timestamp greater than 0
          if (task.getLastRun() > 0) {
            scheduledTask.setLastRun(new Date(task.getLastRun()));
          }
        }
        catch (IllegalArgumentException e) {
          // this is bad, Plexus did not find the component, possibly the task.getType() contains bad class
          // name
          log.warn("Unable to initialize task " + task.getName() + ", couldn't load service class " + task.getId(),
              e);
        }
      }
    }

  }

  public <T> void addTask(ScheduledTask<T> task) {
    // RunNowSchedules are not saved
    if (RunNowSchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      return;
    }

    synchronized (getApplicationConfiguration()) {
      List<CScheduledTask> tasks = getCurrentConfiguration(true);

      CScheduledTask foundTask = findTask(task.getId(), tasks);

      CScheduledTask storeableTask = translateFrom(task);

      if (storeableTask != null) {
        if (foundTask != null) {
          tasks.remove(foundTask);

          storeableTask.setLastRun(foundTask.getLastRun());
        }

        tasks.add(storeableTask);
      }

      if (log.isTraceEnabled()) {
        log.trace("Task with ID={} added, config {} modified.", task.getId(), storeableTask != null ? "IS"
            : "is NOT", new Exception("This is an exception only to provide caller backtrace"));
      }

      try {
        getApplicationConfiguration().saveConfiguration();
      }
      catch (IOException e) {
        log.warn("Could not save task changes!", e);
      }
    }
  }

  public <T> void removeTask(ScheduledTask<T> task) {
    synchronized (getApplicationConfiguration()) {
      List<CScheduledTask> tasks = getCurrentConfiguration(true);

      CScheduledTask foundTask = findTask(task.getId(), tasks);

      if (foundTask != null) {
        tasks.remove(foundTask);
      }

      if (log.isTraceEnabled()) {
        log.trace("Task with ID={} removed, config {} modified.", task.getId(), foundTask != null ? "IS" : "is NOT",
            new Exception("This is an exception only to provide caller backtrace"));
      }

      try {
        getApplicationConfiguration().saveConfiguration();
      }
      catch (IOException e) {
        log.warn("Could not save task changes!", e);
      }
    }

    // TODO: need to also add task to a history file
  }

  public SchedulerTask<?> createTaskInstance(String taskType) throws IllegalArgumentException {
    return lookupTask(taskType);
  }

  private SchedulerTask<?> lookupTask(final String taskType) {
    log.debug("Looking up task for: " + taskType);
    final Provider<SchedulerTask<?>> taskProvider = tasks.get(taskType);
    if (taskProvider == null) {
      throw new IllegalArgumentException("Could not find task of type: " + taskType);
    }
    return taskProvider.get();
  }

  public <T> T createTaskInstance(final Class<T> taskType) throws IllegalArgumentException {
    log.debug("Creating task: {}", taskType);

    try {
      // first try a full class name lookup (modern sisu-style)
      return (T) lookupTask(taskType.getCanonicalName());
    }
    catch (IllegalArgumentException e) {
      // fallback to old plexus hint style
      return (T) lookupTask(taskType.getSimpleName());
    }
  }

  // ==

  private CScheduledTask findTask(String id, List<CScheduledTask> tasks) {
    synchronized (getApplicationConfiguration()) {
      for (Iterator<CScheduledTask> iter = tasks.iterator(); iter.hasNext(); ) {
        CScheduledTask storedTask = iter.next();

        if (storedTask.getId().equals(id)) {
          return storedTask;
        }
      }

      return null;
    }
  }

  private Schedule translateFrom(CScheduleConfig modelSchedule, Date nextRun) {
    Schedule schedule = null;

    Date startDate = null;
    Date endDate = null;

    if (modelSchedule.getStartDate() > 0) {
      startDate = new Date(modelSchedule.getStartDate());
    }

    if (modelSchedule.getEndDate() > 0) {
      endDate = new Date(modelSchedule.getEndDate());
    }

    if (CScheduleConfig.TYPE_ADVANCED.equals(modelSchedule.getType())) {
      try {
        schedule = new CronSchedule(modelSchedule.getCronCommand());
      }
      catch (ParseException e) {
        // this will not happen, since it was persisted, hence already submitted
      }
    }
    else if (CScheduleConfig.TYPE_MONTHLY.equals(modelSchedule.getType())) {
      Set<Integer> daysToRun = new HashSet<Integer>();

      for (Iterator iter = modelSchedule.getDaysOfMonth().iterator(); iter.hasNext(); ) {
        String day = (String) iter.next();

        try {
          daysToRun.add(Integer.valueOf(day));
        }
        catch (NumberFormatException nfe) {
          log.error("Invalid day being added to monthly schedule - " + day + " - skipping.");
        }
      }

      schedule = new MonthlySchedule(startDate, endDate, daysToRun);
    }
    else if (CScheduleConfig.TYPE_WEEKLY.equals(modelSchedule.getType())) {
      Set<Integer> daysToRun = new HashSet<Integer>();

      for (Iterator iter = modelSchedule.getDaysOfWeek().iterator(); iter.hasNext(); ) {
        String day = (String) iter.next();

        try {
          daysToRun.add(Integer.valueOf(day));
        }
        catch (NumberFormatException nfe) {
          log.error("Invalid day being added to weekly schedule - " + day + " - skipping.");
        }
      }

      schedule = new WeeklySchedule(startDate, endDate, daysToRun);
    }
    else if (CScheduleConfig.TYPE_DAILY.equals(modelSchedule.getType())) {
      schedule = new DailySchedule(startDate, endDate);
    }
    else if (CScheduleConfig.TYPE_HOURLY.equals(modelSchedule.getType())) {
      schedule = new HourlySchedule(startDate, endDate);
    }
    else if (CScheduleConfig.TYPE_ONCE.equals(modelSchedule.getType())) {
      schedule = new OnceSchedule(startDate);
    }
    else if (CScheduleConfig.TYPE_RUN_NOW.equals(modelSchedule.getType())) {
      schedule = new RunNowSchedule();
    }
    else if (CScheduleConfig.TYPE_MANUAL.equals(modelSchedule.getType())) {
      schedule = new ManualRunSchedule();
    }
    else {
      throw new IllegalArgumentException("Unknown Schedule type: " + modelSchedule.getClass().getName());
    }

    if (nextRun != null) {
      Date resetFrom = nextRun;
      // NEXUS-4465: Cron schedule will add 1 second to given time to calculate next scheduled time
      // so we subtract it in case that next schedule is actually a valid time to run
      if (schedule instanceof CronSchedule) {
        resetFrom = new Date(resetFrom.getTime() - 1000);
      }
      schedule.getIterator().resetFrom(resetFrom);
    }

    return schedule;
  }

  private <T> CScheduledTask translateFrom(ScheduledTask<T> task) {
    CScheduledTask storeableTask = new CScheduledTask();

    storeableTask.setEnabled(task.isEnabled());
    storeableTask.setId(task.getId());
    storeableTask.setName(task.getName());
    storeableTask.setType(task.getType());
    storeableTask.setStatus(task.getTaskState().name());

    if (task.getLastRun() != null) {
      storeableTask.setLastRun(task.getLastRun().getTime());
    }

    if (task.getNextRun() != null) {
      storeableTask.setNextRun(task.getNextRun().getTime());
    }

    for (String key : task.getTaskParams().keySet()) {
      CProps props = new CProps();
      props.setKey(key);
      props.setValue(task.getTaskParams().get(key));

      storeableTask.addProperty(props);
    }

    Schedule schedule = task.getSchedule();
    CScheduleConfig storeableSchedule = new CScheduleConfig();

    if (schedule != null) {
      if (CronSchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_ADVANCED);

        storeableSchedule.setCronCommand(((CronSchedule) schedule).getCronString());
      }
      else if (MonthlySchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_MONTHLY);

        storeableSchedule.setStartDate(((MonthlySchedule) schedule).getStartDate().getTime());

        Date endDate = ((MonthlySchedule) schedule).getEndDate();

        if (endDate != null) {
          storeableSchedule.setEndDate(endDate.getTime());
        }

        for (Iterator iter = ((MonthlySchedule) schedule).getDaysToRun().iterator(); iter.hasNext(); ) {
          // TODO: String.valueOf is used because currently the days to run are integers in the monthly
          // schedule
          // needs to be string
          storeableSchedule.addDaysOfMonth(String.valueOf(iter.next()));
        }
      }
      else if (WeeklySchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_WEEKLY);

        storeableSchedule.setStartDate(((WeeklySchedule) schedule).getStartDate().getTime());

        Date endDate = ((WeeklySchedule) schedule).getEndDate();

        if (endDate != null) {
          storeableSchedule.setEndDate(endDate.getTime());
        }

        for (Iterator iter = ((WeeklySchedule) schedule).getDaysToRun().iterator(); iter.hasNext(); ) {
          // TODO: String.valueOf is used because currently the days to run are integers in the weekly
          // schedule
          // needs to be string
          storeableSchedule.addDaysOfWeek(String.valueOf(iter.next()));
        }
      }
      else if (DailySchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_DAILY);

        storeableSchedule.setStartDate(((DailySchedule) schedule).getStartDate().getTime());

        Date endDate = ((DailySchedule) schedule).getEndDate();

        if (endDate != null) {
          storeableSchedule.setEndDate(endDate.getTime());
        }
      }
      else if (HourlySchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_HOURLY);

        storeableSchedule.setStartDate(((HourlySchedule) schedule).getStartDate().getTime());

        Date endDate = ((HourlySchedule) schedule).getEndDate();

        if (endDate != null) {
          storeableSchedule.setEndDate(endDate.getTime());
        }
      }
      else if (OnceSchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_ONCE);

        storeableSchedule.setStartDate(((OnceSchedule) schedule).getStartDate().getTime());
      }
      else if (RunNowSchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_RUN_NOW);
      }
      else if (ManualRunSchedule.class.isAssignableFrom(schedule.getClass())) {
        storeableSchedule.setType(CScheduleConfig.TYPE_MANUAL);
      }
      else {
        throw new IllegalArgumentException("Unknown Schedule type: " + schedule.getClass().getName());
      }
    }

    storeableTask.setSchedule(storeableSchedule);

    return storeableTask;
  }
}
