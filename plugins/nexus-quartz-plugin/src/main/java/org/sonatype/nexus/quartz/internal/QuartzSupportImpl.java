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
package org.sonatype.nexus.quartz.internal;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.quartz.QuartzSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.eclipse.sisu.BeanEntry;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.DefaultThreadExecutor;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.spi.JobFactory;
import org.quartz.spi.ThreadExecutor;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.utils.DBConnectionManager;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link QuartzSupport}.
 *
 * @since 3.0
 */
@Singleton
@Named
public class QuartzSupportImpl
    extends LifecycleSupport
    implements QuartzSupport, JobFactory
{
  private static final String QUARTZ_POOL_SIZE_KEY = QuartzConstants.CONFIG_PREFIX + ".poolSize";

  private static final int QUARTZ_POOL_SIZE_DEFAULT = 20;

  /**
   * Used by {@link H2ConnectionProvider} too to size pool.
   */
  static final String QUARTZ_POOL_SIZE =
      "${" + QUARTZ_POOL_SIZE_KEY + ":-" + QUARTZ_POOL_SIZE_DEFAULT + "}";

  private final String SCHEDULER_NAME = "NX-Quartz-Scheduler";

  private final int threadPoolSize;

  private final H2ConnectionProvider h2ConnectionProvider;

  private final QuartzDatabaseMigrator migrator;

  private final Iterable<BeanEntry<Named, Job>> jobEntries;

  private final List<QuartzCustomizer> quartzCustomizers;

  private Scheduler scheduler;

  private boolean active;

  @Inject
  public QuartzSupportImpl(final @Named(QUARTZ_POOL_SIZE) int threadPoolSize,
                           final H2ConnectionProvider h2ConnectionProvider,
                           final QuartzDatabaseMigrator migrator,
                           final Iterable<BeanEntry<Named, Job>> jobEntries,
                           final List<QuartzCustomizer> quartzCustomizers)
      throws Exception
  {
    checkArgument(threadPoolSize > 0, "Invalid thread pool size: %s", threadPoolSize);
    this.threadPoolSize = threadPoolSize;
    this.h2ConnectionProvider = checkNotNull(h2ConnectionProvider);
    this.migrator = checkNotNull(migrator);
    this.jobEntries = checkNotNull(jobEntries);
    this.quartzCustomizers = checkNotNull(quartzCustomizers);
    this.active = true;
  }

  /**
   * Making method public to be able to hack some tests in Pro.
   */
  @VisibleForTesting
  @Override
  public boolean isStarted() {
    return super.isStarted();
  }

  @PostConstruct
  public void postConstruct() {
    try {
      start();
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @PreDestroy
  public void preDestroy() {
    try {
      stop();
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected void doStart() throws Exception {
    // hack: Quartz and c3p0 are fragments hosted in this plugin's bundle
    // Still, as it spawns threads, we need this
    // this thread executor executes two important thread: quartz misfire handler and quartz's default thread
    final ThreadExecutor threadExecutor = new DefaultThreadExecutor()
    {
      @Override
      public void execute(final Thread thread) {
        thread.setContextClassLoader(QuartzSupport.class.getClassLoader());
        super.execute(thread);
      }
    };

    // create JDBC JobStore
    final JobStoreTX jobStoreTX = new JobStoreTX();
    jobStoreTX.setDriverDelegateClass(StdJDBCDelegate.class.getName());
    jobStoreTX.setUseProperties(Boolean.FALSE.toString());
    jobStoreTX.setDataSource(QuartzConstants.STORE_NAME);
    jobStoreTX.setTablePrefix("QRTZ_");
    jobStoreTX.setIsClustered(false);
    jobStoreTX.setUseProperties(Boolean.TRUE.toString());
    jobStoreTX.setThreadExecutor(threadExecutor);
    jobStoreTX.setAcquireTriggersWithinLock(true);

    // start the pool
    h2ConnectionProvider.initialize();

    // create schema if needed
    mayCreateDBSchema(h2ConnectionProvider.getConnection());

    // register ConnectionProvider
    DBConnectionManager.getInstance().addConnectionProvider(QuartzConstants.STORE_NAME, h2ConnectionProvider);

    // create Scheduler (implicitly registers it with repository)
    DirectSchedulerFactory.getInstance().createScheduler(
        SCHEDULER_NAME,
        UUID.randomUUID().toString(),
        new QuartzThreadPool(threadPoolSize),
        threadExecutor,
        jobStoreTX,
        null,
        null,
        0,
        -1,
        -1,
        false,
        null,
        1,
        0L
    );
    scheduler = DirectSchedulerFactory.getInstance().getScheduler(SCHEDULER_NAME);
    scheduler.setJobFactory(this);

    // invoke customisers
    for (QuartzCustomizer quartzCustomizer : quartzCustomizers) {
      quartzCustomizer.onCreated(this, scheduler);
    }
    log.info("Quartz Scheduler created.");
    setActive(active);
  }

  private void mayCreateDBSchema(final Connection connection) throws Exception {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      migrator.migrateAll(connection, QuartzDatabaseMigrations.MIGRATIONS);
    }
    finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  @Override
  protected void doStop() throws Exception {
    // invoke customisers
    for (QuartzCustomizer quartzCustomizer : quartzCustomizers) {
      quartzCustomizer.onDestroyed(this, scheduler);
    }
    scheduler.shutdown();
    scheduler = null;
    // unregister it from repository
    SchedulerRepository.getInstance().remove(SCHEDULER_NAME);
    h2ConnectionProvider.shutdown();
    log.info("Quartz Scheduler stopped.");
  }

  /**
   * Returns {@code true} if scheduler is started and is ready (Quartz is started, is not in "stand-by" mode not shut
   * down)..
   */
  public boolean isActive() {
    try {
      // Quartz peculiarity: isStarted is TRUE if method was invoked at all (even if it's followed by stand-by or shut down)
      return scheduler != null && scheduler.isStarted() && !scheduler.isInStandbyMode() && !scheduler.isShutdown();
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Sets the {@code active} flag. Quartz, if created, will be managed also (put in stand-by or started), depending on
   * the value of the {@code active} flag.
   */
  public void setActive(final boolean active) throws Exception {
    this.active = active;
    if (scheduler != null) {
      if (!active && scheduler.isStarted()) {
        // invoke customisers
        for (QuartzCustomizer quartzCustomizer : quartzCustomizers) {
          quartzCustomizer.onStandBy(this, scheduler);
        }
        scheduler.standby();
        log.info("Scheduler put into stand-by mode");
      }
      else if (active && scheduler.isInStandbyMode()) {
        // invoke customisers
        for (QuartzCustomizer quartzCustomizer : quartzCustomizers) {
          quartzCustomizer.onReady(this, scheduler);
        }
        scheduler.start();
        log.info("Scheduler put into ready mode");
      }
    }
  }

  /**
   * Returns the actual size of the pool being used by Quartz scheduler.
   */
  public int getThreadPoolSize() {
    return threadPoolSize;
  }

  // JobFactory

  /**
   * Job factory for Quartz: Jobs are looked up from SISU.
   */
  @Override
  public Job newJob(final TriggerFiredBundle bundle, final Scheduler scheduler) throws SchedulerException {
    final BeanEntry<Named, Job> jobEntry = locate(bundle.getJobDetail().getJobClass());
    if (jobEntry != null) {
      return jobEntry.getProvider().get(); // to support not-singletons
    }
    throw new SchedulerException("Cannot create new instance of Job: " + bundle.getJobDetail().getJobClass().getName());
  }

  /**
   * Locates SISU BeanEntry based on job class.
   */
  private BeanEntry<Named, Job> locate(final Class<? extends Job> jobClass) {
    for (BeanEntry<Named, Job> jobEntry : jobEntries) {
      if (jobEntry.getImplementationClass().equals(jobClass)) {
        return jobEntry;
      }
    }
    return null;
  }


  // Public API

  @Override
  public Scheduler getScheduler() {
    return scheduler;
  }

  @Override

  public <T extends Job> JobKey execute(final Class<T> clazz) {
    return scheduleJob(clazz, TriggerBuilder.newTrigger().startNow().build());
  }

  @Override
  public <T extends Job> JobKey scheduleJob(final Class<T> clazz, final Trigger trigger) {
    final BeanEntry<Named, Job> jobEntry = locate(clazz);
    final JobDetail jobDetail = JobBuilder.newJob().ofType(clazz).withDescription(
        jobEntry.getDescription()).build();
    try {
      scheduler.scheduleJob(jobDetail, trigger);
      return jobDetail.getKey();
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public boolean isRunning(final JobKey jobKey) {
    checkNotNull(jobKey);
    try {
      final List<JobExecutionContext> currentlyRunning = scheduler.getCurrentlyExecutingJobs();
      for (JobExecutionContext context : currentlyRunning) {
        if (context.getJobDetail().getKey().equals(jobKey)) {
          return true;
        }
      }
      return false;
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public boolean cancelJob(final JobKey jobKey) {
    checkNotNull(jobKey);
    try {
      return scheduler.interrupt(jobKey);
    }
    catch (UnableToInterruptJobException e) {
      log.debug("Unable to interrupt job with key {}", jobKey, e);
    }
    return false;
  }

  @Override
  public boolean removeJob(final JobKey jobKey) {
    cancelJob(jobKey);
    try {
      log.debug("Removing job with key {}", jobKey);
      return scheduler.unscheduleJob(new TriggerKey(jobKey.getName(), jobKey.getGroup()));
    }
    catch (SchedulerException e) {
      throw Throwables.propagate(e);
    }
  }
}
