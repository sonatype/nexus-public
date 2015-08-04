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
import org.sonatype.nexus.util.ConstantNumberSequence;
import org.sonatype.nexus.util.FibonacciNumberSequence;
import org.sonatype.nexus.util.NumberSequence;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test for fixed rate walker throttle controller.
 *
 * @author cstamas
 */
public class FixedRateWalkerThrottleControllerTest
    extends TestSupport
{
  protected FixedRateWalkerThrottleController fixedRateWalkerThrottleController;

  @Test
  public void testDoesItHelpAtAll() {
    final int measuredTpsHigh = performAndMeasureActualTps(20, new ConstantNumberSequence(1));
    final int measuredTpsMid = performAndMeasureActualTps(10, new ConstantNumberSequence(1));
    final int measuredTpsLow = performAndMeasureActualTps(5, new ConstantNumberSequence(1));

    System.err.println(measuredTpsHigh + "\n" + measuredTpsMid + "\n" + measuredTpsLow);
    assertThat("Mid TPS should be less than high TPS run", measuredTpsMid, lessThan(measuredTpsHigh));
    assertThat("Low TPS should be less than medium TPS run", measuredTpsLow, lessThan(measuredTpsMid));
  }

  // ==

  protected int performAndMeasureActualTps(final int wantedTps, final NumberSequence loadChange) {
    fixedRateWalkerThrottleController =
        new FixedRateWalkerThrottleController(wantedTps, new FibonacciNumberSequence(1));
    fixedRateWalkerThrottleController.setSliceSize(1);

    final TestThrottleInfo info = new TestThrottleInfo();
    final WalkerContext context = Mockito.mock(WalkerContext.class);
    // sleeptime starts oscillating after the 10th iteration, give some extra iterations to bring down the average
    final int iterationCount = 25;
    final long startTime = System.currentTimeMillis();
    fixedRateWalkerThrottleController.walkStarted(context);
    for (int i = 0; i < iterationCount; i++) {
      info.simulateInvocation(loadChange.next());
      long sleepTime = fixedRateWalkerThrottleController.throttleTime(info);
      sleep(sleepTime); // sleep as much as throttle controller says to sleep
    }
    fixedRateWalkerThrottleController.walkEnded(context, info);

    final int measuredTps =
        fixedRateWalkerThrottleController.calculateCPS(iterationCount, System.currentTimeMillis() - startTime);

    // System.out.println( "MeasuredTps=" + measuredTps );
    // System.out.println( "lastSliceTps=" + fixedRateWalkerThrottleController.getLastSliceTps() );
    // System.out.println( "GlobalAvgTps=" + fixedRateWalkerThrottleController.getGlobalAverageTps() );
    // System.out.println( "GlobalMaxTps=" + fixedRateWalkerThrottleController.getGlobalMaximumTps() );

    return measuredTps;
  }

  // ==

  protected static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException e) {
      // need to kill test too
      throw new RuntimeException(e);
    }
  }

  protected static class TestThrottleInfo
      implements ThrottleInfo
  {
    private final long started;

    private long totalProcessItemSpentMillis;

    private long totalProcessItemInvocationCount;

    public TestThrottleInfo() {
      this.started = System.currentTimeMillis();
      this.totalProcessItemSpentMillis = 0;
      this.totalProcessItemInvocationCount = 0;
    }

    public void simulateInvocation(final long spentTimeInProcessItem) {
      // we need to sleep to keep getTotalTimeWalking() and totalProcessItemSpentMillis aligned
      sleep(spentTimeInProcessItem);
      totalProcessItemSpentMillis = totalProcessItemSpentMillis + spentTimeInProcessItem;
      totalProcessItemInvocationCount++;
    }

    @Override
    public long getTotalProcessItemInvocationCount() {
      return totalProcessItemInvocationCount;
    }

    @Override
    public long getTotalTimeWalking() {
      return System.currentTimeMillis() - started;
    }
  }
}
