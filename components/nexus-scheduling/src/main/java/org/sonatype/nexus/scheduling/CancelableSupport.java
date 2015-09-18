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

/**
 * Support for per-thread storing of cancelable flag. Periodically checking the {@link #checkCancellation()} is the
 * preferred way to detect cancellation in components outside the tasks. Within task, you have the {@link
 * TaskSupport#isCanceled()} method.
 *
 * @since 3.0
 */
public class CancelableSupport
{
  public static class CancelableFlagHolder
  {
    private boolean canceled = false;

    public void cancel() {
      canceled = true;
    }

    public boolean isCanceled() {
      return canceled;
    }
  }

  private CancelableSupport() {
    // no instances of this please
  }

  private static final ThreadLocal<CancelableFlagHolder> CURRENT_CANCEL_STATE = new ThreadLocal<>();

  public static void setCurrent(CancelableFlagHolder holder) {
    if (holder == null) {
      CURRENT_CANCEL_STATE.remove();
    }
    else {
      CURRENT_CANCEL_STATE.set(holder);
    }
  }

  public static CancelableFlagHolder getCurrent() {
    return CURRENT_CANCEL_STATE.get();
  }

  /**
   * Checks for user cancellation or thread interruption. In any of those both cases, {@link
   * TaskInterruptedException} is thrown that might be caught and handled by caller. If not handled, thread will
   * die-off. If handled, caller must ensure and handle interrupt flag of current thread. This method will throw
   * only once the exception, in case of multiple invocation, only the first call will result in exception,
   * the subsequent calls will cleanly return.
   */
  public static void checkCancellation()
      throws TaskInterruptedException
  {
    Thread.yield();
    final CancelableFlagHolder current = getCurrent();
    if (current != null && current.isCanceled()) {
      throw new TaskInterruptedException("Thread \"" + Thread.currentThread().getName() + "\" is canceled!",
          true);
    }
    if (Thread.interrupted()) {
      throw new TaskInterruptedException("Thread \"" + Thread.currentThread().getName() + "\" is interrupted!",
          false);
    }
  }
}