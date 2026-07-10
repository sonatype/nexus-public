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
package org.sonatype.nexus.security.authc;

import javax.annotation.Nullable;

/**
 * Tracks failed authentication attempts per username, enforcing exponential backoff
 * once the failure threshold is exceeded.
 *
 * <p>
 * IP-based rate limiting is intentionally omitted. Per-IP application-layer blocking
 * enables cross-user denial of service in shared-IP environments (corporate NAT, CGNAT,
 * VPN egress) and is explicitly not recommended by OWASP. IP-based protection belongs
 * at the infrastructure layer (WAF, load balancer) where it can be independently allowlisted.
 */
public interface AuthRateLimiterService
{

  /**
   * Checks whether the given username is currently rate-limited without incrementing the
   * failure counter. Use this for a pre-authentication guard to short-circuit blocked users
   * before credentials are validated.
   *
   * @param username the authenticating user; must not be {@code null}
   * @return {@code null} if the user is not currently blocked,
   *         or a {@link RateLimitResult} describing the backoff when blocked
   */
  @Nullable
  RateLimitResult check(String username);

  /**
   * Records a failed authentication attempt for the given username and checks whether
   * the request should be blocked. The counter is incremented on every call; call
   * {@link #recordSuccess} to clear it after a successful login.
   *
   * @param username the authenticating user; must not be {@code null}
   * @return {@code null} if the attempt count is within the threshold,
   *         or a {@link RateLimitResult} describing the backoff when blocked
   */
  @Nullable
  RateLimitResult checkAndRecord(String username);

  /**
   * Resets the failure counter for the given username upon a successful login.
   *
   * @param username the successfully authenticated user
   */
  void recordSuccess(String username);

  /**
   * Resets the failure counter for the given username unconditionally (e.g. on admin unlock).
   *
   * @param username the user whose counter should be cleared
   */
  void reset(String username);

  /**
   * Checks whether the given token hash is currently rate-limited without incrementing the
   * failure counter. Used for API key authentication where each token should be rate-limited
   * independently to prevent one bad token from affecting all users of the same format.
   *
   * @param tokenHash a hash of the API token; must not be {@code null}
   * @return {@code null} if the token is not currently blocked,
   *         or a {@link RateLimitResult} describing the backoff when blocked
   */
  @Nullable
  RateLimitResult checkByToken(String tokenHash);

  /**
   * Records a failed authentication attempt for the given token hash and checks whether
   * the request should be blocked. The counter is incremented on every call; call
   * {@link #recordSuccessByToken} to clear it after a successful authentication.
   *
   * @param tokenHash a hash of the API token; must not be {@code null}
   * @return {@code null} if the attempt count is within the threshold,
   *         or a {@link RateLimitResult} describing the backoff when blocked
   */
  @Nullable
  RateLimitResult checkAndRecordByToken(String tokenHash);

  /**
   * Resets the failure counter for the given token hash upon a successful authentication.
   *
   * @param tokenHash a hash of the API token
   */
  void recordSuccessByToken(String tokenHash);
}
