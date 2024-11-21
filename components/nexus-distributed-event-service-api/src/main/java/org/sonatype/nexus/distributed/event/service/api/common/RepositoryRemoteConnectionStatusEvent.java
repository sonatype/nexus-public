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
package org.sonatype.nexus.distributed.event.service.api.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.distributed.event.service.api.EventType.UPDATED;

/**
 * Indicates repository is auto-blocked or unblocked.
 *
 * @since 3.41
 */
public class RepositoryRemoteConnectionStatusEvent
    extends DistributedEventSupport
{
  public static final String NAME = "RepositoryRemoteConnectionStatusEvent";

  private final String repositoryName;

  private final int remoteConnectionStatusTypeOrdinal;

  private final String reason;

  private final long blockedUntilMillis;

  private final String requestUrl;

  @JsonCreator
  public RepositoryRemoteConnectionStatusEvent(
      @JsonProperty("repositoryName") final String repositoryName,
      @JsonProperty("remoteConnectionStatusTypeOrdinal") final int remoteConnectionStatusTypeOrdinal,
      @JsonProperty("reason") final String reason,
      @JsonProperty("blockedUntilMillis") final long blockedUntilMillis,
      @JsonProperty("requestUrl") final String requestUrl)
  {
    super(UPDATED);
    this.repositoryName = checkNotNull(repositoryName);
    this.remoteConnectionStatusTypeOrdinal = remoteConnectionStatusTypeOrdinal;
    this.reason = reason;
    this.blockedUntilMillis = blockedUntilMillis;
    this.requestUrl = requestUrl;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getReason() {
    return reason;
  }

  public int getRemoteConnectionStatusTypeOrdinal() {
    return remoteConnectionStatusTypeOrdinal;
  }

  public long getBlockedUntilMillis() {
    return blockedUntilMillis;
  }

  public String getRequestUrl() {
    return requestUrl;
  }

  @Override
  public String toString() {
    return "RepositoryRemoteConnectionStatusEvent{" +
        "repositoryName='" + repositoryName + '\'' + "," +
        "reason='" + reason + '\'' +
        '}';
  }
}
