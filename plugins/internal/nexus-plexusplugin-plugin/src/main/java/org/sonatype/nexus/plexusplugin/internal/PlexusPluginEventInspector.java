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
package org.sonatype.nexus.plexusplugin.internal;

import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.plexusplugin.PlexusPlugin;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Simple event inspector that routes all the events to {@link PlexusPlugin}.
 */
@Component(role = EventSubscriber.class, hint = "plexusplugin")
public class PlexusPluginEventInspector
    implements EventSubscriber
{
  @Requirement
  private PlexusPlugin plexusPlugin;

  @Subscribe
  @AllowConcurrentEvents
  public void on(Event<?> evt) {
    plexusPlugin.newEventReceived(evt);
  }
}
