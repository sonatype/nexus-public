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
package org.sonatype.nexus.content.raw.internal.recipe;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.inject.Named;

import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

/**
 * Raw proxy facet.
 *
 * @since 3.next
 */
@Named
public class RawProxyFacet
    extends ProxyFacetSupport
{
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    return content()
        .get(componentPath(context))
        .map(Content::new)
        .orElse(null);
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) throws IOException {
    log.debug("Not implemented yet");
    //caching will be worked on in - NEXUS-23605
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException {
    final String path = componentPath(context);
    return new Content(content().put(path, payload));
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return removePrefixingSlash(componentPath(context));
  }

  /**
   * Determines what 'component' this request relates to.
   */
  private String componentPath(final Context context) {
    final TokenMatcher.State tokenMatcherState = context.getAttributes().require(TokenMatcher.State.class);
    return tokenMatcherState.getTokens().get(RawProxyRecipe.PATH_NAME);
  }

  private RawContentFacet content() {
    return getRepository().facet(RawContentFacet.class);
  }

  private String removePrefixingSlash(final String url) {
    if(url != null && url.startsWith("/")) {
      return url.replaceFirst("/", "");
    }
    return url;
  }
}
