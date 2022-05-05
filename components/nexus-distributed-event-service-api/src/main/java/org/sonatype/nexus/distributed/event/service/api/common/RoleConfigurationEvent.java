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

import org.sonatype.nexus.distributed.event.service.api.EventType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Indicates that a 'Role' configuration has been created, updated or deleted.
 *
 * @since 3.38
 */
public class RoleConfigurationEvent
    extends DistributedEventSupport
{
  public static final String NAME = "RoleConfigurationEvent";

  private final String roleId;

  @JsonCreator
  public RoleConfigurationEvent(
      @JsonProperty("roleId") String roleId,
      @JsonProperty("eventType") final EventType eventType)
  {
    super(eventType);
    this.roleId = checkNotNull(roleId);
  }

  public String getRoleId() {
    return roleId;
  }

  @Override
  public String toString() {
    return "RoleConfigurationEvent{" +
        "roleId='" + roleId + '\'' +
        '}';
  }
}
