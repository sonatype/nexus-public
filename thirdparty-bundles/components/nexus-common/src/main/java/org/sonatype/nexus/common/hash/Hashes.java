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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link HashCode} helpers.
 *
 * @since 3.0
 */
public final class Hashes
{
  private Hashes() {
    // no instance
  }

  /**
   * Computes the hash of the given stream using the given algorithm.
   */
  public static HashCode hash(final HashAlgorithm algorithm, final InputStream inputStream) throws IOException {
    checkNotNull(algorithm);
    checkNotNull(inputStream);

    try (HashingInputStream hashingStream = new HashingInputStream(algorithm.function(), inputStream)) {
      ByteStreams.copy(hashingStream, ByteStreams.nullOutputStream());
      return hashingStream.hash();
    }
  }

  /**
   * Computes the hash of the given stream using multiple algorithms in one pass.
   */
  public static Map<HashAlgorithm, HashCode> hash(final Iterable<HashAlgorithm> algorithms,
                                                  final InputStream inputStream)
      throws IOException
  {
    checkNotNull(algorithms);
    checkNotNull(inputStream);

    try (MultiHashingInputStream hashingStream = new MultiHashingInputStream(algorithms, inputStream)) {
      ByteStreams.copy(hashingStream, ByteStreams.nullOutputStream());
      return hashingStream.hashes();
    }
  }

  /**
   * Computes the hash of the given stream using the given function.
   */
  public static HashCode hash(final HashFunction function, final InputStream input) throws IOException {
    Hasher hasher = function.newHasher();
    OutputStream output = Funnels.asOutputStream(hasher);
    ByteStreams.copy(input, output);
    return hasher.hash();
  }
}
