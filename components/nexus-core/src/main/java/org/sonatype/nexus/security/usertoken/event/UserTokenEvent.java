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
package org.sonatype.nexus.security.usertoken.event;

public class UserTokenEvent
{
  public enum UserTokenEventTypes {
    CREATED("Created"),
    DELETED("Deleted"),
    ALL_DELETED("All Deleted"),
    PURGED("Purged"),
    CONFIG_UPDATED("Configuration Changed");

    private final String type;

    UserTokenEventTypes(final String type) {
      this.type = type;
    }

    public String getType() {
      return this.type;
    }
  }

  private final UserTokenEventTypes eventType;

  public UserTokenEvent(final UserTokenEventTypes eventType) {
    this.eventType = eventType;
  }

  public UserTokenEventTypes getEventType() {
    return eventType;
  }
}
