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
package org.sonatype.nexus.guice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.sonatype.nexus.web.WebResource;

// FIXME: Unify with UrlWebResource impl

/**
 * {@link WebResource} contributed from a Nexus plugin.
 */
class StaticWebResource
    implements WebResource
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final URL resourceURL;

  private final String publishedPath;

  private final boolean cacheable;

  private final String contentType;

  private final long size;

  private final long lastModified;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  public StaticWebResource(final URL resourceURL, final String publishedPath, final String contentType) {
    this(resourceURL, publishedPath, contentType, true);
  }

  public StaticWebResource(final URL resourceURL, final String publishedPath, final String contentType,
      final boolean cacheable)
  {
    this.resourceURL = resourceURL;
    this.publishedPath = publishedPath;
    this.contentType = contentType;
    this.cacheable = cacheable;
    try {
      final URLConnection urlConnection = resourceURL.openConnection();
      try (final InputStream is = urlConnection.getInputStream()) {
        // support for legacy int and modern long content-length
        long size = urlConnection.getContentLengthLong();
        if (size == -1) {
          size = urlConnection.getContentLength();
        }
        this.size = size;

        this.lastModified = urlConnection.getLastModified();
      }
    }
    catch (IOException e) {
      throw new IllegalArgumentException("Static resource " + resourceURL + " inaccessible", e);
    }
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public String getPath() {
    return publishedPath;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSize() {
    return size;
  }

  public long getLastModified() {
    return lastModified;
  }

  public boolean isCacheable() {
    return cacheable;
  }

  public InputStream getInputStream() throws IOException {
    return resourceURL.openStream();
  }

  @Override
  public String toString() {
    return "StaticWebResource{url=" + resourceURL + '}';
  }
}
