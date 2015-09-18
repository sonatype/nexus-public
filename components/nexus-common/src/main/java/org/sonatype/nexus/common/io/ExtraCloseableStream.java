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
package org.sonatype.nexus.common.io;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A decorator that gives an InputStream the ability to close an additional {@link Closeable} resource when the
 * InputStream is closed.
 *
 * @since 3.0
 */
public class ExtraCloseableStream
    extends FilterInputStream
{
  private final Closeable needsClosing;

  public ExtraCloseableStream(final InputStream in, final Closeable needsClosing) {
    super(in);
    this.needsClosing = needsClosing;
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    }
    finally {
      needsClosing.close();
    }
  }
}
