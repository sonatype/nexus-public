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

import java.time.Duration;

import org.sonatype.goodies.lifecycle.LifecycleSupport;

/**
 * Common scaffolding for {@link CooperationFactory} implementations.
 *
 * @since 3.14
 */
public abstract class CooperationFactorySupport
    extends LifecycleSupport
    implements CooperationFactory
{
  @Override
  public Builder configure() {
    return new MutableConfig();
  }

  /**
   * Builds a new {@link Cooperation} point with the given configuration.
   *
   * @param id unique identifier for this cooperation point
   */
  protected abstract Cooperation build(String id, Config config);

  /**
   * Configuration holder for {@link Cooperation} points.
   */
  public static class Config
  {
    protected int majorTimeoutSeconds = 0;

    protected int minorTimeoutSeconds = 0;

    protected int threadsPerKey = 0;

    public Duration majorTimeout() {
      return Duration.ofSeconds(majorTimeoutSeconds);
    }

    public Duration minorTimeout() {
      return Duration.ofSeconds(minorTimeoutSeconds);
    }

    public int threadsPerKey() {
      return threadsPerKey;
    }

    protected Config copy() {
      Config copy = new Config();
      copy.majorTimeoutSeconds = majorTimeoutSeconds;
      copy.minorTimeoutSeconds = minorTimeoutSeconds;
      copy.threadsPerKey = threadsPerKey;
      return copy;
    }
  }

  /**
   * Mutable {@link Builder} of {@link Config}s.
   */
  private final class MutableConfig
      extends Config
      implements Builder
  {
    @Override
    public Builder majorTimeout(final Duration majorTimeout) {
      this.majorTimeoutSeconds = (int) majorTimeout.getSeconds();
      return this;
    }

    @Override
    public Builder minorTimeout(final Duration minorTimeout) {
      this.minorTimeoutSeconds = (int) minorTimeout.getSeconds();
      return this;
    }

    @Override
    public Builder threadsPerKey(final int threadsPerKey) {
      this.threadsPerKey = threadsPerKey;
      return this;
    }

    @Override
    public Cooperation build(final String id) {
      return CooperationFactorySupport.this.build(id, copy());
    }
  }
}
