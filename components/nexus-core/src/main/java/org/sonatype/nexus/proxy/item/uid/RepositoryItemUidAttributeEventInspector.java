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
package org.sonatype.nexus.proxy.item.uid;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.plugins.events.PluginActivatedEvent;
import org.sonatype.nexus.proxy.events.NexusInitializedEvent;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Named
@Singleton
public class RepositoryItemUidAttributeEventInspector
    extends ComponentSupport
    implements EventSubscriber
{
  private final RepositoryItemUidAttributeManager manager;

  @Inject
  public RepositoryItemUidAttributeEventInspector(final RepositoryItemUidAttributeManager manager) {
    this.manager = manager;
  }

  @Subscribe
  public void inspect(final NexusInitializedEvent evt) {
    manager.reset();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final PluginActivatedEvent evt) {
    manager.reset();
  }
}
