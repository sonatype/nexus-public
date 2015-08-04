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
package org.sonatype.nexus.proxy.maven.routing.internal.task;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collection of static methods allowing to use {@link Cancelable} and track cancellation or interruption state in
 * less intrusive way than passing it deep into caller hierarchy.
 *
 * @author cstamas
 * @since 2.4
 */
public class CancelableUtil
{
  /**
   * Thread local variable holding the current {@link Cancelable} of the given (current) {@link Thread}.
   */
  private static final ThreadLocal<Cancelable> CURRENT = new ThreadLocal<Cancelable>()
  {
    @Override
    protected Cancelable initialValue() {
      return new CancelableSupport();
    }
  };

  /**
   * Static helper class, do not instantiate it.
   */
  private CancelableUtil() {
    // no instances of this please
  }

  /**
   * Protected method that is meant to register current thread's {@link Cancelable} instance. See
   * {@link CancelableRunnableSupport}.
   */
  protected static void setCurrentCancelable(final Cancelable cancelable) {
    if (cancelable == null) {
      CURRENT.set(new CancelableSupport());
    }
    else {
      CURRENT.set(cancelable);
    }
  }

  /**
   * Returns thread's current {@link Cancelable} instance, never returns {@code null}.
   *
   * @return the {@link Cancelable} instance, never {@code null}.
   */
  protected static Cancelable getCurrentCancelable() {
    return CURRENT.get();
  }

  /**
   * Checks for user cancellation or thread interruption. If canceled, {@link RunnableCanceledException} is thrown
   * that might be caught and handled by caller. Thread interruption causes {@link RunnableInterruptedException} to
   * be
   * thrown. Same applies here. Note that thread interruption flag is cleared after this call.
   */
  protected static void checkInterruption(final Cancelable c)
      throws RunnableCanceledException, RunnableInterruptedException
  {
    final Cancelable cancelable = checkNotNull(c);
    Thread.yield();
    // check proper way of cancelation
    if (cancelable.isCanceled()) {
      throw new RunnableCanceledException("Thread \"" + Thread.currentThread().getName() + "\" is canceled!");
    }
    // check thread interruption, but this should be avoided for it's nastiness
    if (Thread.interrupted()) {
      throw new RunnableInterruptedException("Thread \"" + Thread.currentThread().getName()
          + "\" is interrupted!");
    }
  }

  // ==

  /**
   * Checks for user cancellation or thread interruption. It gets the current {@link Cancelable} using
   * {@link #getCurrentCancelable()} and uses {@link #checkInterruption(Cancelable)} method. This method can and
   * should be called by "implementors" of some long running task on regular basis, to detect thread cancelation in
   * timely fashion (or Thread interruption, but using that should be avoided). This method does same thing as
   * {@link CancelableRunnableSupport#checkInterruption()} does, but while former is usable in "1st level" code, or
   * when instance of it is passed around, this method is usable even in case when between
   * {@link CancelableRunnableSupport} and caller are layers of 3rd party or legacy code even.
   */
  public static void checkInterruption()
      throws RunnableCanceledException, RunnableInterruptedException
  {
    checkInterruption(getCurrentCancelable());
  }
}
