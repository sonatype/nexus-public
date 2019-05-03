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

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AvailableCpuHealthCheckTest
{

  private AvailableCpuHealthCheck underTest;

  @Before
  public void setup() {
    underTest = new AvailableCpuHealthCheck();
  }

  @Test
  public void testCheck_plentyOfCores() {
    Runtime runtime = mock(Runtime.class);
    when(runtime.availableProcessors()).thenReturn(86);
    underTest.setRuntime(runtime);
    Result result = underTest.check();
    assertThat(result.isHealthy(), is(true));
    assertThat(result.getMessage(), is("The host system is allocating a maximum of 86 cores to the application."));
  }

  @Test
  public void testCheck_notEnoughCores() {
    Runtime runtime = mock(Runtime.class);
    when(runtime.availableProcessors()).thenReturn(1);
    underTest.setRuntime(runtime);
    Result result = underTest.check();
    assertThat(result.isHealthy(), is(false));
    assertThat(result.getMessage(), is("The host system is allocating a maximum of 1 cores to the application." +
        " A minimum of " + AvailableCpuHealthCheck.MIN_RECOMMENDED_CPU_COUNT + " is recommended."));
  }
}
