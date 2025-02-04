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
package org.sonatype.nexus.internal.status;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.annotations.VisibleForTesting;

/**
 * Health check that indicates if the available JVM reported CPU count is below the recommended threshold.
 * <p>
 * The {@see Runtime#availableProcessors} API used to check the number of available processors may return different
 * results during the lifetime of the JVM, but we will always consider it not healthy if the CPU is dynamically reduced
 * below threshold.
 *
 * @since 3.17
 */
@Named("Available CPUs")
@Singleton
public class AvailableCpuHealthCheck
    extends HealthCheckComponentSupport
{
  /**
   * Minimum recommended CPU Count
   */
  static final int MIN_RECOMMENDED_CPU_COUNT = 4;

  private static final String HEALTHY_MESSAGE =
      "The host system is allocating a maximum of %d cores to the application.";

  private static final String UNHEALTHY_MESSAGE = HEALTHY_MESSAGE + " A minimum of %d is recommended.";

  private int minCpuCount;

  public AvailableCpuHealthCheck() {
    this(MIN_RECOMMENDED_CPU_COUNT);
  }

  @VisibleForTesting
  AvailableCpuHealthCheck(final int minCpuCount) {
    this.minCpuCount = minCpuCount;
  }

  private Runtime getRuntime() {
    return Runtime.getRuntime();
  }

  @Override
  protected Result check() {
    int available = getRuntime().availableProcessors();

    if (minCpuCount > available) {
      return Result.unhealthy(UNHEALTHY_MESSAGE, available, minCpuCount);
    }

    return Result.healthy(HEALTHY_MESSAGE, available);
  }
}
