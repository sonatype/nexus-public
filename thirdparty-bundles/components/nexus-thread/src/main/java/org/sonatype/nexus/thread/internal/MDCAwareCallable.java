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
package org.sonatype.nexus.thread.internal;

import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Callable that properly sets MDC context before invoking the delegate. The delegate will execute in a
 * managed thread with properly set MDC context. To be used with managed threads.
 *
 * @since 2.6
 */
public class MDCAwareCallable<T>
    implements Callable<T>
{
  private final Callable<T> delegate;

  private final Map<String, String> mdcContext;

  public MDCAwareCallable(final Callable<T> delegate) {
    this.delegate = checkNotNull(delegate);
    this.mdcContext = MDCUtils.getCopyOfContextMap();
  }

  @Override
  public T call() throws Exception {
    MDCUtils.setContextMap(mdcContext);
    return delegate.call();
  }
}
