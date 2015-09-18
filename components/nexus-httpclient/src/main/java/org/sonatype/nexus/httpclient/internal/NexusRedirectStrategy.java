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

import javax.annotation.Nullable;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.net.HttpHeaders.LOCATION;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTP;

// FIXME: Sort out where this was used/needed apply or remove
/**
 * This special strategy will kick in only if Nexus performs content retrieval.
 * In every other case (non-GET method or GET method used in remote
 * availability check) this strategy defaults to {@link DefaultRedirectStrategy} behavior.
 * <p/>In case of content retrieval, the "do not follow redirect to index pages (collections), but accept and follow
 * any other redirects" strategy kicks in. If index page redirect is detected (by checking the URL path for trailing
 * slash), redirection mechanism of HC4 is stopped, and hence, the response will return with redirect response code
 * (301, 302 or 307). Main goal of this {@link org.apache.http.client.RedirectStrategy} is to save the
 * subsequent (the one following the redirect) request once we learn it would lead us to index page, as we
 * don't need index pages (hence, we do not fetch it only to throw it away).
 * <p/>
 * Usual problems are misconfiguration, where a repository published over HTTPS is configured with HTTP (ie.
 * admin mistyped the URL). Seemingly all work, but that is a source of performance issue, as every outgoing
 * Nexus request will "bounce", as usually HTTP port will redirect Nexus to HTTPS port, and then the artifact
 * will be fetched. Remedy for these scenarios is to edit the proxy repository configuration and update the
 * URL to proper protocol.
 * <p/>
 * This code <strong>assumes</strong> that remote repository is set up by best practices and common conventions,
 * hence, index page redirect means that target URL ends with slash. For more about this topic, read the
 * "To slash or not to slash" Google blog entry.
 *
 * @see <a href="http://googlewebmastercentral.blogspot.hu/2010/04/to-slash-or-not-to-slash.html">To slash or not to
 *      slash</a>
 * @since 3.0
 */
public class NexusRedirectStrategy
    extends DefaultRedirectStrategy
{
  /**
   * HttpContext key marking request is about content retrieval, hence, the special redirection strategy should
   * be applied.
   */
  public static final String CONTENT_RETRIEVAL_MARKER_KEY = NexusRedirectStrategy.class.getName() + "#retrieveItem";

  private static final Logger log = LoggerFactory.getLogger(NexusRedirectStrategy.class);

  @Override
  public boolean isRedirected(final HttpRequest request, final HttpResponse response, final HttpContext context)
      throws ProtocolException
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

      log.debug("Following redirection {} -> {}", sourceUri, targetUri);
      return true;
    }

    return false;
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
