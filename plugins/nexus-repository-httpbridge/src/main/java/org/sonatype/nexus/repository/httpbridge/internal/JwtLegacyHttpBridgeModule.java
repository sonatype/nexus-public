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
package org.sonatype.nexus.repository.httpbridge.internal;

import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.JwtFilter;
import org.sonatype.nexus.security.JwtHelper;
import org.sonatype.nexus.security.JwtSecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authc.apikey.ApiKeyAuthenticationFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;

import static org.eclipse.sisu.inject.Sources.prioritize;

/**
 * Repository HTTP bridge module for legacy URLs using {@link JwtSecurityFilter}.
 *
 * @since 3.38
 */
public class JwtLegacyHttpBridgeModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(LegacyViewServlet.class);

    bind(ExhaustRequestFilter.class);

    requireBinding(WebSecurityManager.class);
    requireBinding(FilterChainResolver.class);
    requireBinding(JwtHelper.class);

    // Bind after core-servlets but before error servlet
    Binder highPriorityBinder = binder().withSource(prioritize(0x50000000));
    highPriorityBinder.install(new LegacyHttpBridgeServletModule()
    {
      @Override
      protected void bindSecurityFilter(final FilterKeyBindingBuilder filter) {
        filter.through(JwtSecurityFilter.class);
      }
    });

    highPriorityBinder.install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain("/content/**",
            NexusAuthenticationFilter.NAME,
            JwtFilter.NAME,
            ApiKeyAuthenticationFilter.NAME,
            AnonymousFilter.NAME,
            AntiCsrfFilter.NAME);

        addFilterChain("/service/local/**",
            NexusAuthenticationFilter.NAME,
            JwtFilter.NAME,
            ApiKeyAuthenticationFilter.NAME,
            AnonymousFilter.NAME,
            AntiCsrfFilter.NAME);
      }
    });
  }

}
