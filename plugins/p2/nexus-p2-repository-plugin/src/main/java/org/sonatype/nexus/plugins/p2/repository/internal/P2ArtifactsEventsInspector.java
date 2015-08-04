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
package org.sonatype.nexus.plugins.p2.repository.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.plugins.p2.repository.P2RepositoryAggregator;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.item.StorageItem;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.isHidden;

@Named
@Singleton
public class P2ArtifactsEventsInspector implements EventSubscriber
{
  private final Provider<P2RepositoryAggregator> p2RepositoryAggregator;

  @Inject
  public P2ArtifactsEventsInspector(final Provider<P2RepositoryAggregator> p2RepositoryAggregator)
  {
    this.p2RepositoryAggregator = checkNotNull(p2RepositoryAggregator);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onItemStored(final RepositoryItemEventStore event) {
    if (isP2ArtifactsXML(event.getItem())) {
      p2RepositoryAggregator.get().updateP2Artifacts(event.getItem());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onItemCached(final RepositoryItemEventCache event) {
    if (isP2ArtifactsXML(event.getItem())) {
      p2RepositoryAggregator.get().updateP2Artifacts(event.getItem());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onItemRemoved(final RepositoryItemEventDelete event) {
    if (isP2ArtifactsXML(event.getItem())) {
      p2RepositoryAggregator.get().removeP2Artifacts(event.getItem());
    }
  }

  private static boolean isP2ArtifactsXML(final StorageItem item) {
    if (item == null) {
      return false;
    }
    return !isHidden(item.getPath()) && isP2ArtifactsXML(item.getPath());
  }

  static boolean isP2ArtifactsXML(final String path) {
    if (path == null) {
      return false;
    }
    return path.endsWith("p2Artifacts.xml");
  }

}
