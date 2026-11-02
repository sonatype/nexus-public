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

import java.util.Map;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Event fired when the format tokens (npm, Docker, ...) of a user are removed by a token reset. Unlike
 * {@link UserTokenDeletedEvent} it is posted whether or not the user also had a user-token record.
 */
public class UserTokenFormatTokensResetEvent
    extends UserTokenEvent
{
  private final String username;

  private final String targetRealm;

  private final Map<String, Integer> deletedByDomain;

  private final String adminUserId;

  private final String adminRealm;

  /**
   * @param adminUserId the administrator who performed the reset, or {@code null} for a self-service reset
   */
  public UserTokenFormatTokensResetEvent(
      final String username,
      final String targetRealm,
      final Map<String, Integer> deletedByDomain,
      @Nullable final String adminUserId,
      @Nullable final String adminRealm)
  {
    super(UserTokenEventTypes.FORMAT_TOKENS_RESET);
    this.username = requireNonNull(username);
    this.targetRealm = targetRealm;
    this.deletedByDomain = Map.copyOf(requireNonNull(deletedByDomain));
    this.adminUserId = adminUserId;
    this.adminRealm = adminRealm;
  }

  public String getUsername() {
    return username;
  }

  public String getTargetRealm() {
    return targetRealm;
  }

  /**
   * The number of tokens removed per API-Key domain, for example {@code NpmToken=1}.
   */
  public Map<String, Integer> getDeletedByDomain() {
    return deletedByDomain;
  }

  @Nullable
  public String getAdminUserId() {
    return adminUserId;
  }

  @Nullable
  public String getAdminRealm() {
    return adminRealm;
  }
}
