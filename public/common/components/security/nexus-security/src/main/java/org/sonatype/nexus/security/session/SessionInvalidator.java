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
package org.sonatype.nexus.security.session;

/**
 * Interface for invalidating user sessions.
 * Implementations are conditionally loaded based on JWT/Session feature flags.
 */
public interface SessionInvalidator
{
  /**
   * Invalidates all active sessions for the given user.
   *
   * @param username the username whose sessions should be invalidated
   * @param userSource the user source/realm (e.g., "default", "LDAP", "SAML"); consulted
   *          by the JWT implementation to scope the invalidation to the correct realm.
   *          Session-mode implementations may ignore this.
   * @param reason human-readable reason for the invalidation (e.g., "password change",
   *          "user deletion", "user deactivation"); surfaced in log lines and audit records
   *          so operators can distinguish invalidation triggers.
   * @return the number of sessions (or invalidation markers) that were recorded
   */
  int invalidateSessionsForUser(String username, String userSource, String reason);
}
