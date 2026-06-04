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
 * Fired when an authentication attempt is blocked by the rate limiter.
 */
public class AuthRateLimitedEvent
{
  private final String username;

  private final int attemptCount;

  private final long retryAfterSeconds;

  @Nullable
  private final String sourceIp;

  private final String authMethod;

  public AuthRateLimitedEvent(
      final String username,
      final int attemptCount,
      final long retryAfterSeconds,
      @Nullable final String sourceIp,
      final String authMethod)
  {
    this.username = username;
    this.attemptCount = attemptCount;
    this.retryAfterSeconds = retryAfterSeconds;
    this.sourceIp = sourceIp;
    this.authMethod = authMethod;
  }

  public String getUsername() {
    return username;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }

  @Nullable
  public String getSourceIp() {
    return sourceIp;
  }

  public String getAuthMethod() {
    return authMethod;
  }
}
