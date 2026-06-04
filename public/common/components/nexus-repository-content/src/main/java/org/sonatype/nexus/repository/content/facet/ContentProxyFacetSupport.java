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
package org.sonatype.nexus.repository.content.facet;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.ETagHeaderUtils;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.OutboundRequestMetricRecorder;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.google.common.base.Preconditions.checkState;

/**
 * Content {@link ProxyFacet} support.
 *
 * @since 3.25
 */
public abstract class ContentProxyFacetSupport
    extends ProxyFacetSupport
{
  // HTTP date format per RFC 7231 (e.g., "Fri, 02 Jan 2026 08:56:24 GMT")
  private static final DateTimeFormatter HTTP_DATE_FORMAT =
      DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC();

  @Override
  protected void indicateVerified(
      final Context context,
      final Content content,
      final CacheInfo cacheInfo) throws IOException
  {
    // refresh internal cache details to record that we know this asset is up-to-date
    Asset asset = content.getAttributes().get(Asset.class);
    if (asset != null) {
      facet(ContentFacet.class).assets().with(asset).markAsCached(cacheInfo);
    }
    else {
      log.debug("Proxied content has no attached asset; cannot refresh cache details");
    }
  }

  /**
   * Override to support ETag and Last-Modified based blob reuse for 200 OK responses.
   * When a remote server returns 200 OK but the ETag or Last-Modified matches the cached content,
   * we treat it as "not modified" to avoid recreating blobs unnecessarily.
   * This is especially important for cloud deployments using S3 blob stores.
   *
   * This fix applies to ALL content-based proxy formats automatically:
   * R, Conan, Conda, APT, Docker, Cargo, Raw, RubyGems, Go, P2, CocoaPods, Hugging Face, etc.
   *
   * @since 3.92
   */
  @Override
  protected boolean isNotModified(final HttpResponse response, @Nullable final Content stale) {
    // Check parent first - if 304 Not Modified, no further checks needed
    boolean parent = super.isNotModified(response, stale);
    if (parent) {
      return true;
    }

    // If no stale content, we can't compare
    if (stale == null) {
      return false;
    }

    // Only process 200 OK responses for comparison
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      return false;
    }

    // Try ETag comparison first
    boolean etagMatch = checkETagMatch(response, stale);
    if (etagMatch) {
      log.debug("ContentProxyFacetSupport: isNotModified - ETag match, reusing cached blob");
      return true;
    }

    // Fall back to Last-Modified comparison if no ETag
    if (getEtagHeader(response) == null && checkLastModifiedMatch(response, stale)) {
      log.debug("ContentProxyFacetSupport: isNotModified - Last-Modified match, reusing cached blob");
      return true;
    }

    log.debug("ContentProxyFacetSupport: isNotModified - No match found, downloading new blob");
    return false;
  }

  /**
   * Check if the response ETag matches the cached content's ETag.
   */
  private boolean checkETagMatch(final HttpResponse response, final Content stale) {
    Header etagHeader = getEtagHeader(response);

    if (etagHeader == null) {
      return false;
    }

    String responseEtag = ETagHeaderUtils.extract(etagHeader.getValue());

    String cachedEtag = stale.getAttributes().get(Content.CONTENT_ETAG, String.class);

    boolean match = cachedEtag != null && Objects.equals(responseEtag, cachedEtag);

    log.debug(
        "ContentProxyFacetSupport: checkETagMatch - responseETag={}, cachedETag={}, match={}",
        responseEtag, cachedEtag, match);

    return match;
  }

  private static Header getEtagHeader(final HttpResponse response) {
    // Check for ETag header - try standard case first, then lowercase
    // Azure Blob Storage, CDNs may return "etag" (lowercase) or "ETag" (standard)
    // HTTP header names are case-insensitive per RFC 7230, but Apache HttpClient lookup is case-sensitive
    Header etagHeader = response.getFirstHeader(HttpHeaders.ETAG);
    if (etagHeader == null) {
      etagHeader = response.getFirstHeader("etag");
    }
    return etagHeader;
  }

  /**
   * Check if the response Last-Modified matches the cached content's Last-Modified.
   * This is useful for servers that don't return ETags (like Packagist).
   */
  private boolean checkLastModifiedMatch(final HttpResponse response, final Content stale) {
    // Check for Last-Modified header
    Header lastModifiedHeader = response.getFirstHeader(HttpHeaders.LAST_MODIFIED);
    if (lastModifiedHeader == null) {
      lastModifiedHeader = response.getFirstHeader("last-modified");
    }

    if (lastModifiedHeader == null) {
      return false;
    }

    // Get cached Last-Modified from stale content
    DateTime cachedLastModified = stale.getAttributes().get(Content.CONTENT_LAST_MODIFIED, DateTime.class);
    if (cachedLastModified == null) {
      return false;
    }

    // Parse response Last-Modified
    DateTime responseLastModified = parseHttpDate(lastModifiedHeader.getValue());
    if (responseLastModified == null) {
      return false;
    }

    // Compare timestamps - if response Last-Modified is same or older than cached, content hasn't changed
    // We use isEqual or isBefore because servers might return slightly different timestamps
    boolean match = !responseLastModified.isAfter(cachedLastModified);

    log.debug(
        "ContentProxyFacetSupport: checkLastModifiedMatch - responseLastModified={}, cachedLastModified={}, match={}",
        responseLastModified, cachedLastModified, match);

    return match;
  }

  /**
   * Parse an HTTP date string into a DateTime object.
   * Handles the standard RFC 7231 format: "Fri, 02 Jan 2026 08:56:24 GMT"
   */
  @Nullable
  private DateTime parseHttpDate(final String dateString) {
    if (dateString == null || dateString.isEmpty()) {
      return null;
    }
    try {
      return HTTP_DATE_FORMAT.parseDateTime(dateString);
    }
    catch (IllegalArgumentException e) {
      log.debug("Failed to parse HTTP date: {}", dateString, e);
      return null;
    }
  }

  protected Payload getPayload(final Repository proxy, final URI uri) throws IOException {
    // Route through Yellowfin if enabled (Pro edition only - uses reflection to avoid hard dependency)
    URI effectiveUri = maybeRouteViaYellowfin(proxy, uri);

    final HttpClient client = proxy.facet(HttpClientFacet.class).getHttpClient();

    HttpGet request = new HttpGet(effectiveUri);
    log.debug("Fetching: {}", request);

    HttpContext httpContext = new BasicHttpContext();
    // Populate context with repository metadata for telemetry
    if (proxy != null && proxy.getFormat() != null && proxy.getType() != null) {
      httpContext.setAttribute(OutboundRequestMetricRecorder.CONTEXT_FORMAT, proxy.getFormat().getValue());
      httpContext.setAttribute(OutboundRequestMetricRecorder.CONTEXT_REPOSITORY_TYPE, proxy.getType().getValue());
    }
    HttpResponse response = client.execute(request, httpContext);
    StatusLine status = response.getStatusLine();
    log.debug("Response: {}, status: {}", response, status);

    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      checkState(entity != null, "No http entity received from remote registry");

      return new HttpEntityPayload(response, entity);
    }
    log.warn("Status code {} contacting {}", status.getStatusCode(), effectiveUri);
    HttpClientUtils.closeQuietly(response);
    return null;
  }

  /**
   * Check if Yellowfin routing is enabled and rewrite URI if so.
   * Uses reflection to avoid compile-time dependency on Pro edition code.
   */
  private URI maybeRouteViaYellowfin(final Repository proxy, final URI originalUri) {
    try {
      // Check repository yellowfinEnabled flag
      Boolean yellowfinEnabled = proxy.getConfiguration()
          .attributes("proxy")
          .get("yellowfinEnabled", Boolean.class);

      if (!Boolean.TRUE.equals(yellowfinEnabled)) {
        return originalUri;
      }

      // Reflectively access YellowfinProxyRouter (Pro edition only)
      Class<?> routerClass = Class.forName("com.sonatype.nexus.clm.yellowfin.YellowfinProxyRouter");
      Object routerInstance = routerClass.getMethod("getInstance").invoke(null);

      if (routerInstance != null) {
        URI rewrittenUri = (URI) routerClass
            .getMethod("maybeRewriteUri", URI.class, boolean.class)
            .invoke(routerInstance, originalUri, true);

        if (rewrittenUri != null) {
          log.debug("Routing via Yellowfin: {} -> {}", originalUri, rewrittenUri);
          return rewrittenUri;
        }
      }
    }
    catch (Exception e) {
      log.debug("Yellowfin routing not available or failed, using direct upstream", e);
    }

    return originalUri; // Fallback: use original URI
  }

}
