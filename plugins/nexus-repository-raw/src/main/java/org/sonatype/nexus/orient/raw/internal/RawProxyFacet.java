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
package org.sonatype.nexus.orient.raw.internal;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.orient.raw.RawContentFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

/**
 * @since 3.0
 */
@Named
public class RawProxyFacet
    extends ProxyFacetSupport
{
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    final String path = componentPath(context);
    return content().get(path);
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) throws IOException {
    content().setCacheInfo(componentPath(context), content, cacheInfo);
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException {
    final String path = componentPath(context);
    return content().put(path, payload);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return componentPath(context).replaceAll(":", "%3A");
  }

  /**
   * Determines what 'component' this request relates to.
   */
  private String componentPath(final Context context) {
    final TokenMatcher.State tokenMatcherState = context.getAttributes().require(TokenMatcher.State.class);
    return tokenMatcherState.getTokens().get("name");
  }

  private RawContentFacet content() {
    return getRepository().facet(RawContentFacet.class);
  }
}
