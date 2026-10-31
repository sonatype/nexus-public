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
   * <p>
   * This interface <b>extends</b> {@link EventAware}, so declaring {@code implements EventAware.Asynchronous}
   * alone is sufficient: the component is discovered as an {@link EventAware} subscriber and routed to the
   * asynchronous bus. Declaring both {@code EventAware, EventAware.Asynchronous} is equivalent and harmless.
   *
   * @apiNote Making {@code Asynchronous} extend {@link EventAware} closes the trap behind NEXUS-52911
   *          (June 2026): a class that kept only the {@code Asynchronous} marker after a refactor previously
   *          compiled cleanly but silently skipped registration, and all webhook dispatch went dark for
   *          ~3 weeks (fixed under NEXUS-53667). With the inheritance in place that failure mode is no
   *          longer possible.
   *          <p>
   *          Note that only <b>singleton</b> beans are auto-registered by {@code NexusEventAwareRegistrar};
   *          per-repository (prototype-scoped) components such as repository {@code Facet}s register and
   *          unregister themselves via {@code FacetSupport} as their repository starts and stops.
   */
  interface Asynchronous
      extends EventAware
  {
    // empty
  }
}
