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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.ReentrantEventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EventBus} implementation using Guava {@link com.google.common.eventbus.EventBus}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class EventBusImpl
    extends ComponentSupport
    implements EventBus
{
  private final com.google.common.eventbus.EventBus delegate;

  @Inject
  public EventBusImpl(@Named("${guava.eventBus:-reentrant}") final String type) {
    log.debug("Type: {}", type);
    this.delegate = createEventBus(type);
    log.info("Delegate: {}", delegate);
  }

  private static com.google.common.eventbus.EventBus createEventBus(final String type) {
    checkNotNull(type);

    switch (type) {
      case "standard":
        return new com.google.common.eventbus.EventBus(new Slf4jSubscriberExceptionHandler(type));

      case "reentrant":
        return new ReentrantEventBus(new Slf4jSubscriberExceptionHandler(type));

      default:
        throw new RuntimeException("Invalid EventBus type: " + type);
    }
  }

  @VisibleForTesting
  public EventBusImpl(final com.google.common.eventbus.EventBus delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public void register(final Object handler) {
    delegate.register(handler);
    log.trace("Registered handler: {}", handler);
  }

  @Override
  public void unregister(final Object handler) {
    delegate.unregister(handler);
    log.trace("Unregistered handler: {}", handler);
  }

  @Override
  public void post(final Object event) {
    log.trace("Event '{}' fired", event);
    delegate.post(event);
  }
}
