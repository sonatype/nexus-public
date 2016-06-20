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
package org.sonatype.nexus.security.user;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User role-mapping event.
 *
 * @since 3.1
 */
public abstract class UserRoleMappingEvent
{
  // NOTE: there is no high-level object representation of user role-mapping, and there is no User object
  // NOTE: ... accessible where the mapping is managed

  private final String userId;

  private final String userSource;

  private final Set<String> roles;

  public UserRoleMappingEvent(final String userId,
                              final String userSource,
                              final Set<String> roles)
  {
    this.userId = checkNotNull(userId);
    this.userSource = checkNotNull(userSource);
    this.roles = checkNotNull(roles);
  }

  public String getUserId() {
    return userId;
  }

  public String getUserSource() {
    return userSource;
  }

  public Set<String> getRoles() {
    return roles;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "userId='" + userId + '\'' +
        ", userSource='" + userSource + '\'' +
        ", roles=" + roles +
        '}';
  }
}
