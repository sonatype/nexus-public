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
package org.sonatype.nexus.proxy.item;

import java.io.IOException;
import java.io.InputStream;

/**
 * The Interface ContentLocator. Implements a strategy to fetch content of an item. For implementors, it's recommended
 * to use {@link AbstractContentLocator}.
 * 
 * @see AbstractContentLocator
 */
public interface ContentLocator
{
  /**
   * Length marking "unknown" length. This means that locator cannot tell how many bytes will {@link InputStream}
   * returned by {@link #getContent()} method provide when fully read. This might happen with generated content, or
   * compressed happening on-the-fly, etc.
   */
  long UNKNOWN_LENGTH = -1L;

  /**
   * Unknown MIME type, basically arbitrary binary data.
   * 
   * @see <a href="http://www.ietf.org/rfc/rfc2046.txt">RFC2046</a> section 4.5.1.
   */
  String UNKNOWN_MIME_TYPE = "application/octet-stream";

  /**
   * Gets the content as opened and ready to read input stream. It has to be closed by the caller explicitly. Depending
   * on return value of {@link #isReusable()}, this method might be called only once or multiple times.
   */
  InputStream getContent() throws IOException;

  /**
   * Returns the MIME type of the content, never {@code null}.
   */
  String getMimeType();

  /**
   * Returns the length of the content in bytes if known, or {@link #UNKNOWN_LENGTH} if content length unknown.
   */
  long getLength();

  /**
   * Returns {@code true} if this locator is reusable, meaning that it is possible to invoke {@link #getContent()}
   * multiple times against it, {@code false} otherwise.
   */
  boolean isReusable();
}
