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

import com.google.common.annotations.VisibleForTesting;

/**
 * Event manager.
 *
 * @see EventAware
 * @since 3.0
 */
@SuppressWarnings("deprecation")
public interface EventManager
    extends EventBus
{
  /**
   * Registers an event handler with the event manager.
   *
   * @param handler to be registered
   *
   * @since 3.2
   */
  @Override
  void register(Object handler);

  /**
   * Unregisters an event handler from the event manager.
   *
   * @param handler to be unregistered
   *
   * @since 3.2
   */
  @Override
  void unregister(Object handler);

  /**
   * Posts an event. The event manager will notify all previously registered handlers about this event.
   *
   * @param event an event
   *
   * @since 3.2
   */
  @Override
  void post(Object event);

  /**
   * Used by UTs and ITs only to "wait for calm period" when all async event handlers have finished.
   */
  @VisibleForTesting
  boolean isCalmPeriod();

  /**
   * Is {@link HasAffinity} support enabled?
   *
   * @since 3.11
   */
  boolean isAffinityEnabled();
}
