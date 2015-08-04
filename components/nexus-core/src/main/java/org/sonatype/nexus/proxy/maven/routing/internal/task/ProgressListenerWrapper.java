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
 * {@link ProgressListener} implementation that wraps another {@link ProgressListener} within. The wrapped
 * {@link ProgressListener} might be {@code null} too, in which case this instance would simply do nothing. The "extra"
 * this class adds, is checking interruption on method calls.
 *
 * @author cstamas
 * @since 2.4
 */
public class ProgressListenerWrapper
    implements ProgressListener
{
  private final ProgressListener wrapped;

  /**
   * Constructor.
   *
   * @param wrapped the {@link ProgressListener} instance to wrap or {@code null}.
   */
  public ProgressListenerWrapper(final ProgressListener wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public void beginTask(final String name, final int workAmount) {
    CancelableUtil.checkInterruption();
    if (wrapped != null) {
      wrapped.beginTask(name, workAmount);
    }
  }

  @Override
  public void working(final String message, final int workAmountDelta) {
    CancelableUtil.checkInterruption();
    if (wrapped != null) {
      wrapped.working(message, workAmountDelta);
    }
  }

  @Override
  public void endTask(final String message) {
    CancelableUtil.checkInterruption();
    if (wrapped != null) {
      wrapped.endTask(message);
    }
  }
}
