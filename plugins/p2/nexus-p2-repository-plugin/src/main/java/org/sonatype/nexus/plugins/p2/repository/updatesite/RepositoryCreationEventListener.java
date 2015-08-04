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
package org.sonatype.nexus.plugins.p2.repository.updatesite;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.plugins.p2.repository.UpdateSiteProxyRepository;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class RepositoryCreationEventListener
    extends ComponentSupport
    implements EventSubscriber
{
  private final ApplicationStatusSource applicationStatusSource;

  private final NexusScheduler scheduler;

  @Inject
  public RepositoryCreationEventListener(final ApplicationStatusSource applicationStatusSource,
                                         final NexusScheduler scheduler)
  {
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
    this.scheduler = checkNotNull(scheduler);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryRegistryEventAdd evt) {
    if (!applicationStatusSource.getSystemStatus().isNexusStarted()) {
      return;
    }
    final UpdateSiteProxyRepository updateSite =
        ((RepositoryRegistryEventAdd) evt).getRepository().adaptToFacet(UpdateSiteProxyRepository.class);

    if (updateSite != null) {
      updateSite.setExposed(false);
      final ScheduledTask<?> mirrorTask = UpdateSiteMirrorTask.submit(scheduler, updateSite, true);
      log.debug("Submitted " + mirrorTask.getName());
    }
  }
}
