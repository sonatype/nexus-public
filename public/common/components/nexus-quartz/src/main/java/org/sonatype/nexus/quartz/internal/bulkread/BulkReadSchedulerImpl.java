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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;

/**
 * Implementation of BulkReadScheduler that wraps a standard Quartz Scheduler and provides optimized bulk read methods
 * using delegation.
 * <p>
 * Instead of 1 + 3N queries, this uses exactly 4 queries regardless of job count.
 */
public class BulkReadSchedulerImpl
    implements BulkReadScheduler
{
  private static final Logger log = LoggerFactory.getLogger(BulkReadSchedulerImpl.class);

  private static final String SQL_SELECT_JOB_DETAILS =
      "SELECT SCHED_NAME, JOB_NAME, JOB_GROUP, DESCRIPTION, JOB_CLASS_NAME, IS_DURABLE, REQUESTS_RECOVERY, JOB_DATA " +
          "FROM QRTZ_JOB_DETAILS WHERE SCHED_NAME = ? AND JOB_GROUP = ?";

  private static final String SQL_SELECT_TRIGGERS =
      "SELECT TRIGGER_NAME, TRIGGER_GROUP, JOB_NAME, JOB_GROUP, DESCRIPTION, NEXT_FIRE_TIME, PREV_FIRE_TIME, " +
          "START_TIME, END_TIME, PRIORITY, CALENDAR_NAME, MISFIRE_INSTR, JOB_DATA, TRIGGER_TYPE " +
          "FROM QRTZ_TRIGGERS WHERE SCHED_NAME = ? AND TRIGGER_GROUP = ?";

  private static final String SQL_SELECT_CRON_TRIGGERS =
      "SELECT TRIGGER_NAME, TRIGGER_GROUP, CRON_EXPRESSION, TIME_ZONE_ID " +
          "FROM QRTZ_CRON_TRIGGERS WHERE SCHED_NAME = ? AND TRIGGER_GROUP = ?";

  private static final String SQL_SELECT_SIMPLE_TRIGGERS =
      "SELECT TRIGGER_NAME, TRIGGER_GROUP, REPEAT_COUNT, REPEAT_INTERVAL, TIMES_TRIGGERED " +
          "FROM QRTZ_SIMPLE_TRIGGERS WHERE SCHED_NAME = ? AND TRIGGER_GROUP = ?";

  private final Scheduler delegate;

  private final ConnectionProvider connectionProvider;

  private final String schedulerName;

  public BulkReadSchedulerImpl(
      final Scheduler delegate,
      final ConnectionProvider connectionProvider,
      final String schedulerName)
  {
    this.delegate = requireNonNull(delegate);
    this.connectionProvider = requireNonNull(connectionProvider);
    this.schedulerName = requireNonNull(schedulerName);
  }

  @Override
  public Map<JobKey, JobDetail> getJobDetailsForGroup(final String groupName) throws SchedulerException {
    return executeWithConnection(conn -> {
      Map<JobKey, JobDetail> jobDetailsMap = fetchAllJobDetails(conn, groupName);
      log.debug("Fetched {} job details for group '{}'", jobDetailsMap.size(), groupName);
      return jobDetailsMap;
    });
  }

  @Override
  public Map<TriggerKey, Trigger> getTriggersForGroup(final String groupName) throws SchedulerException {
    return executeWithConnection(conn -> {
      Map<TriggerKey, Trigger> result = new HashMap<>();

      // Step 1: Fetch all triggers for the group (1 query)
      Map<TriggerKey, TriggerData> triggersMap = fetchAllTriggers(conn, groupName);

      // Step 2: Fetch all cron trigger details (1 query)
      Map<TriggerKey, CronTriggerData> cronTriggers = fetchAllCronTriggers(conn, groupName);

      // Step 3: Fetch all simple trigger details (1 query)
      Map<TriggerKey, SimpleTriggerData> simpleTriggers = fetchAllSimpleTriggers(conn, groupName);

      // Step 4: Build trigger objects
      for (Map.Entry<TriggerKey, TriggerData> entry : triggersMap.entrySet()) {
        TriggerKey triggerKey = entry.getKey();
        TriggerData triggerData = entry.getValue();

        try {
          // Build the trigger object based on its type
          OperableTrigger trigger = buildTrigger(triggerData, cronTriggers.get(triggerKey),
              simpleTriggers.get(triggerKey));
          result.put(triggerKey, trigger);
        }
        catch (SQLException e) {
          log.error("Failed to build trigger for key: {}", triggerKey, e);
        }
      }

      log.debug("Fetched {} triggers for group '{}'", result.size(), groupName);
      return result;
    });
  }

  @Override
  public Scheduler getDelegate() {
    return delegate;
  }

  /**
   * Fetches all job details for a group in a single query.
   */
  private Map<JobKey, JobDetail> fetchAllJobDetails(final Connection conn, final String groupName) throws SQLException {
    Map<JobKey, JobDetail> jobDetails = new HashMap<>();

    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_JOB_DETAILS)) {
      ps.setString(1, schedulerName);
      ps.setString(2, groupName);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          JobDetailData data = JobDetailData.fromResultSet(rs);
          JobDetail jobDetail = QuartzObjectBuilder.buildJobDetail(data);
          jobDetails.put(data.getKey(), jobDetail);
        }
      }
    }
    return jobDetails;
  }

  /**
   * Fetches all triggers for a group in a single query.
   */
  private Map<TriggerKey, TriggerData> fetchAllTriggers(
      final Connection conn,
      final String groupName) throws SQLException
  {
    Map<TriggerKey, TriggerData> triggers = new HashMap<>();

    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TRIGGERS)) {
      ps.setString(1, schedulerName);
      ps.setString(2, groupName);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          TriggerData data = TriggerData.fromResultSet(rs);
          triggers.put(data.getKey(), data);
        }
      }
    }

    return triggers;
  }

  /**
   * Fetches all cron trigger details for a group in a single query.
   */
  private Map<TriggerKey, CronTriggerData> fetchAllCronTriggers(
      final Connection conn,
      final String groupName) throws SQLException
  {
    Map<TriggerKey, CronTriggerData> cronTriggers = new HashMap<>();

    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_CRON_TRIGGERS)) {
      ps.setString(1, schedulerName);
      ps.setString(2, groupName);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          CronTriggerData data = CronTriggerData.fromResultSet(rs);
          cronTriggers.put(data.getKey(), data);
        }
      }
    }

    log.debug("Fetched {} cron triggers for group '{}'", cronTriggers.size(), groupName);
    return cronTriggers;
  }

  /**
   * Fetches all simple trigger details for a group in a single query.
   */
  private Map<TriggerKey, SimpleTriggerData> fetchAllSimpleTriggers(
      final Connection conn,
      final String groupName) throws SQLException
  {
    Map<TriggerKey, SimpleTriggerData> simpleTriggers = new HashMap<>();

    try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SIMPLE_TRIGGERS)) {
      ps.setString(1, schedulerName);
      ps.setString(2, groupName);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          SimpleTriggerData data = SimpleTriggerData.fromResultSet(rs);
          simpleTriggers.put(data.getKey(), data);
        }
      }
    }

    log.debug("Fetched {} simple triggers for group '{}'", simpleTriggers.size(), groupName);
    return simpleTriggers;
  }

  /**
   * Builds a trigger object from the fetched data.
   */
  private OperableTrigger buildTrigger(
      final TriggerData triggerData,
      final CronTriggerData cronData,
      final SimpleTriggerData simpleData) throws SQLException
  {
    if ("CRON".equals(triggerData.triggerType()) && cronData != null) {
      return QuartzObjectBuilder.buildCronTrigger(triggerData, cronData);
    }
    else if ("SIMPLE".equals(triggerData.triggerType()) && simpleData != null) {
      return QuartzObjectBuilder.buildSimpleTrigger(triggerData, simpleData);
    }
    else {
      return QuartzObjectBuilder.buildBasicTrigger(triggerData);
    }
  }

  /**
   * Executes a database operation with a connection from the connection provider. Handles connection management,
   * ensures all queries execute in a single transaction by disabling auto-commit, and maps SQLException to
   * SchedulerException.
   * <p>
   * The auto-commit state is restored after the operation completes, regardless of success or failure.
   */
  private <T> T executeWithConnection(final ConnectionCallback<T> callback) throws SchedulerException {
    try (Connection conn = connectionProvider.getConnection()) {
      boolean original = disableAutoCommit(conn);
      try {
        return callback.execute(conn);
      }
      catch (SQLException e) {
        throw new SchedulerException("Database operation failed", e);
      }
      finally {
        restoreAutoCommit(conn, original);
      }
    }
    catch (SQLException e) {
      throw new SchedulerException("Database operation failed", e);
    }
  }

  /**
   * Disables auto-commit on the connection and returns the original auto-commit state.
   */
  private boolean disableAutoCommit(final Connection conn) throws SQLException {
    boolean original = conn.getAutoCommit();
    conn.setAutoCommit(false);
    return original;
  }

  /**
   * Restores the auto-commit state on the connection. Logs an error if restoration fails.
   */
  private void restoreAutoCommit(final Connection conn, final boolean originalState) {
    try {
      conn.setAutoCommit(originalState);
    }
    catch (SQLException e) {
      log.error("Failed to restore auto-commit state to {}", originalState, e);
    }
  }

  /**
   * Functional interface for database operations that need a connection.
   */
  @FunctionalInterface
  private interface ConnectionCallback<T>
  {
    T execute(Connection conn) throws SQLException;
  }

  // ===== Delegate all standard Scheduler methods =====

  @Override
  public String getSchedulerName() throws SchedulerException {
    return delegate.getSchedulerName();
  }

  @Override
  public String getSchedulerInstanceId() throws SchedulerException {
    return delegate.getSchedulerInstanceId();
  }

  @Override
  public SchedulerContext getContext() throws SchedulerException {
    return delegate.getContext();
  }

  @Override
  public void start() throws SchedulerException {
    delegate.start();
  }

  @Override
  public void startDelayed(int seconds) throws SchedulerException {
    delegate.startDelayed(seconds);
  }

  @Override
  public boolean isStarted() throws SchedulerException {
    return delegate.isStarted();
  }

  @Override
  public void standby() throws SchedulerException {
    delegate.standby();
  }

  @Override
  public boolean isInStandbyMode() throws SchedulerException {
    return delegate.isInStandbyMode();
  }

  @Override
  public void shutdown() throws SchedulerException {
    delegate.shutdown();
  }

  @Override
  public void shutdown(boolean waitForJobsToComplete) throws SchedulerException {
    delegate.shutdown(waitForJobsToComplete);
  }

  @Override
  public boolean isShutdown() throws SchedulerException {
    return delegate.isShutdown();
  }

  @Override
  public SchedulerMetaData getMetaData() throws SchedulerException {
    return delegate.getMetaData();
  }

  @Override
  public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
    return delegate.getCurrentlyExecutingJobs();
  }

  @Override
  public void setJobFactory(JobFactory factory) throws SchedulerException {
    delegate.setJobFactory(factory);
  }

  @Override
  public ListenerManager getListenerManager() throws SchedulerException {
    return delegate.getListenerManager();
  }

  @Override
  public Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException {
    return delegate.scheduleJob(jobDetail, trigger);
  }

  @Override
  public Date scheduleJob(Trigger trigger) throws SchedulerException {
    return delegate.scheduleJob(trigger);
  }

  @Override
  public void scheduleJobs(
      Map<JobDetail, Set<? extends Trigger>> triggersAndJobs,
      boolean replace) throws SchedulerException
  {
    delegate.scheduleJobs(triggersAndJobs, replace);
  }

  @Override
  public void scheduleJob(
      JobDetail jobDetail,
      Set<? extends Trigger> triggersForJob,
      boolean replace) throws SchedulerException
  {
    delegate.scheduleJob(jobDetail, triggersForJob, replace);
  }

  @Override
  public boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
    return delegate.unscheduleJob(triggerKey);
  }

  @Override
  public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException {
    return delegate.unscheduleJobs(triggerKeys);
  }

  @Override
  public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
    return delegate.rescheduleJob(triggerKey, newTrigger);
  }

  @Override
  public void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException {
    delegate.addJob(jobDetail, replace);
  }

  @Override
  public void addJob(
      JobDetail jobDetail,
      boolean replace,
      boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException
  {
    delegate.addJob(jobDetail, replace, storeNonDurableWhileAwaitingScheduling);
  }

  @Override
  public boolean deleteJob(JobKey jobKey) throws SchedulerException {
    return delegate.deleteJob(jobKey);
  }

  @Override
  public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
    return delegate.deleteJobs(jobKeys);
  }

  @Override
  public void triggerJob(JobKey jobKey) throws SchedulerException {
    delegate.triggerJob(jobKey);
  }

  @Override
  public void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException {
    delegate.triggerJob(jobKey, data);
  }

  @Override
  public void pauseJob(JobKey jobKey) throws SchedulerException {
    delegate.pauseJob(jobKey);
  }

  @Override
  public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
    delegate.pauseJobs(matcher);
  }

  @Override
  public void pauseTrigger(TriggerKey triggerKey) throws SchedulerException {
    delegate.pauseTrigger(triggerKey);
  }

  @Override
  public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
    delegate.pauseTriggers(matcher);
  }

  @Override
  public void resumeJob(JobKey jobKey) throws SchedulerException {
    delegate.resumeJob(jobKey);
  }

  @Override
  public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
    delegate.resumeJobs(matcher);
  }

  @Override
  public void resumeTrigger(TriggerKey triggerKey) throws SchedulerException {
    delegate.resumeTrigger(triggerKey);
  }

  @Override
  public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
    delegate.resumeTriggers(matcher);
  }

  @Override
  public void pauseAll() throws SchedulerException {
    delegate.pauseAll();
  }

  @Override
  public void resumeAll() throws SchedulerException {
    delegate.resumeAll();
  }

  @Override
  public List<String> getJobGroupNames() throws SchedulerException {
    return delegate.getJobGroupNames();
  }

  @Override
  public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
    return delegate.getJobKeys(matcher);
  }

  @Override
  public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
    return delegate.getTriggersOfJob(jobKey);
  }

  @Override
  public List<String> getTriggerGroupNames() throws SchedulerException {
    return delegate.getTriggerGroupNames();
  }

  @Override
  public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
    return delegate.getTriggerKeys(matcher);
  }

  @Override
  public Set<String> getPausedTriggerGroups() throws SchedulerException {
    return delegate.getPausedTriggerGroups();
  }

  @Override
  public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
    return delegate.getJobDetail(jobKey);
  }

  @Override
  public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
    return delegate.getTrigger(triggerKey);
  }

  @Override
  public TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException {
    return delegate.getTriggerState(triggerKey);
  }

  @Override
  public void resetTriggerFromErrorState(final TriggerKey triggerKey) throws SchedulerException {
    delegate.resetTriggerFromErrorState(triggerKey);
  }

  @Override
  public void addCalendar(
      String calName,
      Calendar calendar,
      boolean replace,
      boolean updateTriggers) throws SchedulerException
  {
    delegate.addCalendar(calName, calendar, replace, updateTriggers);
  }

  @Override
  public boolean deleteCalendar(String calName) throws SchedulerException {
    return delegate.deleteCalendar(calName);
  }

  @Override
  public Calendar getCalendar(String calName) throws SchedulerException {
    return delegate.getCalendar(calName);
  }

  @Override
  public List<String> getCalendarNames() throws SchedulerException {
    return delegate.getCalendarNames();
  }

  @Override
  public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException {
    return delegate.interrupt(jobKey);
  }

  @Override
  public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
    return delegate.interrupt(fireInstanceId);
  }

  @Override
  public boolean checkExists(JobKey jobKey) throws SchedulerException {
    return delegate.checkExists(jobKey);
  }

  @Override
  public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
    return delegate.checkExists(triggerKey);
  }

  @Override
  public void clear() throws SchedulerException {
    delegate.clear();
  }
}
