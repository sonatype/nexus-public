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
package org.sonatype.nexus.quartz.internal.datastore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.quartz.internal.store.ConfigStoreConnectionProvider;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
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
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class QuartzDAOTest
    extends TestSupport
{
  static final String SCHEDULER_NAME = "nexus";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .handle(new QuartzJobDataTypeHandler())
      .access(QuartzDAO.class)
      .access(QuartzTestDAO.class);

  @Test
  public void testSchemaCreationFunctionsAndIsReRunnable() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      QuartzDAO underTest = session.access(QuartzDAO.class);
      underTest.createSchema();

      QuartzTestDAO testDao = session.access(QuartzTestDAO.class);

      assertThat(testDao.tables(), hasSize(11));
      assertThat(testDao.primaryKeys(), hasSize(11));
      assertThat(testDao.foreignKeys(), hasSize(5));
      assertThat(testDao.indexes(), hasSize(testDao.expectedIndexes()));

      // check that schema creation can be re-run
      underTest.createSchema();
    }
  }

  @Test
  public void testQuartzSchedulerWorksWithTheCreatedSchema() throws Exception {
    Scheduler scheduler = createScheduler();

    MyJobListener listener = new MyJobListener();

    JobDetail jobDetail = newJob(SimpleJob.class)
        .withIdentity("SimpleJob", Scheduler.DEFAULT_GROUP)
        .build();

    Date startTime = DateBuilder.futureDate(3, IntervalUnit.SECOND);
    Trigger trigger = newTrigger()
        .withIdentity("SimpleSimpleTrigger", Scheduler.DEFAULT_GROUP)
        .startAt(startTime)
        .build();

    scheduler.getListenerManager().addJobListener(listener, NameMatcher.jobNameEquals("SimpleJob"));
    scheduler.scheduleJob(jobDetail, trigger);

    await().atMost(1, TimeUnit.SECONDS).until(() -> getJobDetail(scheduler, "SimpleJob"), notNullValue());

    await().atMost(1, TimeUnit.SECONDS).until(() -> getTriggersOfJob(scheduler, "SimpleJob"), Matchers.not(empty()));

    await().atMost(4, TimeUnit.SECONDS).until(() -> listener.done, equalTo(true));

    await().atMost(1, TimeUnit.SECONDS).until(() -> getTriggersOfJob(scheduler, "SimpleJob"), empty());

    scheduler.shutdown();
  }

  private String getDatabaseId() throws SQLException {
    try (Connection con = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      return con.getMetaData().getDatabaseProductName();
    }
  }

  private String getDriverDelegateClass() throws SQLException {
    switch(getDatabaseId()) {
      case "H2":
        return "org.quartz.impl.jdbcjobstore.HSQLDBDelegate";
      case "PostgreSQL":
        return "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate";
      default:
        return "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
    }
  }

  private Scheduler createScheduler() throws Exception {
    DBConnectionManager.getInstance().addConnectionProvider(
        "myDS", new ConfigStoreConnectionProvider(sessionRule));
    JobStoreTX jobStore = new JobStoreTX();
    jobStore.setDataSource("myDS");
    jobStore.setDriverDelegateClass(getDriverDelegateClass());
    SimpleThreadPool threadPool = new SimpleThreadPool(3, Thread.NORM_PRIORITY);
    threadPool.initialize();
    DirectSchedulerFactory.getInstance().createScheduler(SCHEDULER_NAME, "1", threadPool, jobStore);
    Scheduler s = DirectSchedulerFactory.getInstance().getScheduler(SCHEDULER_NAME);
    s.clear();
    s.start();
    return s;
  }

  private JobDetail getJobDetail(final Scheduler scheduler, final String jobName) throws SchedulerException {
    JobDetail job = scheduler.getJobDetail(JobKey.jobKey("SimpleJob"));
    logger.info("JobDetail for job name: {}, {}", jobName, job);
    return job;
  }

  private List<? extends Trigger> getTriggersOfJob(final Scheduler scheduler, final String jobName) throws SchedulerException {
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(JobKey.jobKey("SimpleJob"));
    logger.info("Triggers for job name: {}, {}", jobName, triggers);
    return triggers;
  }

  static class MyJobListener
      extends JobListenerSupport
  {
    boolean done;

    @Override
    public void jobWasExecuted(final JobExecutionContext context, final JobExecutionException jobException) {
      done = true;
    }

    @Override
    public String getName() {
      return "My Listener";
    }
  }

  public static class SimpleJob
      implements Job
  {
    static final Logger log = LoggerFactory.getLogger(SimpleJob.class);

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
      log.info("..... Simple Job .....");
    }
  }
}
