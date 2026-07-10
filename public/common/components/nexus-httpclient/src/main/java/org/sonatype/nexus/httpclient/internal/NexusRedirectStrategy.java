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
package org.sonatype.nexus.httpclient.internal;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import com.google.common.base.Throwables;
import jakarta.annotation.Nullable;
import jakarta.validation.ValidationException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.LOCATION;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTP;

/**
 * Default redirect strategy for all repository formats that provides:
 * <ul>
 * <li>Prevention of redirects to index pages (paths ending with /) during content retrieval</li>
 * <li>Path traversal attack prevention by normalizing .. and . sequences in redirect URLs</li>
 * <li>Preservation of percent-encoding for signed URLs when preserveEncodedCharacters flag is enabled</li>
 * </ul>
 *
 * <p>
 * During content retrieval (marked with CONTENT_RETRIEVAL_MARKER_KEY), this strategy:
 * <ul>
 * <li>Blocks redirects to index pages to save unnecessary requests</li>
 * <li>Follows all other redirects normally</li>
 * </ul>
 *
 * <p>
 * When preserveEncodedCharacters is enabled in the HttpContext:
 * <ul>
 * <li>Detects path traversal sequences in redirect URLs (both literal .. and encoded %2e%2e)</li>
 * <li>Normalizes paths with traversal sequences for security</li>
 * <li>Preserves exact percent-encoding for legitimate URLs (critical for AWS S3, Cloudflare R2, etc.)</li>
 * </ul>
 *
 * <p>
 * Common use cases:
 * <ul>
 * <li>Misconfigured repositories: HTTP URL redirecting to HTTPS causes performance issues</li>
 * <li>CDN redirects: Signed URLs require exact encoding preservation</li>
 * <li>Security: Path traversal attacks via redirect URLs</li>
 * </ul>
 *
 * @see SecurePathNormalizer
 * @see <a href="http://googlewebmastercentral.blogspot.hu/2010/04/to-slash-or-not-to-slash.html">To slash or not to
 *      slash</a>
 * @since 3.0
 */
