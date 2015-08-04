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
package org.sonatype.nexus.rest.schedules;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;
import org.sonatype.nexus.rest.formfield.AbstractFormFieldResource;
import org.sonatype.nexus.rest.model.ScheduledServiceAdvancedResource;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceDailyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceHourlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceMonthlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.scheduling.NexusTask;
import org.sonatype.nexus.scheduling.TaskUtils;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.TaskState;
import org.sonatype.scheduling.iterators.MonthlySchedulerIterator;
import org.sonatype.scheduling.schedules.CronSchedule;
import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.HourlySchedule;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.scheduling.schedules.MonthlySchedule;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.scheduling.schedules.WeeklySchedule;

import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

public abstract class AbstractScheduledServicePlexusResource
    extends AbstractFormFieldResource
{
  private NexusScheduler nexusScheduler;

  public static final String SCHEDULED_SERVICE_ID_KEY = "scheduledServiceId";

  private DateFormat timeFormat = new SimpleDateFormat("HH:mm");

  @Inject
  public void setNexusScheduler(final NexusScheduler nexusScheduler) {
    this.nexusScheduler = nexusScheduler;
  }

  protected NexusScheduler getNexusScheduler() {
    return nexusScheduler;
  }

  protected String getScheduleShortName(Schedule schedule) {
    if (ManualRunSchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_MANUAL;
    }
    else if (RunNowSchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_RUN_NOW;
    }
    else if (OnceSchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_ONCE;
    }
    else if (HourlySchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_HOURLY;
    }
    else if (DailySchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_DAILY;
    }
    else if (WeeklySchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_WEEKLY;
    }
    else if (MonthlySchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_MONTHLY;
    }
    else if (CronSchedule.class.isAssignableFrom(schedule.getClass())) {
      return ScheduledServiceBaseResourceConverter.SCHEDULE_TYPE_ADVANCED;
    }
    else {
      return schedule.getClass().getName();
    }
  }

  protected String formatDate(Date date) {
    return Long.toString(date.getTime());
  }

  protected String formatTime(Date date) {
    return timeFormat.format(date);
  }

  protected List<ScheduledServicePropertyResource> formatServiceProperties(Map<String, String> map) {
    List<ScheduledServicePropertyResource> list = new ArrayList<ScheduledServicePropertyResource>();

    for (String key : map.keySet()) {
      if (!TaskUtils.isPrivateProperty(key)) {
        ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
        prop.setKey(key);
        prop.setValue(map.get(key));
        list.add(prop);
      }
    }

    return list;
  }

  protected List<String> formatRecurringDayOfWeek(Set<Integer> days) {
    List<String> list = new ArrayList<String>();

    for (Integer day : days) {
      switch (day.intValue()) {
        case 1: {
          list.add("sunday");
          break;
        }
        case 2: {
          list.add("monday");
          break;
        }
        case 3: {
          list.add("tuesday");
          break;
        }
        case 4: {
          list.add("wednesday");
          break;
        }
        case 5: {
          list.add("thursday");
          break;
        }
        case 6: {
          list.add("friday");
          break;
        }
        case 7: {
          list.add("saturday");
          break;
        }
      }
    }

    return list;
  }

  protected Set<Integer> formatRecurringDayOfWeek(List<String> days) {
    Set<Integer> set = new HashSet<Integer>();

    for (String day : days) {
      if ("sunday".equals(day)) {
        set.add(new Integer(1));
      }
      else if ("monday".equals(day)) {
        set.add(new Integer(2));
      }
      else if ("tuesday".equals(day)) {
        set.add(new Integer(3));
      }
      else if ("wednesday".equals(day)) {
        set.add(new Integer(4));
      }
      else if ("thursday".equals(day)) {
        set.add(new Integer(5));
      }
      else if ("friday".equals(day)) {
        set.add(new Integer(6));
      }
      else if ("saturday".equals(day)) {
        set.add(new Integer(7));
      }
    }

    return set;
  }

  protected List<String> formatRecurringDayOfMonth(Set<Integer> days) {
    List<String> list = new ArrayList<String>();

    for (Integer day : days) {
      if (MonthlySchedulerIterator.LAST_DAY_OF_MONTH.equals(day)) {
        list.add("last");
      }
      else {
        list.add(String.valueOf(day));
      }
    }

    return list;
  }

  protected Set<Integer> formatRecurringDayOfMonth(List<String> days) {
    Set<Integer> set = new HashSet<Integer>();

    for (String day : days) {
      if ("last".equals(day)) {
        set.add(MonthlySchedulerIterator.LAST_DAY_OF_MONTH);
      }
      else {
        set.add(Integer.valueOf(day));
      }
    }

    return set;
  }

  protected Date parseDate(String date, String time) {
    Calendar cal = Calendar.getInstance();
    Calendar timeCalendar = Calendar.getInstance();

    try {
      timeCalendar.setTime(timeFormat.parse(time));

      cal.setTime(new Date(Long.parseLong(date)));
      cal.add(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
      cal.add(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));

      if (getLogger().isDebugEnabled()) {
        getLogger().debug("Parsed date from task creation request: " + cal.getTime());
      }
    }
    catch (ParseException e) {
      cal = null;
    }

    return cal == null ? null : cal.getTime();
  }

  public String getModelName(ScheduledServiceBaseResource model) {
    return model.getName();
  }

  public NexusTask<?> getModelNexusTask(ScheduledServiceBaseResource model, Request request)
      throws IllegalArgumentException, ResourceException
  {
    String serviceType = model.getTypeId();

    NexusTask<?> task = getNexusScheduler().createTaskInstance(serviceType);

    for (Iterator iter = model.getProperties().iterator(); iter.hasNext(); ) {
      ScheduledServicePropertyResource prop = (ScheduledServicePropertyResource) iter.next();
      task.addParameter(prop.getKey(), prop.getValue());
    }

    TaskUtils.setAlertEmail(task, model.getAlertEmail());
    TaskUtils.setId(task, model.getId());
    TaskUtils.setName(task, model.getName());

    return task;
  }

  public void validateStartDate(String date)
      throws InvalidConfigurationException
  {
    Calendar cal = Calendar.getInstance();
    Date startDate = new Date(Long.parseLong(date));
    cal.setTime(startDate);

    Calendar nowCal = Calendar.getInstance();
    nowCal.add(Calendar.DAY_OF_YEAR, -1);
    nowCal.set(Calendar.HOUR, 0);
    nowCal.set(Calendar.MINUTE, 0);
    nowCal.set(Calendar.SECOND, 0);
    nowCal.set(Calendar.MILLISECOND, 0);

    // This is checking just the year/month/day, time isn't of concern right now
    // basic check that the day timestamp is roughly in the correct range
    if (cal.before(nowCal)) {
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("Validation error for startDate: " + startDate.toString());
      }

      ValidationResponse vr = new ApplicationValidationResponse();
      ValidationMessage vm = new ValidationMessage("startDate", "Date cannot be in the past.");
      vr.addValidationError(vm);
      throw new InvalidConfigurationException(vr);
    }
  }

  public void validateTime(String key, Date date)
      throws InvalidConfigurationException
  {
    if (date.before(new Date())) {
      ValidationResponse vr = new ApplicationValidationResponse();
      ValidationMessage vm = new ValidationMessage(key, "Time cannot be in the past.");
      vr.addValidationError(vm);
      throw new InvalidConfigurationException(vr);
    }
  }

  public Schedule getModelSchedule(ScheduledServiceBaseResource model)
      throws ParseException, InvalidConfigurationException
  {
    Schedule schedule = null;

    if (ScheduledServiceAdvancedResource.class.isAssignableFrom(model.getClass())) {
      schedule = new CronSchedule(((ScheduledServiceAdvancedResource) model).getCronCommand());
    }
    else if (ScheduledServiceMonthlyResource.class.isAssignableFrom(model.getClass())) {
      Date date =
          parseDate(((ScheduledServiceMonthlyResource) model).getStartDate(),
              ((ScheduledServiceMonthlyResource) model).getRecurringTime());

      schedule =
          new MonthlySchedule(date, null,
              formatRecurringDayOfMonth(((ScheduledServiceMonthlyResource) model).getRecurringDay()));
    }
    else if (ScheduledServiceWeeklyResource.class.isAssignableFrom(model.getClass())) {
      Date date =
          parseDate(((ScheduledServiceWeeklyResource) model).getStartDate(),
              ((ScheduledServiceWeeklyResource) model).getRecurringTime());

      schedule =
          new WeeklySchedule(date, null,
              formatRecurringDayOfWeek(((ScheduledServiceWeeklyResource) model).getRecurringDay()));
    }
    else if (ScheduledServiceDailyResource.class.isAssignableFrom(model.getClass())) {
      Date date =
          parseDate(((ScheduledServiceDailyResource) model).getStartDate(),
              ((ScheduledServiceDailyResource) model).getRecurringTime());

      schedule = new DailySchedule(date, null);
    }
    else if (ScheduledServiceHourlyResource.class.isAssignableFrom(model.getClass())) {
      Date date =
          parseDate(((ScheduledServiceHourlyResource) model).getStartDate(),
              ((ScheduledServiceHourlyResource) model).getStartTime());

      schedule = new HourlySchedule(date, null);
    }
    else if (ScheduledServiceOnceResource.class.isAssignableFrom(model.getClass())) {
      Date date =
          parseDate(((ScheduledServiceOnceResource) model).getStartDate(),
              ((ScheduledServiceOnceResource) model).getStartTime());

      validateStartDate(((ScheduledServiceOnceResource) model).getStartDate());
      validateTime("startTime", date);

      schedule =
          new OnceSchedule(parseDate(((ScheduledServiceOnceResource) model).getStartDate(),
              ((ScheduledServiceOnceResource) model).getStartTime()));
    }
    else {
      schedule = new ManualRunSchedule();
    }

    return schedule;
  }

  public <T> ScheduledServiceBaseResource getServiceRestModel(ScheduledTask<T> task) {
    ScheduledServiceBaseResource resource = null;

    if (RunNowSchedule.class.isAssignableFrom(task.getSchedule().getClass())
        || ManualRunSchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceBaseResource();
    }
    else if (OnceSchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceOnceResource();

      OnceSchedule taskSchedule = (OnceSchedule) task.getSchedule();
      ScheduledServiceOnceResource res = (ScheduledServiceOnceResource) resource;

      res.setStartDate(formatDate(taskSchedule.getStartDate()));
      res.setStartTime(formatTime(taskSchedule.getStartDate()));
    }
    else if (HourlySchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceHourlyResource();

      HourlySchedule taskSchedule = (HourlySchedule) task.getSchedule();
      ScheduledServiceHourlyResource res = (ScheduledServiceHourlyResource) resource;

      res.setStartDate(formatDate(taskSchedule.getStartDate()));
      res.setStartTime(formatTime(taskSchedule.getStartDate()));
    }
    else if (DailySchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceDailyResource();

      DailySchedule taskSchedule = (DailySchedule) task.getSchedule();
      ScheduledServiceDailyResource res = (ScheduledServiceDailyResource) resource;

      res.setStartDate(formatDate(taskSchedule.getStartDate()));
      res.setRecurringTime(formatTime(taskSchedule.getStartDate()));
    }
    else if (WeeklySchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceWeeklyResource();

      WeeklySchedule taskSchedule = (WeeklySchedule) task.getSchedule();
      ScheduledServiceWeeklyResource res = (ScheduledServiceWeeklyResource) resource;

      res.setStartDate(formatDate(taskSchedule.getStartDate()));
      res.setRecurringTime(formatTime(taskSchedule.getStartDate()));
      res.setRecurringDay(formatRecurringDayOfWeek(taskSchedule.getDaysToRun()));
    }
    else if (MonthlySchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceMonthlyResource();

      MonthlySchedule taskSchedule = (MonthlySchedule) task.getSchedule();
      ScheduledServiceMonthlyResource res = (ScheduledServiceMonthlyResource) resource;

      res.setStartDate(formatDate(taskSchedule.getStartDate()));
      res.setRecurringTime(formatTime(taskSchedule.getStartDate()));
      res.setRecurringDay(formatRecurringDayOfMonth(taskSchedule.getDaysToRun()));
    }
    else if (CronSchedule.class.isAssignableFrom(task.getSchedule().getClass())) {
      resource = new ScheduledServiceAdvancedResource();

      CronSchedule taskSchedule = (CronSchedule) task.getSchedule();
      ScheduledServiceAdvancedResource res = (ScheduledServiceAdvancedResource) resource;

      res.setCronCommand(taskSchedule.getCronString());
    }

    if (resource != null) {
      resource.setId(task.getId());
      resource.setEnabled(task.isEnabled());
      resource.setName(task.getName());
      resource.setSchedule(getScheduleShortName(task.getSchedule()));
      resource.setTypeId(task.getType());
      resource.setProperties(formatServiceProperties(task.getTaskParams()));
      resource.setAlertEmail(TaskUtils.getAlertEmail(task));
    }

    return resource;
  }

  protected <T> Date getNextRunTime(ScheduledTask<T> task) {
    Date nextRunTime = null;

    // Run now type tasks should never have a next run time
    if (!task.getSchedule().getClass().isAssignableFrom(RunNowSchedule.class) && task.getNextRun() != null) {
      nextRunTime = task.getNextRun();
    }

    return nextRunTime;
  }

  protected String getLastRunResult(ScheduledTask<?> task) {
    String lastRunResult = "n/a";

    if (task.getLastStatus() != null) {
      lastRunResult = TaskState.BROKEN.equals(task.getLastStatus()) ? "Error" : "Ok";
      if (task.getDuration() != 0) {
        long milliseconds = task.getDuration();
        int hours = (int) ((milliseconds / 1000) / 3600);
        int minutes = (int) ((milliseconds / 1000) / 60 - hours * 60);
        int seconds = (int) ((milliseconds / 1000) % 60);

        lastRunResult += " [";
        if (hours != 0) {
          lastRunResult += hours;
          lastRunResult += "h";
        }
        if (minutes != 0 || hours != 0) {
          lastRunResult += minutes;
          lastRunResult += "m";
        }
        lastRunResult += seconds;
        lastRunResult += "s";
        lastRunResult += "]";
      }
    }
    return lastRunResult;
  }

  protected String getReadableState(TaskState taskState) {
    switch (taskState) {
      case SUBMITTED:
      case WAITING:
      case FINISHED:
      case BROKEN:
        return "Waiting";
      case RUNNING:
        return "Running";
      case SLEEPING:
        return "Blocked";
      case CANCELLING:
        return "Cancelling";
      case CANCELLED:
        return "Cancelled";
      default:
        throw new IllegalStateException();
    }
  }

}
