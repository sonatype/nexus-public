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

import javax.annotation.Nullable;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Helper to get current user-id.
 *
 * @since 3.1
 */
public class UserIdHelper
{
  private UserIdHelper() {
    // empty
  }

  /**
   * Value when subject is missing or subject principal is not known.
   */
  public static final String UNKNOWN = "*UNKNOWN";

  /**
   * Value for privileged system execution.
   */
  public static final String SYSTEM = "*SYSTEM";

  /**
   * Get the current user-id.
   *
   * @see #get(Subject)
   */
  public static String get() {
    return get(SecurityUtils.getSubject());
  }

  /**
   * Get the user-id from the given subject or {@link #UNKNOWN}.
   */
  public static String get(@Nullable final Subject subject) {
    if (subject != null) {
      Object principal = subject.getPrincipal();
      if (principal != null) {
        return principal.toString();
      }
    }
    return UNKNOWN;
  }

  /**
   * Check if current user-id is {@link #SYSTEM}.
   */
  public static boolean isSystem() {
    return get().equals(SYSTEM);
  }

  /**
   * Check if current user-id is {@link #UNKNOWN}.
   */
  public static boolean isUnknown() {
    return get().equals(UNKNOWN);
  }
}

