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
package org.sonatype.nexus.repository.view.payloads;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.apache.http.util.EntityUtils;
import org.sonatype.nexus.repository.view.Payload;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Payload for HEAD responses that preserves header information without a body.
 * Returns the Content-Length and Content-Type from HTTP response headers,
 * but provides an empty input stream since HEAD responses have no body.
 *
 */
public class HeaderOnlyPayload
    implements Payload
{
  private final HttpResponse response;

  private final long contentLength;

  private final String contentType;

  public HeaderOnlyPayload(final HttpResponse response) {
    this.response = checkNotNull(response);
    this.contentLength = extractContentLength(response);
    this.contentType = extractContentType(response);
  }

  private long extractContentLength(final HttpResponse response) {
    Header header = response.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
    if (header != null) {
      try {
        return Long.parseLong(header.getValue());
      }
      catch (NumberFormatException e) {
        // Invalid Content-Length header, return -1 to indicate unknown
        return -1;
      }
    }
    return -1;
  }

  @Nullable
  private String extractContentType(final HttpResponse response) {
    Header header = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
    if (header != null) {
      return header.getValue();
    }
    return null;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    // HEAD responses have no body, return empty stream
    return new ByteArrayInputStream(new byte[0]);
  }

  @Override
  public long getSize() {
    return contentLength;
  }

  @Nullable
  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public void close() throws IOException {
    EntityUtils.consumeQuietly(response.getEntity());
  }
}
