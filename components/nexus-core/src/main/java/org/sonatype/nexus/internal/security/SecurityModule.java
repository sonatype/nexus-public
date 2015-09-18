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
package org.sonatype.nexus.internal.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.internal.security.anonymous.OrientAnonymousConfigurationStore;
import org.sonatype.nexus.internal.security.realm.OrientRealmConfigurationStore;
import org.sonatype.nexus.security.FilterProviderSupport;
import org.sonatype.nexus.security.anonymous.AnonymousConfigurationStore;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.NexusApiKeyAuthenticationFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authc.NexusBasicHttpAuthenticationFilter;
import org.sonatype.nexus.security.authz.PermissionsFilter;
import org.sonatype.nexus.security.realm.RealmConfigurationStore;

import com.google.inject.AbstractModule;

import static org.sonatype.nexus.security.FilterProviderSupport.filterKey;

/**
 * Security module.
 */
@Named
public class SecurityModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(filterKey(AnonymousFilter.NAME)).to(AnonymousFilter.class);
    bind(filterKey(NexusBasicHttpAuthenticationFilter.NAME)).to(NexusBasicHttpAuthenticationFilter.class);
    bind(filterKey(NexusApiKeyAuthenticationFilter.NAME)).to(NexusApiKeyAuthenticationFilter.class);
    bind(filterKey(PermissionsFilter.NAME)).to(PermissionsFilter.class);

    // FIXME: Sort out, and deal with naming the "authcBasic" are presently auth-token bits
    bind(filterKey("authcBasic")).toProvider(AuthcBasicFilterProvider.class);

    // FIXME: This likely should be normalized with the auth-token bits
    bind(filterKey("authcApiKey")).toProvider(AuthcApiKeyFilterProvider.class);

    bind(AnonymousConfigurationStore.class).to(OrientAnonymousConfigurationStore.class);
    bind(RealmConfigurationStore.class).to(OrientRealmConfigurationStore.class);
  }

  // FIXME: Probably do not need provider here at all anymore

  @Singleton
  static class AuthcBasicFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcBasicFilterProvider(final NexusAuthenticationFilter filter) {
      super(filter);
    }
  }

  @Singleton
  static class AuthcApiKeyFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    AuthcApiKeyFilterProvider(final NexusApiKeyAuthenticationFilter filter) {
      super(filter);
    }
  }
}
