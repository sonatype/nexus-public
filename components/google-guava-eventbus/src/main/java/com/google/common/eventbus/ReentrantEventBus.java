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
package com.google.common.eventbus;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A Guava {@link EventBus} that differs from default one by dispatching events as they appear (is re-entrant).
 * Guava will queue up all event and dispatch them in the order they were posted, without re-entrance.
 *
 * @since 3.0
 */
public class ReentrantEventBus
    extends EventBus
{
  /**
   * Queues of events for the current thread to dispatch.
   */
  private final ThreadLocal<Queue<EventWithSubscriber>> eventsToDispatch =
      new ThreadLocal<Queue<EventWithSubscriber>>()
      {
        @Override
        protected Queue<EventWithSubscriber> initialValue() {
          return new LinkedList<>();
        }
      };

  public ReentrantEventBus() {
    // empty
  }

  public ReentrantEventBus(final String identifier) {
    super(identifier);
  }

  public ReentrantEventBus(final SubscriberExceptionHandler subscriberExceptionHandler) {
    super(subscriberExceptionHandler);
  }

  @Override
  void enqueueEvent(Object event, EventSubscriber subscriber) {
    eventsToDispatch.get().offer(new EventWithSubscriber(event, subscriber));
  }

  @Override
  void dispatchQueuedEvents() {
    Queue<EventWithSubscriber> events = eventsToDispatch.get();
    eventsToDispatch.remove();
    EventWithSubscriber eventWithSubscriber;
    while ((eventWithSubscriber = events.poll()) != null) {
      dispatch(eventWithSubscriber.event, eventWithSubscriber.subscriber);
    }
  }
}
