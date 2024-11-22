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

import javax.inject.Named;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.JwtSecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authz.PermissionsFilter;

import com.codahale.metrics.Clock;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * Dropwizard Metrics</a> guice configuration using {@link JwtSecurityFilter}
 *
 * @since 3.38
 */
@Named
@FeatureFlag(name = JWT_ENABLED)
public class JwtMetricsModule
    extends MetricsModule
{
  private static final Logger log = LoggerFactory.getLogger(JwtMetricsModule.class);

  @Override
  protected void configure() {
    // NOTE: AdminServletModule (metrics-guice integration) generates invalid links, so wire up servlets ourselves

    final Clock clock = Clock.defaultClock();
    bind(Clock.class).toInstance(clock);

    final JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    bind(JsonFactory.class).toInstance(jsonFactory);

    install(new MetricsServletModule(MOUNT_POINT)
    {
      @Override
      protected void bindSecurityFilter() {
        filter(MOUNT_POINT + "/*").through(JwtSecurityFilter.class);
      }
    });

    // require permission to use endpoints
    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT + "/**",
            NexusAuthenticationFilter.NAME,
            JwtFilter.NAME,
            AnonymousFilter.NAME,
            AntiCsrfFilter.NAME,
            PermissionsFilter.config("nexus:metrics:read"));
      }
    });

    log.info("Metrics support configured");
  }
}
