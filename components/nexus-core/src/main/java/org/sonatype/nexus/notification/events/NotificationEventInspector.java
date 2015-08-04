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
package org.sonatype.nexus.notification.events;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.notification.NotificationManager;
import org.sonatype.nexus.notification.NotificationRequest;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A "bridge" that funnels events into notifications using the event to notification router.
 *
 * @author cstamas
 */
@Named
@Singleton
public class NotificationEventInspector
    implements EventSubscriber
{
  private final NotificationEventRouter notificationEventRouter;

  private final NotificationManager notificationManager;

  @Inject
  public NotificationEventInspector(final NotificationEventRouter notificationEventRouter,
                                    final NotificationManager notificationManager)
  {
    this.notificationEventRouter = checkNotNull(notificationEventRouter);
    this.notificationManager = checkNotNull(notificationManager);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(Event<?> evt) {
    if (!notificationManager.isEnabled()) {
      return;
    }
    final NotificationRequest route = notificationEventRouter.getRequestForEvent(evt);
    if (route != null && !route.isEmpty()) {
      notificationManager.notifyTargets(route);
    }
  }
}
