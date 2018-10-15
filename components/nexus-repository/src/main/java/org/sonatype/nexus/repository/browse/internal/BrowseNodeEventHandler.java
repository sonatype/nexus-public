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
package org.sonatype.nexus.repository.browse.internal;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.config.internal.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.ReplicationModeOverrides.clearReplicationModeOverrides;
import static org.sonatype.nexus.orient.ReplicationModeOverrides.dontWaitForReplicationResults;

/**
 * Listens to any events that require managing folder data and calls the format-specific handler.
 *
 * @since 3.6
 */
@Singleton
@Named
public class BrowseNodeEventHandler
    implements EventAware, EventAware.Asynchronous
{
  private final BrowseNodeManager browseNodeManager;

  @Inject
  public BrowseNodeEventHandler(final BrowseNodeManager browseNodeManager) {
    this.browseNodeManager = checkNotNull(browseNodeManager);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent event) {
    handle(event, e -> browseNodeManager.createFromAsset(e.getRepositoryName(), e.getAsset()));
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    handle(event, e -> browseNodeManager.deleteAssetNode(e.getAssetId()));
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentDeletedEvent event) {
    handle(event, e -> browseNodeManager.deleteComponentNode(e.getComponentId()));
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ConfigurationDeletedEvent event) {
    handle(event, e -> browseNodeManager.deleteByRepository(e.getRepositoryName()));
  }

  private <E extends EntityEvent> void handle(final E event, final Consumer<E> consumer) {
    checkNotNull(event);
    if (event.isLocal()) {
      dontWaitForReplicationResults();
      try {
        consumer.accept(event);
      }
      finally {
        clearReplicationModeOverrides();
      }
    }
  }
}
