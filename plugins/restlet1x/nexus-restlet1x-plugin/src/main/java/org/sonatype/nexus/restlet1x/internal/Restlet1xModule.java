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
package org.sonatype.nexus.restlet1x.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.filter.FilterProviderSupport;
import org.sonatype.nexus.security.filter.authz.NexusTargetMappingAuthorizationFilter;

import com.google.inject.AbstractModule;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;

import static org.sonatype.nexus.security.filter.FilterProviderSupport.filterKey;

/**
 * Restlet 1.x Guice module.
 *
 * @since 2.7
 */
@Named
public class Restlet1xModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    install(new RestletServletModule());

    // FIXME: Unsure why this is needed
    requireBinding(FilterChainResolver.class);

    bind(filterKey("trperms")).toProvider(TargetRepositoryFilterProvider.class);
    bind(filterKey("tiperms")).toProvider(TargetRepositoryIndexFilterProvider.class);
    bind(filterKey("tgperms")).toProvider(TargetGroupFilterProvider.class);
    bind(filterKey("tgiperms")).toProvider(TargetGroupIndexFilterProvider.class);
  }

  @Singleton
  static class TargetRepositoryFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    public TargetRepositoryFilterProvider(final NexusTargetMappingAuthorizationFilter filter) {
      super(filter);
      filter.setPathPrefix("/service/local/repositories/([^/]*)/content/(.*)");
      filter.setPathReplacement("/repositories/@1/@2");
    }
  }

  @Singleton
  static class TargetRepositoryIndexFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    public TargetRepositoryIndexFilterProvider(final NexusTargetMappingAuthorizationFilter filter) {
      super(filter);
      filter.setPathPrefix("/service/local/repositories/([^/]*)/index_content(.*)");
      filter.setPathReplacement("/repositories/@1/@2");
    }
  }

  @Singleton
  static class TargetGroupFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    public TargetGroupFilterProvider(final NexusTargetMappingAuthorizationFilter filter) {
      super(filter);
      filter.setPathPrefix("/service/local/repo_groups/([^/]*)/content(.*)");
      filter.setPathReplacement("/groups/@1/@2");
    }
  }

  @Singleton
  static class TargetGroupIndexFilterProvider
      extends FilterProviderSupport
  {
    @Inject
    public TargetGroupIndexFilterProvider(final NexusTargetMappingAuthorizationFilter filter) {
      super(filter);
      filter.setPathPrefix("/service/local/repo_groups/([^/]*)/index_content(.*)");
      filter.setPathReplacement("/groups/@1/@2");
    }
  }
}
