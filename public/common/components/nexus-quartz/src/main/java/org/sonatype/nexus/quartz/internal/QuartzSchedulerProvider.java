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

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;

import org.sonatype.nexus.common.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.quartz.internal.bulkread.BulkReadScheduler;
import org.sonatype.nexus.quartz.internal.bulkread.BulkReadSchedulerImpl;

import org.quartz.Scheduler;
import org.quartz.impl.DefaultThreadExecutor;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.SchedulerRepository;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadExecutor;
import org.quartz.utils.ConnectionProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * {@link Scheduler} provider.
 *
 * @since 3.19
 */
@Lazy
@Component
@ManagedLifecycle(phase = SERVICES)
public class QuartzSchedulerProvider
    extends LifecycleSupport
    implements FactoryBean<BulkReadScheduler>
{
  private static final String SCHEDULER_NAME = "nexus";

  private final NodeAccess nodeAccess;

  private final Provider<JobStore> jobStore;

  private final ConnectionProvider connectionProvider;

  private final JobFactory jobFactory;

  private final int threadPoolSize;

  private final int threadPriority;

  private volatile BulkReadScheduler scheduler;

  @Autowired
  public QuartzSchedulerProvider(
      final NodeAccess nodeAccess,
      final Provider<JobStore> jobStore,
      final ConnectionProvider connectionProvider,
      final JobFactory jobFactory,
      @Value("${nexus.quartz.poolSize:20}") final int threadPoolSize,
      @Value("${nexus.quartz.taskThreadPriority:5}") final int threadPriority)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.jobStore = checkNotNull(jobStore);
    this.connectionProvider = checkNotNull(connectionProvider);
    this.jobFactory = checkNotNull(jobFactory);
    checkArgument(threadPoolSize > 0, "Invalid thread-pool size: %s", threadPoolSize);
    this.threadPoolSize = threadPoolSize;
    this.threadPriority = threadPriority;
    log.info("Thread-pool size: {}, Thread-pool priority: {}", threadPoolSize, threadPriority);
  }

  @Override
  protected void doStop() throws Exception {
    if (scheduler != null) {
      scheduler.shutdown();
      scheduler = null;
    }
    SchedulerRepository.getInstance().remove(SCHEDULER_NAME);
  }

  @Lazy
  @Override
  public BulkReadScheduler getObject() throws Exception {
    BulkReadScheduler localRef = scheduler;
    if (localRef == null) {
      synchronized (this) {
        localRef = scheduler;
        if (localRef == null) {
          scheduler = localRef = createScheduler();
        }
      }
    }
    return localRef;
  }

  @Override
  public Class<?> getObjectType() {
    return BulkReadScheduler.class;
  }

  private BulkReadScheduler createScheduler() {
    try {
      // ensure executed threads have TCCL set
      ThreadExecutor threadExecutor = new DefaultThreadExecutor()
      {
        @Override
        public void execute(final Thread thread) {
          thread.setContextClassLoader(QuartzSchedulerProvider.class.getClassLoader());
          super.execute(thread);
        }
      };

      // create Scheduler (implicitly registers it with repository)
      DirectSchedulerFactory.getInstance()
          .createScheduler(
              SCHEDULER_NAME,
              nodeAccess.getId(), // instance-id
              new QuartzThreadPool(threadPoolSize, threadPriority),
              threadExecutor,
              jobStore.get(),
              null, // scheduler plugin-map
              null, // rmi-registry host
              0, // rmi-registry port
              -1, // idle-wait time
              -1, // db-failure retry-interval
              true, // jmx-export
              null, // custom jmx object-name, lets use the default
              1, // max batch-size
              0L // batch time-window
          );
      Scheduler s = DirectSchedulerFactory.getInstance().getScheduler(SCHEDULER_NAME);
      s.setJobFactory(jobFactory);

      // re-logging with version, as by default we limit quartz logging to WARN, hiding its default version logging
      log.info("Quartz Scheduler v{}", s.getMetaData().getVersion());

      s.standby();

      // Wrap with BulkReadScheduler for optimized bulk read operations (no N+1 query problem)
      log.info("Wrapping scheduler with BulkReadScheduler for optimized bulk read queries");
      return new BulkReadSchedulerImpl(s, connectionProvider, SCHEDULER_NAME);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
