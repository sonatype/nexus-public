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

import org.sonatype.nexus.proxy.walker.WalkerThrottleController.ThrottleInfo;

/**
 * A simple single-threaded ThrottleInfo used in Walker implementation.
 *
 * @author cstamas
 * @since 2.0
 */
public class DefaultThrottleInfo
    implements ThrottleInfo
{
  private final long walkStarted;

  private long totalProcessItemSpentMillis;

  private long totalProcessItemInvocationCount;

  private long lastProcessItemEnterTime;

  public DefaultThrottleInfo() {
    this.walkStarted = now();
    this.totalProcessItemSpentMillis = 0;
    this.totalProcessItemInvocationCount = 0;
  }

  protected long now() {
    return System.currentTimeMillis();
  }

  public void enterProcessItem() {
    this.lastProcessItemEnterTime = now();
  }

  public void exitProcessItem() {
    totalProcessItemSpentMillis += now() - lastProcessItemEnterTime;
    totalProcessItemInvocationCount++;
  }

  @Override
  public long getTotalProcessItemInvocationCount() {
    return totalProcessItemInvocationCount;
  }

  @Override
  public long getTotalTimeWalking() {
    return now() - walkStarted;
  }
}
