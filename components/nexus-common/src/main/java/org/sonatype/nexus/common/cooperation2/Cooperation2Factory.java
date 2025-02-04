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
package org.sonatype.nexus.common.cooperation2;

import java.time.Duration;

/**
 * Supplies {@link Cooperation2} points allowing different threads to cooperate on computationally intensive tasks.
 *
 * @since 3.41
 */
public interface Cooperation2Factory
{
  /**
   * Start configuring a new {@link Cooperation2} point.
   */
  Builder configure();

  /**
   * Fluent builder for configuring {@link Cooperation2} points.
   */
  interface Builder
  {
    /**
     * @param majorTimeout when waiting for the main I/O request
     */
    Builder majorTimeout(Duration majorTimeout);

    /**
     * @param minorTimeout when waiting for any I/O dependencies
     */
    Builder minorTimeout(Duration minorTimeout);

    /**
     * @param threadsPerKey limits the threads waiting under each key
     */
    Builder threadsPerKey(int threadsPerKey);

    /**
     * @params disabled indicates whether the resulting co-operation should disable concurrency controls.
     */
    Builder enabled(boolean disabled);

    /**
     * Builds a new {@link Cooperation2} point with this configuration.
     *
     * @param id unique identifier for this cooperation point
     */
    Cooperation2 build(String id);

    /**
     * Builds a new {@link Cooperation2} point with this configuration.
     *
     * @param id unique identifier for this cooperation point
     */
    Cooperation2 build(Class<?> id, String... keys);
  }
}
