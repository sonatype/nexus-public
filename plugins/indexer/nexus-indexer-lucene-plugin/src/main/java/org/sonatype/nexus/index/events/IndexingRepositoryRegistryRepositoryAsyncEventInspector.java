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
package org.sonatype.nexus.index.events;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.events.Asynchronous;
import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.index.tasks.RepairIndexTask;
import org.sonatype.nexus.index.tasks.UpdateIndexTask;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryRepositoryEvent;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesPathAwareTask;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang.StringUtils;

/**
 * Listens for events and manages indexes by doing reindexes when needed (on repository configuration updates).
 * <p>
 * This EventInspector HAS TO BY ASYNC
 *
 * @author Toni Menzel
 * @author cstamas
 */
@Named
@Singleton
public class IndexingRepositoryRegistryRepositoryAsyncEventInspector
    extends ComponentSupport
    implements EventSubscriber, Asynchronous
{
  private final RepositoryRegistry repoRegistry;

  private final NexusScheduler nexusScheduler;

  private final ApplicationStatusSource applicationStatusSource;

  @Inject
  public IndexingRepositoryRegistryRepositoryAsyncEventInspector(final RepositoryRegistry repoRegistry,
                                                                 final NexusScheduler nexusScheduler,
                                                                 final ApplicationStatusSource applicationStatusSource)
  {
    this.repoRegistry = repoRegistry;
    this.nexusScheduler = nexusScheduler;
    this.applicationStatusSource = applicationStatusSource;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryRegistryRepositoryEvent evt) {
    inspect(evt);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryConfigurationUpdatedEvent evt) {
    inspect(evt);
  }

  protected void inspect(Event<?> evt) {
    if (!applicationStatusSource.getSystemStatus().isNexusStarted()) {
      return;
    }

    Repository repository = null;
    if (evt instanceof RepositoryRegistryRepositoryEvent) {
      repository = ((RepositoryRegistryRepositoryEvent) evt).getRepository();
    }
    else if (evt instanceof RepositoryConfigurationUpdatedEvent) {
      repository = ((RepositoryConfigurationUpdatedEvent) evt).getRepository();
    }

    try {
      // check registry for existance, wont be able to do much
      // if doesn't exist yet
      repoRegistry.getRepositoryWithFacet(repository.getId(), MavenRepository.class);

      inspectForIndexerManager(evt, repository);
    }
    catch (NoSuchRepositoryException e) {
      log.debug("Attempted to handle repository that isn't yet in registry");
    }
  }

  private void inspectForIndexerManager(Event<?> evt, Repository repository) {
    try {
      // we are handling repo events, like addition and removal
      if (evt instanceof RepositoryRegistryEventAdd) {
        // create the initial index
        if (repository.isIndexable()) {
          // Create the initial index for the repository
          reindexRepo(repository, false, "Creating initial index, repositoryId=" + repository.getId());
        }
      }
      else if (evt instanceof RepositoryConfigurationUpdatedEvent) {
        RepositoryConfigurationUpdatedEvent event = (RepositoryConfigurationUpdatedEvent) evt;

        // we need to do a full reindex of a Maven2 Proxy repository if:
        // a) if remoteUrl changed
        // b) if download remote index enabled (any repo type)
        // c) if repository is made searchable
        // TODO: are we sure only a) needs a check for Maven2? I think all of them need
        if (event.isRemoteUrlChanged() || event.isDownloadRemoteIndexEnabled() || event.isMadeSearchable()) {
          String taskName = null;

          String logMessage = null;

          if (event.isRemoteUrlChanged()) {
            taskName = append(taskName, "remote URL changed");

            logMessage = append(logMessage, "remote URL changed");
          }

          if (event.isDownloadRemoteIndexEnabled()) {
            taskName = append(taskName, "enabled download of indexes");

            logMessage = append(logMessage, "enabled download of indexes");
          }

          if (event.isMadeSearchable()) {
            taskName = append(taskName, "enabled searchable");

            logMessage = append(logMessage, "enabled searchable");
          }

          taskName = taskName + ", repositoryId=" + event.getRepository().getId() + ".";

          logMessage =
              logMessage + " on repository \"" + event.getRepository().getName() + "\" (id="
                  + event.getRepository().getId() + "), doing full reindex of it.";

          reindexRepo(event.getRepository(), true, taskName);

          log.info(logMessage);
        }
      }
    }
    catch (Exception e) {
      log.error("Could not maintain indexing contexts!", e);
    }
  }

  private void reindexRepo(Repository repository, boolean full, String taskName) {
    AbstractNexusRepositoriesPathAwareTask<Object> rt;
    if (full) {
      rt = nexusScheduler.createTaskInstance(RepairIndexTask.class);
    }
    else {
      rt = nexusScheduler.createTaskInstance(UpdateIndexTask.class);
    }

    rt.setRepositoryId(repository.getId());

    nexusScheduler.submit(taskName, rt);
  }

  private String append(String message, String append) {
    if (StringUtils.isBlank(message)) {
      return StringUtils.capitalize(append);
    }
    else {
      return message + ", " + append;
    }
  }
}