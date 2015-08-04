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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CScheduleConfig;
import org.sonatype.nexus.configuration.model.CScheduledTask;
import org.sonatype.scheduling.DefaultScheduledTask;
import org.sonatype.scheduling.DefaultScheduler;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.Scheduler;
import org.sonatype.scheduling.SchedulerTask;
import org.sonatype.scheduling.TaskConfigManager;
import org.sonatype.scheduling.TaskState;
import org.sonatype.scheduling.schedules.CronSchedule;
import org.sonatype.scheduling.schedules.DailySchedule;
import org.sonatype.scheduling.schedules.MonthlySchedule;
import org.sonatype.scheduling.schedules.OnceSchedule;
import org.sonatype.scheduling.schedules.Schedule;
import org.sonatype.scheduling.schedules.WeeklySchedule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Note: This test logs will be full of WARNs like these:
 *
 * <pre>
 * WARN o.s.n.c.a.DefaultNexusConfiguration - Could not obtain Shiro subject:
 * org.apache.shiro.UnavailableSecurityManagerException: No SecurityManager accessible to the calling code, either
 * bound to the org.apache.shiro.util.ThreadContext or as a vm static singleton. This is an invalid application
 * configuration.
 * </pre>
 *
 * But this WARN happens in DefaultNexusConfig, while the userId is tried to be fetched for logging purposes only (...
 * config is changed by XXX), and is not directly related to this test. Other solution would be to add security to this
 * test below, but then a lot of extra cruft would be needed to manage (start and stop) SecurityManager and
 * CacheManager, cleanup.... which really would not belong to this test. As the config logging change (and on who's
 * behalf
 * it happens) is really not the concern of this test, it's left to rather spam the logs.
 *
 * @author cstamas
 */
