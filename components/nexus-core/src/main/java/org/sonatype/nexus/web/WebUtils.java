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
package org.sonatype.nexus.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.nexus.util.io.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Web response related utilities, that helps in ordinary tasks with HTTP servlet responses.
 *
 * @since 2.8
 */
@Named
@Singleton
public class WebUtils
{
  /**
   * Buffer size to be used when pushing content to the {@link HttpServletResponse#getOutputStream()} stream. Default
   * is Jetty default or 8KB.
   */
  private static final int BUFFER_SIZE = SystemPropertiesHelper
      .getInteger(WebUtils.class.getName() + ".BUFFER_SIZE", -1);

  private final String serverString;

  @Inject
  public WebUtils(final ApplicationStatusSource applicationStatusSource) {
    this.serverString = "Nexus/" + checkNotNull(applicationStatusSource).getSystemStatus().getVersion();
  }

  /**
   * Sends 302 Temporary redirect to client. To be used when client MUST NOT cache the redirection.
   */
  public void sendTemporaryRedirect(final HttpServletResponse response, final String url) throws IOException {
    response.setStatus(HttpServletResponse.SC_FOUND);
    response.addHeader("Location", url);
    response.flushBuffer();
  }

  /**
   * Equips the response with standard headers like "Server".
   */
  public void equipResponseWithStandardHeaders(final HttpServletResponse response) {
    response.setHeader("Server", serverString);
    // NEXUS-6569 Add X-Frame-Options header
    response.setHeader("X-Frame-Options", "SAMEORIGIN");
    // NEXUS-5023 disable IE for sniffing into response content
    response.setHeader("X-Content-Type-Options", "nosniff");
  }

  /**
   * Equips the response with headers to be added to non-content responses, that should not be cached.
   *
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-5155">NEXUS-5155</a>
   */
  public void addNoCacheResponseHeaders(final HttpServletResponse response) {
    response.setHeader("Pragma", "no-cache"); // HTTP/1.0
    response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"); // HTTP/1.1
    response.setHeader("Cache-Control", "post-check=0, pre-check=0"); // MS IE
    response.setHeader("Expires", "0"); // No caching on Proxies in between client and Nexus
  }

  /**
   * Sends content by copying all bytes from the input stream to the response setting the preferred buffer
   * size. At the end, it flushes response buffer. Passed in {@link InputStream} is fully consumed and closed.
   * The passed in {@link HttpServletResponse} after this call returns is committed and flushed.
   */
  public void sendContent(final InputStream input, final HttpServletResponse response) throws IOException {
    int bufferSize = BUFFER_SIZE;
    if (bufferSize < 1) {
      // if no user override, ask container for bufferSize
      bufferSize = response.getBufferSize();
      if (bufferSize < 1) {
        bufferSize = 8192;
        response.setBufferSize(bufferSize);
      }
    }
    else {
      // user override present, tell container what buffer size we'd like
      response.setBufferSize(bufferSize);
    }
    try (final InputStream from = input; final OutputStream to = response.getOutputStream()) {
      StreamSupport.copy(from, to, bufferSize);
      response.flushBuffer();
    }
  }
}
