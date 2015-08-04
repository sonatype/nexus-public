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
package org.sonatype.nexus.feeds.record;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Boot listening event inspector. This one is intentionally not async, to mark exact time stamps of Nexus important
 * events: when it booted and when shutdown was commenced.
 *
 * @author cstamas
 */
@Named
@Singleton
public class NexusBootEventInspector
    extends AbstractFeedRecorderEventInspector
    implements EventSubscriber
{
  @Subscribe
  public void on(final NexusStartedEvent e) {
    inspect(e);
  }

  @Subscribe
  public void on(final NexusStoppedEvent e) {
    inspect(e);
  }

  protected void inspect(Event<?> evt) {
    if (evt instanceof NexusStartedEvent) {
      getFeedRecorder().addSystemEvent(
          FeedRecorder.SYSTEM_BOOT_ACTION,
          "Started Nexus (version " + getApplicationStatusSource().getSystemStatus().getVersion() + " "
              + getApplicationStatusSource().getSystemStatus().getEditionShort() + ")");
    }
    else if (evt instanceof NexusStoppedEvent) {
      getFeedRecorder().addSystemEvent(
          FeedRecorder.SYSTEM_BOOT_ACTION,
          "Stopping Nexus (version " + getApplicationStatusSource().getSystemStatus().getVersion() + " "
              + getApplicationStatusSource().getSystemStatus().getEditionShort() + ")");
      getFeedRecorder().shutdown();
    }
  }

}
