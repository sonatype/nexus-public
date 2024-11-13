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

public class UserTokenConfigChangedEvent extends UserTokenEvent
{
  private final boolean enabled;

  private final boolean protectContent;

  private final boolean expirationEnabled;

  private final int expirationDays;

  public UserTokenConfigChangedEvent(final boolean enabled, final boolean protectContent,
                                     final boolean expirationEnabled, final int expirationDays) {
    super(UserTokenEventTypes.CONFIG_UPDATED);
    this.enabled = enabled;
    this.protectContent = protectContent;
    this.expirationEnabled = expirationEnabled;
    this.expirationDays = expirationDays;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isProtectContent() {
    return protectContent;
  }

  public boolean isExpirationEnabled() {
    return expirationEnabled;
  }

  public int getExpirationDays() {
    return expirationDays;
  }
}
