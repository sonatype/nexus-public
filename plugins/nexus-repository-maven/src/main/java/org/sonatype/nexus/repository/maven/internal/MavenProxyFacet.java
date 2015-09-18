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
package org.sonatype.nexus.repository.maven.internal;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.inject.Named;

import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

/**
 * Maven specific implementation of {@link ProxyFacetSupport}.
 *
 * @since 3.0
 */
@Named
public class MavenProxyFacet
    extends ProxyFacetSupport
{
  private MavenFacet mavenFacet;

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.mavenFacet = facet(MavenFacet.class);
  }

  @Override
  protected Content getCachedPayload(final Context context) throws IOException {
    return mavenFacet.get(mavenPath(context));
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException, InvalidContentException {
   return mavenFacet.put(mavenPath(context), payload);
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) throws IOException {
    mavenFacet.setCacheInfo(mavenPath(context), content, cacheInfo);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1); // omit leading slash
  }

  @Nonnull
  private MavenPath mavenPath(@Nonnull final Context context) {
    return context.getAttributes().require(MavenPath.class);
  }
}
