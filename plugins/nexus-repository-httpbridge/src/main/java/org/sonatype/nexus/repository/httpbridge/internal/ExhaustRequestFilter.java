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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpMethods;

import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;

/**
 * {@link Filter} which exhausts request bodies for specific user-agents when errors occur.
 *
 * This is needed to keep HTTPClient based clients such as Apache-Maven happy because they
 * always expect the request body to be fully consumed. Even when the server is responding
 * with an error, such as unauthenticated user (401).
 *
 * @see https://issues.apache.org/jira/browse/HTTPCLIENT-1188
 *
 * @since 3.2
 */
@Named
@Singleton
public class ExhaustRequestFilter
    extends ComponentSupport
    implements Filter
{
  private final Pattern exhaustForAgentsPattern;

  @Inject
  public ExhaustRequestFilter(@Named("${nexus.view.exhaustForAgents:-Apache-Maven.*}") final String exhaustForAgents) {
    this.exhaustForAgentsPattern = Pattern.compile(exhaustForAgents.replace("\\s,\\s", "|"));
  }

  @Override
  public void init(final FilterConfig filterConfig) {
    // empty
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException
  {
    try {
      chain.doFilter(request, response);
    }
    finally {
      if (exhaustRequest(request, response)) {
        try (InputStream in = request.getInputStream()) {
          ByteStreams.exhaust(in);
        }
        catch (Exception e) {
          log.debug("Unable to exhaust request", e);
        }
      }
    }
  }

  @Override
  public void destroy() {
    // empty
  }

  /**
   * Returns {@code true} if we need to exhaust the request before responding to the client.
   */
  private boolean exhaustRequest(final ServletRequest request, final ServletResponse response) {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      // only needed when an error occurs...
      if (httpResponse.getStatus() >= 400) {
        String method = httpRequest.getMethod();
        // ...for upload requests
        if (HttpMethods.PUT.equals(method) || HttpMethods.POST.equals(method)) {
          /// ...from an affected user-agent
          String agent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
          return exhaustForAgentsPattern.matcher(agent).matches();
        }
      }
    }
    return false;
  }
}
