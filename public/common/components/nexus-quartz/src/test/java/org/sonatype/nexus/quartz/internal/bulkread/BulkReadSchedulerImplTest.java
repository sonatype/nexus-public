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
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.IntStream;

import org.sonatype.nexus.quartz.internal.datastore.QuartzDAO;
import org.sonatype.nexus.quartz.internal.datastore.QuartzJobDataTypeHandler;
import org.sonatype.nexus.quartz.internal.store.ConfigStoreConnectionProvider;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestReporter;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Unit tests for {@link BulkReadSchedulerImpl}.
 */
class BulkReadSchedulerImplTest
{
  private static final String SCHEDULER_NAME = "nexus-test";

  private static final String TEST_GROUP = "test-group";

  @DataSessionConfiguration(daos = QuartzDAO.class, typeHandlers = QuartzJobDataTypeHandler.class)
  private TestDataSessionSupplier sessionSupplier;

  private BulkReadScheduler bulkReadScheduler;

  private Scheduler underlyingScheduler;

  @BeforeEach
  void setup() throws Exception {
    // Create the Quartz schema
    sessionSupplier.openSession(DEFAULT_DATASTORE_NAME).access(QuartzDAO.class).createSchema();

    // Setup the scheduler with BulkReadScheduler wrapper
    ConfigStoreConnectionProvider connectionProvider = new ConfigStoreConnectionProvider(sessionSupplier);
    DBConnectionManager.getInstance().addConnectionProvider("testDS", connectionProvider);

    JobStoreTX jobStore = new JobStoreTX();
    jobStore.setDataSource("testDS");
    jobStore.setDriverDelegateClass(getDriverDelegateClass());

    SimpleThreadPool threadPool = new SimpleThreadPool(3, Thread.NORM_PRIORITY);
    threadPool.initialize();

    DirectSchedulerFactory.getInstance().createScheduler(SCHEDULER_NAME, "1", threadPool, jobStore);
    underlyingScheduler = DirectSchedulerFactory.getInstance().getScheduler(SCHEDULER_NAME);
    underlyingScheduler.clear();

    // Wrap with BulkReadScheduler
    bulkReadScheduler = new BulkReadSchedulerImpl(underlyingScheduler, connectionProvider, SCHEDULER_NAME);
  }

  @AfterEach
  void afterEach() throws Exception {
    if (underlyingScheduler != null) {
      underlyingScheduler.shutdown();
    }
  }

  @DatabaseTest
  void testGetJobDetailsForGroup_empty() throws Exception {
    Map<JobKey, JobDetail> result = bulkReadScheduler.getJobDetailsForGroup(TEST_GROUP);

    assertThat(result, notNullValue());
    assertThat(result.entrySet(), hasSize(0));
  }

  @DatabaseTest
  void testGetJobDetailsForGroup_singleJob() throws Exception {
    // Create a job
    JobDetail job = JobBuilder.newJob(TestJob.class)
        .withIdentity("job1", TEST_GROUP)
        .withDescription("Test Job 1")
        .storeDurably()
        .build();

    underlyingScheduler.addJob(job, false);

    // Bulk read
    Map<JobKey, JobDetail> result = bulkReadScheduler.getJobDetailsForGroup(TEST_GROUP);

    // Assert with detailed validation
    assertThat(result, notNullValue());
    assertThat(result.entrySet(), hasSize(1));
    assertThat(result, hasKey(jobKey("job1", TEST_GROUP)));
    assertJobDetailEqual(result.get(jobKey("job1", TEST_GROUP)), "job1", TEST_GROUP, "Test Job 1");
  }

  @DatabaseTest
  void testGetJobDetailsForGroup_multipleJobs() throws Exception {
    // Create multiple jobs using helper
    createJobs(5, "job", "Test Job");

    // Bulk read
    Map<JobKey, JobDetail> result = bulkReadScheduler.getJobDetailsForGroup(TEST_GROUP);

    // Assert all jobs with detailed validation
    assertJobDetails(result, 5, "job", "Test Job");
  }

  @DatabaseTest
  void testGetTriggersForGroup_empty() throws Exception {
    Map<TriggerKey, Trigger> result = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);

