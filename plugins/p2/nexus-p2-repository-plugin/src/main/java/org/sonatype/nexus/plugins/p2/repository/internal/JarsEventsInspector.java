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

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.plugins.p2.repository.P2MetadataGenerator;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.EagerSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.p2.repository.internal.NexusUtils.retrieveFile;
import static org.sonatype.nexus.plugins.p2.repository.internal.P2ArtifactAnalyzer.getP2Type;

@Named
@EagerSingleton
public class JarsEventsInspector
{

  private static final Logger LOG = LoggerFactory.getLogger(JarsEventsInspector.class);

  private final Provider<P2MetadataGenerator> p2MetadataGenerator;

  @Inject
  public JarsEventsInspector(final Provider<P2MetadataGenerator> p2MetadataGenerator,
                             final EventBus eventBus)
  {
    this.p2MetadataGenerator = checkNotNull(p2MetadataGenerator);
    checkNotNull(eventBus).register(this);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onItemStored(final RepositoryItemEventStore event) {
    if (shouldProcessItem(event.getItem())) {
      p2MetadataGenerator.get().generateP2Metadata(event.getItem());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onItemCached(final RepositoryItemEventCache event) {
    if (shouldProcessItem(event.getItem())) {
      p2MetadataGenerator.get().generateP2Metadata(event.getItem());
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onItemRemoved(final RepositoryItemEventDelete event) {
    if (shouldProcessItem(event.getItem())) {
      p2MetadataGenerator.get().removeP2Metadata(event.getItem());
    }
  }

  // TODO optimize by saving the fact that is a bundle/feature as item attribute and check that one first
  private boolean shouldProcessItem(final StorageItem item) {
    if (item != null && p2MetadataGenerator.get().getConfiguration(item.getRepositoryId()) != null) {
      try {
        final File file = retrieveFile(
            item.getRepositoryItemUid().getRepository(), item.getPath()
        );
        return getP2Type(file) != null;
      }
      catch (final Exception e) {
        LOG.debug(
            "Could not determine if p2 metadata should be generated for '{}'. No metadata will be generated",
            item.getPath(), e
        );
      }
    }
    return false;
  }
}
