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

/**
 * Support class for {@link Cancelable} implementations of {@link Runnable} interfaces. Extends {@link RunnableSupport}
 * by adding cancelation support. To properly detect cancelation, implementor should use {@link #isCanceled()} or
 * {@link #checkInterruption()} (throws {@link RunnableCanceledException} and {@link RunnableInterruptedException}
 * runtime exceptions that you might or might not handle). For code that is buried under layers of third party or
 * legacy
 * code, {@link CancelableUtil#checkInterruption()} should be used, just called on regular basis (ie. within a cycle).
 * Usable with plain Executors too and other {@link Runnable} accepting components, as long as you preserve this
 * instance and cancel it "manually" for example:
 *
 * <pre>
 *   final Executor executor = ...
 *   final RunnableSupport job = new RunnableSupport("foo"){...}
 *   executor.submit(job);
 *   ...
 *   job.cancel();
 * </pre>
 *
 * @author cstamas
 * @since 2.4
 */
public abstract class CancelableRunnableSupport
    extends RunnableSupport
    implements CancelableRunnable
{
  private final CancelableSupport cancelableSupport;

  protected CancelableRunnableSupport(final ProgressListener progressListener, final String name) {
    super(progressListener, name);
    this.cancelableSupport = new CancelableSupport();
  }

  protected void checkInterruption()
      throws RunnableCanceledException, RunnableInterruptedException
  {
    CancelableUtil.checkInterruption(cancelableSupport);
  }

  @Override
  public boolean isCanceled() {
    return cancelableSupport.isCanceled();
  }

  @Override
  public void cancel() {
    cancelableSupport.cancel();
  }

  @Override
  public void run() {
    if (isCanceled()) {
      log.debug("{} canceled before running, bailing out.", getName());
      return;
    }
    final Cancelable oldCancelable = CancelableUtil.getCurrentCancelable();
    try {
      CancelableUtil.setCurrentCancelable(cancelableSupport);
      super.run();
    }
    finally {
      CancelableUtil.setCurrentCancelable(oldCancelable);
    }
  }
}
