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
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Slf4j logging {@link SubscriberExceptionHandler}.
 *
 * @since 3.0
 */
final class Slf4jSubscriberExceptionHandler
    implements SubscriberExceptionHandler
{
  private final Logger logger;

  public Slf4jSubscriberExceptionHandler(final String identifier) {
    checkNotNull(identifier);
    this.logger = LoggerFactory.getLogger(EventBus.class.getName() + "." + identifier);
  }

  @Override
  public void handleException(final Throwable exception, final SubscriberExceptionContext context) {
    logger.error("Could not dispatch event {} to subscriber {} method [{}]",
        context.getEvent(), context.getSubscriber(), context.getSubscriberMethod(), exception);
  }
}
