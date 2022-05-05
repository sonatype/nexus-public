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

import org.sonatype.nexus.security.SecurityFilter;

import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.PingServlet;
import com.google.inject.servlet.ServletModule;

/**
 * Servlet module for Metrics module.
 *
 * @since 3.38
 */
public abstract class MetricsServletModule
    extends ServletModule
{
  private final String mountPoint;

  protected MetricsServletModule(final String mountPoint) {
    this.mountPoint = mountPoint;
  }

  @Override
  protected void configureServlets() {
    bind(MetricsServlet.class);
    bind(HealthCheckServlet.class);

    serve(mountPoint + "/ping").with(new PingServlet());
    serve(mountPoint + "/threads").with(new ThreadDumpServlet());
    serve(mountPoint + "/data").with(MetricsServlet.class);
    serve(mountPoint + "/healthcheck").with(HealthCheckServlet.class);
    serve(mountPoint + "/prometheus").with(new io.prometheus.client.exporter.MetricsServlet());

    // record metrics for all webapp access
    filter("/*").through(new InstrumentedFilter());

    bind(SecurityFilter.class);

    // configure security
    bindSecurityFilter();
  }

  protected abstract void bindSecurityFilter();
}
