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
package org.sonatype.nexus.common.app;

import java.util.Optional;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * Request to freeze the application.
 *
 * @since 3.21
 */
public class FreezeRequest
{
  @Nullable
  private final String token;

  private final String reason;

  private final DateTime frozenAt;

  @Nullable
  private final String frozenBy;

  @Nullable
  private final String frozenByIp;

  public FreezeRequest(
      @Nullable final String token,
      final String reason,
      final DateTime frozenAt,
      @Nullable final String frozenBy,
      @Nullable final String frozenByIp)
  {
    this.token = token;
    this.reason = checkNotNull(reason);
    this.frozenAt = checkNotNull(frozenAt);
    this.frozenBy = frozenBy;
    this.frozenByIp = frozenByIp;
  }

  /**
   * The optional system token for this freeze.
   */
  public Optional<String> token() {
    return ofNullable(token);
  }

  /**
   * Is this a user request?
   *
   * @since 3.24
   */
  public boolean isUserRequest() {
    return token == null; // user requests have no system token
  }

  /**
   * The reason for this freeze.
   */
  public String reason() {
    return reason;
  }

  /**
   * When the freeze began.
   */
  public DateTime frozenAt() {
    return frozenAt;
  }

  /**
   * The user that requested the freeze; empty if it was an internal request.
   */
  public Optional<String> frozenBy() {
    return ofNullable(frozenBy);
  }

  /**
   * The client IP that requested the freeze; empty if it was an internal request.
   */
  public Optional<String> frozenByIp() {
    return ofNullable(frozenByIp);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "token=" + token +
        ", reason=" + reason +
        ", frozenAt=" + frozenAt +
        ", frozenBy=" + frozenBy +
        ", frozenByIp=" + frozenByIp +
        '}';
  }
}
