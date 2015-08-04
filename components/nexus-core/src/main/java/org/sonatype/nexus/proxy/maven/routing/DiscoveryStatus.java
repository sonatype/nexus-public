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
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Remote discovery status of a repository.
 *
 * @author cstamas
 * @since 2.4
 */
public class DiscoveryStatus
{
  /**
   * Remote discovery status enumeration.
   */
  public static enum DStatus
  {
    /**
     * Given repository is not a proxy (remote discovery not applicable).
     */
    NOT_A_PROXY,

    /**
     * Remote discovery not enabled for given repository.
     */
    DISABLED,

    /**
     * Remote discovery enabled without results yet (is still working).
     */
    ENABLED_IN_PROGRESS,

    /**
     * Remote discovery enabled but not possible to perform it (like proxy being blocked).
     */
    ENABLED_NOT_POSSIBLE,

    /**
     * Remote discovery enabled and was successful.
     */
    SUCCESSFUL,

    /**
     * Remote discovery enabled and was unsuccessful.
     */
    UNSUCCESSFUL,

    /**
     * Remote discovery enabled and failed (due to some error).
     */
    ERROR;

    /**
     * Returns {@code true} if this discovery status represents a case when proxy remote discovery is enabled (by
     * configuration).
     *
     * @return {@code true} if proxy who's status is this has remote discovery enabled.
     */
    public boolean isEnabled() {
      return this.ordinal() >= ENABLED_IN_PROGRESS.ordinal();
    }
  }

  private final DStatus status;

  private final String lastDiscoveryStrategy;

  private final String lastDiscoveryMessage;

  private final long lastDiscoveryTimestamp;

  /**
   * Constructor for statuses that represent state where discovery not yet ran.
   */
  public DiscoveryStatus(final DStatus status) {
    checkArgument(status.ordinal() < DStatus.ENABLED_NOT_POSSIBLE.ordinal());
    this.status = checkNotNull(status);
    this.lastDiscoveryStrategy = null;
    this.lastDiscoveryMessage = null;
    this.lastDiscoveryTimestamp = -1;
  }

  /**
   * Constructor for statuses that represent state where discovery did ran.
   */
  public DiscoveryStatus(final DStatus status, final String lastDiscoveryStrategy,
                         final String lastDiscoveryMessage, final long lastDiscoveryTimestamp)
  {
    checkArgument(status.ordinal() >= DStatus.ENABLED_NOT_POSSIBLE.ordinal());
    checkArgument(lastDiscoveryTimestamp > 0);
    this.status = checkNotNull(status);
    this.lastDiscoveryStrategy = checkNotNull(lastDiscoveryStrategy);
    this.lastDiscoveryMessage = checkNotNull(lastDiscoveryMessage);
    this.lastDiscoveryTimestamp = lastDiscoveryTimestamp;
  }

  /**
   * Remote discovery status.
   *
   * @return remote discovery status.
   */
  public DStatus getStatus() {
    return status;
  }

  /**
   * Last discovery's strategy, if last execution exists.
   *
   * @return strategy ID or {@code null}.
   */
  public String getLastDiscoveryStrategy() {
    return lastDiscoveryStrategy;
  }

  /**
   * Last discovery's message, if last execution exists.
   *
   * @return message or {@code null}.
   */
  public String getLastDiscoveryMessage() {
    return lastDiscoveryMessage;
  }

  /**
   * Last discovery's run timestamp, if last execution exists.
   *
   * @return last run timestamp or -1 if not run yet.
   */
  public long getLastDiscoveryTimestamp() {
    return lastDiscoveryTimestamp;
  }
}
