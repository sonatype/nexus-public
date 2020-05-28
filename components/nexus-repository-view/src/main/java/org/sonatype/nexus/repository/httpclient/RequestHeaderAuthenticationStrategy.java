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
package org.sonatype.nexus.repository.httpclient;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * Custom authentication strategy for handling situation when response from server
 * doesn't contain WWW_AUTHENTICATE header with requested authentication type.
 *
 * @since 3.24
 */
public class RequestHeaderAuthenticationStrategy
    extends TargetAuthenticationStrategy
{
  private static final String REQUEST_ATTRIBUTE = "http.request";

  private static final String BEARER_SCHEME = "Bearer";

  public RequestHeaderAuthenticationStrategy() {
    super();
  }

  @Override
  public Map<String, Header> getChallenges(
      final HttpHost authhost, final HttpResponse response, final HttpContext context)
      throws MalformedChallengeException
  {
    HttpRequest request = (HttpRequest) context.getAttribute(REQUEST_ATTRIBUTE);
    if (request != null) {
      Optional<Header> bearerTokenRequestOpt = getBearerHeader(request, HttpHeaders.AUTHORIZATION);
      Optional<Header> bearerTokenResponseOpt = getBearerHeader(response, HttpHeaders.WWW_AUTHENTICATE);

      /*
        Add necessary bearer token from request to response if it's absent, it's required for avoid infinite loop in docker
        (see NEXUS-23360)
       */
      if (bearerTokenRequestOpt.isPresent() && !bearerTokenResponseOpt.isPresent()) {
        response.addHeader(HttpHeaders.WWW_AUTHENTICATE, bearerTokenRequestOpt.get().getValue());
      }
    }
    return super.getChallenges(authhost, response, context);
  }

  private Optional<Header> getBearerHeader(final HttpMessage message, final String headerName) {
    return Arrays.stream(message.getHeaders(headerName))
        .filter(header -> header.getValue().startsWith(BEARER_SCHEME))
        .findAny();
  }
}
