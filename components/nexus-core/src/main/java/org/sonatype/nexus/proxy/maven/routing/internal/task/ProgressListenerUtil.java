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
 * Collection of static methods allowing to use {@link ProgressListener} and use it in less intrusive way than passing
 * it deep into caller hierarchy.
 *
 * @author cstamas
 * @since 2.4
 */
public class ProgressListenerUtil
{
  /**
   * Thread local variable holding the current {@link ProgressListener} of the given (current) {@link Thread}.
   */
  private static final ThreadLocal<ProgressListener> CURRENT = new ThreadLocal<ProgressListener>()
  {
    @Override
    protected ProgressListener initialValue() {
      return new ProgressListenerWrapper(null);
    }
  };

  /**
   * Static helper class, do not instantiate it.
   */
  private ProgressListenerUtil() {
    // no instances of this please
  }

  /**
   * Protected method that is meant to register current thread's {@link ProgressListener} instance. See
   * {@link RunnableSupport}.
   */
  protected static void setCurrentProgressListener(final ProgressListener progressListener) {
    CURRENT.set(new ProgressListenerWrapper(progressListener));
  }

  // ==

  /**
   * Returns thread's current {@link ProgressListener} instance, never returns {@code null}. Meant to be used by code
   * doing the work and wanting to mark that progress on tasks's {@link ProgressListener}.
   *
   * @return the {@link ProgressListener} instance, never {@code null}.
   */
  public static ProgressListener getCurrentProgressListener() {
    return CURRENT.get();
  }
}
