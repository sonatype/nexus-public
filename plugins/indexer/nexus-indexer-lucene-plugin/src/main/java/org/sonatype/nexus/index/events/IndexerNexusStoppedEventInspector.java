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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.Subscribe;

/**
 * Catches Nexus shutdown event and cleanly stops the IndexManager
 *
 * @author bdemers
 */
@Named
@Singleton
public class IndexerNexusStoppedEventInspector
    extends ComponentSupport
    implements EventSubscriber
{
  private final IndexerManager indexerManager;

  @Inject
  public IndexerNexusStoppedEventInspector(final IndexerManager indexerManager) {
    this.indexerManager = indexerManager;
  }

  protected IndexerManager getIndexerManager() {
    return indexerManager;
  }

  @Subscribe
  public void inspect(final NexusStoppedEvent evt) {
    try {
      indexerManager.shutdown(false);
    }
    catch (IOException e) {
      log.error("Error while stopping IndexerManager:", e);
    }
  }
}