public class DefaultTaskConfigManagerTest
    extends NexusAppTestSupport
{
  private DefaultScheduler defaultScheduler;

  private DefaultTaskConfigManager defaultManager;

  private NexusConfiguration applicationConfiguration;

  private static final String PROPERTY_KEY_START_DATE = "startDate";

  private static final String PROPERTY_KEY_END_DATE = "endDate";

  private static final String PROPERTY_KEY_CRON_EXPRESSION = "cronExpression";

  private static final String SCHEDULE_TYPE_ONCE = "once";

  private static final String SCHEDULE_TYPE_DAILY = "daily";

  private static final String SCHEDULE_TYPE_WEEKLY = "weekly";

  private static final String SCHEDULE_TYPE_MONTHLY = "monthly";

  private static final String SCHEDULE_TYPE_ADVANCED = "advanced";

  private static final String TASK_NAME = "test";

  private static final String CRON_EXPRESSION = "0 0/5 14,18,3-9,2 ? JAN,MAR,SEP MON-FRI 2002-2010";

  @Override
  protected void customizeModules(final List<Module> modules) {
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(SchedulerTask.class)
            .annotatedWith(Names.named(TestNexusTask.class.getName()))
            .to(TestNexusTask.class);
      }
    });
  }

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    defaultScheduler = (DefaultScheduler) lookup(Scheduler.class.getName());
    defaultManager = (DefaultTaskConfigManager) lookup(TaskConfigManager.class.getName());
    applicationConfiguration = lookup(NexusConfiguration.class);
    applicationConfiguration.loadConfiguration();
  }

  @After
  public void cleanup() {
    // kill the pool
    defaultScheduler.shutdown();
  }

  @Test
  public void testStoreOnceSchedule()
      throws Exception
  {
    Date date = new Date();
    HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
    scheduleProperties.put(PROPERTY_KEY_START_DATE, date);
    genericTestStore(SCHEDULE_TYPE_ONCE, scheduleProperties);
  }

  @Test
  public void testStoreDailySchedule()
      throws Exception
  {
    Date startDate = new Date();
    Date endDate = new Date();
    HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
    scheduleProperties.put(PROPERTY_KEY_START_DATE, startDate);
    scheduleProperties.put(PROPERTY_KEY_END_DATE, endDate);
    genericTestStore(SCHEDULE_TYPE_DAILY, scheduleProperties);
  }

  @Test
  public void testStoreWeeklySchedule()
      throws Exception
  {
    Date startDate = new Date();
    Date endDate = new Date();
    HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
    scheduleProperties.put(PROPERTY_KEY_START_DATE, startDate);
    scheduleProperties.put(PROPERTY_KEY_END_DATE, endDate);
    genericTestStore(SCHEDULE_TYPE_WEEKLY, scheduleProperties);
  }

  @Test
  public void testStoreMonthlySchedule()
      throws Exception
  {
    Date startDate = new Date();
    Date endDate = new Date();
    HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
    scheduleProperties.put(PROPERTY_KEY_START_DATE, startDate);
    scheduleProperties.put(PROPERTY_KEY_END_DATE, endDate);
    genericTestStore(SCHEDULE_TYPE_MONTHLY, scheduleProperties);
  }

  @Test
  public void testStoreAdvancedSchedule()
      throws Exception
  {
    HashMap<String, Object> scheduleProperties = new HashMap<String, Object>();
    scheduleProperties.put(PROPERTY_KEY_CRON_EXPRESSION, CRON_EXPRESSION);
    genericTestStore(SCHEDULE_TYPE_ADVANCED, scheduleProperties);
  }

  @Test
  public void testInitializeCronSchedule()
      throws Exception
  {
    final CScheduledTask cst = new CScheduledTask();
    cst.setId("foo-" + System.currentTimeMillis());
    cst.setEnabled(true);
    cst.setName("foo");
    cst.setType(TestNexusTask.class.getName());
    cst.setNextRun(new SimpleDateFormat("yyyy-MM-DD hh:mm:ss").parse("2099-01-01 20:00:00").getTime());

    // System.out.println( new Date( cst.getNextRun() ) );

    final CScheduleConfig csc = new CScheduleConfig();
    csc.setType(SCHEDULE_TYPE_ADVANCED);
    csc.setCronCommand("0 0 20 ? * TUE,THU,SAT");
    cst.setSchedule(csc);

    defaultManager.initializeTasks(defaultScheduler, Arrays.asList(cst));

    final ScheduledTask<?> task = defaultScheduler.getTaskById(cst.getId());
    assertThat(new Date(task.getNextRun().getTime()), is(new Date(cst.getNextRun())));
  }

  public void genericTestStore(String scheduleType, HashMap<String, Object> scheduleProperties)
      throws ParseException
  {
    ScheduledTask<Integer> task = null;
    try {
      task = createScheduledTask(createSchedule(scheduleType, scheduleProperties));

      defaultManager.addTask(task);

      // loadConfig();

      assertThat(getTaskConfiguration().size(), equalTo(1));
      MatcherAssert.assertThat(TaskState.valueOf(((CScheduledTask) getTaskConfiguration().get(0)).getStatus()),
          equalTo(TaskState.SUBMITTED));
      assertThat(((CScheduledTask) getTaskConfiguration().get(0)).getName(), equalTo(TASK_NAME));

      defaultManager.removeTask(task);

      // loadConfig();
      // assertTrue( getTaskConfiguration().size() == 0 );
    }
    finally {
      if (task != null) {
        task.cancel();
        defaultManager.removeTask(task);
      }
    }
  }

  private Schedule createSchedule(String type, HashMap<String, Object> properties)
      throws ParseException
  {
    if (SCHEDULE_TYPE_ONCE.equals(type)) {
      return new OnceSchedule((Date) properties.get(PROPERTY_KEY_START_DATE));
    }
    else if (SCHEDULE_TYPE_DAILY.equals(type)) {
      return new DailySchedule((Date) properties.get(PROPERTY_KEY_START_DATE),
          (Date) properties.get(PROPERTY_KEY_END_DATE));
    }
    else if (SCHEDULE_TYPE_WEEKLY.equals(type)) {
      Set<Integer> daysToRun = new HashSet<Integer>();
      daysToRun.add(new Integer(1));
      return new WeeklySchedule((Date) properties.get(PROPERTY_KEY_START_DATE),
          (Date) properties.get(PROPERTY_KEY_END_DATE), daysToRun);
    }
    else if (SCHEDULE_TYPE_MONTHLY.equals(type)) {
      Set<Integer> daysToRun = new HashSet<Integer>();
      daysToRun.add(new Integer(1));
      return new MonthlySchedule((Date) properties.get(PROPERTY_KEY_START_DATE),
          (Date) properties.get(PROPERTY_KEY_END_DATE), daysToRun);
    }
    else if (SCHEDULE_TYPE_ADVANCED.equals(type)) {
      return new CronSchedule((String) properties.get(PROPERTY_KEY_CRON_EXPRESSION));
    }

    return null;
  }

  private ScheduledTask<Integer> createScheduledTask(Schedule schedule) {
    TestCallable callable = new TestCallable();
    return new DefaultScheduledTask<Integer>("1", TASK_NAME, callable.getClass().getSimpleName(),
        // TODO this is only use for testing, but we are expecting that the TaskHint matches the Classname.
        defaultScheduler, callable, schedule);
  }

  private List<CScheduledTask> getTaskConfiguration() {
    return applicationConfiguration.getConfigurationModel().getTasks();
  }

  public class TestCallable
      implements Callable<Integer>
  {
    private int runCount = 0;

    public Integer call()
        throws Exception
    {
      return runCount++;
    }

    public int getRunCount() {
      return runCount;
    }
  }

}
