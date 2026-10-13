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
package org.sonatype.nexus.bootstrap.entrypoint.distribution;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.util.ClassUtils;

/**
 * Resolves the active {@link NexusDistributionType} from <b>packaging</b> &mdash; the presence of a
 * distribution-specific
 * marker class on the classpath.
 *
 * <h2>Why packaging, and not configuration</h2>
 * The distribution axis must never be settable through a property, environment variable, or system property. If it
 * were,
 * a self-hosted instance could declare itself {@code cloud} and inherit the more permissive cloud licensing behaviour.
 * The value therefore comes from a class that only the corresponding distribution puts on the classpath:
 * <ul>
 * <li>{@link #CLOUD_MARKER} &mdash; contributed only by the cloud distribution ({@code nexus-cloud-edition}),
 * mirroring the {@code CloudNexusEdition} precedent.</li>
 * <li>{@link #SELF_HOSTED_MARKERS} &mdash; explicitly contributed by the self-hosted Pro/CE distribution. Self-hosted
 * is a positively-declared value, <em>not</em> the mere absence of the cloud marker.</li>
 * </ul>
 *
 * <h2>Available from the first component scan</h2>
 * Because detection reads only the classpath, it does not depend on any bean, license, database, lifecycle event, or
 * published property. It is therefore correct from the very first component scan onward and is immune to the scan-order
 * timing hazard that affects the property-based {@code @ConditionalOnEdition} evaluation (see NEXUS-46395).
 *
 * <h2>Cloud precedence</h2>
 * If the cloud marker is present the result is {@link NexusDistributionType#CLOUD} regardless of any other marker,
 * matching
 * the "if this class is loaded, cloud wins" rule of {@code CloudNexusEdition}. This makes detection robust even if a
 * self-hosted marker were ever to leak onto a cloud classpath.
 */
public final class NexusDistribution
{
  /**
   * Marker class contributed <b>only</b> by the cloud distribution.
   */
  private static final String CLOUD_MARKER =
      "com.sonatype.nexus.bootstrap.entrypoint.distribution.cloud.CloudDistributionMarker";

  /**
   * Marker classes contributed by the self-hosted distributions; presence of any of them means self-hosted. Only the
   * Pro/CE distribution contributes one (via {@code nexus-community-edition}). The OSS
   * ({@code nexus-repository-core-edition}) distribution ships none: it does not depend on the (private) machinery that
   * reads these markers ({@code OnDistributionCondition} lives in {@code nexus-private-spring}, which no public module
   * depends on), so {@link #detect(ClassLoader)} is never invoked there and a marker would be dead weight.
   */
  private static final List<String> SELF_HOSTED_MARKERS =
      List.of("com.sonatype.nexus.bootstrap.entrypoint.distribution.selfhosted.SelfHostedDistributionMarker");

  /**
   * Test-only override. This is a JVM-internal seam for unit tests, deliberately <em>not</em> a configuration channel:
   * it cannot be reached from a property, environment variable, or system property, so it does not weaken the
   * "packaging decides distribution" guarantee.
   */
  private static volatile NexusDistributionType testOverride;

  private NexusDistribution() {
    // static utility
  }

  /**
   * Detect the active distribution for the given class loader.
   *
   * @param classLoader the class loader to probe (may be {@code null}, in which case the default class loader is used)
   * @return the active {@link NexusDistributionType}, or {@code null} when no packaging marker is on the classpath
   *         (only
   *         expected in bare test contexts; production distributions always contribute a marker)
   */
  @Nullable
  public static NexusDistributionType detect(@Nullable final ClassLoader classLoader) {
    NexusDistributionType forced = testOverride;
    if (forced != null) {
      return forced;
    }
    // Cloud wins if its marker is loaded (see class Javadoc: cloud precedence).
    if (ClassUtils.isPresent(CLOUD_MARKER, classLoader)) {
      return NexusDistributionType.CLOUD;
    }
    for (String marker : SELF_HOSTED_MARKERS) {
      if (ClassUtils.isPresent(marker, classLoader)) {
        return NexusDistributionType.SELF_HOSTED;
      }
    }
    return null;
  }

  /**
   * Force the detected distribution. <b>Test use only.</b> Callers must reset with {@link #clearTestOverride()} in
   * teardown to avoid leaking state across tests. Never call this from production code: it is a JVM-internal seam that
   * bypasses packaging-based detection, and using it outside tests would defeat the "packaging decides distribution"
   * guarantee.
   */
  @VisibleForTesting
  public static void setTestOverride(@Nullable final NexusDistributionType type) {
    testOverride = type;
  }

  /**
   * Clear any {@link #setTestOverride(NexusDistributionType) test override}. <b>Test use only.</b>
   */
  @VisibleForTesting
  public static void clearTestOverride() {
    testOverride = null;
  }
}
