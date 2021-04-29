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
package org.sonatype.nexus.internal.metrics;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.FreezeRequest;
import org.sonatype.nexus.common.app.FreezeService;

import com.codahale.metrics.Metric;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class ReadOnlyMetricSetTest
    extends TestSupport
{
  @Mock
  private FreezeService freezeService;

  @Test
  public void testMetrics_defaultWhenNoFreezeService() throws Exception {
    ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet(() -> null);
    Map<String, Metric> metrics = readOnlyMetricSet.getMetrics();

    assertThat(getMetricValue(metrics.get("enabled")), is(false));
    assertThat(getMetricValue(metrics.get("pending")), is(0));
    assertThat(getMetricValue(metrics.get("freezeTime")), is(0L));
  }

  @Test
  public void testMetrics_falseWhenNotInReadonlyMode() throws Exception {
    when(freezeService.isFrozen()).thenReturn(false);
    when(freezeService.currentFreezeRequests()).thenReturn(Collections.emptyList());

    ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet(() -> freezeService);
    Map<String, Metric> metrics = readOnlyMetricSet.getMetrics();

    assertThat(getMetricValue(metrics.get("enabled")), is(false));
    assertThat(getMetricValue(metrics.get("pending")), is(0));
    assertThat(getMetricValue(metrics.get("freezeTime")), is(0L));
  }

  @Test
  public void testMetrics_validWhenInReadonlyMode() throws Exception {
    when(freezeService.isFrozen()).thenReturn(true);
    when(freezeService.currentFreezeRequests()).thenReturn(
        asList(new FreezeRequest("SYSTEM", "system initiator", new DateTime(1504111817165L), null, null),
            new FreezeRequest("USER", "user initiator", new DateTime(1504111817166L), null, null)));

    ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet(() -> freezeService);
    Map<String, Metric> metrics = readOnlyMetricSet.getMetrics();

    assertThat(getMetricValue(metrics.get("enabled")), is(true));
    assertThat(getMetricValue(metrics.get("pending")), is(2));
    assertThat(getMetricValue(metrics.get("freezeTime")), is(1504111817165L));
  }

  // so Metric is a super useful interface...
  private Object getMetricValue(final Metric metric) throws Exception {
    Method m = metric.getClass().getMethod("getValue");
    return m.invoke(metric);
  }
}
