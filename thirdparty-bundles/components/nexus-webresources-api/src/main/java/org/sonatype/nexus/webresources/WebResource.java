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
import java.net.URLConnection;

import javax.annotation.Nullable;

/**
 * A resource to be exposed via web (http/https) protocols.
 *
 * @since 2.8
 */
public interface WebResource
{
  long UNKNOWN_SIZE = -1L;

  long UNKNOWN_LAST_MODIFIED = 0L;

  String UNKNOWN_CONTENT_TYPE = "application/octet-stream";

  String HTML = "text/html";

  String PLAIN = "text/plain";

  String CSS = "text/css";

  String JAVASCRIPT = "application/x-javascript";

  /**
   * The path where the resource is mounted under the servlet-context.
   */
  String getPath();

  /**
   * The content-type of the resource, or {@code null} or {@link #UNKNOWN_CONTENT_TYPE} if unknown.
   */
  @Nullable
  String getContentType();

  /**
   * The size of the content, or {@link #UNKNOWN_SIZE} if unknown.
   *
   * @see URLConnection#getContentLengthLong()
   */
  long getSize();

  /**
   * The last modified time, or {@link #UNKNOWN_LAST_MODIFIED} if unknown.
   *
   * @see URLConnection#getLastModified()
   */
  long getLastModified();

  /**
   * True if the resource should be cached.
   */
  boolean isCacheable();

  /**
   * Resource content stream.
   */
  InputStream getInputStream() throws IOException;

  /**
   * Allows web-resources to prepare for handling requests.
   */
  interface Prepareable
  {
    WebResource prepare() throws IOException;
  }
}
