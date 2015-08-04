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

public class ProgressListenerWrapper
    implements ProgressListener
{
  private final ProgressListener wrapped;

  public ProgressListenerWrapper(final ProgressListener wrapped) {
    this.wrapped = wrapped;
  }

  public void beginTask(final String name) {
    TaskUtil.checkInterruption();

    if (wrapped != null) {
      wrapped.beginTask(name);
    }
  }

  public void beginTask(final String name, final int toDo) {
    TaskUtil.checkInterruption();

    if (wrapped != null) {
      wrapped.beginTask(name, toDo);
    }
  }

  public void working(final int workDone) {
    TaskUtil.checkInterruption();

    if (wrapped != null) {
      wrapped.working(workDone);
    }
  }

  public void working(final String message) {
    TaskUtil.checkInterruption();

    if (wrapped != null) {
      wrapped.working(message);
    }
  }

  public void working(final String message, final int work) {
    TaskUtil.checkInterruption();

    if (wrapped != null) {
      wrapped.working(message, work);
    }
  }

  public void endTask(final String message) {
    TaskUtil.checkInterruption();

    if (wrapped != null) {
      wrapped.endTask(message);
    }
  }

  public boolean isCanceled() {
    if (wrapped != null) {
      return wrapped.isCanceled();
    }
    else {
      return false;
    }
  }

  public void cancel() {
    if (wrapped != null) {
      wrapped.cancel();
    }
  }
}
