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
package org.sonatype.nexus.apachehttpclient;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link HttpRoutePlanner} that uses different proxies / url scheme (http/https) and bypasses proxy for specific
 * hosts (non proxy hosts).
 *
 * @since 2.6
 */
class NexusHttpRoutePlanner
    extends DefaultRoutePlanner
{

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  /**
   * Set of patterns for matching hosts names against. Never null.
   */
  private final Set<Pattern> nonProxyHostPatterns;

  /**
   * Mapping between protocol scheme and proxy to be used
   */
  private final Map<String, HttpHost> proxies;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  /**
   * @since 2.5
   */
  NexusHttpRoutePlanner(final Map<String, HttpHost> proxies,
                        final Set<Pattern> nonProxyHostPatterns,
                        final SchemePortResolver schemePortResolver)
  {
    super(schemePortResolver);
    this.proxies = checkNotNull(proxies);
    this.nonProxyHostPatterns = checkNotNull(nonProxyHostPatterns);
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public HttpRoute determineRoute(final HttpHost target, final HttpRequest request, final HttpContext context)
      throws HttpException
  {
    return super.determineRoute(target, request, context);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  @Override
  protected HttpHost determineProxy(
      final HttpHost target,
      final HttpRequest request,
      final HttpContext context) throws HttpException
  {
    if (noProxyFor(target.getHostName())) {
      return null;
    }
    return proxies.get(target.getSchemeName());
  }

  private boolean noProxyFor(final String hostName) {
    for (final Pattern nonProxyHostPattern : nonProxyHostPatterns) {
      if (nonProxyHostPattern.matcher(hostName).matches()) {
        return true;
      }
    }
    return false;
  }
}
