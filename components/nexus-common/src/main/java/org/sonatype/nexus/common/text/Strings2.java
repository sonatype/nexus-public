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
package org.sonatype.nexus.common.text;

import java.util.Locale;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * String helpers.
 *
 * @since 3.0
 */
public final class Strings2
{
  /**
   * Platform new-line separator.
   */
  public static final String NL = System.lineSeparator();

  /**
   * Password mask.
   */
  public static final String MASK = "****";

  private Strings2() {}

  /**
   * Returns {@code true} if given string is null, or length is zero.
   */
  public static boolean isEmpty(@Nullable final String value) {
    return value == null || value.length() == 0;
  }

  /**
   * Returns {@code true} if given string is not empty.
   *
   * @see #isEmpty
   *
   * @deprecated Prefer {@code !isEmpty(value}
   */
  @Deprecated
  public static boolean isNotEmpty(@Nullable final String value) {
    return !isEmpty(value);
  }

  /**
   * Returns {@code true} if given string is null, or length is zero after {@link String#trim()}.
   */
  public static boolean isBlank(@Nullable final String value) {
    // TODO: Consider using Character.isWhitespace() to determine blank-ness for commons-lang/plexus-utils impl parity
    return value == null || value.trim().length() == 0;
  }

  /**
   * Returns {@code true} if given string is not blank.
   *
   * @see #isBlank
   *
   * @deprecated Prefer {@code !isBlank(value}
   */
  @Deprecated
  public static boolean isNotBlank(@Nullable final String value) {
    return !isBlank(value);
  }

  /**
   * Returns standard password {@link #MASK} for given value unless null.
   */
  @Nullable
  public static String mask(@Nullable final String password) {
    if (password != null) {
      return MASK;
    }
    return null;
  }

  /**
   * Returns lower-case {@link Locale#ENGLISH} string.
   */
  public static String lower(final String value) {
    checkNotNull(value);
    return value.toLowerCase(Locale.ENGLISH);
  }

  /**
   * Returns upper-case {@link Locale#ENGLISH} string.
   */
  public static String upper(final String value) {
    checkNotNull(value);
    return value.toUpperCase(Locale.ENGLISH);
  }

  /**
   * Converts bytes into a UTF-8 encoded string.
   */
  public static String utf8(final byte[] bytes) {
    return new String(bytes, Charsets.UTF_8);
  }

  /**
   * Converts a string into UTF-8 encoded bytes.
   */
  public static byte[] utf8(final String string) {
    return string.getBytes(Charsets.UTF_8);
  }

  /**
   * Encode separator into input at given delay.
   */
  public static String encodeSeparator(final String input, final char separator, final int delay) {
    StringBuilder buff = new StringBuilder();

    int i = 0;
    for (char c : input.toCharArray()) {
      if (i != 0 && i % delay == 0) {
        buff.append(separator);
      }
      buff.append(c);
      i++;
    }

    return buff.toString();
  }

  /**
   * Decode Base-64 UTF-8 string.
   */
  public static String decodeBase64(final String value) {
    return utf8(BaseEncoding.base64().decode(value));
  }

  /**
   * Encode Base-64 UTF-8 string.
   */
  public static String encodeBase64(final String value) {
    return BaseEncoding.base64().encode(utf8(value));
  }
}
