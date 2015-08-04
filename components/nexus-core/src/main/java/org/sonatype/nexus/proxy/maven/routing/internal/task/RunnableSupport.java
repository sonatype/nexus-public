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

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Support class for implementations of {@link Runnable} interfaces. This class just adds support for
 * {@link ProgressListener} and names the runnable, but does not add cancelation support. Usable with plain Executors
 * too and other {@link Runnable} accepting components.
 *
 * @author cstamas
 * @since 2.4
 */
public abstract class RunnableSupport
    extends ComponentSupport
    implements Runnable
{
  private final ProgressListenerWrapper progressListenerWrapper;

  private final String name;

  protected RunnableSupport(final ProgressListener progressListener, final String name) {
    checkArgument(name != null && name.trim().length() > 0);
    this.progressListenerWrapper = new ProgressListenerWrapper(progressListener);
    this.name = name;
  }

  protected String getName() {
    return name;
  }

  protected ProgressListener getProgressListener() {
    return progressListenerWrapper;
  }

  @Override
  public void run() {
    final ProgressListener oldProgressListener = ProgressListenerUtil.getCurrentProgressListener();
    try {
      ProgressListenerUtil.setCurrentProgressListener(getProgressListener());
      log.debug("{} running...", getName());
      doRun();
      log.debug("{} done...", getName());
    }
    catch (InterruptedException | RunnableInterruptedException e) {
      log.info("{} interrupted: {}", getName(), e.getMessage());
    }
    catch (RunnableCanceledException e) {
      log.info("{} canceled: {}", getName(), e.getMessage());
    }
    catch (Exception e) {
      log.warn("{} failed:", getName(), e);
      throw Throwables.propagate(e);
    }
    finally {
      ProgressListenerUtil.setCurrentProgressListener(oldProgressListener);
    }
  }

  protected abstract void doRun()
      throws Exception;
}
