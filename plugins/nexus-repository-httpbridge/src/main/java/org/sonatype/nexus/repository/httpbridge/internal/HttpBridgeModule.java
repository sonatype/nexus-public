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

import javax.inject.Named;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.SecurityFilter;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authc.apikey.ApiKeyAuthenticationFilter;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;

/**
 * Repository HTTP bridge module.
 *
 * @since 3.0
 */
@Named
public class HttpBridgeModule
    extends AbstractModule
{
  public static final String MOUNT_POINT = "/repository";

  static final boolean SUPPORT_LEGACY_CONTENT = SystemPropertiesHelper
      .getBoolean(HttpBridgeModule.class.getName() + ".legacy", false);

  @Override
  protected void configure() {
    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(ViewServlet.class);
        serve(MOUNT_POINT + "/*").with(ViewServlet.class);
        bindViewFiltersFor(MOUNT_POINT + "/*");

        if (SUPPORT_LEGACY_CONTENT) {
          // this technically makes non-group repositories visible under /content/groups,
          // but this is acceptable since their IDs are unique and it keeps things simple
          serve("/content/groups/*", "/content/repositories/*").with(ViewServlet.class);
          bindViewFiltersFor("/content/groups/*", "/content/repositories/*");
        }
      }

      /**
       * Helper to make sure view-related filters are bound in the correct order.
       */
      private void bindViewFiltersFor(final String urlPattern, final String... morePatterns) {
        FilterKeyBindingBuilder filter = filter(urlPattern, morePatterns);
        filter.through(ExhaustRequestFilter.class);
        filter.through(SecurityFilter.class);
      }
    });

    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT + "/**",
            NexusAuthenticationFilter.NAME,
            ApiKeyAuthenticationFilter.NAME,
            AnonymousFilter.NAME);

        if (SUPPORT_LEGACY_CONTENT) {
          addFilterChain("/content/**",
              NexusAuthenticationFilter.NAME,
              ApiKeyAuthenticationFilter.NAME,
              AnonymousFilter.NAME);
        }
      }
    });
  }
}
