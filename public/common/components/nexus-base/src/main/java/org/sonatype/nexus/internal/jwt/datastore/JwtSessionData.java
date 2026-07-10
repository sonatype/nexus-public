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

/**
 * Data object representing a revoked JWT session or a user-wide JWT invalidation.
 * The {@link #type} discriminates: {@code "SESSION"} for per-session revocations (from logout),
 * {@code "USER_INVALIDATION"} for per-user cutoffs (from password change).
 */
public class JwtSessionData
{
  public static final String TYPE_SESSION = "SESSION";

  public static final String TYPE_USER_INVALIDATION = "USER_INVALIDATION";

  private String userSessionId;

  private String username;

  private String userSource;

  private OffsetDateTime revokedAt;

  private OffsetDateTime expiresAt;

  private String type = TYPE_SESSION;

  public String getUserSessionId() {
    return userSessionId;
  }

  public void setUserSessionId(final String userSessionId) {
    this.userSessionId = userSessionId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getUserSource() {
    return userSource;
  }

  public void setUserSource(final String userSource) {
    this.userSource = userSource;
  }

  public OffsetDateTime getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(final OffsetDateTime revokedAt) {
    this.revokedAt = revokedAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(final OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }
}
