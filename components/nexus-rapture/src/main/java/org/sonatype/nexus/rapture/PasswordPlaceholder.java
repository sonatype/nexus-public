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
package org.sonatype.nexus.rapture;

import javax.annotation.Nullable;

import org.sonatype.nexus.crypto.secrets.Secret;

import com.google.common.annotations.VisibleForTesting;

/**
 * Helper for password-placeholder data.
 *
 * @since 3.0
 */
public class PasswordPlaceholder
{
  private PasswordPlaceholder() {
    // empty
  }

  /**
   * Token used for passwords that are defined, but which are not transmitted.
   */
  @VisibleForTesting
  static final String VALUE = "#~NXRM~PLACEHOLDER~PASSWORD~#";

  /**
   * Returns password placeholder.
   */
  public static String get() {
    return VALUE;
  }

  /**
   * Returns fake password placeholder unless value is {@code null}.
   */
  @Nullable
  public static String get(@Nullable final String value) {
    if (value != null) {
      return VALUE;
    }
    return null;
  }

  /**
   * Returns fake password placeholder unless value is {@code null}.
   */
  @Nullable
  public static String get(@Nullable final Secret value) {
    if (value != null) {
      return VALUE;
    }
    return null;
  }

  /**
   * Determine if given value is a password placeholder.
   */
  public static boolean is(@Nullable final String value) {
    return VALUE.equals(value);
  }

  /**
   * @param value
   * @return true if the value is not the password placeholder
   */
  public static boolean isNot(@Nullable final String value) {
    return !PasswordPlaceholder.is(value);
  }
}
