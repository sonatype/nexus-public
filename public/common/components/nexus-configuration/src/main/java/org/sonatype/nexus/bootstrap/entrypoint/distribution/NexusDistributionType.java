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

/**
 * The <em>distribution</em> a running Nexus instance was packaged for.
 * <p>
 * Distribution is one of the two orthogonal axes that used to be conflated under the single "edition" concept:
 * <ul>
 * <li><b>distribution</b> &mdash; <em>where</em> Nexus runs: {@link #SELF_HOSTED} vs {@link #CLOUD} (this enum)</li>
 * <li><b>edition/license tier</b> &mdash; <em>what</em> is licensed: community, pro, pro+, enterprise
 * (see {@code NexusEdition})</li>
 * </ul>
 * The two are independent: a self-hosted instance and a cloud instance can both run the same licensed edition.
 * <p>
 * The active distribution is decided by <b>packaging</b>, never by configuration &mdash; see {@link NexusDistribution}
 * for how it is detected and why it must not come from a property, environment variable, or system property.
 */
public enum NexusDistributionType
{
  /**
   * Nexus is running as a self-hosted (customer-operated) instance. Contributed by the self-hosted distributions.
   */
  SELF_HOSTED("selfhosted"),

  /**
   * Nexus is running as the Sonatype-operated cloud service. Contributed by the cloud distribution only.
   */
  CLOUD("cloud");

  private final String id;

  NexusDistributionType(final String id) {
    this.id = id;
  }

  /**
   * Stable, lower-case identifier. Matches the attribute names on {@code @ConditionalOnDistribution}
   * ({@code selfhosted} / {@code cloud}).
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the stable {@link #getId() id} rather than the enum constant name, so condition messages and logs render
   * the same lower-case identifier ({@code selfhosted} / {@code cloud}) everywhere.
   */
  @Override
  public String toString() {
    return id;
  }
}
