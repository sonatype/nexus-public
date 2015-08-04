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
package org.sonatype.nexus.testsuite.client;

import org.sonatype.nexus.testsuite.client.exception.EventsAreStillBeingHandledException;
import org.sonatype.sisu.goodies.common.Time;

/**
 * Events Nexus Client Subsystem.
 *
 * @since 2.3
 */
public interface Events
{

  /**
   * Wait Nexus to not handle events for a window of 5 seconds (quiet period). Timeouts after 1 minute.
   *
   * @throws EventsAreStillBeingHandledException
   *          If could not find a window of 10 seconds in defined timeout of 1 minute
   */
  void waitForCalmPeriod()
      throws EventsAreStillBeingHandledException;

  /**
   * Wait Nexus to not handle events for a window of 5 seconds (quiet period). Timeouts after defined time.
   *
   * @param timeout timeout
   * @throws EventsAreStillBeingHandledException
   *          If could not find a window of 10 seconds in defined timeout
   */
  void waitForCalmPeriod(Time timeout)
      throws EventsAreStillBeingHandledException;

  /**
   * Wait Nexus to not handle events for a defined window (quiet period). Timeouts after defined time.
   *
   * @param timeout timeout
   * @param window  of time when no events are posted
   * @throws EventsAreStillBeingHandledException
   *          If could not find a window in defined timeout
   */
  void waitForCalmPeriod(Time timeout, Time window)
      throws EventsAreStillBeingHandledException;

}
