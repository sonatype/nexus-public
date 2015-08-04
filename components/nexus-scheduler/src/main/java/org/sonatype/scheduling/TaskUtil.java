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
package org.sonatype.scheduling;

public class TaskUtil
{
  private static final ThreadLocal<ProgressListener> CURRENT = new ThreadLocal<ProgressListener>()
  {
    protected ProgressListener initialValue() {
      return new ProgressListenerWrapper(null);
    }
  };

  protected static void setCurrent(final ProgressListener progressListener) {
    if (progressListener != null) {
      CURRENT.set(new CancellableProgressListenerWrapper(progressListener));
    }
    else {
      CURRENT.set(new ProgressListenerWrapper(null));
    }
  }

  /**
   * Returns current {@link ProgressListener} instance, never returns null.
   */
  public static ProgressListener getCurrentProgressListener() {
    return CURRENT.get();
  }

  /**
   * Checks for user cancellation or thread interruption. In any of those both cases, {@link
   * TaskInterruptedException}
   * is thrown that might be caught and handled by caller. If not handled, thread will die-off. If handled, caller
   * must ensure and handle interrupt flag of current thread.
   */
  public static void checkInterruption()
      throws TaskInterruptedException
  {
    Thread.yield();

    if (getCurrentProgressListener().isCanceled()) {
      throw new TaskInterruptedException("Thread \"" + Thread.currentThread().getName() + "\" is canceled!",
          true);
    }

    if (Thread.interrupted()) {
      throw new TaskInterruptedException("Thread \"" + Thread.currentThread().getName() + "\" is interrupted!",
          false);
    }
  }
}
