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
package org.sonatype.nexus.common.io;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Manages cooperation between multiple threads to reduce duplicated I/O requests.
 *
 * Only one thread is allowed to proceed for a given I/O request key; other threads will wait for it to complete
 * and share the result. If an exception occurs on the lead thread any waiting threads share the same exception.
 *
 * If the original thread takes too long then one of the waiting threads may be woken up to repeat the request.
 * Further threads may be woken up if that thread takes too long, with each wakeup staggered by the same timeout.
 *
 * @since 3.14
 */
public interface Cooperation
{
  @FunctionalInterface
  interface IOCheck<T>
  {
    @Nullable
    T check() throws IOException;
  }

  @FunctionalInterface
  interface IOCall<T>
  {
    /**
     * @param failover {@code true} if this is a 'fail over' thread that
     *          might want to check caches before repeating the request
     */
    T call(boolean failover) throws IOException;
  }

  /**
   * Requests cooperation before proceeding with the given I/O request.
   *
   * @param requestKey used to match I/O requests for cooperation purposes
   * @param request function that performs some I/O and returns the result
   *
   * @throws IOException when the request fails due to I/O issues
   * @throws CooperationException when the current thread cannot cooperate
   */
  <T> T cooperate(String requestKey, IOCall<T> request) throws IOException;

  /**
   * Requests to join with any cached cooperation results when failing over.
   * If no cached results are found the underlying cooperation may choose to
   * wait and try again, for example to account for lag in distributed setups.
   *
   * @param request function that tries to retrieve cached cooperation results
   * @return {@code null} if no cached results were found
   *
   * @throws IOException when the request fails due to I/O issues
   */
  @Nullable
  <T> T join(IOCheck<T> request) throws IOException;

  /**
   * @return number of threads cooperating per request-key.
   */
  Map<String, Integer> getThreadCountPerKey();
}
