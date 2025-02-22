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
package org.sonatype.nexus.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionAwareThreadFactory
    extends NexusThreadFactory
{

  private static final Logger log = LoggerFactory.getLogger(ExceptionAwareThreadFactory.class);

  public ExceptionAwareThreadFactory(final String poolId, final String threadGroupName) {
    super(poolId, threadGroupName);
  }

  public ExceptionAwareThreadFactory(final String poolId, final String threadGroupName, final int threadPriority) {
    super(poolId, threadGroupName, threadPriority);
  }

  public ExceptionAwareThreadFactory(
      final String poolId,
      final String threadGroupName,
      final int threadPriority,
      final boolean daemonThread)
  {
    super(poolId, threadGroupName, threadPriority, daemonThread);
  }

  @Override
  public Thread newThread(final Runnable r) {
    Thread tr = super.newThread(r);
    tr.setUncaughtExceptionHandler((t, e) -> {
      log.error("Uncaught Exception occurred on thread: {}, Exception message: {}", t.getName(), e.getMessage());
    });
    return tr;
  }
}
