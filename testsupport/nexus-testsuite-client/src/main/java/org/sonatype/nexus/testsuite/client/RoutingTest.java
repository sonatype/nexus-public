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

import org.sonatype.nexus.testsuite.client.exception.RoutingJobsAreStillRunningException;
import org.sonatype.sisu.goodies.common.Time;

/**
 * IT client for automatic routing feature.
 *
 * @author cstamas
 * @since 2.4
 */
public interface RoutingTest
{
  /**
   * Blocks for one minute, or less, if routing update jobs finished earlier.
   */
  void waitForAllRoutingUpdateJobToStop()
      throws RoutingJobsAreStillRunningException;

  /**
   * Blocks for given timeout, or less, if routing update jobs finished earlier.
   */
  void waitForAllRoutingUpdateJobToStop(Time timeout)
      throws RoutingJobsAreStillRunningException;
}
