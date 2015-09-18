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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;
import org.sonatype.nexus.scheduling.TaskFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.eclipse.sisu.BeanEntry;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link TaskFactory} that hooks into SISU.
 *
 * @since 3.0
 */
@Singleton
@Named
public class DefaultTaskFactory
    extends ComponentSupport
    implements TaskFactory
{
  private final Iterable<BeanEntry<Named, Task>> tasks;

  private final List<TaskDescriptor<?>> taskDescriptors;

  @Inject
  public DefaultTaskFactory(final Iterable<BeanEntry<Named, Task>> tasks,
                            final List<TaskDescriptor<?>> taskDescriptors)
  {
    this.tasks = checkNotNull(tasks);
    this.taskDescriptors = checkNotNull(taskDescriptors);
  }

  @Override
  public List<TaskDescriptor<?>> listTaskDescriptors() {
    return Lists.newArrayList(allTaskDescriptors().values());
  }

  @Override
  public <T extends Task> TaskDescriptor<T> resolveTaskDescriptorByTypeId(final String taskTypeId) {
    // look for descriptors first
    for (TaskDescriptor<?> taskDescriptor : taskDescriptors) {
      if (taskDescriptor.getId().equals(taskTypeId)) {
        return (TaskDescriptor<T>) taskDescriptor;
      }
    }
    // not found by descriptor, try tasks directly
    for (BeanEntry<Named, Task> entry : tasks) {
      // checks: task FQCN, task SimpleName or @Named
      if (entry.getImplementationClass().getName().equals(taskTypeId)
          || entry.getImplementationClass().getSimpleName().equals(taskTypeId)
          || entry.getKey().value().equals(taskTypeId)) {
        return (TaskDescriptor<T>) findTaskDescriptor(entry);
      }
    }
    return null;
  }

  @Override
  public <T extends Task> T createTaskInstance(final TaskConfiguration taskConfiguration)
      throws IllegalArgumentException
  {
    checkNotNull(taskConfiguration);
    taskConfiguration.validate();
    log.debug("Creating task by hint: {}", taskConfiguration);
    final TaskDescriptor<T> taskDescriptor = getTaskDescriptorByTypeId(taskConfiguration.getTypeId());
    for (BeanEntry<Named, Task> entry : tasks) {
      if (entry.getImplementationClass().equals(taskDescriptor.getType())) {
        final T task = (T) entry.getProvider().get();
        task.configure(taskConfiguration);
        return task;
      }
    }
    throw new IllegalArgumentException("No Task of type \'" + taskConfiguration.getTypeId() + "\' found");
  }

  // ==

  /**
   * Gets TaskDescriptor by it's ID, when the taskTypeId is know to be coming from a trusted value. This method,
   * unlike {@link #resolveTaskDescriptorByTypeId(String)} is not "laxed".
   */
  private <T extends Task> TaskDescriptor<T> getTaskDescriptorByTypeId(final String taskTypeId) {
    final Map<String, TaskDescriptor<?>> all = allTaskDescriptors();
    if (all.containsKey(taskTypeId)) {
      return (TaskDescriptor<T>) all.get(taskTypeId);
    }
    throw new IllegalArgumentException("No Task of type \'" + taskTypeId + "\' found");
  }

  /**
   * Returns a map with "typeId" Task descriptor mapping for all existing tasks. For tasks without descriptors,
   * descriptor will be created.
   */
  private Map<String, TaskDescriptor<?>> allTaskDescriptors() {
    // TODO: consistency checks? a) task : descriptors are 1:1, b) descriptor IDs are unique?
    // TODO: we might emit warning if some of those does not stand
    // using task class as "key" to detect tasks w/ descriptor vs tasks w/o descriptor
    final Map<Class<? extends Task>, TaskDescriptor<?>> descriptorMap = Maps.newHashMap();
    for (TaskDescriptor<?> taskDescriptor : taskDescriptors) {
      descriptorMap.put(taskDescriptor.getType(), taskDescriptor);
    }
    for (BeanEntry<Named, Task> entry : tasks) {
      if (!descriptorMap.containsKey(entry.getImplementationClass())) {
        final TaskDescriptor<?> taskDescriptor = createTaskDescriptor(entry);
        descriptorMap.put(taskDescriptor.getType(), taskDescriptor);
      }
    }
    // repack the map into result map keyed by typeId
    final Map<String, TaskDescriptor<?>> result = Maps.newHashMap();
    for (TaskDescriptor<?> taskDescriptor : descriptorMap.values()) {
      result.put(taskDescriptor.getId(), taskDescriptor);
    }
    return result;
  }

  /**
   * Returns {@link TaskDescriptor} by given Task's bean entry. Will perform a search for provided task descriptors,
   * and if not found, will create one using {@link #createTaskDescriptor(BeanEntry)}.
   */
  private <T extends Task> TaskDescriptor<T> findTaskDescriptor(final BeanEntry<Named, Task> taskBeanEntry) {
    // look for descriptor first
    for (TaskDescriptor<?> taskDescriptor : taskDescriptors) {
      if (taskDescriptor.getType().equals(taskBeanEntry.getImplementationClass())) {
        return (TaskDescriptor<T>) taskDescriptor;
      }
    }
    // not found by descriptor, create one for it
    return (TaskDescriptor<T>) createTaskDescriptor(taskBeanEntry);
  }

  /**
   * Creates {@link TaskDescriptor} for given Task's bean entry class. To be used for tasks without descriptors, it
   * will create one on the fly with defaults.
   */
  private <T extends Task> TaskDescriptor<T> createTaskDescriptor(final BeanEntry<Named, Task> taskBeanEntry) {
    final String taskName =
        taskBeanEntry.getDescription() != null
            ? taskBeanEntry.getDescription()
            : taskBeanEntry.getImplementationClass().getSimpleName();
    // by default, tasks w/o descriptors are not exposed, and not visible while run/scheduled
    return new TaskDescriptorSupport<T>(
        (Class<T>) taskBeanEntry.getImplementationClass(),
        taskName,
        false,
        false
    ) { };
  }
}
