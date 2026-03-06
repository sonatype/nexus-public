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
 * Event fired when an administrator lists user tokens.
 */
public class UserTokenAdminListedEvent
    extends UserTokenEvent
{
  private final String adminUserId;

  private final String adminRealm;

  private final String realmFilter;

  private final String userIdFilter;

  private final boolean includeExpired;

  private final int resultCount;

  public UserTokenAdminListedEvent(
      final String adminUserId,
      final String adminRealm,
      final String realmFilter,
      final String userIdFilter,
      final boolean includeExpired,
      final int resultCount)
  {
    super(UserTokenEventTypes.ADMIN_LISTED);
    this.adminUserId = adminUserId;
    this.adminRealm = adminRealm;
    this.realmFilter = realmFilter;
    this.userIdFilter = userIdFilter;
    this.includeExpired = includeExpired;
    this.resultCount = resultCount;
  }

  public String getAdminUserId() {
    return adminUserId;
  }

  public String getAdminRealm() {
    return adminRealm;
  }

  public String getRealmFilter() {
    return realmFilter;
  }

  public String getUserIdFilter() {
    return userIdFilter;
  }

  public boolean isIncludeExpired() {
    return includeExpired;
  }

  public int getResultCount() {
    return resultCount;
  }
}
