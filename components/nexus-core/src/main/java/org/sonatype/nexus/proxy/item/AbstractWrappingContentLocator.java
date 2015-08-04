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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * An abstract wrapper for ContentLocator. It implements all methods, but is declared abstract. Subclass it to add some
 * spice.
 * 
 * @author cstamas
 */
public abstract class AbstractWrappingContentLocator
    implements ContentLocator, Closeable
{
  private final ContentLocator contentLocator;

  public AbstractWrappingContentLocator(final ContentLocator contentLocator) {
    this.contentLocator = contentLocator;
  }

  protected ContentLocator getTarget() {
    return contentLocator;
  }

  @Override
  public InputStream getContent() throws IOException {
    return getTarget().getContent();
  }

  @Override
  public String getMimeType() {
    return getTarget().getMimeType();
  }

  @Override
  public long getLength() {
    return getTarget().getLength();
  }

  @Override
  public boolean isReusable() {
    return getTarget().isReusable();
  }

  /**
   * Cleans up, closes the wrapped content locator, if it is instance of {@link Closeable}. To be used in cases when
   * you actually don't need the stream (as some error cropped up), and you never requested the stream instance using
   * {@link #getContent()}.
   * 
   * @throws IOException if an I/O error occurs.
   * @since 2.5
   */
  @Override
  public void close() throws IOException {
    if (contentLocator instanceof Closeable) {
      ((Closeable) contentLocator).close();
    }
  }
}
