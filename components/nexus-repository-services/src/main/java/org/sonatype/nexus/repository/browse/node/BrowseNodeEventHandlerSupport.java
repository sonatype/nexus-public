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
package org.sonatype.nexus.repository.browse.node;

import java.util.concurrent.atomic.AtomicBoolean;

import org.sonatype.goodies.lifecycle.LifecycleSupport;

/**
 * Manages common tasks for the BrowseNodeEventHandler
 * @since 3.35
 */
public abstract class BrowseNodeEventHandlerSupport
    extends LifecycleSupport
    implements BrowseNodeEventHandler
{
  private final AtomicBoolean pauseHandling = new AtomicBoolean(false);

  @Override
  public boolean shouldHandle() {
    return !pauseHandling.get();
  }

  @Override
  public void pauseEventProcessing() {
    log.info("BrowseNode event processing has been paused");
    pauseHandling.set(true);
  }

  @Override
  public void resumeEventProcessing() {
    log.info("BrowseNode event processing has been resumed");
    pauseHandling.set(false);
  }
}
