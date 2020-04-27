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
package org.sonatype.nexus.repository.npm.internal.search.legacy;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils;
import org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;

import org.joda.time.DateTime;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * npm search index facet for proxy repositories: it is getting index document from proxy cache.
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
@Named
public class NpmSearchIndexFacetProxy
    extends FacetSupport
    implements NpmSearchIndexFacet
{
  @Nonnull
  @Override
  public Content searchIndex(@Nullable final DateTime since) throws IOException {
    try {
      final Request getRequest = new Request.Builder()
          .action(GET)
          .path("/" + NpmFacetUtils.REPOSITORY_ROOT_ASSET)
          .build();
      Context context = new Context(getRepository(), getRequest);
      context.getAttributes().set(ProxyTarget.class, ProxyTarget.SEARCH_INDEX);
      Content fullIndex = getRepository().facet(ProxyFacet.class).get(context);
      if (fullIndex == null) {
        throw new IOException("Could not retrieve registry root");
      }
      return NpmSearchIndexFilter.filterModifiedSince(fullIndex, since);
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void invalidateCachedSearchIndex() {
    // nop, proxy index is subject of proxy caching logic
  }
}
