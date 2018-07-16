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
  public BrowseNodeEventHandler(final BrowseNodeManager browseNodeManager)
  {
    this.browseNodeManager = checkNotNull(browseNodeManager);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent event) {
    if (shouldProcess(event)) {
      browseNodeManager.createFromAsset(event.getRepositoryName(), event.getAsset());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    if (shouldProcess(event)) {
      browseNodeManager.deleteAssetNode(event.getAssetId());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentDeletedEvent event) {
    if (shouldProcess(event)) {
      browseNodeManager.deleteComponentNode(event.getComponentId());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ConfigurationDeletedEvent event) {
    if (shouldProcess(event)) {
      browseNodeManager.deleteByRepository(event.getRepositoryName());
    }
  }

  private boolean shouldProcess(final EntityEvent event) {
    checkNotNull(event);
    return event.isLocal();
  }
}
