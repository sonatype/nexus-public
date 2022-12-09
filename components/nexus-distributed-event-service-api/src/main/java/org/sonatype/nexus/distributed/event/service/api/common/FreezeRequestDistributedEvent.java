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

import org.sonatype.nexus.common.app.FreezeRequest;
import org.sonatype.nexus.distributed.event.service.api.EventType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Indicates that database freeze or release request has been initiated.
 */
public class FreezeRequestDistributedEvent
    extends DistributedEventSupport
{

  private final boolean frozen;

  private final String token;

  private final String reason;

  private final String frozenAt;

  private final String frozenBy;

  private final String frozenByIp;

  @JsonCreator
  public FreezeRequestDistributedEvent(
      @JsonProperty("frozen") final boolean frozen,
      @JsonProperty("token") final String token,
      @JsonProperty("reason") final String reason,
      @JsonProperty("frozenAt") final String frozenAt,
      @JsonProperty("frozenBy") final String frozenBy,
      @JsonProperty("frozenByIp") final String frozenByIp)
  {
    super(EventType.UPDATED);
    this.frozen = frozen;
    this.token = token;
    this.reason = reason;
    this.frozenAt = frozenAt;
    this.frozenBy = frozenBy;
    this.frozenByIp = frozenByIp;
  }

  public FreezeRequestDistributedEvent(final boolean frozen, final FreezeRequest request) {
    this(frozen, request.token().orElse(null), request.reason(), request.frozenAt().toString(),
        request.frozenBy().orElse(null), request.frozenByIp().orElse(null));
  }

  public boolean isFrozen() {
    return frozen;
  }

  public String getToken() {
    return token;
  }

  public String getReason() {
    return reason;
  }

  public String getFrozenAt() {
    return frozenAt;
  }

  public String getFrozenBy() {
    return frozenBy;
  }

  public String getFrozenByIp() {
    return frozenByIp;
  }

  @Override
  public String toString() {
    return "DatabaseFreezeChangeEvent{" +
        "frozen=" + frozen +
        '}';
  }
}
