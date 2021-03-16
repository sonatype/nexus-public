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
package org.sonatype.nexus.repository.npm.internal.httpclient;

import java.net.URI;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.internal.NpmFormat;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * NEXUS-23750: Support npm GitHub Package Registry.
 * GitHub Package Repository uses a HTTP 302 response + Location header to tell npm CLI, that tarball is on an AWS server.
 * AWS does not allow to use both the Authorization Bearer token and the X-Amz-Credential parameter.
 *
 * @since 3.30
 */
@Named(NpmFormat.NAME)
@Singleton
public class NpmRedirectStrategy
    extends DefaultRedirectStrategy
{
  private static final String AMAZON_CREDENTIAL_PARAM = "X-Amz-Credential";

  @Override
  public HttpUriRequest getRedirect(
      final HttpRequest httpRequest, final HttpResponse httpResponse, final HttpContext httpContext)
      throws ProtocolException
  {
    boolean isRedirected = isRedirected(httpRequest, httpResponse, httpContext);
    if (isRedirected) {
      URI locationURI = createLocationURI(httpResponse.getFirstHeader(HttpHeaders.LOCATION).getValue());
      String query = locationURI.getQuery();
      if (query != null && query.contains(AMAZON_CREDENTIAL_PARAM)) {
        httpRequest.removeHeaders(HttpHeaders.AUTHORIZATION);
      }
    }
    return super.getRedirect(httpRequest, httpResponse, httpContext);
  }
}
