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
package org.sonatype.nexus.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.EXPIRES;
import static com.google.common.net.HttpHeaders.PRAGMA;

/**
 * Servlet helpers.
 *
 * @since 3.0
 */
public class ServletHelper
{
  private ServletHelper() {
    // empty
  }

  /**
   * Helper to check if {@code ?debug} flag is on request.
   */
  public static boolean isDebug(final HttpServletRequest request) {
    checkNotNull(request);
    String value = request.getParameter("debug");

    // not set
    if (value == null) {
      return false;
    }

    // ?debug
    if (value.trim().length() == 0) {
      return true;
    }

    // ?debug=<flag>
    return Boolean.parseBoolean(value);
  }

  /**
   * Buffer size to be used when pushing content to the {@link HttpServletResponse#getOutputStream()} stream.
   *
   * Default is Jetty default or 8KB.
   */
  private static final int BUFFER_SIZE =
      SystemPropertiesHelper.getInteger(ServletHelper.class.getName() + ".BUFFER_SIZE", -1);

  /**
   * Equips the response with headers to be added to non-content responses, that should not be cached.
   *
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-5155">NEXUS-5155</a>
   */
  public static void addNoCacheResponseHeaders(final HttpServletResponse response) {
    response.setHeader(PRAGMA, "no-cache"); // HTTP/1.0
    response.setHeader(CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate"); // HTTP/1.1
    response.setHeader(CACHE_CONTROL, "post-check=0, pre-check=0"); // MS IE
    response.setHeader(EXPIRES, "0"); // No caching on Proxies in between client and Nexus
  }

  /**
   * Sends content by copying all bytes from the input stream to the response setting the preferred buffer
   * size. At the end, it flushes response buffer. Passed in {@link InputStream} is fully consumed and closed.
   * The passed in {@link HttpServletResponse} after this call returns is committed and flushed.
   */
  public static void sendContent(final InputStream input, final HttpServletResponse response) throws IOException {
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
      final byte[] buf = new byte[bufferSize];
      while (true) {
        int r = from.read(buf);
        if (r == -1) {
          break;
        }
        to.write(buf, 0, r);
      }
      response.flushBuffer();
    }
  }
}
