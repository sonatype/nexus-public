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

import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.SecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.NexusBasicHttpAuthenticationFilter;
import org.sonatype.nexus.security.authz.PermissionsFilter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.PingServlet;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <a href="http://metrics.codahale.com">Codahale Metrics</a> guice configuration.
 * 
 * Installs servlet endpoints:
 * 
 * <ul>
 * <li>/internal/ping</li>
 * <li>/internal/threads</li>
 * <li>/internal/metrics</li>
 * <li>/internal/healthcheck</li>
 * </ul>
 * 
 * Protected by {@code nexus:metrics:read} permission.
 * 
 * @since 2.5
 */
public class MetricsModule
    extends AbstractModule
{
  private static final Logger log = LoggerFactory.getLogger(MetricsModule.class);

  // TODO: Change MP to /service/* ?

  private static final String MOUNT_POINT = "/internal";

  @Override
  protected void configure() {
    // NOTE: AdminServletModule (metrics-guice integration) generates invalid links, so wire up servlets ourselves

    final Clock clock = Clock.defaultClock();
    bind(Clock.class).toInstance(clock);

    final JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    bind(JsonFactory.class).toInstance(jsonFactory);

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(MetricsServlet.class);
        bind(HealthCheckServlet.class);

        serve(MOUNT_POINT + "/ping").with(new PingServlet());
        serve(MOUNT_POINT + "/threads").with(new ThreadDumpServlet());
        serve(MOUNT_POINT + "/metrics").with(MetricsServlet.class);
        serve(MOUNT_POINT + "/healthcheck").with(HealthCheckServlet.class);

        // record metrics for all webapp access
        filter("/*").through(new InstrumentedFilter());

        bind(SecurityFilter.class);

        // configure security
        filter(MOUNT_POINT + "/*").through(SecurityFilter.class);
      }
    });

    // require permission to use endpoints
    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        // TODO: Expose resource permissions for ping/threads/metrics/healthcheck?
        // TODO: Maybe consider re-implementing as JAX-RS endpoints?

        addFilterChain(MOUNT_POINT + "/**",
            NexusBasicHttpAuthenticationFilter.NAME,
            AnonymousFilter.NAME,
            PermissionsFilter.config("nexus:metrics:read"));
      }
    });

    log.info("Metrics support configured");
  }
}
