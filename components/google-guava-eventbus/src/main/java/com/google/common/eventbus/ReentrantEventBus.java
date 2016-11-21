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

import static com.google.common.eventbus.Dispatcher.immediate;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

/**
 * A Guava {@link EventBus} that differs from default one by dispatching events as they appear (is re-entrant).
 * Guava will queue up all event and dispatch them in the order they were posted, without re-entrance.
 *
 * @since 3.0
 */
public class ReentrantEventBus
    extends EventBus
{
  public ReentrantEventBus() {
    this(LoggingHandler.INSTANCE);
  }

  public ReentrantEventBus(final String identifier) {
    this(identifier, LoggingHandler.INSTANCE);
  }

  public ReentrantEventBus(final SubscriberExceptionHandler exceptionHandler) {
    this("reentrant", exceptionHandler);
  }

  /**
   * @since 3.2
   */
  protected ReentrantEventBus(final String identifier, final SubscriberExceptionHandler exceptionHandler) {
    super(identifier, directExecutor(), immediate(), exceptionHandler);
  }
}
