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
package org.sonatype.nexus.siesta;

import javax.inject.Named;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.JwtSecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * Siesta plugin module using {@link JwtSecurityFilter}.
 *
 * @since 3.38
 */
@Named
@FeatureFlag(name = JWT_ENABLED)
public class JwtSiestaModule
  extends SiestaModule
{
  private static final Logger log = LoggerFactory.getLogger(JwtSiestaModule.class);

  @Override
  protected ServletModule configureServletModule() {
    return new ServletModule()
    {
      @Override
      protected void configureServlets() {
        log.debug("Mount point: {}", MOUNT_POINT);

        bind(SiestaServlet.class);
        serve(MOUNT_POINT + "/*").with(SiestaServlet.class, ImmutableMap.of(
            "resteasy.servlet.mapping.prefix", MOUNT_POINT
        ));
        filter(MOUNT_POINT + "/*").through(JwtSecurityFilter.class);
      }
    };
  }

  @Override
  protected Module configureFilterChainModule() {
    return new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT + "/**",
            NexusAuthenticationFilter.NAME,
            JwtFilter.NAME,
            AnonymousFilter.NAME,
            AntiCsrfFilter.NAME);
      }
    };
  }
}
