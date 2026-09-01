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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.crypto.hash.HashSpi;

/**
 * A {@link DefaultHashService} that resolves its {@link HashSpi} providers once, at construction time, instead of
 * per hash.
 *
 * <p>
 * Shiro 2.x's stock {@link org.apache.shiro.crypto.hash.HashProvider#getByAlgorithmName(String)} (used by
 * {@link DefaultHashService#computeHash(HashRequest)}) calls {@link ServiceLoader#load(Class)} on <em>every</em>
 * invocation. Because credential verification runs {@code computeHash} on every Basic-Auth'd request, that repeated
 * {@code ServiceLoader} lookup re-scans {@code META-INF/services} across every (nested) jar on the classpath on each
 * request, generating heavy {@code UrlJarFiles$Cache} lock contention under load.
 *
 * <p>
 * This class eliminates that per-request work by materialising every {@link HashSpi} provider once, in the
 * constructor, into an immutable {@code algorithm -> provider} map. Per-request resolution is then an O(1) map lookup
 * that touches no shared mutable state. Materialising once in the constructor also side-steps the fact that
 * {@link ServiceLoader} is explicitly documented as not safe for concurrent use: the loader is fully drained on a
 * single thread here and never touched again, and the resulting map is safely published via a {@code final} field.
 */
class CachingHashService
    extends DefaultHashService
{
  private final SecureRandom random;

  private final Map<String, HashSpi> hashSpiByAlgorithm;

  CachingHashService() {
    // NOTE: DefaultHashService also holds a private SecureRandom, but it is not accessible to subclasses, so we
    // keep our own for the newHashFactory(random) call below.
    this.random = new SecureRandom();
    this.hashSpiByAlgorithm = loadHashSpisByAlgorithm();
  }

  @Override
  public Hash computeHash(final HashRequest request) {
    if (request == null || request.getSource() == null || request.getSource().isEmpty()) {
      return null;
    }

    String algorithmName = getAlgorithmName(request);

    HashSpi hashSpi = hashSpiByAlgorithm.get(algorithmName);
    if (hashSpi != null) {
      return hashSpi.newHashFactory(random).generate(request);
    }

    throw new UnsupportedOperationException("Cannot create a hash with the given algorithm: " + algorithmName);
  }

  /**
   * Drains the {@link HashSpi} {@link ServiceLoader} exactly once and indexes each provider by the algorithm names it
   * implements. Runs single-threaded from the constructor. The first provider registered for a given algorithm wins,
   * matching the first-wins result of Shiro's stock {@code HashProvider} (a sequential stream + {@code findAny()} over
   * the same {@code ServiceLoader} iteration order).
   *
   * <p>
   * Discovery is bound to the classloader that loaded {@link HashSpi} (which also loads the bundled
   * {@code SimpleHashProvider} supplying SHA-2), rather than the constructing thread's context classloader, so the
   * result does not depend on which thread instantiates this service. If no providers are found we fail fast with a
   * clear message instead of letting every later credential verification throw an opaque
   * {@link UnsupportedOperationException}.
   */
  private static Map<String, HashSpi> loadHashSpisByAlgorithm() {
    Map<String, HashSpi> byAlgorithm = new HashMap<>();
    for (HashSpi hashSpi : ServiceLoader.load(HashSpi.class, HashSpi.class.getClassLoader())) {
      for (String algorithm : hashSpi.getImplementedAlgorithms()) {
        byAlgorithm.putIfAbsent(algorithm, hashSpi);
      }
    }
    if (byAlgorithm.isEmpty()) {
      throw new IllegalStateException("No " + HashSpi.class.getName()
          + " providers were found on the classpath; password hashing cannot function"
          + " (is shiro-crypto-hash present?)");
    }
    return Map.copyOf(byAlgorithm);
  }
}
