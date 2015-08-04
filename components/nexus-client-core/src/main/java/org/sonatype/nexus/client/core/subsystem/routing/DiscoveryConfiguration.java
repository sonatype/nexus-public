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
package org.sonatype.nexus.client.core.subsystem.routing;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The routing discovery configuration for a proxy repository.
 *
 * @author cstamas
 * @since 2.4
 */
public class DiscoveryConfiguration
{
  private boolean enabled;

  private int intervalHours;

  /**
   * Constructor.
   */
  public DiscoveryConfiguration(final boolean enabled, final int intervalHours) {
    setEnabled(enabled);
    setIntervalHours(intervalHours);
  }

  /**
   * Returns {@code true} if discovery is enabled.
   *
   * @return {@code true} if enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Toggles the enabled state of discovery.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns the amount of hours that makes the interval of discovery updates.
   *
   * @return hours of the interval.
   */
  public int getIntervalHours() {
    return intervalHours;
  }

  /**
   * Sets the interval of discovery updates, in hours.
   */
  public void setIntervalHours(int intervalHours) {
    checkArgument(intervalHours >= 1, "Only positive number allowed but we got " + intervalHours);
    this.intervalHours = intervalHours;
  }
}
