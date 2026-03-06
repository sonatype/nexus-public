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

/**
 * Event fired when an administrator deletes a user token for another user.
 */
public class UserTokenAdminDeletedEvent
    extends UserTokenDeletedEvent
{
  private final String targetRealm;

  private final String adminUserId;

  private final String adminRealm;

  public UserTokenAdminDeletedEvent(
      final String targetUserId,
      final String targetRealm,
      final String adminUserId,
      final String adminRealm)
  {
    super(UserTokenEventTypes.ADMIN_DELETED, 1, targetUserId);
    this.targetRealm = targetRealm;
    this.adminUserId = adminUserId;
    this.adminRealm = adminRealm;
  }

  public String getTargetRealm() {
    return targetRealm;
  }

  public String getAdminUserId() {
    return adminUserId;
  }

  public String getAdminRealm() {
    return adminRealm;
  }
}
