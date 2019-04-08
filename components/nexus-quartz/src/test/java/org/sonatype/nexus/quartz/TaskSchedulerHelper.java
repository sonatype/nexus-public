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
package org.sonatype.nexus.quartz;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.testsupport.TestUtil;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.BaseUrlManager;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LastShutdownTimeService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.StateGuardModule;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseStatusDelayedExecutor;
import org.sonatype.nexus.quartz.internal.orient.JobStoreImpl;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.spi.SchedulerSPI;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.quartz.spi.JobFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IT tool: this beast brings up real SISU container and complete Quartz environment.
 */
public class TaskSchedulerHelper
{
  private final TestUtil util = new TestUtil(TaskSchedulerHelper.class);

  @Inject
  private Injector injector;

  @Inject
  private MutableBeanLocator locator;

  @Inject
  protected TaskScheduler taskScheduler;

  @Inject
  private SchedulerSPI scheduler;

  @Inject
  private JobStoreImpl jobStore;

  private EventManager eventManager;

  private ApplicationDirectories applicationDirectories;

  private BaseUrlManager baseUrlManager;

  private LastShutdownTimeService lastShutdownTimeService;

  private NodeAccess nodeAccess;

  private final DatabaseInstance databaseInstance;

  private DatabaseStatusDelayedExecutor statusDelayedExecutor;

  public TaskSchedulerHelper(final DatabaseInstance databaseInstance) {
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  public void init(@Nullable final Integer poolSize, @Nullable final JobFactory factory) throws Exception {
    eventManager = new SimpleEventManager();
    applicationDirectories = mock(ApplicationDirectories.class);
    baseUrlManager = mock(BaseUrlManager.class);
    nodeAccess = mock(NodeAccess.class);
    lastShutdownTimeService = mock(LastShutdownTimeService.class);
    statusDelayedExecutor = mock(DatabaseStatusDelayedExecutor.class);

    Module module = binder -> {
      Properties properties = new Properties();
      properties.put("basedir", util.getBaseDir());
      if (poolSize != null) {
        properties.put("nexus.quartz.poolSize", poolSize);
      }
      binder.bind(ParameterKeys.PROPERTIES)
          .toInstance(properties);

      binder.bind(EventManager.class).toInstance(eventManager);

      File workDir = util.createTempDir(util.getTargetDir(), "workdir");
      when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(workDir);
      binder.bind(ApplicationDirectories.class)
          .toInstance(applicationDirectories);

      binder.bind(BaseUrlManager.class)
          .toInstance(baseUrlManager);

      binder.bind(DatabaseInstance.class)
          .annotatedWith(Names.named("config"))
          .toInstance(databaseInstance);

      doAnswer(i  -> {
        ((Runnable) i.getArguments()[0]).run();
        return null;
      }).when(statusDelayedExecutor).execute(notNull(Runnable.class));
      binder.bind(DatabaseStatusDelayedExecutor.class)
          .toInstance(statusDelayedExecutor);

      when(nodeAccess.getId()).thenReturn("test-12345");
      when(nodeAccess.getMemberIds()).thenReturn(ImmutableSet.of("test-12345"));
      binder.bind(NodeAccess.class)
          .toInstance(nodeAccess);
      if (factory != null) {
        binder.bind(JobFactory.class).toInstance(factory);
      }

      binder.bind(LastShutdownTimeService.class).toInstance(lastShutdownTimeService);
      when(lastShutdownTimeService.estimateLastShutdownTime()).thenReturn(Optional.empty());
    };

    this.injector = Guice.createInjector(new WireModule(
        module, new StateGuardModule(),
        new SpaceModule(new URLClassSpace(TaskSchedulerHelper.class.getClassLoader()), BeanScanning.INDEX)
    ));
    injector.injectMembers(this);
  }

  public void start() throws Exception {
    jobStore.start();
    scheduler.start();
    scheduler.resume();
  }

  public void stop() throws Exception {
    scheduler.pause();
    scheduler.stop();
    jobStore.stop();

    locator.clear();
  }

  public TaskScheduler getTaskScheduler() {
    return taskScheduler;
  }

  public SchedulerSPI getScheduler() {
    return scheduler;
  }

  public EventManager getEventManager() {
    return eventManager;
  }
}
