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
package org.sonatype.scheduling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.scheduling.schedules.RunNowSchedule;
import org.sonatype.scheduling.schedules.Schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple facade to ScheduledThreadPoolExecutor.
 *
 * @author cstamas
 */
@Named
@Singleton
public class DefaultScheduler
    implements Scheduler
{
  private static final Logger logger = LoggerFactory.getLogger(DefaultScheduler.class);

  private final TaskConfigManager taskConfig;

  private final AtomicInteger idGen;

  private final ScheduledExecutorService scheduledExecutorService;

  private final ConcurrentHashMap<String, List<ScheduledTask<?>>> tasksMap;

  /**
   * Deprecated constructor.
   *
   * @deprecated Use another constructor. Left only for some UTs use.
   */
  @Deprecated
  public DefaultScheduler(final TaskConfigManager taskConfig) {
    this(taskConfig, new TaskExecutorProvider()
    {
      @Override
      public ScheduledExecutorService getTaskExecutor() {
        final ScheduledThreadPoolExecutor scheduledExecutorService =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(20, new ThreadFactoryImpl(
                Thread.MIN_PRIORITY));
        scheduledExecutorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduledExecutorService.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return scheduledExecutorService;
      }
    });
  }

  @Inject
  public DefaultScheduler(final TaskConfigManager taskConfig, TaskExecutorProvider scheduledExecutorServiceProvider) {
    this.taskConfig = taskConfig;
    this.scheduledExecutorService = scheduledExecutorServiceProvider.getTaskExecutor();
    this.idGen = new AtomicInteger(0);
    this.tasksMap = new ConcurrentHashMap<String, List<ScheduledTask<?>>>();
  }

  protected Logger getLogger() {
    return logger;
  }

  // ==

  public void initializeTasks() {
    getLogger().info("Initializing Scheduler...");

    // this call delegates to task config manager that loads up the persisted tasks (if any)
    // and performs a series of callbacks to this to make them "alive"
    taskConfig.initializeTasks(this);

    // wind up the "idGen" source, to the max ID we got loaded up from config (the generated IDs are persisted)
    int maxId = 0;
    for (Map.Entry<String, List<ScheduledTask<?>>> entry : getAllTasks().entrySet()) {
      for (ScheduledTask<?> task : entry.getValue()) {
        try {
          maxId = Math.max(maxId, Integer.parseInt(task.getId()));
        }
        catch (NumberFormatException e) {
          // be forgiving about non number IDs
          // hint1: sadly, some Nexus ITs does have them
          // hint2: they will not clash with numbers anyway
        }
      }
    }
    idGen.set(maxId);
  }

  public void shutdown() {
    getLogger().info("Shutting down Scheduler...");
    try {
      scheduledExecutorService.shutdown();
      boolean stopped = scheduledExecutorService.awaitTermination(3L, TimeUnit.SECONDS);
      if (!stopped) {
        final Map<String, List<ScheduledTask<?>>> runningTasks = getRunningTasks();
        if (!runningTasks.isEmpty()) {
          scheduledExecutorService.shutdownNow();
          getLogger().warn("Scheduler shut down forcibly with tasks running.");
        }
        else {
          getLogger().info("Scheduler shut down cleanly with tasks scheduled.");
        }
      }
    }
    catch (InterruptedException e) {
      getLogger().info("Termination interrupted", e);
    }
  }

  @Deprecated
  public SchedulerTask<?> createTaskInstance(String taskType)
      throws IllegalArgumentException
  {
    return taskConfig.createTaskInstance(taskType);
  }

  public <T> T createTaskInstance(Class<T> taskType)
      throws IllegalArgumentException
  {
    return taskConfig.createTaskInstance(taskType);
  }

  public ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  protected <T> void addToTasksMap(ScheduledTask<T> task, boolean store) {
    tasksMap.putIfAbsent(task.getType(), new CopyOnWriteArrayList<ScheduledTask<?>>());
    tasksMap.get(task.getType()).add(task);

    if (store) {
      taskConfig.addTask(task);
    }
  }

  protected <T> void removeFromTasksMap(ScheduledTask<T> task) {
    final List<ScheduledTask<?>> tasks = tasksMap.get(task.getType());

    if (tasks != null) {
      tasks.remove(task);

      // this is potentially problematic, might _remove_ concurrently added new task
      // but, this is only here to keep map keys small, but the keys (task types) are actually
      // rather small, so I see no point of pruning map for keys
      // if ( tasks.size() == 0 )
      // {
      // tasksMap.remove( task.getType() );
      // }
    }

    taskConfig.removeTask(task);
  }

  protected void taskRescheduled(ScheduledTask<?> task) {
    taskConfig.addTask(task);
  }

  protected String generateId() {
    return String.valueOf(idGen.incrementAndGet());
  }

  public <T> ScheduledTask<T> initialize(String id, String name, String type, Callable<T> callable,
                                         Schedule schedule, boolean enabled)
  {
    return schedule(id, name, type, callable, schedule, enabled, false);
  }

  public ScheduledTask<Object> submit(String name, Runnable runnable) {
    return schedule(name, runnable, new RunNowSchedule());
  }

  public ScheduledTask<Object> schedule(String name, Runnable runnable, Schedule schedule) {
    // use the name of the class as the type.
    return schedule(name, runnable.getClass().getSimpleName(), Executors.callable(runnable), schedule);
  }

  public <T> ScheduledTask<T> submit(String name, Callable<T> callable) {
    return schedule(name, callable, new RunNowSchedule());
  }

  public <T> ScheduledTask<T> schedule(String name, Callable<T> callable, Schedule schedule) {
    return schedule(name, callable.getClass().getSimpleName(), callable, schedule);
  }

  protected <T> ScheduledTask<T> schedule(String name, String type, Callable<T> callable, Schedule schedule) {
    return schedule(generateId(), name, type, callable, schedule, true);
  }

  protected <T> ScheduledTask<T> schedule(String id, String name, String type, Callable<T> callable,
                                          Schedule schedule, boolean enabled, boolean store)
  {
    DefaultScheduledTask<T> dct = new DefaultScheduledTask<T>(id, name, type, this, callable, schedule);
    dct.setEnabled(enabled);
    addToTasksMap(dct, store);
    dct.start();
    return dct;
  }

  protected <T> ScheduledTask<T> schedule(String id, String name, String type, Callable<T> callable,
                                          Schedule schedule, boolean store)
  {
    return schedule(id, name, type, callable, schedule, true, store);
  }

  public <T> ScheduledTask<T> updateSchedule(ScheduledTask<T> task)
      throws RejectedExecutionException, NullPointerException
  {
    // Simply add the task to config, will find existing by id, remove, then store new
    taskConfig.addTask(task);
    return task;
  }

  // ==

  public Map<String, List<ScheduledTask<?>>> getAllTasks() {
    Map<String, List<ScheduledTask<?>>> result = new HashMap<String, List<ScheduledTask<?>>>(tasksMap.size());

    for (Map.Entry<String, List<ScheduledTask<?>>> entry : tasksMap.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        result.put(entry.getKey(), new ArrayList<ScheduledTask<?>>(entry.getValue()));
      }
    }

    return result;
  }

  private static boolean StringUtils_isEmpty(String str) {
    return ((str == null) || (str.trim().length() == 0));
  }

  public ScheduledTask<?> getTaskById(String id)
      throws NoSuchTaskException
  {
    if (StringUtils_isEmpty(id)) {
      throw new IllegalArgumentException("The Tasks cannot have null IDs!");
    }

    final Collection<List<ScheduledTask<?>>> activeTasks = getAllTasks().values();

    for (List<ScheduledTask<?>> tasks : activeTasks) {
      for (ScheduledTask<?> task : tasks) {
        if (task.getId().equals(id)) {
          return task;
        }
      }
    }

    throw new NoSuchTaskException(id);
  }

  public Map<String, List<ScheduledTask<?>>> getActiveTasks() {
    Map<String, List<ScheduledTask<?>>> result = getAllTasks();

    List<ScheduledTask<?>> tasks = null;

    // filter for activeOrSubmitted
    for (Iterator<String> c = result.keySet().iterator(); c.hasNext(); ) {
      String cls = c.next();
      tasks = result.get(cls);

      for (Iterator<ScheduledTask<?>> i = tasks.iterator(); i.hasNext(); ) {
        ScheduledTask<?> task = i.next();

        if (!task.getTaskState().isActiveOrSubmitted()) {
          i.remove();
        }
      }

      if (tasks.isEmpty()) {
        c.remove();
      }
    }

    return result;
  }

  public Map<String, List<ScheduledTask<?>>> getRunningTasks() {
    Map<String, List<ScheduledTask<?>>> result = getAllTasks();
    List<ScheduledTask<?>> tasks = null;

    // filter for RUNNING
    for (Iterator<String> c = result.keySet().iterator(); c.hasNext(); ) {
      String cls = c.next();
      tasks = result.get(cls);

      for (Iterator<ScheduledTask<?>> i = tasks.iterator(); i.hasNext(); ) {
        ScheduledTask<?> task = i.next();

        if (!TaskState.RUNNING.equals(task.getTaskState())) {
          i.remove();
        }
      }

      if (tasks.isEmpty()) {
        c.remove();
      }
    }

    return result;
  }
}
