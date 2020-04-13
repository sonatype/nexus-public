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
package org.sonatype.nexus.quartz.orient;

import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.jdbc.OrientJdbcConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.matchers.NameMatcher;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class OrientQuartzJdbcIT {

  private static final String SIMPLE_JOB = "SimpleJob";

  @ClassRule
  public static DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("quartz-test");

  private final Logger log = LoggerFactory.getLogger(getClass());

  private Scheduler scheduler;

  private String dbUrl;

  @Before
  public void before() throws Exception {
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      OrientQuartzSchema.register(db);
      dbUrl = db.getURL();
    }
    SimpleThreadPool threadPool = new SimpleThreadPool(3, Thread.NORM_PRIORITY);
    threadPool.initialize();
    OrientConnectionProvider connProvider = new OrientConnectionProvider();
    connProvider.setConnectionString(dbUrl);
    connProvider.setUser("admin");
    connProvider.setPassword("admin");
    connProvider.setUsePool(true);
    connProvider.setPoolMin(3);
    connProvider.setPoolMax(3);
    connProvider.initialize();
    DBConnectionManager.getInstance().addConnectionProvider("orientDS", connProvider);
    DBConnectionManager.getInstance().getConnection("orientDS");
    JobStoreTX jobStore = new JobStoreTX();
    jobStore.setDataSource("orientDS");
    jobStore.setDriverDelegateClass(OrientDelegate.class.getName());
    DirectSchedulerFactory.getInstance().createScheduler("nexus", "1", threadPool, jobStore);
    scheduler = DirectSchedulerFactory.getInstance().getScheduler("nexus");
    scheduler.clear();

    scheduler.start();
  }

  @After
  public void after() throws Exception {
    scheduler.shutdown();
  }

  @SuppressWarnings("java:S2699") // sonar doesn't detect awaitility assertions https://jira.sonarsource.com/browse/SONARJAVA-3334
  @Test
  public void test() throws Exception {
    MyJobListener listener = new MyJobListener("foobar");

    JobDetail jobDetail = newJob(SimpleJob.class)
        .withIdentity(SIMPLE_JOB, Scheduler.DEFAULT_GROUP)
        .usingJobData("foo", "bar")
        .build();

    Date startTime = DateBuilder.futureDate(3, IntervalUnit.SECOND);
    Trigger trigger = newTrigger()
        .withIdentity("SimpleSimpleTrigger", Scheduler.DEFAULT_GROUP)
        .startAt(startTime)
        .build();

    scheduler.getListenerManager().addJobListener(listener, NameMatcher.jobNameEquals(SIMPLE_JOB));
    scheduler.scheduleJob(jobDetail, trigger);

    await().atMost(1, TimeUnit.SECONDS).until(this::getJobDetail, notNullValue());

    await().atMost(1, TimeUnit.SECONDS).until(this::getFooJobData, equalTo("bar"));

    await().atMost(1, TimeUnit.SECONDS).until(this::getTriggersOfJob, not(empty()));

    await().atMost(4, TimeUnit.SECONDS).until(listener::isDone, equalTo(true));

    await().atMost(1, TimeUnit.SECONDS).until(this::getTriggersOfJob, empty());
  }

  @Test
  public void testSelectJobForTriggers() throws Exception {
    SimpleClassLoadHelper classLoaderHelper = new SimpleClassLoadHelper();
    JobDetail jobDetail = newJob(SimpleJob.class)
        .withIdentity(SIMPLE_JOB, Scheduler.DEFAULT_GROUP)
        .usingJobData("moo", "baz")
        .build();

    Date startTime = DateBuilder.futureDate(3, IntervalUnit.DAY);
    Trigger trigger = newTrigger()
        .withIdentity("SimpleSimpleTrigger", Scheduler.DEFAULT_GROUP)
        .startAt(startTime)
        .build();

    scheduler.scheduleJob(jobDetail, trigger);

    await().atMost(1, TimeUnit.SECONDS).until(this::getTriggersOfJob, not(empty()));

    String jdbcUrl = "jdbc:orient:" + dbUrl;
    Properties info = new Properties();
    info.put("user", "admin");
    info.put("password", "admin");
    info.put("db.usePool", false);
    OrientDelegate delegate = new OrientDelegate();
    delegate.initialize(log, "QRTZ_", "nexus", "1", classLoaderHelper, false, "");

    try (Connection conn = new OrientJdbcConnection(jdbcUrl, info)) {
      JobDetail jobForTrigger = delegate.selectJobForTrigger(conn, classLoaderHelper, trigger.getKey(), true);
      assertThat(jobForTrigger, notNullValue());
      assertThat(jobForTrigger.getKey(), equalTo(jobDetail.getKey()));
      assertThat(jobForTrigger.getJobClass(), equalTo(jobDetail.getJobClass()));
      assertThat(jobForTrigger.requestsRecovery(), equalTo(jobDetail.requestsRecovery()));
    }

    scheduler.deleteJob(JobKey.jobKey(SIMPLE_JOB, Scheduler.DEFAULT_GROUP));

    try (Connection conn = new OrientJdbcConnection(jdbcUrl, info)) {
      JobDetail jobForTrigger = delegate.selectJobForTrigger(conn, classLoaderHelper, trigger.getKey(), true);
      assertThat(jobForTrigger, nullValue());
    }
  }

  private JobDetail getJobDetail() throws SchedulerException {
    JobDetail job = scheduler.getJobDetail(JobKey.jobKey(SIMPLE_JOB));
    log.info("JobDetail for job name: {}, {}", SIMPLE_JOB, job);
    return job;
  }

  private String getFooJobData() throws SchedulerException {
    JobDetail job = scheduler.getJobDetail(JobKey.jobKey(SIMPLE_JOB));
    return job.getJobDataMap().getString("foo");
  }

  private List<? extends Trigger> getTriggersOfJob() throws SchedulerException {
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(JobKey.jobKey(SIMPLE_JOB));
    log.info("Triggers for job name: {}, {}", SIMPLE_JOB, triggers);
    return triggers;
  }

  public static class MyJobListener
      extends JobListenerSupport
  {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;

    private boolean done;

    MyJobListener(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    boolean isDone() {
      log.info("Listener: {}, {}", name, done);
      return done;
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
      log.info("Job was executed: {}", context.getJobDetail().getKey());
      done = true;
    }
  }
}
