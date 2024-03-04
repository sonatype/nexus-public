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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link TaskFactory} implementation.
 *
 * Resolves {@link TaskDescriptor} components via {@link BeanLocator} singleton components.
 *
 * Resolves {@link Task} components via {@link BeanLocator} lookup by {@link TaskDescriptor#getType()}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class TaskFactoryImpl
    extends ComponentSupport
    implements TaskFactory
{
  private final BeanLocator beanLocator;

  /**
   * Map of descriptor-id to descriptor instance.
   */
  private final Map<String, TaskDefinition> taskDefinitions = Maps.newConcurrentMap();

  @Inject
  public TaskFactoryImpl(final BeanLocator beanLocator) {
    this.beanLocator = checkNotNull(beanLocator);

    // watch for TaskDescriptor components
    beanLocator.watch(Key.get(TaskDescriptor.class, Named.class), new TaskDescriptorMediator(), this);
  }

  /**
   * Simple struct to hold descriptor and bean entry together.
   */
  private static class TaskDefinition
  {
    private final TaskDescriptor descriptor;

    private final BeanEntry<Annotation, ? extends Task> beanEntry;

    private TaskDefinition(final TaskDescriptor descriptor,
                           final BeanEntry<Annotation, ? extends Task> beanEntry)
    {
      this.descriptor = checkNotNull(descriptor);
      this.beanEntry = checkNotNull(beanEntry);
    }
  }

  /**
   * Sisu {@link Mediator} to maintain mapping of type-id to descriptor instance as they come and go.
   */
  private static class TaskDescriptorMediator
      implements Mediator<Named, TaskDescriptor, TaskFactoryImpl>
  {
    @Override
    public void add(final BeanEntry<Named, TaskDescriptor> entry, final TaskFactoryImpl watcher) throws Exception {
      watcher.addDescriptor(entry.getValue());
    }

    @Override
    public void remove(final BeanEntry<Named, TaskDescriptor> entry, final TaskFactoryImpl watcher) throws Exception {
      watcher.removeDescriptor(entry.getValue().getId());
    }
  }

  /**
   * Registers a Task implementation: based on passed in descriptor the task's {@link BeanEntry} is looked up too,
   * validated and cached, keyed by {@link TaskDescriptor#getId()}.
   */
  @VisibleForTesting
  void addDescriptor(final TaskDescriptor descriptor) {
    String typeId = descriptor.getId();
    log.debug("Adding task type-id: {}", typeId);

    // resolve task component
    Class<? extends Task> type = descriptor.getType();
    log.debug("Resolving task bean-entry for type-id {} of type: {}", typeId, type.getName());
    Iterator<? extends BeanEntry<Annotation, ? extends Task>> entries = beanLocator.locate(Key.get(type)).iterator();
    if (!entries.hasNext()) {
      log.warn("Missing task-component for type-id: {}; ignoring it", typeId);
      return;
    }

    BeanEntry<Annotation, ? extends Task> entry = entries.next();
    if (entry.getImplementationClass().getAnnotation(Singleton.class) != null) {
      log.warn(
          "Task type-id {} implementation {} is singleton; ignoring it",
          typeId,
          entry.getImplementationClass().getName());
      return;
    }
    log.debug("Adding task type-id: {} -> {}", typeId, entry.getImplementationClass().getName());
    TaskDefinition prevTaskDefinition = taskDefinitions.put(typeId, new TaskDefinition(descriptor, entry));
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
    taskDefinitions.remove(typeId);
  }

  /**
   * Creates a new instance of Task having provided type-id, by using {@link BeanEntry#getProvider()}, hence
   * new instance is created every time (tasks are enforced to not be singletons, see {@link
   * #addDescriptor(TaskDescriptor)}.
   */
  @VisibleForTesting
  Task newInstance(final String typeId) {
    TaskDefinition taskDefinition = taskDefinitions.get(typeId);
    checkArgument(taskDefinition != null, "Unknown task type-id: %s", typeId);

    Class<? extends Task> type = taskDefinition.descriptor.getType();
    return type.cast(taskDefinition.beanEntry.getProvider().get());
  }

  @Override
  public List<TaskDescriptor> getDescriptors() {
    return Collections.unmodifiableList(
        taskDefinitions.values().stream().map(d -> d.descriptor).collect(Collectors.toList())
    );
  }

  @Override
  @Nullable
  public TaskDescriptor findDescriptor(final String typeId) {
    TaskDefinition taskDefinition = taskDefinitions.get(typeId);
    if (taskDefinition != null) {
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
}
