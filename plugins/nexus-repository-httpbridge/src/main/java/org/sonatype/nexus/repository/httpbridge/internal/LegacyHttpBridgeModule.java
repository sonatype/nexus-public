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
import org.sonatype.nexus.security.SecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authc.apikey.ApiKeyAuthenticationFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.servlet.ServletModule;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;

import static org.eclipse.sisu.inject.Sources.prioritize;

/**
 * Repository HTTP bridge module for legacy URLs.
 *
 * @since 3.7
 */
public class LegacyHttpBridgeModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(LegacyViewServlet.class);

    bind(ExhaustRequestFilter.class);

    requireBinding(WebSecurityManager.class);
    requireBinding(FilterChainResolver.class);

    // Bind after core-servlets but before error servlet
    Binder highPriorityBinder = binder().withSource(prioritize(0x50000000));
    highPriorityBinder.install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        // this technically makes non-group repositories visible under /content/groups,
        // but this is acceptable since their IDs are unique and it keeps things simple
        serve("/content/groups/*", "/content/repositories/*", "/content/sites/*").with(LegacyViewServlet.class);
        bindViewFiltersFor("/content/groups/*", "/content/repositories/*", "/content/sites/*");

        // this makes /service/local/x/x available, as a view servlet. Note that we have to strip the last forward
        // slash so that our first group would be "/service/local/x" without the forward slash. This is needed so that
        // the following matcher group starts with the forward slash, which is needed for the NXRM to detect the endpoint.
        serveRegex("/service/local/.*?(/.*)").with(LegacyViewServlet.class);
        bindViewFiltersRegexFor("/service/local/.*?(/.*)");
      }

      /**
       * Helper to make sure view-related filters are bound in the correct order by servlet filter.
       */
      private void bindViewFiltersFor(final String urlPattern, final String... morePatterns) {
        bindViewFilters(filter(urlPattern, morePatterns));
      }

      /**
       * Helper to make sure view-related filters are bound in the correct order by regex filter.
       */
      private void bindViewFiltersRegexFor(final String urlPattern, final String... morePatterns) {
        bindViewFilters(filterRegex(urlPattern, morePatterns));
      }

      private void bindViewFilters(FilterKeyBindingBuilder filter) {
        filter.through(ExhaustRequestFilter.class);
        filter.through(SecurityFilter.class);
      }
    });

    highPriorityBinder.install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain("/content/**",
            NexusAuthenticationFilter.NAME,
            ApiKeyAuthenticationFilter.NAME,
            AnonymousFilter.NAME,
            AntiCsrfFilter.NAME);

        addFilterChain("/service/local/**",
            NexusAuthenticationFilter.NAME,
            ApiKeyAuthenticationFilter.NAME,
            AnonymousFilter.NAME,
            AntiCsrfFilter.NAME);
      }
    });
  }
}
