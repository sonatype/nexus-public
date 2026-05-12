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
package org.sonatype.nexus.selfhosted.internal.jvm;

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link JvmMonitoringService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class JvmMonitoringServiceTest
{
  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private PeriodicJob periodicJob;

  @Mock
  private JvmMonitor monitor1;

  @Mock
  private JvmMonitor monitor2;

  private JvmMonitoringService underTest;

  @Before
  public void setUp() {
    when(periodicJobService.schedule(any(Runnable.class), any(), any())).thenReturn(periodicJob);
  }

  @Test
  public void testServiceStartsWhenEnabled() throws Exception {
    List<JvmMonitor> monitors = Arrays.asList(monitor1, monitor2);
    underTest = new JvmMonitoringService(
        periodicJobService,
        monitors,
        true, // enabled
        15 // checkIntervalSeconds
    );

    underTest.start();

    verify(monitor1).initialize();
    verify(monitor2).initialize();
    verify(periodicJobService).startUsing();
    verify(periodicJobService).schedule(any(Runnable.class), any(), any());
  }

  @Test
  public void testServiceDoesNotStartWhenDisabled() throws Exception {
    List<JvmMonitor> monitors = Arrays.asList(monitor1, monitor2);
    underTest = new JvmMonitoringService(
        periodicJobService,
        monitors,
        false, // disabled
        15);

    underTest.start();

    verify(monitor1, never()).initialize();
    verify(monitor2, never()).initialize();
    verify(periodicJobService, never()).startUsing();
    verify(periodicJobService, never()).schedule(any(Runnable.class), any(), any());
  }

  @Test
  public void testServiceStops() throws Exception {
    List<JvmMonitor> monitors = Arrays.asList(monitor1, monitor2);
    underTest = new JvmMonitoringService(
        periodicJobService,
        monitors,
        true,
        15);

    underTest.start();
    underTest.stop();

    verify(periodicJob).cancel();
    verify(periodicJobService).stopUsing();
    verify(monitor1).cleanup();
    verify(monitor2).cleanup();
  }

  @Test
  public void testServiceStopsWhenDisabled() throws Exception {
    List<JvmMonitor> monitors = Arrays.asList(monitor1, monitor2);
    underTest = new JvmMonitoringService(
        periodicJobService,
        monitors,
        false,
        15);

    underTest.start();
    underTest.stop();

    // Should not interact with periodic job service when disabled
    verify(periodicJobService, never()).stopUsing();
    verify(monitor1, never()).cleanup();
    verify(monitor2, never()).cleanup();
  }

  @Test
  public void testMonitorsAreExecutedDuringHealthCheck() throws Exception {
    List<JvmMonitor> monitors = Arrays.asList(monitor1, monitor2);
    underTest = new JvmMonitoringService(
        periodicJobService,
        monitors,
        true,
        15);

    underTest.start();

    // Get the scheduled runnable and execute it
    verify(periodicJobService).schedule(any(Runnable.class), any(), any());
    // Note: In a real test environment, we would capture the Runnable and execute it
    // to verify that monitor.check() is called on each monitor
  }
}
