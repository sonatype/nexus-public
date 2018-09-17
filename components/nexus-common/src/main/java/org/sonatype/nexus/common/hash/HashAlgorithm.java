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

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * A hash algorithm name paired with a {@link HashFunction}.
 *
 * @since 3.0
 */
public class HashAlgorithm
{
  public static final HashAlgorithm MD5 = new HashAlgorithm("md5", Hashing.md5());

  public static final HashAlgorithm SHA1 = new HashAlgorithm("sha1", Hashing.sha1());

  public static final HashAlgorithm SHA256 = new HashAlgorithm("sha256", Hashing.sha256());

  public static final HashAlgorithm SHA512 = new HashAlgorithm("sha512", Hashing.sha512());

  public static final Map<String, HashAlgorithm> ALL_HASH_ALGORITHMS = ImmutableMap
      .of(MD5.name, MD5, SHA1.name, SHA1, SHA256.name, SHA256, SHA512.name, SHA512);

  private final String name;

  private final HashFunction function;

  public HashAlgorithm(String name, HashFunction function) {
    this.name = checkNotNull(name);
    this.function = checkNotNull(function);
  }

  public String name() {
    return name;
  }

  public HashFunction function() {
    return function;
  }

  public static Optional<HashAlgorithm> getHashAlgorithm(final String algorithm) {
    return ofNullable(ALL_HASH_ALGORITHMS.get(algorithm));
  }
}
