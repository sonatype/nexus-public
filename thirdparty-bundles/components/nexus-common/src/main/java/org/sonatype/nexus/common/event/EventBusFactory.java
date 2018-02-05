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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

/**
 * Factory to create custom {@link EventBus} instances with behaviour not exposed via the public API.
 *
 * @since 3.2.1
 */
public class EventBusFactory
{
  private EventBusFactory() {
    // empty
  }

  /**
   * Creates a reentrant {@link EventBus} that dispatches events immediately as they appear on the same thread.
   *
   * (The old Guava behaviour used thread-local queues to provide strong non-reentrant ordering.)
   */
  public static EventBus reentrantEventBus(final String name) {
    return newEventBus(name, directExecutor());
  }

  /**
   * Creates a reentrant {@link EventBus} that dispatches events immediately as they appear using the executor.
   *
   * (The old Guava behaviour used a global queue to provide weak non-reentrant ordering before async dispatch.)
   */
  public static EventBus reentrantAsyncEventBus(final String name, final Executor executor) {
    return newEventBus(name, executor);
  }

  private static EventBus newEventBus(final String name, final Executor executor) {
    try {
      Class<?> dispatcherClass = EventBus.class.getClassLoader().loadClass("com.google.common.eventbus.Dispatcher");

      // immediate dispatcher means events are always processed in a reentrant fashion
      Method immediateDispatcherMethod = dispatcherClass.getDeclaredMethod("immediate");
      immediateDispatcherMethod.setAccessible(true);

      // EventBus constructor that accepts custom executor is not yet part of the public API
      Constructor<EventBus> eventBusConstructor = EventBus.class.getDeclaredConstructor(
          String.class, Executor.class, dispatcherClass, SubscriberExceptionHandler.class);
      eventBusConstructor.setAccessible(true);

      Object immediateDispatcher = immediateDispatcherMethod.invoke(null);
      SubscriberExceptionHandler exceptionHandler = new Slf4jSubscriberExceptionHandler(name);

      return eventBusConstructor.newInstance(name, executor, immediateDispatcher, exceptionHandler);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new LinkageError("Unable to create EventBus with custom executor", e);
    }
  }
}
