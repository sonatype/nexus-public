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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to set the {@code userId} MDC attribute.
 *
 * @since 2.7.2
 */
public class UserIdMdcHelper
{
  private static final Logger log = LoggerFactory.getLogger(UserIdMdcHelper.class);

  public static final String KEY = "userId";

  /**
   * Value for {@link #KEY} when subject principal is not known.
   */
  public static final String UNKNOWN = "*UNKNOWN";

  /**
   * Value for {@link #KEY} for privileged system execution.
   *
   * @since 3.0
   */
  public static final String SYSTEM = "*SYSTEM";

  public static boolean isSet() {
    String userId = MDC.get(KEY);
    return !(Strings.isNullOrEmpty(userId) || UNKNOWN.equals(userId));
  }

  public static void setIfNeeded() {
    if (!isSet()) {
      set();
    }
  }

  public static void set(final Subject subject) {
    checkNotNull(subject);
    String userId = userId(subject);
    log.trace("Set: {}", userId);
    MDC.put(KEY, userId);
  }

  @VisibleForTesting
  static String userId(final @Nullable Subject subject) {
    if (subject != null) {
      Object principal = subject.getPrincipal();
      if (principal != null) {
        return principal.toString();
      }
    }
    return UNKNOWN;
  }

  public static void set() {
    Subject subject = SecurityUtils.getSubject();
    if (subject == null) {
      MDC.put(KEY, UNKNOWN);
    }
    else {
      set(subject);
    }
  }

  /**
   * @since 3.0
   */
  public static void unknown() {
    MDC.put(KEY, UNKNOWN);
  }

  /**
   * @since 3.0
   */
  public static void system() {
    MDC.put(KEY, SYSTEM);
  }

  public static void unset() {
    MDC.remove(KEY);
  }
}

