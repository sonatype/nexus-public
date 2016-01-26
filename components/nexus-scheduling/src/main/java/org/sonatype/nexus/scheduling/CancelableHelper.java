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
package org.sonatype.nexus.scheduling;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for per-thread storing of cancelable flag.
 *
 * Periodically checking the {@link #checkCancellation()} is the preferred way to detect cancellation in components
 * outside the tasks. Within task, you have the {@link TaskSupport#isCanceled()} method.
 *
 * @since 3.0
 */
public class CancelableHelper
{
  private CancelableHelper() {
    // empty
  }

  // FIXME: its not clear why we have to install/remove a flag here, vs managing this per-thread.

  private static final ThreadLocal<AtomicBoolean> currentFlagHolder = new ThreadLocal<>();

  public static void set(final AtomicBoolean flag) {
    checkNotNull(flag);
    currentFlagHolder.set(flag);
  }

  public static void remove() {
    currentFlagHolder.remove();
  }

  /**
   * Throws {@link TaskInterruptedException} if current task is canceled or interrupted.
   */
  public static void checkCancellation() {
    Thread.yield();
    AtomicBoolean current = currentFlagHolder.get();
    if (current != null && current.get()) {
      throw new TaskInterruptedException("Thread '" + Thread.currentThread().getName() + "' is canceled", true);
    }
    if (Thread.interrupted()) {
      throw new TaskInterruptedException("Thread '" + Thread.currentThread().getName() + "' is interrupted", false);
    }
  }
}