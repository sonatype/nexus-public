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
package org.sonatype.nexus.scheduling.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * Default {@link TaskFactory} implementation.
 * <p>
 * Resolves {@link TaskDescriptor} components via {@link ApplicationContext} singleton components.
 * <p>
 * Resolves {@link Task} components via {@link ApplicationContext} lookup by {@link TaskDescriptor#getType()}.
 *
 * @since 3.0
 */
@ManagedLifecycle(phase = TASKS)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class TaskFactoryImpl
    extends StateGuardLifecycleSupport
    implements TaskFactory
{
  private final DatabaseCheck databaseCheck;

  private final List<TaskDescriptor> descriptors;

  private final ApplicationContext context;

  /**
   * Map of descriptor-id to descriptor instance.
   */
  private final Map<String, TaskDefinition> taskDefinitions = Maps.newConcurrentMap();

  /**
   * Flag to indicate if taskDefinitions has been fully initialized.
   * This is volatile to ensure visibility across threads - a thread will only see initialized=true
   * after the populating thread has finished adding all descriptors.
   */
  private volatile boolean initialized = false;

  @Autowired
  public TaskFactoryImpl(
      final DatabaseCheck databaseCheck,
      final List<TaskDescriptor> descriptors,
      final ApplicationContext context)
  {
    this.databaseCheck = checkNotNull(databaseCheck);
    this.descriptors = checkNotNull(descriptors);
    this.context = checkNotNull(context);
  }

  @Override
  protected void doStart() {
    createTaskDefinitionMap();
  }

  /**
   * Simple struct to hold descriptor and bean entry together.
   */
  private static class TaskDefinition
  {
    private final TaskDescriptor descriptor;

    private final ObjectProvider<? extends Task> beanEntry;

    private TaskDefinition(
        final TaskDescriptor descriptor,
        final ObjectProvider<? extends Task> beanEntry)
    {
      this.descriptor = checkNotNull(descriptor);
      this.beanEntry = checkNotNull(beanEntry);
    }
  }

  /**
   * Registers a Task implementation: based on passed in descriptor the task's {@link ApplicationContext} is looked up
   * too, validated and cached, keyed by {@link TaskDescriptor#getId()}.
   */
  @VisibleForTesting
  void addDescriptor(final TaskDescriptor descriptor) {
    String typeId = descriptor.getId();
    log.debug("Adding task type-id: {}", typeId);

    // resolve task component
    Class<? extends Task> type = descriptor.getType();
    log.debug("Resolving task bean-entry for type-id {} of type: {}", typeId, type.getName());
    ObjectProvider<? extends Task> provider = context.getBeanProvider(type);

    if (provider == null) {
      log.warn("Missing task-component for type-id: {}; ignoring it", typeId);
      return;
    }

    Task sample = provider.getIfUnique();
    if (sample == null) {
      log.warn(
          "Task type-id {} implementation {} is singleton; ignoring it",
          typeId,
          type.getName());
      return;
    }

    log.debug("Adding task type-id: {} -> {}", typeId, sample.getClass().getName());
    TaskDefinition prevTaskDefinition = taskDefinitions.put(typeId, new TaskDefinition(descriptor, provider));
    if (prevTaskDefinition != null) {
      log.warn(
          "Duplicate task type-id {} implementations: {} replaced by {}",
          typeId,
          prevTaskDefinition.descriptor.getType().getName(),
          descriptor.getType().getName());
    }
  }

  /**
   * Unregisters a Task implementation by it's type-id ({@link TaskDescriptor#getId()}).
   */
  @VisibleForTesting
  void removeDescriptor(final String typeId) {
    log.debug("Removing task type-id: {}", typeId);
    taskDefinitions().remove(typeId);
  }

  /**
   * Creates a new instance of Task having provided type-id, by using {@link ObjectProvider#getIfUnique()}, hence new
   * instance is created every time (tasks are enforced to not be singletons, see
   * {@link #addDescriptor(TaskDescriptor)}.
   */
  @VisibleForTesting
  Task newInstance(final String typeId) {
    TaskDefinition taskDefinition = taskDefinitions().get(typeId);
    checkArgument(taskDefinition != null, "Unknown task type-id: %s", typeId);

    Class<? extends Task> type = taskDefinition.descriptor.getType();
    return type.cast(taskDefinition.beanEntry.getIfUnique());
  }

  @Override
  public List<TaskDescriptor> getDescriptors() {
    return Collections.unmodifiableList(
        taskDefinitions().values()
            .stream()
            .map(d -> d.descriptor)
            .filter(d -> databaseCheck.isAllowedByVersion(d.getClass()))
            .collect(Collectors.toList()));
  }

  @Override
  @Nullable
  public TaskDescriptor findDescriptor(final String typeId) {
    TaskDefinition taskDefinition = taskDefinitions().get(typeId);
    if (taskDefinition != null) {
      if (!databaseCheck.isAllowedByVersion(taskDefinition.descriptor.getClass())) {
        return null;
      }

      return taskDefinition.descriptor;
    }
    return null;
  }

  @Override
  public Task create(final TaskConfiguration config, final TaskInfo taskInfo) {
    checkNotNull(config);
    log.debug("Creating task instance: {}", config);

    // ensure configuration is sane
    config.validate();

    // create and configure the task
    Task task = newInstance(config.getTypeId());
    task.configure(config);
    task.setTaskInfo(taskInfo);

    return task;
  }

  /**
   * Returns the task definitions map, ensuring it is fully initialized first.
   * Uses double-checked locking with a volatile flag to ensure thread-safety:
   * - The volatile read of 'initialized' provides visibility of all writes made before it was set to true
   * - The synchronized block ensures only one thread populates the map
   * - The second check inside the synchronized block prevents duplicate initialization
   */
  private Map<String, TaskDefinition> taskDefinitions() {
    if (!initialized) {
      createTaskDefinitionMap();
    }
    return taskDefinitions;
  }

  private synchronized void createTaskDefinitionMap() {
    if (!initialized) {
      descriptors.forEach(this::addDescriptor);
      initialized = true;
    }
  }
}