@Component
@Qualifier("default")
public class NexusRedirectStrategy
    extends DefaultRedirectStrategy
{
  /**
   * HttpContext key marking request is about content retrieval, hence, the special redirection strategy should
   * be applied.
   */
  public static final String CONTENT_RETRIEVAL_MARKER_KEY = NexusRedirectStrategy.class.getName() + "#retrieveItem";

  private static final Logger log = LoggerFactory.getLogger(NexusRedirectStrategy.class);

  private final AntiSsrfService antiSsrfService;

  @Autowired
  public NexusRedirectStrategy(final AntiSsrfService antiSsrfService) {
    this.antiSsrfService = checkNotNull(antiSsrfService);
  }

  /**
   * Validates that the redirect target host doesn't resolve to a private/local network address.
   *
   * @param uri the URI to validate
   * @throws ProtocolException if the host is blocked by SSRF protection
   */
  private void validateRedirectHost(final URI uri) throws ProtocolException {
    String host = uri.getHost();
    // expected when host is null its a relative URI
    if (host != null) {
      try {
        antiSsrfService.validateHost(host);
      }
      catch (ValidationException e) {
        throw new ProtocolException("Redirect to private network blocked: " + e.getMessage(), e);
      }
    }
  }

  @Override
  public boolean isRedirected(
      final HttpRequest request,
      final HttpResponse response,
      final HttpContext context) throws ProtocolException
  {
    if (super.isRedirected(request, response, context)) {
      // code below comes from DefaultRedirectStrategy, as method super.getLocationURI cannot be used
      // since it modifies context state, and would result in false circular reference detection
      final Header locationHeader = response.getFirstHeader(LOCATION);
      if (locationHeader == null) {
        // got a redirect response, but no location header
        throw new ProtocolException("Received redirect response " + response.getStatusLine() +
            " but no location present");
      }

      // Some complication here as it appears that something may be null, but its not clear what was null
      final URI sourceUri = ((HttpUriRequest) request).getURI();
      final String sourceScheme = schemeOf(sourceUri);
      final String sourceHost = hostOf(sourceUri);

      final URI targetUri = createLocationURI(locationHeader.getValue());
      final String targetScheme = schemeOf(targetUri);
      final String targetHost = hostOf(targetUri);

      // FIXME: sourceUri might be relative while targetUri is always absolute, which can lead to
      // false-positive logging because the source scheme (null) differs from the target ("http")
      // One way to fix this is to resolve the relative sourceUri against the original proxy URL,
      // but this relies on access to the original configuration.

      // nag about redirection peculiarities, in any case
      if (!Objects.equals(sourceScheme, targetScheme)) {
        if (HTTP.equals(targetScheme)) {
          // security risk: HTTPS > HTTP downgrade, you are not safe as you think!
          log.debug("Downgrade from HTTPS to HTTP during redirection {} -> {}", sourceUri, targetUri);
        }
        else if ("https".equals(targetScheme) && Objects.equals(sourceHost, targetHost)) {
          // misconfiguration: your repository configured with wrong protocol and causes performance problems?
          log.debug("Protocol upgrade during redirection on same host {} -> {}", sourceUri, targetUri);
        }
      }

      // this logic below should trigger only for content fetches made by RRS retrieveItem
      // hence, we do this ONLY if the HttpRequest is "marked" as such request
      if (Boolean.TRUE == context.getAttribute(CONTENT_RETRIEVAL_MARKER_KEY)) {
        if (targetUri.getPath().endsWith("/")) {
          log.debug("Not following redirection to index {} -> {}", sourceUri, targetUri);
          return false;
        }
      }

      // Validate redirect target doesn't resolve to private/local network (SSRF protection)
      validateRedirectHost(targetUri);

      log.debug("Following redirection {} -> {}", sourceUri, targetUri);
      return true;
    }

    return false;
  }

  @Override
  public URI getLocationURI(
      final HttpRequest request,
      final HttpResponse response,
      final HttpContext context) throws ProtocolException
  {
    if (!shouldPreserveEncoding(context)) {
      // When preserveEncodedCharacters is false or not set, use default behavior (normalize)
      URI uri = super.getLocationURI(request, response, context);
      // Validate redirect target doesn't resolve to private/local network (SSRF protection)
      validateRedirectHost(uri);
      return uri;
    }

    // When preserveEncodedCharacters is true, handle redirect URL with encoding preservation
    try {
      Header locationHeader = extractLocationHeader(response);
      URI redirectUri = parseRedirectUri(locationHeader);

      // Handle path traversal attacks (security takes precedence over encoding preservation)
      URI secureUri = handlePathTraversalIfNeeded(redirectUri);
      if (secureUri != redirectUri) {
        validateRedirectHost(secureUri);
        return secureUri;
      }

      // Encode special characters for signed URLs (AWS S3, Cloudflare R2, etc.)
      URI encodedUri = encodeSpecialCharactersIfNeeded(redirectUri);
      validateRedirectHost(encodedUri);
      return encodedUri;
    }
    catch (Exception e) {
      Throwables.throwIfInstanceOf(e, ProtocolException.class);
      throw new ProtocolException("Invalid redirect URI: " + response.getFirstHeader(LOCATION).getValue(), e);
    }
  }

  /**
   * Check if encoding preservation is enabled in the HTTP context.
   */
  private boolean shouldPreserveEncoding(final HttpContext context) {
    Object preserveEncodedChars = context.getAttribute("preserveEncodedCharacters");
    return Boolean.TRUE.equals(preserveEncodedChars);
  }

  /**
   * Extract and validate the Location header from the redirect response.
   */
  private Header extractLocationHeader(final HttpResponse response) throws ProtocolException {
    Header locationHeader = response.getFirstHeader(LOCATION);
    if (locationHeader == null) {
      throw new ProtocolException("Received redirect response " + response.getStatusLine() +
          " but no location present");
    }
    return locationHeader;
  }

  /**
   * Parse the redirect URI from the Location header value.
   */
  private URI parseRedirectUri(final Header locationHeader) throws Exception {
    String locationValue = locationHeader.getValue();
    log.debug("Original Location header: {}", locationValue);

    URI uri = new URI(locationValue);
    log.debug("Parsed URI - getRawPath(): {}, getPath(): {}", uri.getRawPath(), uri.getPath());

    return uri;
  }

  /**
   * Detect and handle path traversal sequences in redirect URLs.
   * Returns a new URI with normalized path if traversal is detected, otherwise returns the original URI.
   */
  private URI handlePathTraversalIfNeeded(final URI uri) throws Exception {
    String rawPath = uri.getRawPath();
    if (rawPath == null) {
      return uri;
    }

    // Decode the path to detect encoded path traversal sequences like %2e%2e (encoded ..)
    // This is critical for security - attackers can use percent-encoding to hide .. sequences
    String decodedPath = uri.getPath();

    if (decodedPath == null) {
      log.error("Redirect URI has null path, cannot check for path traversal: {}", uri);
      return uri; // Cannot safely check for traversal, return original URI
    }

    if (!SecurePathNormalizer.containsPathTraversal(decodedPath)) {
      return uri; // No path traversal detected
    }

    // Path traversal detected - security takes precedence over preserving encoding
    log.warn("Path traversal sequences detected in redirect: {}", uri);

    String normalizedPath = SecurePathNormalizer.normalizePath(decodedPath);
    URI normalizedUri = buildUri(uri, normalizedPath);

    log.debug("Normalized URI with path traversal removed: {}", normalizedUri);
    return normalizedUri;
  }

  /**
   * Encode literal special characters for signed URLs while preserving already-encoded sequences.
   * Returns a new URI with encoded path if encoding is needed, otherwise returns the original URI.
   */
  private URI encodeSpecialCharactersIfNeeded(final URI uri) throws Exception {
    String rawPath = uri.getRawPath();
    if (rawPath == null) {
      return uri;
    }

    // For signed URLs (AWS S3, Cloudflare R2), encode literal special characters
    // like + to %2B while preserving already-encoded sequences
    String encodedPath = SecurePathNormalizer.encodeSpecialCharacters(rawPath);

    if (rawPath.equals(encodedPath)) {
      log.debug("No special characters to encode, returning original URI: {}", uri);
      return uri;
    }

    log.debug("Encoding special characters in redirect path: {} -> {}", rawPath, encodedPath);
    URI encodedUri = buildUri(uri, encodedPath);

    log.debug("Returning URI with encoded special characters: {}", encodedUri);
    return encodedUri;
  }

  /**
   * Build a new URI with the specified path while preserving other URI components.
   */
  private URI buildUri(final URI originalUri, final String newPath) throws Exception {
    return new URI(
        originalUri.getScheme(),
        originalUri.getUserInfo(),
        originalUri.getHost(),
        originalUri.getPort(),
        newPath,
        originalUri.getQuery(),
        originalUri.getFragment());
  }

  /**
   * Return the scheme of given uri in lower-case, or null if the scheme can not be determined.
   */
  @Nullable
  private static String schemeOf(final URI uri) {
    if (uri != null) {
      String scheme = uri.getScheme();
      if (scheme != null) {
        return scheme.toLowerCase(Locale.US);
      }
    }
    return null;
  }

  /**
   * Return host of given uri or null.
   */
  @Nullable
  private static String hostOf(final URI uri) {
    if (uri != null) {
      return uri.getHost();
    }
    return null;
  }
}
