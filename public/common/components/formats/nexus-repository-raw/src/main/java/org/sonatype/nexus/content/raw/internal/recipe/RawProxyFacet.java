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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE;

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

  private boolean queryParamsForwardingEnabled;

  @Autowired
  @VisibleForTesting
  void setQueryParamsForwardingEnabled(
      @Value(RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE) final boolean queryParamsForwardingEnabled)
  {
    this.queryParamsForwardingEnabled = queryParamsForwardingEnabled;
  }

  @VisibleForTesting
  final QueryParameterForwardingHelper queryParamHelper = new QueryParameterForwardingHelper();

  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    return content().get(cacheKey(context)).orElse(null);
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException {
    return content().put(cacheKey(context), payload);
  }

  @Override
  protected void doConfigure(Configuration configuration) throws Exception {
    super.doConfigure(configuration);
    configureQueryParamForwarding(configuration);
  }

  @Override
  protected void doUpdate(Configuration configuration) throws Exception {
    super.doUpdate(configuration);
    configureQueryParamForwarding(configuration);
  }

  @SuppressWarnings("unchecked")
  private void configureQueryParamForwarding(final Configuration configuration) {
    if (!queryParamsForwardingEnabled) {
      queryParamHelper.updateConfig(false, Collections.emptyList());
      return;
    }
    boolean forward = Boolean.TRUE.equals(configuration.attributes("raw")
        .get("forwardQueryParameters", Boolean.class, Boolean.FALSE));
    List<String> excluded = configuration.attributes("raw")
        .get("excludedQueryParameters", List.class, Collections.emptyList());
    queryParamHelper.updateConfig(forward, excluded);
  }

  @Override
  protected String getUrl(final Context context) {
    // Return only the path - query parameters will be appended in buildFetchHttpRequest()
    // after all encoding is complete to avoid the encoding pipeline converting ? to %3F
    return getEscapeHelper().uriSegments(removeSlashPrefix(assetPath(context)));
  }

  @Override
  protected String encodeUrl(final String url) throws UnsupportedEncodingException {
    // ALWAYS apply format-specific raw encoding (required for raw repos to work correctly)
    // When EncodingHelper is active, it already encoded: #, ?, [, ]
    // So we only encode chars unique to raw format: ^, \u202F (avoid double-encoding)
    // When EncodingHelper is null (feature disabled), encode all raw-specific chars
    ImmutableSet<String> charsToEncode =
        getEncodingHelper() != null
            ? CHARS_UNIQUE_TO_RAW // Only encode ^, \u202F (avoid double-encoding #, ?, [, ])
            : CHARS_ALL_RAW; // Encode all when feature disabled

    String encodedUrl = url;
    for (String ch : charsToEncode) {
      encodedUrl = encodedUrl.replace(ch, URLEncoder.encode(ch, StandardCharsets.UTF_8));
    }
    return encodedUrl;
  }

  @Override
  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context, final Content stale) {
    // Append query string AFTER all path encoding is complete to avoid the encoding
    // pipeline in ProxyFacetSupport converting ? to %3F
    Parameters parameters = context.getRequest().getParameters();
    String queryString = queryParamHelper.buildQueryString(parameters);

    URI finalUri = uri;
    if (!queryString.isEmpty()) {
      try {
        finalUri = new URIBuilder(uri)
            .setCustomQuery(queryString) // Properly handles existing query strings
            .build();
        log.debug("Raw proxy: forwarding request to {} with query parameters", finalUri);
        // Note: Parameter values not logged for security. Use excludedQueryParameters to filter sensitive params.
      }
      catch (URISyntaxException e) {
        log.warn("Failed to append query parameters to URI: {}. Using URI without query string.",
            e.getMessage());
        // Fall back to original URI (better than crashing)
        finalUri = uri;
      }
    }

    return super.buildFetchHttpRequest(finalUri, context, stale);
  }

  /**
   * Builds a cache key that includes query parameters when forwarding is enabled,
   * so each unique parameter combination is stored as a separate cached asset.
   */
  private String cacheKey(final Context context) {
    String path = assetPath(context);
    String queryString = queryParamHelper.buildQueryString(context.getRequest().getParameters());
    if (!queryString.isEmpty()) {
      return path + "?" + queryString;
    }
    return path;
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
