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
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;

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
  private final boolean enabled;

  private final BrowseNodeWrapper browseNodeWrapper;

  private final BrowseNodeStore browseNodeStore;

  @Inject
  public BrowseNodeEventHandler(final BrowseNodeConfiguration configuration,
                                final BrowseNodeWrapper browseNodeWrapper,
                                final BrowseNodeStore browseNodeStore)
  {
    this.enabled = checkNotNull(configuration).isEnabled();
    this.browseNodeWrapper = checkNotNull(browseNodeWrapper);
    this.browseNodeStore = checkNotNull(browseNodeStore);
  }

  @Subscribe
  public void on(final AssetCreatedEvent event) {
    if (shouldProcess(event)) {
      browseNodeWrapper.createFromAsset(event.getRepositoryName(), event.getAsset());
    }
  }

  /**
   * Handles the AssetDeletedEvent. This implementation will delete the asset path entry,
   * and any parent path entries that will no longer have children.
   */
  @Subscribe
  public void on(final AssetDeletedEvent event) {
    if (shouldProcess(event)) {
      browseNodeStore.deleteNodeByAssetId(event.getAssetId());
    }
  }

  private boolean shouldProcess(final EntityEvent event) {
    checkNotNull(event);
    return enabled && event.isLocal();
  }
}
