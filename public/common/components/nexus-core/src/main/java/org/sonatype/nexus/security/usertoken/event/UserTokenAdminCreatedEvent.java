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
 * Event fired when an administrator creates a user token for another user.
 */
public class UserTokenAdminCreatedEvent
    extends UserTokenEvent
{
  private final String targetUserId;

  private final String targetRealm;

  private final String adminUserId;

  private final String adminRealm;

  private final String nameCode;

  public UserTokenAdminCreatedEvent(
      final String targetUserId,
      final String targetRealm,
      final String adminUserId,
      final String adminRealm,
      final String nameCode)
  {
    super(UserTokenEventTypes.ADMIN_CREATED);
    this.targetUserId = targetUserId;
    this.targetRealm = targetRealm;
    this.adminUserId = adminUserId;
    this.adminRealm = adminRealm;
    this.nameCode = nameCode;
  }

  public String getTargetUserId() {
    return targetUserId;
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

  public String getNameCode() {
    return nameCode;
  }
}
