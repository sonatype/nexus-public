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
package org.sonatype.nexus.security.internal;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashSpi;

/**
 * Test-only {@link HashSpi} registered via {@code META-INF/services} so it is discovered by
 * {@link CachingHashService}'s {@link java.util.ServiceLoader} scan. It implements a synthetic algorithm that no real
 * request uses, and counts how many times it is instantiated — letting a test prove that provider discovery happens
 * once per {@link CachingHashService} instance rather than on every {@code computeHash} call (the regression the class
 * fixes). Its hashing methods are never invoked for the synthetic algorithm.
 */
public class CountingHashSpi
    implements HashSpi
{
  static final String ALGORITHM = "COUNTING-TEST";

  private static final AtomicInteger INSTANTIATIONS = new AtomicInteger();

  public CountingHashSpi() {
    INSTANTIATIONS.incrementAndGet();
  }

  static int instantiations() {
    return INSTANTIATIONS.get();
  }

  @Override
  public Set<String> getImplementedAlgorithms() {
    return Set.of(ALGORITHM);
  }

  @Override
  public Hash fromString(final String format) {
    throw new UnsupportedOperationException("CountingHashSpi is discovery-only");
  }

  @Override
  public HashFactory newHashFactory(final Random random) {
    throw new UnsupportedOperationException("CountingHashSpi is discovery-only");
  }
}
