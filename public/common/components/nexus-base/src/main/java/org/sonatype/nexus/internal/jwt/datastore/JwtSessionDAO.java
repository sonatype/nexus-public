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
package org.sonatype.nexus.internal.jwt.datastore;

import java.time.OffsetDateTime;

import org.sonatype.nexus.datastore.api.ContentDataAccess;

import org.apache.ibatis.annotations.Param;

/**
 * DAO for accessing revoked JWT sessions
 */
public interface JwtSessionDAO
    extends ContentDataAccess
{
  /**
   * Create the jwt_session table if it does not exist.
   */
  void createSchema();

  /**
   * Revoke a JWT session by storing its userSessionId.
   *
   * @param session the session data to revoke
   */
  void revokeSession(@Param("session") JwtSessionData session);

  /**
   * Revoke a JWT session without the type column (pre-2.127 schema).
   *
   * @param session the session data to revoke
   */
  void revokeSessionLegacy(@Param("session") JwtSessionData session);

  /**
   * Check if a JWT session is revoked.
   *
   * @param userSessionId the unique session identifier from JWT claim
   * @return true if the session is revoked, false otherwise
   */
  boolean isRevoked(@Param("userSessionId") String userSessionId);

  /**
   * Check if a JWT session is revoked without the type column (pre-2.127 schema).
   *
   * @param userSessionId the unique session identifier from JWT claim
   * @return true if the session is revoked, false otherwise
   */
  boolean isRevokedLegacy(@Param("userSessionId") String userSessionId);

  /**
   * Delete expired session revocations that are past their JWT expiration time.
   *
   * @return the number of rows deleted
   */
  int deleteExpiredSessions();

  /**
   * Record a user-wide JWT invalidation cutoff. Any JWT for this (username, userSource) with
   * {@code iat <= session.revokedAt} is considered revoked from the filter's perspective.
   *
   * @param session populated with userSessionId (synthetic UUID), username, userSource,
   *          revokedAt (cutoff), and expiresAt (cutoff + JWT max lifetime)
   */
  void invalidateUser(@Param("session") JwtSessionData session);

  /**
   * Check whether the given username has been globally invalidated after the JWT was issued.
   *
   * <p>
   * A password change records a USER_INVALIDATION row that applies to every JWT session for
   * that username, across all realms the user may be authenticating through. This method returns
   * {@code true} if any USER_INVALIDATION row exists for {@code username} whose {@code revoked_at}
   * is strictly later than {@code iat} — regardless of the realm the JWT was issued from.
   *
   * @param username the username from the JWT
   * @param iat the JWT issued-at timestamp
   * @return true if the username has been invalidated after the JWT was issued
   */
  boolean isUserInvalidatedAfter(
      @Param("username") String username,
      @Param("iat") OffsetDateTime iat);
}
