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
package org.sonatype.nexus.testsuite.testsupport.system;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.events.TaskEvent;
import org.sonatype.nexus.scheduling.events.TaskEventStarted;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class TaskTestSystem
    extends TestSystemSupport
    implements EventAware, EventAware.Asynchronous
{
  private static final Logger log = LoggerFactory.getLogger(TaskTestSystem.class);

  private final List<TaskEvent> events = new CopyOnWriteArrayList<>();

  private final List<TaskInfo> tasks = new CopyOnWriteArrayList<>();

  private final TaskScheduler scheduler;

  @Inject
  public TaskTestSystem(final TaskScheduler scheduler, final EventManager eventManager) {
    super(eventManager);
    this.scheduler = checkNotNull(scheduler);
  }

  @Override
  protected void doAfter() {
    clear();
    tasks.forEach(TaskInfo::remove);
    tasks.clear();
  }

  @Subscribe
  public void on(final TaskEvent event) {
    log.debug("Recieved event: {}", event);
    events.add(event);
  }

  public void remove(final String taskId) {
    List<TaskInfo> tasksToRemove = scheduler.listsTasks().stream()
        .filter(task -> taskId.equals(task.getTypeId()))
        .collect(Collectors.toList());
    tasksToRemove.forEach(TaskInfo::remove);

    List<TaskInfo> newState = tasks
        .stream()
        .filter(it -> !taskId.equals(it.getId()))
        .collect(Collectors.toList());

    tasks.clear();
    tasks.addAll(newState);
  }

  /**
   * Clear the recorded event queue.
   */
  public void clear() {
    events.clear();
  }

  /**
   * Count the number of tasks that have started since the current test started, or {@code clear} was called.
   *
   * @param typeId the task id type.
   */
  public long eventStarted(final String typeId) {
    return events(TaskEventStarted.class, typeId).count();
  }

  /**
   * Count the number of tasks that have started since the current test started, or {@code clear} was called.
   */
  public long eventStarted(final String typeId, final Map<String, String> configuration) {
    return events(TaskEventStarted.class, typeId)
        .filter(taskInfo -> taskInfo.getConfiguration().asMap().equals(configuration))
        .count();
  }

  /**
   * Count the number of tasks that have completed successfully since the current test started,
   * or {@code clear} was called.
   */
  public long eventDone(final String typeId) {
    return events(TaskEventStoppedDone.class, typeId).count();
  }

  /**
   * Count the number of tasks that have completed successfully since the current test started,
   * or {@code clear} was called.
   */
  public long eventDone(final String typeId, final Map<String, String> configuration) {
    return events(TaskEventStoppedDone.class, typeId)
        .filter(taskInfo -> taskInfo.getConfiguration().asMap().equals(configuration))
        .count();
  }

  /**
   * Count the number of tasks that have failed since the current test started, or {@code clear} was called.
   */
  public long eventFailed(final String typeId) {
    return events(TaskEventStoppedFailed.class, typeId).count();
  }

  /**
   * Count the number of tasks that have failed since the current test started, or {@code clear} was called.
   */
  public long eventFailed(final String typeId, final Map<String, String> configuration) {
    return events(TaskEventStoppedFailed.class, typeId)
        .filter(taskInfo -> taskInfo.getConfiguration().asMap().equals(configuration))
        .count();
  }

  public TaskInfo create(final String name, final String typeId) {
    return create(name, typeId, Collections.emptyMap(), __ -> {});
  }

  public TaskInfo create(final String name, final String typeId, final Map<String, String> attributes) {
    return create(name, typeId, attributes, __ -> {});
  }

  public TaskInfo create(
      final String name,
      final String typeId,
      final Map<String, String> attributes,
      final Consumer<TaskConfiguration> mutator)
  {
    TaskConfiguration taskConfiguration = scheduler.createTaskConfigurationInstance(typeId);
    attributes.forEach(taskConfiguration::setString);
    taskConfiguration.setName(name);
    taskConfiguration.setEnabled(true);
    mutator.accept(taskConfiguration);

    TaskInfo taskInfo = scheduler.scheduleTask(taskConfiguration, scheduler.getScheduleFactory().manual());
    tasks.add(taskInfo);
    return taskInfo;
  }

  private Stream<TaskInfo> events(final Class<? extends TaskEvent> clazz, final String typeId) {
    return events.stream()
        .filter(clazz::isInstance)
        .map(TaskEvent::getTaskInfo)
        .filter(info -> info.getTypeId().equals(typeId));
  }
}
