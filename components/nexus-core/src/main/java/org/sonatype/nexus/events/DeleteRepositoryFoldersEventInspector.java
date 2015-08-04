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
package org.sonatype.nexus.events;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.events.RepositoryRegistryEventPostRemove;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.tasks.DeleteRepositoryFoldersTask;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Spawns a background task to delete repository folders upon removal.
 *
 * @author cstamas
 */
@Named
@Singleton
public class DeleteRepositoryFoldersEventInspector
    extends ComponentSupport
    implements EventSubscriber, Asynchronous
{
  // flag whether removed repositories are moved to global trash or deleted permanently
  private final boolean deletePermanently = SystemPropertiesHelper
      .getBoolean(DeleteRepositoryFoldersEventInspector.class.getName() + ".deletePermanently", false);

  private final NexusScheduler nexusScheduler;

  @Inject
  public DeleteRepositoryFoldersEventInspector(final NexusScheduler nexusScheduler) {
    this.nexusScheduler = nexusScheduler;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryRegistryEventPostRemove evt) {
    Repository repository = evt.getRepository();

    try {
      // remove the storage folders for the repository
      DeleteRepositoryFoldersTask task = nexusScheduler.createTaskInstance(DeleteRepositoryFoldersTask.class);

      task.setRepository(repository);
      task.setDeleteForever(deletePermanently);

      nexusScheduler.submit("Deleting repository folder for repository \"" + repository.getName() + "\" (id="
          + repository.getId() + ").", task);
    }
    catch (Exception e) {
      log.warn("Could not remove repository folders for repository {}", repository, e);
    }
  }
}
