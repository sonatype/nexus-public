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
package org.sonatype.nexus.proxy.walker;

/**
 * Interface to control throttling (if desired) of Walk executed within context that this controller resides.
 *
 * @author cstamas
 * @since 2.0
 */
public interface WalkerThrottleController
{
  /**
   * Key used in RequestContext to pass it into Walker.
   */
  String CONTEXT_KEY = WalkerThrottleController.class.getName();

  /**
   * WalkerThrottleController that does not do walk throttling. Used when no user supplied throttle control found in
   * walker context.
   */
  WalkerThrottleController NO_THROTTLING = new AbstractWalkerThrottleController()
  {
  };

  /**
   * Carries some "stats" about walk, that makes possible to perform some calculation about it's speed.
   *
   * @author cstamas
   */
  public interface ThrottleInfo
  {
    /**
     * The total invocation count of processItem() method ("How many items were processed so far?").
     */
    long getTotalProcessItemInvocationCount();

    /**
     * The total time (in milliseconds) since walking begun. The returned time always reflects the truly actual
     * spent time in walking, counting time even spent with throttle calculations ("gross time" spent).
     */
    long getTotalTimeWalking();
  }

  /**
   * Invoked at walk start.
   */
  void walkStarted(WalkerContext context);

  /**
   * Invoked at walk end.
   */
  void walkEnded(WalkerContext context, ThrottleInfo info);

  /**
   * Returns true if the controllers wants to use "throttled walker" execution. Throttling in this case would mean
   * intentionally slowing down the "walk" by inserting some amount (see {@link #throttleTime(long)} method) of
   * Thread.sleep() invocations.
   */
  boolean isThrottled();

  /**
   * Returns the next desired sleep time this context wants to have applied. It might be in some relation to the time
   * spent in processItem() methods of registered WalkerProcessors, but does not have to be.
   *
   * @param info The info object holding some (probably) used informations to calculate next desired sleep time.
   * @return any value bigger than zero means "sleep as many millis to throttle". Any values less or equal to zero
   *         are
   *         neglected (will not invoke sleep).
   */
  long throttleTime(ThrottleInfo info);
}
