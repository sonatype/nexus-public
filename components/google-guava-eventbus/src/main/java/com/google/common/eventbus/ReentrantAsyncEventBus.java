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

import java.util.concurrent.Executor;

import static com.google.common.eventbus.Dispatcher.immediate;

/**
 * Reentrant asynchronous {@link EventBus} that dispatches events immediately as they appear using the executor.
 *
 * (The old Guava behaviour used a global queue to provide weak non-reentrant ordering before async dispatch.)
 *
 * @since 3.2
 */
public class ReentrantAsyncEventBus
    extends EventBus
{
  private static final String IDENTIFIER = "reentrant.async";

  public ReentrantAsyncEventBus(final Executor executor) {
    super(IDENTIFIER, executor, immediate(), new Slf4jSubscriberExceptionHandler(IDENTIFIER));
  }
}
