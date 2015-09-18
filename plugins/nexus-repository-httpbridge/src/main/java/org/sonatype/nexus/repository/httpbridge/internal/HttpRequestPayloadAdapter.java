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

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.repository.view.Payload;

import org.joda.time.DateTime;

/**
 * HTTP request payload adapts {@link HttpServletRequest} body-content to {@link Payload}.
 *
 * @since 3.0
 */
class HttpRequestPayloadAdapter
    implements Payload
{
  private final HttpServletRequest request;

  private final String contentType;

  private final long size;

  public HttpRequestPayloadAdapter(final HttpServletRequest request) {
    this.request = request;
    this.contentType = request.getContentType();
    this.size = request.getContentLength();
  }

  private static DateTime parseLastModified(final long value) {
    if (value != -1) {
      return new DateTime(value);
    }
    return null;
  }

  @Nullable
  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return request.getInputStream();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "contentType='" + contentType + '\'' +
        ", size=" + size +
        '}';
  }
}
