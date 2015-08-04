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

import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.util.NumberSequence;

import com.google.common.base.Preconditions;

/**
 * Fixed rate WalkerThrottleController ensures that a walk will not advance FASTER than the limit TPS set. Note: this
 * controller is "dumb", if for any reason (ie. complex operations in processItem() method, system under load or simply
 * slow hardware it runs on) the limit TPS is not even reached, this controller will NOT interfere (slow down the walk)
 * but the application will still probably bashing the underlying OS/System resources or will fight with other threads
 * for CPU!
 *
 * @author cstamas
 * @since 2.0
 */
public class FixedRateWalkerThrottleController
    extends AbstractWalkerThrottleController
{
  /**
   * Callback interface to be used with this throttle controller.
   *
   * @author cstamas
   */
  public static interface FixedRateWalkerThrottleControllerCallback
  {
    /**
     * Invoked for ever "adjustment slice" after all the relevant values are updated.
     */
    void onAdjustment(final FixedRateWalkerThrottleController controller);
  }

  /**
   * The stepping source of sleep times. The choice of number sequence defines the "steepness" how speed adjustments
   * are made.
   */
  private final NumberSequence currentSleepTime;

  /**
   * The callback, if any.
   */
  private final FixedRateWalkerThrottleControllerCallback callback;

  /**
   * The average TPS of overall run.
   */
  private int globalAverageTps;

  /**
   * The absolute maximum TPS achieved by walk from start, for statistical purposes (ie. to show off that your Nexus
   * can do more than your colleague's one, same for company maintained forges). :D
   */
  private int globalMaximumTps;

  /**
   * The variable limit (ceiling) of TPS. How strictly this controller will enforce it, depends on other variables
   * (ie. how much time will be spent in "overspeed" depends on adjustment frequency too and other).
   */
  private int limiterTps;

  /**
   * Internally maintained timestamp (in milliseconds) when last adjustment was made.
   */
  private long lastAdjustmentTimestamp;

  /**
   * Internally maintained slice size (in milliseconds), milliseconds betwen two subsequent adjustments.
   */
  private long lastSliceSize;

  /**
   * Internally maintained processItem invocation count when last adjustment was made.
   */
  private long lastAdjustmentProcessItemInvocationCount;

  /**
   * The TPS measured in last adjustment "slice".
   */
  private int lastSliceTps;

  /**
   * How often (in milliseconds) should adjustment to throttling be made. To small value might cause unneded overhead
   * and "oscillation", to big value might cause that throttling becomes not effective (ie. walk will spend too much
   * time in "overspeed" before adjustment kicks in).
   */
  private long sliceSize;

  /**
   * Creates a new instance of fixed rate throttle controller without callback and some defaults (adjustment slice
   * size is 2 seconds).
   *
   * @param limiterTps     the TPS limit you want to enforce, values -1 means do not limit (you can tune the value on
   *                       runtime too), 0 means "freeze" (walk will effectively sleep only, will not advance), and
   *                       any positive
   *                       number means the maximum wanted TPS.
   * @param numberSequence The numberSequence must be non-null, and will be reset, and it's initial state have to be
   *                       a
   *                       non-negative number, otherwise IllegalArgumentException is thrown.
   * @throws IllegalArgumentException If passed in numberSequence is not having needed characteristics.
   */
  public FixedRateWalkerThrottleController(final int limiterTps, final NumberSequence numberSequence)
      throws IllegalArgumentException
  {
    this(limiterTps, numberSequence, null);
  }

  /**
   * Creates a new instance of fixed rate throttle controller with callback and some sensible defaults (adjustment
   * slice size is 2 seconds).
   *
   * @param limiterTps     the TPS limit you want to enforce, values -1 means do not limit (you can tune the value on
   *                       runtime too), 0 means "freeze" (walk will effectively sleep only, will not advance), and
   *                       any positive
   *                       number means the maximum wanted TPS.
   * @param numberSequence The numberSequence must be non-null, and will be reset, and it's initial state have to be
   *                       a
   *                       non-negative number, otherwise IllegalArgumentException is thrown.
   * @throws IllegalArgumentException If passed in numberSequence is not having needed characteristics.
   * @callback the callback
   */
  public FixedRateWalkerThrottleController(final int limiterTps, final NumberSequence numberSequence,
                                           final FixedRateWalkerThrottleControllerCallback callback)
      throws IllegalArgumentException
  {
    this.limiterTps = limiterTps;
    numberSequence.reset();
    Preconditions.checkArgument(numberSequence.peek() >= 0,
        "Passed in NumberSequence in reset state must have a non-negative peek() return value!");
    this.currentSleepTime = numberSequence;
    this.callback = callback;
    this.sliceSize = TimeUnit.SECONDS.toMillis(2);
  }

  public int getGlobalAverageTps() {
    return globalAverageTps;
  }

  public int getGlobalMaximumTps() {
    return globalMaximumTps;
  }

  public int getLastSliceTps() {
    return lastSliceTps;
  }

  public long getCurrentSleepTime() {
    return currentSleepTime.peek();
  }

  public int getLimiterTps() {
    return limiterTps;
  }

  public void setLimiterTps(final int limiterTps) {
    this.limiterTps = limiterTps;
  }

  /**
   * @return the current adjustment period in ms (time that has to pass before an adjustment is made)
   */
  public long getSliceSize() {
    return sliceSize;
  }

  /**
   * Set the current adjustment period in ms (time that has to pass before an adjustment is made)
   */
  public void setSliceSize(final long sliceSize) {
    Preconditions.checkArgument(sliceSize > 0);
    this.sliceSize = sliceSize;
  }

  // == WalkerThrottleController iface
  @Override
  public void walkStarted(final WalkerContext context) {
    globalAverageTps = 0;
    globalMaximumTps = 0;
    lastSliceTps = 0;
    lastAdjustmentTimestamp = System.currentTimeMillis();
    lastAdjustmentProcessItemInvocationCount = 0;
  }

  @Override
  public void walkEnded(final WalkerContext context, final ThrottleInfo info) {
    mayCalculateStats(info, true);
  }

  @Override
  public boolean isThrottled() {
    return limiterTps > -1;
  }

  @Override
  public long throttleTime(final ThrottleInfo info) {
    if (mayCalculateStats(info, false)) {
      if (lastSliceTps > limiterTps) {
        // hold down the horses, increase sleepTime
        if (currentSleepTime.peek() < 0) {
          currentSleepTime.reset();
        }
        else {
          currentSleepTime.next();
        }
      }
      else {
        // lessen the sleep time
        if (currentSleepTime.peek() > 0) {
          currentSleepTime.prev();
        }
      }

      if (callback != null) {
        callback.onAdjustment(this);
      }
    }

    return currentSleepTime.peek();
  }

  // ==

  protected boolean mayCalculateStats(final ThrottleInfo info, final boolean forced) {
    if (adjustmentNeeded(forced)) {
      // global average TPS since walk started
      globalAverageTps = calculateCPS(info.getTotalProcessItemInvocationCount(), info.getTotalTimeWalking());
      // local TPS achieved in current "adjustment" slice
      lastSliceTps =
          calculateCPS(info.getTotalProcessItemInvocationCount() - lastAdjustmentProcessItemInvocationCount,
              lastSliceSize);
      lastAdjustmentProcessItemInvocationCount = info.getTotalProcessItemInvocationCount();
      // the maximum TPS since the walk started
      globalMaximumTps = Math.max(lastSliceTps, globalMaximumTps);
      return true;
    }
    else {
      return false;
    }
  }

  protected boolean adjustmentNeeded(final boolean forced) {
    final long now = System.currentTimeMillis();
    final long latestSlice = now - lastAdjustmentTimestamp;
    if (forced || latestSlice > sliceSize) {
      lastSliceSize = latestSlice;
      this.lastAdjustmentTimestamp = now;
      return true;
    }
    else {
      return false;
    }
  }

  protected int calculateCPS(final long count, final long millis) {
    if (count == 0) {
      return 0;
    }

    if (millis == 0) {
      return -1; // um, would be division by zero, but since we deal with a measure that might be positive only
      // (count-per-seconds), returning -1 is good for now
    }

    return (int) (((double) count / (double) millis) * 1000.0d); // we do this to make measure in seconds, but
    // to retain as much precision as possible
  }
}
