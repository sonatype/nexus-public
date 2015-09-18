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
package org.sonatype.nexus.common.hash;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.HashingInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link InputStream} that maintains multiple hashes and the number of bytes of data read from it.
 *
 * @see HashingInputStream
 * @since 3.0
 */
public class MultiHashingInputStream
    extends FilterInputStream
{
  private final Map<HashAlgorithm, Hasher> hashers = Maps.newLinkedHashMap();

  private long count;

  public MultiHashingInputStream(Iterable<HashAlgorithm> algorithms, InputStream inputStream) {
    super(checkNotNull(inputStream));
    for (HashAlgorithm algorithm : checkNotNull(algorithms)) {
      hashers.put(algorithm, algorithm.function().newHasher());
    }
  }

  @Override
  public int read() throws IOException {
    int b = in.read();
    if (b != -1) {
      for (Hasher hasher : hashers.values()) {
        hasher.putByte((byte) b);
      }
      count++;
    }
    return b;
  }

  @Override
  public int read(@Nonnull byte[] bytes, int off, int len) throws IOException {
    int numRead = in.read(bytes, off, len);
    if (numRead != -1) {
      for (Hasher hasher : hashers.values()) {
        hasher.putBytes(bytes, off, numRead);
      }
      count += numRead;
    }
    return numRead;
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public void mark(int readlimit) {
    // no-op
  }

  @Override
  public void reset() throws IOException {
    throw new IOException("reset not supported");
  }

  /**
   * Gets the {@link HashCode}s based on the data read from this stream.
   */
  public Map<HashAlgorithm, HashCode> hashes() {
    Map<HashAlgorithm, HashCode> hashes = Maps.newHashMap();
    for (HashAlgorithm algorithm : hashers.keySet()) {
      Hasher hasher = hashers.get(algorithm);
      hashes.put(algorithm, hasher.hash());
    }
    return hashes;
  }

  /**
   * Gets the number of bytes read from this stream.
   */
  public long count() {
    return count;
  }
}
