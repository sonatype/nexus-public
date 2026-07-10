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
package org.sonatype.nexus.common.event;

import com.google.common.eventbus.EventBus;

/**
 * Marker to indicate that component should be registered and unregistered with the synchronous {@link EventBus}.
 *
 * @since 3.0
 */
public interface EventAware
{
  /**
   * Marker for {@link EventAware} component to register and unregister with the asynchronous {@link EventBus}.
   *
   * @apiNote This nested interface does <b>not</b> extend {@link EventAware}. To be auto-registered
   *          as an event subscriber, a component <b>must</b> declare both markers explicitly:
   *          {@code implements EventAware, EventAware.Asynchronous}. Declaring {@code Asynchronous} alone
   *          compiles cleanly but silently skips registration — both
   *          {@code EventManagerImpl}'s {@code List<EventAware>} injection and
   *          {@code EventAwareBeanPostProcessor} key on {@code bean instanceof EventAware}. NEXUS-52911
   *          shipped that exact regression in June 2026 (all webhook dispatch went dark for ~3 weeks);
   *          NEXUS-53667 corrected it and pinned it with a test. Keep both markers on any async event
   *          subscriber.
   */
  interface Asynchronous
  {
    // empty
  }
}
