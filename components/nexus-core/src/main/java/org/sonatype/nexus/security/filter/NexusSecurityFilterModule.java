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
package org.sonatype.nexus.security.filter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.filter.authc.NexusApiKeyAuthenticationFilter;
import org.sonatype.nexus.security.filter.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.filter.authz.FailureLoggingHttpMethodPermissionFilter;
import org.sonatype.security.web.filter.authc.LogoutAuthenticationFilter;

import com.google.inject.AbstractModule;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;

import static org.sonatype.nexus.security.filter.FilterProviderSupport.filterKey;

/**
 * Sets up Nexus's security filter configuration; this is a @Named module so it will be auto-installed by Sisu.
 */
@Named
public class NexusSecurityFilterModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    // FIXME: Unsure why this is needed
    requireBinding(FilterChainResolver.class);

    bind(filterKey("authcBasic")).toProvider(AuthcBasicFilterProvider.class);
    bind(filterKey("authcNxBasic")).toProvider(AuthcNxBasicFilterProvider.class);

    bind(filterKey("logout")).to(LogoutAuthenticationFilter.class).in(Singleton.class);
    bind(filterKey("perms")).to(FailureLoggingHttpMethodPermissionFilter.class).in(Singleton.class);

    bind(filterKey("authcApiKey")).toProvider(AuthcApiKeyFilterProvider.class);

    // HACK: Disable CSRFGuard support for now, its too problematic
    //bind(filterKey("csrfToken")).to(CsrfGuardFilter.class).in(Singleton.class);
  }

  @Singleton
  static class AuthcBasicFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcBasicFilterProvider(final NexusAuthenticationFilter filter) {
      super(filter);
      // Do not change this string, third-party clients depend on it
      filter.setApplicationName("Sonatype Nexus Repository Manager API");
      filter.setFakeAuthScheme(Boolean.toString(false));
    }
  }

  /**
   * Special filter used by login resource so that browser BASIC auth dialogs will not be shown.
   */
  @Singleton
  static class AuthcNxBasicFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcNxBasicFilterProvider(final NexusAuthenticationFilter filter) {
      super(filter);
      filter.setApplicationName("Sonatype Nexus Repository Manager API (specialized auth)");
      filter.setFakeAuthScheme(Boolean.toString(true));
    }
  }

  @Singleton
  static class AuthcApiKeyFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcApiKeyFilterProvider(final NexusApiKeyAuthenticationFilter filter) {
      super(filter);
      filter.setApplicationName("Sonatype Nexus Repository Manager API (X-...-ApiKey auth)");
    }
  }
}
