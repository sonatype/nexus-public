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
package org.sonatype.nexus.blobstore;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;

import com.google.common.io.CountingInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A utility to log how fast the input stream was read.
 *
 * @since 3.21
 */
public class PerformanceLoggingInputStream
    extends FilterInputStream
{
  private final PerformanceLogger performanceLogger;

  private final CountingInputStream countingInputStream;

  private long totalNanosElapsed;

  public PerformanceLoggingInputStream(final InputStream source, final PerformanceLogger performanceLogger) {
    this(new CountingInputStream(source), performanceLogger);

  }

  private PerformanceLoggingInputStream(
      final CountingInputStream countingInputStream,
      final PerformanceLogger performanceLogger)
  {
    super(countingInputStream);
    this.countingInputStream = checkNotNull(countingInputStream);
    this.performanceLogger = checkNotNull(performanceLogger);
  }

  @Override
  public void close() throws IOException {
    in.close();
    performanceLogger.logRead(countingInputStream.getCount(), totalNanosElapsed);
  }

  @Override
  public int read() throws IOException {
    long start = System.nanoTime();
    int val = in.read();
    totalNanosElapsed += System.nanoTime() - start;
    return val;
  }

  @Override
  public int read(byte[] b) throws IOException {
    long start = System.nanoTime();
    int bytesRead = in.read(b);
    totalNanosElapsed += System.nanoTime() - start;
    return bytesRead;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    long start = System.nanoTime();
    int bytesRead = in.read(b, off, len);
    totalNanosElapsed += System.nanoTime() - start;
    return bytesRead;
  }
}