    assertThat(result, notNullValue());
    assertThat(result.entrySet(), hasSize(0));
  }

  @DatabaseTest
  void testGetTriggersForGroup_cronTrigger() throws Exception {
    // Create a cron trigger using helper
    String cronExpression = "0 0 12 * * ?";
    createCronTriggers(1, "job", "trigger", cronExpression);

    // Bulk read
    Map<TriggerKey, Trigger> result = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);

    // Assert with detailed validation
    assertTriggers(result, 1, 0, "job", null, "trigger", "trigger", cronExpression, 0);
  }

  @DatabaseTest
  void testGetTriggersForGroup_simpleTrigger() throws Exception {
    // Create a simple trigger using helper
    int intervalInSeconds = 60;
    createSimpleTriggers(1, "simple-job", "trigger", intervalInSeconds);

    // Bulk read
    Map<TriggerKey, Trigger> result = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);

    // Assert with detailed validation
    assertTriggers(result, 0, 1, null, "simple-job", "trigger", "trigger", "", intervalInSeconds * 1000L);
  }

  @DatabaseTest
  void testGetTriggersForGroup_multipleTriggers() throws Exception {
    // Create 2 simple triggers and 1 cron trigger using helpers
    String cronExpression = "0 0 12 * * ?";
    int simpleInterval = 60;

    createSimpleTriggers(2, "simple-job", "simple-trigger", simpleInterval);
    createCronTriggers(1, "cron-job", "cron-trigger", cronExpression);

    // Bulk read
    Map<TriggerKey, Trigger> result = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);

    // Assert with detailed validation: 1 cron, 2 simple triggers
    assertTriggers(result, 1, 2, "cron-job", "simple-job", "cron-trigger", "simple-trigger", cronExpression,
        simpleInterval * 1000L);
  }

  @DatabaseTest
  void testBulkReadPerformance(TestReporter testReporter) throws Exception {
    // Create 100 jobs and triggers using helpers
    String cronExpression = "0 0 12 * * ?";
    createCronTriggers(100, "job", "trigger", cronExpression);

    // Bulk read jobs
    long startJobs = System.currentTimeMillis();
    Map<JobKey, JobDetail> jobs = bulkReadScheduler.getJobDetailsForGroup(TEST_GROUP);
    long jobsTime = System.currentTimeMillis() - startJobs;

    // Bulk read triggers
    long startTriggers = System.currentTimeMillis();
    Map<TriggerKey, Trigger> triggers = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);
    long triggersTime = System.currentTimeMillis() - startTriggers;

    assertThat(jobs.entrySet(), hasSize(100));
    assertThat(triggers.entrySet(), hasSize(100));

    testReporter.publishEntry("performance",
        "Bulk read 100 jobs in " + jobsTime + "ms, 100 triggers in " + triggersTime + "ms");
  }

  @DatabaseTest
  void testDelegateMethodsWorkCorrectly() throws Exception {
    // Test that delegate methods still work
    assertThat(bulkReadScheduler.getSchedulerName(), equalTo(SCHEDULER_NAME));
    assertThat(bulkReadScheduler.isStarted(), is(false));

    bulkReadScheduler.start();
    assertThat(bulkReadScheduler.isStarted(), is(true));

    bulkReadScheduler.standby();
    assertThat(bulkReadScheduler.isInStandbyMode(), is(true));
  }

  @DatabaseTest
  void testBulkReadMatchesStandardQuartzAPI_jobs() throws Exception {
    // Create test jobs using standard API
    String cronExpression = "0 0 12 * * ?";
    createCronTriggers(5, "job", "trigger", cronExpression);

    // Get jobs via bulk read
    Map<JobKey, JobDetail> bulkJobs = bulkReadScheduler.getJobDetailsForGroup(TEST_GROUP);

    // Compare each bulk-read job with standard API result
    for (Map.Entry<JobKey, JobDetail> entry : bulkJobs.entrySet()) {
      JobKey key = entry.getKey();
      JobDetail bulkJob = entry.getValue();
      JobDetail standardJob = underlyingScheduler.getJobDetail(key);

      assertJobDetailEqual(standardJob, bulkJob);
    }
  }

  @DatabaseTest
  void testBulkReadMatchesStandardQuartzAPI_cronTriggers() throws Exception {
    // Create test cron triggers using standard API
    String cronExpression = "0 0 12 * * ?";
    createCronTriggers(3, "job", "trigger", cronExpression);

    // Get triggers via bulk read
    Map<TriggerKey, Trigger> bulkTriggers = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);

    // Compare each bulk-read trigger with standard API result
    for (Map.Entry<TriggerKey, Trigger> entry : bulkTriggers.entrySet()) {
      TriggerKey key = entry.getKey();
      Trigger bulkTrigger = entry.getValue();
      Trigger standardTrigger = underlyingScheduler.getTrigger(key);

      assertTriggerEqual(standardTrigger, bulkTrigger);
      assertCronTriggerEqual((CronTrigger) standardTrigger, (CronTrigger) bulkTrigger);
    }
  }

  @DatabaseTest
  void testBulkReadMatchesStandardQuartzAPI_simpleTriggers() throws Exception {
    // Create test simple triggers using standard API
    int intervalInSeconds = 60;
    createSimpleTriggers(3, "job", "trigger", intervalInSeconds);

    // Get triggers via bulk read
    Map<TriggerKey, Trigger> bulkTriggers = bulkReadScheduler.getTriggersForGroup(TEST_GROUP);

    // Compare each bulk-read trigger with standard API result
    for (Map.Entry<TriggerKey, Trigger> entry : bulkTriggers.entrySet()) {
      TriggerKey key = entry.getKey();
      Trigger bulkTrigger = entry.getValue();
      Trigger standardTrigger = underlyingScheduler.getTrigger(key);

      assertTriggerEqual(standardTrigger, bulkTrigger);
      assertSimpleTriggerEqual((SimpleTrigger) standardTrigger, (SimpleTrigger) bulkTrigger);
    }
  }

  @DatabaseTest
  void testJobDataMapDeserialization() throws Exception {
    // Create a job with complex JobDataMap
    JobDataMap originalData = new JobDataMap();
    originalData.put("stringKey", "testValue");
    originalData.put("intKey", 42);
    originalData.put("boolKey", true);
    originalData.put("longKey", 12345L);

    JobDetail job = JobBuilder.newJob(TestJob.class)
        .withIdentity("jobWithData", TEST_GROUP)
        .withDescription("Job with data map")
        .usingJobData(originalData)
        .storeDurably()
        .build();

    underlyingScheduler.addJob(job, false);

    // Bulk read and verify JobDataMap is correctly deserialized
    Map<JobKey, JobDetail> result = bulkReadScheduler.getJobDetailsForGroup(TEST_GROUP);
    JobDetail retrievedJob = result.get(jobKey("jobWithData", TEST_GROUP));

    assertThat(retrievedJob, notNullValue());
    JobDataMap retrievedData = retrievedJob.getJobDataMap();

    // Verify all data is correctly deserialized
    assertThat(retrievedData.getString("stringKey"), equalTo("testValue"));
    assertThat(retrievedData.getInt("intKey"), equalTo(42));
    assertThat(retrievedData.getBoolean("boolKey"), equalTo(true));
    assertThat(retrievedData.getLong("longKey"), equalTo(12345L));
  }

  private String getDriverDelegateClass() throws SQLException {
    try (Connection con = sessionSupplier.openConnection(DEFAULT_DATASTORE_NAME)) {
      String dbName = con.getMetaData().getDatabaseProductName();
      switch (dbName) {
        case "H2":
          return HSQLDBDelegate.class.getName();
        case "PostgreSQL":
          return PostgreSQLDelegate.class.getName();
        default:
          return StdJDBCDelegate.class.getName();
      }
    }
  }

  /**
   * Creates multiple durable jobs with specified count and name prefix.
   */
  private void createJobs(
      final int count,
      final String namePrefix,
      final String description) throws SchedulerException
  {
    IntStream.rangeClosed(1, count).forEach(i -> {
      try {
        JobDetail job = JobBuilder.newJob(TestJob.class)
            .withIdentity(namePrefix + i, TEST_GROUP)
            .withDescription(description + " " + i)
            .storeDurably()
            .build();
        underlyingScheduler.addJob(job, false);
      }
      catch (SchedulerException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Creates multiple cron triggers with their associated jobs.
   */
  private void createCronTriggers(
      final int count,
      final String jobNamePrefix,
      final String triggerNamePrefix,
      final String cronExpression) throws SchedulerException
  {
    IntStream.rangeClosed(1, count).forEach(i -> {
      try {
        JobDetail job = JobBuilder.newJob(TestJob.class)
            .withIdentity(jobNamePrefix + i, TEST_GROUP)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerNamePrefix + i, TEST_GROUP)
            .forJob(job)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build();

        underlyingScheduler.scheduleJob(job, trigger);
      }
      catch (SchedulerException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Creates multiple simple triggers with their associated jobs.
   */
  private void createSimpleTriggers(
      final int count,
      final String jobNamePrefix,
      final String triggerNamePrefix,
      final int intervalInSeconds) throws SchedulerException
  {
    IntStream.rangeClosed(1, count).forEach(i -> {
      try {
        JobDetail job = JobBuilder.newJob(TestJob.class)
            .withIdentity(jobNamePrefix + i, TEST_GROUP)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(triggerNamePrefix + i, TEST_GROUP)
            .forJob(job)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(intervalInSeconds)
                .repeatForever())
            .build();

        underlyingScheduler.scheduleJob(job, trigger);
      }
      catch (SchedulerException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Asserts that a job detail has the expected properties.
   */
  private void assertJobDetailEqual(
      final JobDetail job,
      final String expectedName,
      final String expectedGroup,
      final String expectedDescription)
  {
    assertThat(job, notNullValue());
    assertThat(job.getKey().getName(), equalTo(expectedName));
    assertThat(job.getKey().getGroup(), equalTo(expectedGroup));
    assertThat(job.getDescription(), equalTo(expectedDescription));
    assertThat(job.getJobClass().getName(), equalTo(TestJob.class.getName()));
  }

  /**
   * Asserts that a cron trigger has the expected properties.
   */
  private void assertCronTriggerEqual(
      final Trigger trigger,
      final String expectedName,
      final String expectedGroup,
      final JobKey expectedJobKey,
      final String expectedCronExpression)
  {
    assertThat(trigger, notNullValue());
    assertThat(trigger, is(instanceOf(CronTrigger.class)));
    assertThat(trigger.getKey().getName(), equalTo(expectedName));
    assertThat(trigger.getKey().getGroup(), equalTo(expectedGroup));
    assertThat(trigger.getJobKey(), equalTo(expectedJobKey));

    CronTrigger cronTrigger = (CronTrigger) trigger;
    assertThat(cronTrigger.getCronExpression(), equalTo(expectedCronExpression));
  }

  /**
   * Asserts that a simple trigger has the expected properties.
   */
  private void assertSimpleTriggerEqual(
      final Trigger trigger,
      final String expectedName,
      final String expectedGroup,
      final JobKey expectedJobKey,
      final long expectedIntervalInMillis)
  {
    assertThat(trigger, notNullValue());
    assertThat(trigger, is(instanceOf(SimpleTrigger.class)));
    assertThat(trigger.getKey().getName(), equalTo(expectedName));
    assertThat(trigger.getKey().getGroup(), equalTo(expectedGroup));
    assertThat(trigger.getJobKey(), equalTo(expectedJobKey));

    SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
    assertThat(simpleTrigger.getRepeatInterval(), equalTo(expectedIntervalInMillis));
  }

  /**
   * Counts triggers by type in a map.
   */
  private long countTriggersByType(final Map<TriggerKey, Trigger> triggers, final Class<?> triggerType) {
    return triggers.values()
        .stream()
        .filter(triggerType::isInstance)
        .count();
  }

  /**
   * Asserts all job details in the map match expected properties.
   */
  private void assertJobDetails(
      final Map<JobKey, JobDetail> jobs,
      final int expectedCount,
      final String namePrefix,
      final String descriptionPrefix)
  {
    assertThat(jobs, notNullValue());
    assertThat(jobs.entrySet(), hasSize(expectedCount));

    IntStream.rangeClosed(1, expectedCount).forEach(i -> {
      JobKey key = jobKey(namePrefix + i, TEST_GROUP);
      assertThat(jobs, hasKey(key));
      assertJobDetailEqual(jobs.get(key), namePrefix + i, TEST_GROUP, descriptionPrefix + " " + i);
    });
  }

  /**
   * Asserts all triggers in the map, validating counts by type and properties.
   */
  private void assertTriggers(
      final Map<TriggerKey, Trigger> triggers,
      final int expectedCronCount,
      final int expectedSimpleCount,
      final String cronJobPrefix,
      final String simpleJobPrefix,
      final String cronTriggerPrefix,
      final String simpleTriggerPrefix,
      final String cronExpression,
      final long simpleIntervalMillis)
  {
    int totalExpected = expectedCronCount + expectedSimpleCount;
    assertThat(triggers, notNullValue());
    assertThat(triggers.entrySet(), hasSize(totalExpected));

    // Assert trigger counts by type
    assertThat(countTriggersByType(triggers, CronTrigger.class), equalTo((long) expectedCronCount));
    assertThat(countTriggersByType(triggers, SimpleTrigger.class), equalTo((long) expectedSimpleCount));

    // Assert cron triggers
    IntStream.rangeClosed(1, expectedCronCount).forEach(i -> {
      TriggerKey key = triggerKey(cronTriggerPrefix + i, TEST_GROUP);
      assertThat(triggers, hasKey(key));
      assertCronTriggerEqual(
          triggers.get(key),
          cronTriggerPrefix + i,
          TEST_GROUP,
          jobKey(cronJobPrefix + i, TEST_GROUP),
          cronExpression);
    });

    // Assert simple triggers
    IntStream.rangeClosed(1, expectedSimpleCount).forEach(i -> {
      TriggerKey key = triggerKey(simpleTriggerPrefix + i, TEST_GROUP);
      assertThat(triggers, hasKey(key));
      assertSimpleTriggerEqual(
          triggers.get(key),
          simpleTriggerPrefix + i,
          TEST_GROUP,
          jobKey(simpleJobPrefix + i, TEST_GROUP),
          simpleIntervalMillis);
    });
  }

  // ===== Comparison Assertion Methods (Standard API vs Bulk Read) =====

  /**
   * Asserts that a bulk-read JobDetail matches the standard Quartz API JobDetail.
   */
  private void assertJobDetailEqual(final JobDetail expected, final JobDetail actual) {
    assertThat("Job key name mismatch", actual.getKey().getName(), equalTo(expected.getKey().getName()));
    assertThat("Job key group mismatch", actual.getKey().getGroup(), equalTo(expected.getKey().getGroup()));
    assertThat("Job description mismatch", actual.getDescription(), equalTo(expected.getDescription()));
    assertThat("Job class mismatch", actual.getJobClass(), equalTo(expected.getJobClass()));
    assertThat("Job durability mismatch", actual.isDurable(), equalTo(expected.isDurable()));
    assertThat("Job requests recovery mismatch", actual.requestsRecovery(), equalTo(expected.requestsRecovery()));
    assertThat("Job data map mismatch", actual.getJobDataMap(), equalTo(expected.getJobDataMap()));
  }

  /**
   * Asserts that a bulk-read Trigger matches the standard Quartz API Trigger (base properties).
   */
  private void assertTriggerEqual(final Trigger expected, final Trigger actual) {
    assertThat("Trigger key name mismatch", actual.getKey().getName(), equalTo(expected.getKey().getName()));
    assertThat("Trigger key group mismatch", actual.getKey().getGroup(), equalTo(expected.getKey().getGroup()));
    assertThat("Trigger job key mismatch", actual.getJobKey(), equalTo(expected.getJobKey()));
    assertThat("Trigger description mismatch", actual.getDescription(), equalTo(expected.getDescription()));
    assertThat("Trigger priority mismatch", actual.getPriority(), equalTo(expected.getPriority()));
    assertThat("Trigger start time mismatch", actual.getStartTime(), equalTo(expected.getStartTime()));
    assertThat("Trigger end time mismatch", actual.getEndTime(), equalTo(expected.getEndTime()));
    assertThat("Trigger calendar name mismatch", actual.getCalendarName(), equalTo(expected.getCalendarName()));
    assertThat("Trigger misfire instruction mismatch", actual.getMisfireInstruction(),
        equalTo(expected.getMisfireInstruction()));
    assertThat("Trigger job data map mismatch", actual.getJobDataMap(), equalTo(expected.getJobDataMap()));
  }

  /**
   * Asserts that a bulk-read CronTrigger matches the standard Quartz API CronTrigger (cron-specific properties).
   */
  private void assertCronTriggerEqual(final CronTrigger expected, final CronTrigger actual) {
    assertThat("Cron expression mismatch", actual.getCronExpression(), equalTo(expected.getCronExpression()));
    assertThat("Cron timezone mismatch", actual.getTimeZone(), equalTo(expected.getTimeZone()));
  }

  /**
   * Asserts that a bulk-read SimpleTrigger matches the standard Quartz API SimpleTrigger (simple-specific
   * properties).
   */
  private void assertSimpleTriggerEqual(final SimpleTrigger expected, final SimpleTrigger actual) {
    assertThat("Simple trigger repeat count mismatch", actual.getRepeatCount(), equalTo(expected.getRepeatCount()));
    assertThat("Simple trigger repeat interval mismatch", actual.getRepeatInterval(),
        equalTo(expected.getRepeatInterval()));
    assertThat("Simple trigger times triggered mismatch", actual.getTimesTriggered(),
        equalTo(expected.getTimesTriggered()));
  }

  /**
   * Simple test job for testing purposes.
   */
  static class TestJob
      implements Job
  {
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
      // No-op for testing
    }
  }
}
