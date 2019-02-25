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

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;

/**
 * Customized {@link com.codahale.metrics.servlets.MetricsServlet} to support injection and download.
 *
 * @since 3.0
 */
@Singleton
public class MetricsServlet
  extends com.codahale.metrics.servlets.MetricsServlet
{
  @Inject
  public MetricsServlet(final MetricRegistry registry) {
    super(registry);

    // JVM metrics are no longer automatically added in codahale-metrics
    registry.register(name("jvm", "vm"), new JvmAttributeGaugeSet());
    registry.register(name("jvm", "memory"), new MemoryUsageGaugeSet());
    registry.register(name("jvm", "buffers"), new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
    registry.register(name("jvm", "fd_usage"), new FileDescriptorRatioGauge());
    registry.register(name("jvm", "thread-states"), new ThreadStatesGaugeSet());
    registry.register(name("jvm", "garbage-collectors"), new GarbageCollectorMetricSet());
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException
  {
    boolean download = Boolean.parseBoolean(req.getParameter("download"));
    if (download) {
      resp.addHeader(CONTENT_DISPOSITION, "attachment; filename='metrics.json'");
    }

    super.doGet(req, resp);
  }
}
