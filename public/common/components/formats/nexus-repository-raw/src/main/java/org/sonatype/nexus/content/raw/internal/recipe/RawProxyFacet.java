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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Raw proxy facet.
 *
 * @since 3.24
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RawProxyFacet
    extends ContentProxyFacetSupport
{
  // Characters that ONLY raw format needs to encode (unique to raw)
  // When preserveEncodedCharacters is true, EncodingHelper already handles: #, ?, [, ]
  private static final ImmutableSet<String> CHARS_UNIQUE_TO_RAW = ImmutableSet.of("^", "\u202F");

  // All characters that raw format needs when feature is disabled
  private static final ImmutableSet<String> CHARS_ALL_RAW = ImmutableSet.of("^", "#", "?", "\u202F", "[", "]");

  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    return content().get(assetPath(context)).orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException {
    return content().put(assetPath(context), payload);
  }

  @Override
  protected String getUrl(final Context context) {
    return getEscapeHelper().uriSegments(removeSlashPrefix(assetPath(context)));
  }

  @Override
  protected String encodeUrl(final String url) throws UnsupportedEncodingException {
    // ALWAYS apply format-specific raw encoding (required for raw repos to work correctly)
    // When preserveEncodedCharacters is true, EncodingHelper already encoded: #, ?, [, ]
    // So we only encode chars unique to raw format: ^, \u202F
    // When preserveEncodedCharacters is false, encode all raw-specific chars
    ImmutableSet<String> charsToEncode =
        (getEncodingHelper() != null && getEncodingHelper().shouldPreserveEncodedCharacters())
            ? CHARS_UNIQUE_TO_RAW // Only encode ^, \u202F (avoid double-encoding #, ?, [, ])
            : CHARS_ALL_RAW; // Encode all when feature disabled

    String encodedUrl = url;
    for (String ch : charsToEncode) {
      encodedUrl = encodedUrl.replace(ch, URLEncoder.encode(ch, "UTF-8"));
    }
    return encodedUrl;
  }

  private RawContentFacet content() {
    return getRepository().facet(RawContentFacet.class);
  }

  /**
   * Determines what 'asset' this request relates to.
   */
  private String assetPath(final Context context) {
    final TokenMatcher.State tokenMatcherState = context.getAttributes().require(TokenMatcher.State.class);
    return tokenMatcherState.getTokens().get(RawRecipeSupport.PATH_NAME);
  }

  private String removeSlashPrefix(final String url) {
    return url != null && url.startsWith("/") ? url.substring(1) : url;
  }
}
