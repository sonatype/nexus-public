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
package org.sonatype.nexus.content.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.guice.FilterChainModule;
import org.sonatype.nexus.security.filter.FilterProviderSupport;
import org.sonatype.nexus.security.filter.authz.NexusTargetMappingAuthorizationFilter;
import org.sonatype.nexus.web.internal.SecurityFilter;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;

import static org.sonatype.nexus.security.filter.FilterProviderSupport.filterKey;

/**
 * Content module.
 *
 * @since 2.8
 */
@Named
public class ContentModule
    extends AbstractModule
{
  private static final String MOUNT_POINT = "/content";

  @Override
  protected void configure() {
    bind(filterKey("contentAuthcBasic")).to(ContentAuthenticationFilter.class).in(Singleton.class);

    bind(filterKey("contentTperms")).toProvider(ContentTargetMappingFilterProvider.class);

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        serve(MOUNT_POINT + "/*").with(ContentServlet.class);
        filter(MOUNT_POINT + "/*").through(SecurityFilter.class);
      }
    });

    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT + "/**", "noSessionCreation,contentAuthcBasic,contentTperms");
      }
    });
  }

  @Singleton
  static class ContentTargetMappingFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    public ContentTargetMappingFilterProvider(final NexusTargetMappingAuthorizationFilter filter) {
      super(filter);
      filter.setPathPrefix(MOUNT_POINT + "(.*)");
      filter.setPathReplacement("@1");
    }
  }
}
