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
package org.sonatype.nexus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.events.EventSubscriberHost;
import org.sonatype.nexus.proxy.NexusProxyTestSupport;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.maven.routing.Config;
import org.sonatype.nexus.proxy.maven.routing.internal.ConfigImpl;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.nexus.threads.FakeAlmightySubject;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.security.guice.SecurityModule;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.collect.ObjectArrays;
import com.google.inject.Binder;
import com.google.inject.Module;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;

public abstract class NexusAppTestSupport
    extends NexusProxyTestSupport
{

  private NexusScheduler nexusScheduler;

  private EventSubscriberHost eventSubscriberHost;

  private EventBus eventBus;

  private NexusConfiguration nexusConfiguration;

  private TemplateManager templateManager;

  protected boolean runWithSecurityDisabled() {
    return true;
  }

  protected boolean shouldLoadConfigurationOnStartup() {
    return false;
  }

  // NxApplication

  private boolean nexusStarted = false;

  /**
   * Preferred way to "boot" Nexus in unit tests. Previously, UTs were littered with code like this:
   *
   * <pre>
   * lookup(Nexus.class); // boot nexus
   * </pre>
   *
   * This was usually in {@link #setUp()} method override, and then another override was made in {@link #tearDown()}.
   * Using this method you don't have to fiddle with "shutdown" anymore, and also, you can invoke it in some prepare
   * method (like setUp) but also from test at any place. You have to ensure this method is not called multiple times,
   * as that signals a bad test (start nexus twice?), and exception will be thrown.
   */
  protected void startNx() throws Exception {
    if (nexusStarted) {
      throw new IllegalStateException("Bad test, as startNx was already invoked once!");
    }
    lookup(NxApplication.class).start();
    nexusStarted = true;
  }

  /**
   * Shutdown Nexus if started.
   */
  @After
  public void stopNx() throws Exception {
    if (nexusStarted) {
      lookup(NxApplication.class).stop();
    }
  }

  /**
   * Returns true if startNx method was invoked, if Nexus was started.
   */
  protected boolean isNexusStarted() {
    return nexusStarted;
  }

  // NxApplication

  @Override
  protected Module[] getTestCustomModules() {
    Module[] modules = super.getTestCustomModules();
    if (modules == null) {
      modules = new Module[0];
    }
    modules = ObjectArrays.concat(modules, new SecurityModule());
    modules = ObjectArrays.concat(modules, new Module()
    {
      @Override
      public void configure(final Binder binder) {
        binder.bind(Config.class).toInstance(new ConfigImpl(enableAutomaticRoutingFeature()));
      }
    });
    return modules;
  }

  protected boolean enableAutomaticRoutingFeature() {
    return false;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    // remove Shiro thread locals, as things like DelegatingSubjects might lead us to old instance of SM
    ThreadContext.remove();
    super.setUp();

    eventBus = lookup(EventBus.class);
    nexusScheduler = lookup(NexusScheduler.class);
    eventSubscriberHost = lookup(EventSubscriberHost.class);
    nexusConfiguration = lookup(NexusConfiguration.class);
    templateManager = lookup(TemplateManager.class);

    if (shouldLoadConfigurationOnStartup()) {
      loadConfiguration();
    }

    if (runWithSecurityDisabled()) {
      shutDownSecurity();
    }
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    // FIXME: This needs to be fired as many component relies on this to cleanup (like EHCache)
    if (eventBus != null) {
      eventBus.post(new NexusStoppedEvent(null));
    }
    waitForTasksToStop();
    super.tearDown();
    // remove Shiro thread locals, as things like DelegatingSubjects might lead us to old instance of SM
    ThreadContext.remove();
  }

  protected EventBus eventBus() {
    return eventBus;
  }

  protected NexusConfiguration nexusConfiguration() {
    return nexusConfiguration;
  }

  protected TemplateSet getRepositoryTemplates() {
    return templateManager.getTemplates().getTemplates(RepositoryTemplate.class);
  }

  protected void shutDownSecurity()
      throws Exception
  {
    System.out.println("== Shutting down SECURITY!");

    loadConfiguration();

    ThreadContext.bind(FakeAlmightySubject.forUserId("disabled-security"));

    System.out.println("== Shutting down SECURITY!");
  }

  protected void loadConfiguration() throws ConfigurationException, IOException {
    nexusConfiguration.loadConfiguration(false);
    nexusConfiguration.saveConfiguration();
  }

  protected void killActiveTasks()
      throws Exception
  {
    Map<String, List<ScheduledTask<?>>> taskMap = nexusScheduler.getActiveTasks();

    for (List<ScheduledTask<?>> taskList : taskMap.values()) {
      for (ScheduledTask<?> task : taskList) {
        task.cancel();
      }
    }
  }

  protected void wairForAsyncEventsToCalmDown()
      throws Exception
  {
    while (!eventSubscriberHost.isCalmPeriod()) {
      Thread.sleep(100);
    }
  }

  protected void waitForTasksToStop()
      throws Exception
  {
    if (nexusScheduler == null) {
      return;
    }

    // Give task a chance to start
    Thread.sleep(50);

    int counter = 0;

    while (nexusScheduler.getActiveTasks().size() > 0) {
      Thread.sleep(100);
      counter++;

      if (counter > 300) {
        System.out.println("TIMEOUT WAITING FOR TASKS TO COMPLETE!!!  Will kill them.");
        printActiveTasks();
        killActiveTasks();
        break;
      }
    }
  }

  protected void printActiveTasks()
      throws Exception
  {
    Map<String, List<ScheduledTask<?>>> taskMap = nexusScheduler.getActiveTasks();

    for (List<ScheduledTask<?>> taskList : taskMap.values()) {
      for (ScheduledTask<?> task : taskList) {
        System.out.println(task.getName() + " with id " + task.getId() + " is in state "
            + task.getTaskState().toString());
      }
    }
  }

}
