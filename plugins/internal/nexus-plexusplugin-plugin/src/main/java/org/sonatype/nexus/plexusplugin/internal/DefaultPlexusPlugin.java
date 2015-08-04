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
package org.sonatype.nexus.plexusplugin.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.plexusplugin.PlexusPlugin;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;

import com.google.common.collect.Lists;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Default implementation of {@link PlexusPlugin}.
 */
@Component(role = PlexusPlugin.class)
public class DefaultPlexusPlugin
    implements PlexusPlugin
{
  @Requirement
  private RepositoryRegistry repositoryRegistry;

  @Requirement
  private RepositoryTypeRegistry repositoryTypeRegistry;

  @Requirement(role = ContentClass.class)
  private Map<String, ContentClass> contentClasses;

  @Requirement
  private NexusScheduler scheduler;

  private final AtomicInteger atomicInteger = new AtomicInteger(0);

  @Override
  public void newEventReceived(final Event<?> evt) {
    atomicInteger.incrementAndGet();
  }

  @Override
  public int getEventReceived() {
    return atomicInteger.get();
  }

  @Override
  public Collection<String> getRegisteredRepositoryIds() {
    final List<String> result = Lists.newArrayList();
    final List<Repository> repositories = repositoryRegistry.getRepositories();
    for (Repository repository : repositories) {
      result.add(repository.toString());
    }
    return result;
  }

  @Override
  public Collection<String> getRegisteredRepositoryTypes() {
    final List<String> result = Lists.newArrayList();
    final Set<RepositoryTypeDescriptor> repositoryTypes = repositoryTypeRegistry
        .getRegisteredRepositoryTypeDescriptors();
    for (RepositoryTypeDescriptor repositoryType : repositoryTypes) {
      result.add(repositoryType.toString());
    }
    return result;
  }

  @Override
  public Collection<String> getContentClasses() {
    final List<String> result = Lists.newArrayList();
    for (ContentClass contentClass : contentClasses.values()) {
      result.add(contentClass.getId());
    }
    return result;
  }

  @Override
  public Collection<String> getScheduledTaskNames() {
    final List<String> result = Lists.newArrayList();
    final Map<String, List<ScheduledTask<?>>> tasksByType = scheduler.getAllTasks();
    for (Map.Entry<String, List<ScheduledTask<?>>> tasks : tasksByType.entrySet()) {
      for (ScheduledTask task : tasks.getValue()) {
        result.add(task.getName());
      }
    }
    return result;
  }
}
