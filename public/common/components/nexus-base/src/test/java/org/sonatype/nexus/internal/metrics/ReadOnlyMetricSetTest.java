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
import java.util.Map;

import com.codahale.metrics.Metric;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ReadOnlyMetricSetTest
{
  @Test
  public void testMetrics_alwaysReturnDefaults() throws Exception {
    ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet();
    Map<String, Metric> metrics = readOnlyMetricSet.getMetrics();

    assertThat(getMetricValue(metrics.get("enabled")), is(false));
    assertThat(getMetricValue(metrics.get("pending")), is(0));
    assertThat(getMetricValue(metrics.get("freezeTime")), is(0L));
  }

  private Object getMetricValue(final Metric metric) throws Exception {
    Method m = metric.getClass().getMethod("getValue");
    return m.invoke(metric);
  }
}
