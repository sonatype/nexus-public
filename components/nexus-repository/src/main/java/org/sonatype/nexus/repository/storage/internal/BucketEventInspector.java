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
package org.sonatype.nexus.repository.storage.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryMetadataUpdatedEvent;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Triggers {@link RepositoryMetadataUpdatedEvent}s in response to {@link BucketUpdatedEvent}s.
 *
 * @since 3.22
 */
@Named
@Singleton
public class BucketEventInspector
    extends ComponentSupport
    implements EventAware
{
  private final RepositoryManager repositoryManager;

  private final EventManager eventManager;

  @Inject
  public BucketEventInspector(final RepositoryManager repositoryManager, final EventManager eventManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.eventManager = checkNotNull(eventManager);
  }

  @Subscribe
  public void onBucketUpdated(final BucketUpdatedEvent event) {
    Repository repository = repositoryManager.get(event.getRepositoryName());
    if (repository != null) {
      eventManager.post(new RepositoryMetadataUpdatedEvent(repository));
    }
    else {
      log.debug("Not posting metadata update event for deleted repository {}", event.getRepositoryName());
    }
  }
}
