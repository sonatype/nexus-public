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
package org.sonatype.nexus.security;

/**
 * An event fired when a user is removed from the system, so cached principals can be expired.
 */
public class UserPrincipalsExpired
{
  private final String userId;

  private final String source;

  /**
   * Applies to any cached user principals that have the given userId and UserManager source.
   *
   * @param userId The removed user's id
   * @param source The UserManager source
   */
  public UserPrincipalsExpired(final String userId, final String source) {
    this.userId = userId;
    this.source = source;
  }

  /**
   * Applies to all cached user principals that have an invalid userId or UserManager source.
   */
  public UserPrincipalsExpired() {
    this(null, null);
  }

  public String getUserId() {
    return userId;
  }

  public String getSource() {
    return source;
  }
}
