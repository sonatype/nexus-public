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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.HashingOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link OutputStream} that maintains multiple hashes.
 *
 * @see HashingOutputStream
 * @since 3.4
 */
public class MultiHashingOutputStream
    extends FilterOutputStream
{
  private final Map<HashAlgorithm, Hasher> hashers = new LinkedHashMap<>();

  public MultiHashingOutputStream(final Iterable<HashAlgorithm> algorithms, final OutputStream out) {
    super(checkNotNull(out));
    checkNotNull(algorithms);
    for (HashAlgorithm algorithm : algorithms) {
      hashers.put(algorithm, algorithm.function().newHasher());
    }
  }

  @Override
  public void write(final int b) throws IOException {
    out.write(b);
    for (Hasher hasher : hashers.values()) {
      hasher.putByte((byte) b);
    }
  }

  /**
   * Gets the {@link HashCode}s based on the data output to this stream.
   *
   * Note this makes a call to Hasher.hash() and therefore can only be called once. Calling this multiple times will
   * result in an exception.
   */
  public Map<HashAlgorithm, HashCode> hashes() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>(hashers.size());
    for (Entry<HashAlgorithm, Hasher> entry : hashers.entrySet()) {
      hashes.put(entry.getKey(), entry.getValue().hash());
    }
    return hashes;
  }
}
