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

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream
    extends InputStream
{
  public static final long UNLIMITED = -1;

  private final InputStream is;

  private final long from;

  private final long count;

  private long readAlready = 0;

  public LimitedInputStream(InputStream is, long from, long count) throws IOException {
    this.is = is;
    this.from = from;
    this.count = count;
    is.skip(from);
  }

  public InputStream getIs() {
    return is;
  }

  public long getFrom() {
    return from;
  }

  public long getCount() {
    return count;
  }

  @Override
  public int read() throws IOException {
    if (readAlready > count) {
      return -1;
    }

    readAlready++;

    return is.read();
  }

  public int available() throws IOException {
    return is.available();
  }

  public void close() throws IOException {
    is.close();
  }

  public synchronized void mark(int readlimit) {
    is.mark(readlimit);
  }

  public synchronized void reset() throws IOException {
    is.reset();
  }

  public boolean markSupported() {
    return is.markSupported();
  }
}
