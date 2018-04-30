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
package org.sonatype.nexus.testcommon.event;

import org.sonatype.nexus.common.event.EventManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;

import static org.sonatype.nexus.common.event.EventBusFactory.reentrantEventBus;

/**
 * Simple {@link EventManager} for UT purposes. Requires manual registration, doesn't support asynchronous dispatch.
 *
 * @since 3.2
 */
@VisibleForTesting
public class SimpleEventManager
    implements EventManager
{
  private final EventBus eventBus = reentrantEventBus("simple");

  @Override
  public void register(final Object handler) {
    eventBus.register(handler);
  }

  @Override
  public void unregister(final Object handler) {
    eventBus.unregister(handler);
  }

  @Override
  public void post(final Object event) {
    eventBus.post(event);
  }

  @Override
  public boolean isCalmPeriod() {
    return true;
  }

  @Override
  public boolean isAffinityEnabled() {
    return false;
  }
}
