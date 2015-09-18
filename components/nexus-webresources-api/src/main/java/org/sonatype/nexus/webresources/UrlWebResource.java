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
package org.sonatype.nexus.webresources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * URL-based {@link WebResource} implementation.
 *
 * @since 2.8
 */
public class UrlWebResource
    implements WebResource
{
  private final URL url;

  private final String path;

  private final boolean cacheable;

  private final String contentType;

  private final long size;

  private final long lastModified;

  public UrlWebResource(final URL url, final String path, final String contentType) {
    this(url, path, contentType, true);
  }

  public UrlWebResource(final URL url, final String path, final String contentType, final boolean cacheable) {
    this.url = checkNotNull(url);
    this.path = checkNotNull(path);
    this.cacheable = cacheable;

    // open connection to get details about the resource
    try {
      final URLConnection connection = this.url.openConnection();
      try (final InputStream ignore = connection.getInputStream()) {
        if (Strings.isNullOrEmpty(contentType)) {
          this.contentType = connection.getContentType();
        }
        else {
          this.contentType = contentType;
        }

        // support for legacy int and modern long content-length
        long size = connection.getContentLengthLong();
        if (size == -1) {
          size = connection.getContentLength();
        }
        this.size = size;

        this.lastModified = connection.getLastModified();
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Resource inaccessible: " + url, e);
    }
  }

  @Override
  public String getPath() {
    if (path != null) {
      return path;
    }
    else {
      return url.getPath();
    }
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return url.openStream();
  }

  @Override
  public boolean isCacheable() {
    return cacheable;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "url=" + url +
        ", path='" + path + '\'' +
        ", cacheable=" + cacheable +
        ", contentType='" + contentType + '\'' +
        ", size=" + size +
        ", lastModified=" + lastModified +
        '}';
  }
}
