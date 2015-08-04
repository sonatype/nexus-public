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
package org.sonatype.nexus.proxy.maven.routing;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Configuration properties of the autorouting.
 *
 * @author cstamas
 * @since 2.4
 */
public class DiscoveryConfig
{
  private final boolean enabled;

  private final long discoveryInterval;

  /**
   * Constructor.
   */
  public DiscoveryConfig(final boolean enabled, final long discoveryInterval) {
    checkArgument(discoveryInterval > 0, "Discovery interval must be strictly positive, greater than 0!");
    this.enabled = enabled;
    this.discoveryInterval = discoveryInterval;
  }

  /**
   * Enabled flag for remote discovery.
   *
   * @return {@code true} if enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Discovery interval in millis.
   *
   * @return interval in millis.
   */
  public long getDiscoveryInterval() {
    return discoveryInterval;
  }
}
