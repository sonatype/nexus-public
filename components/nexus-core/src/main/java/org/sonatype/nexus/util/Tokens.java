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
package org.sonatype.nexus.util;

import java.io.UnsupportedEncodingException;

import org.sonatype.sisu.goodies.common.TestAccessible;

import com.google.common.base.Throwables;
import org.codehaus.plexus.util.Base64;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * Provides static methods for working with token-like thingies.
 *
 * @since 2.7
 */
public class Tokens
{
  @NonNls
  public static final String UTF_8 = "UTF8";

  @NonNls
  public static final String NL = System.getProperty("line.separator");

  @NonNls
  @TestAccessible
  static final String MASK = "****";

  public static String encode(final String input, final char separator, final int delay) {
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
   * Converts bytes into a UTF8 encoded string.
   */
  public static String string(final byte[] bytes) {
    try {
      return new String(bytes, UTF_8);
    }
    catch (UnsupportedEncodingException e) {
      // should never happen
      throw Throwables.propagate(e);
    }
  }

  /**
   * Converts a string into UTF8 encoded bytes.
   */
  public static byte[] bytes(final String string) {
    try {
      return string.getBytes(UTF_8);
    }
    catch (UnsupportedEncodingException e) {
      // should never happen
      throw Throwables.propagate(e);
    }
  }

  public static char[] encodeHex(final byte[] bytes) {
    return DigesterUtils.encodeHex(bytes);
  }

  public static String encodeHexString(final byte[] bytes) {
    return new String(encodeHex(bytes));
  }

  public static byte[] encodeBase64(final byte[] bytes) {
    return Base64.encodeBase64(bytes);
  }

  public static String encodeBase64String(final byte[] bytes) {
    return string(encodeBase64(bytes));
  }

  public static byte[] decodeBase64(final byte[] bytes) {
    return Base64.decodeBase64(bytes);
  }

  public static String decodeBase64String(final byte[] bytes) {
    return string(Base64.decodeBase64(bytes));
  }

  public static String decodeBase64String(final String str) {
    return string(Base64.decodeBase64(bytes(str)));
  }

  public static String mask(final @Nullable String password) {
    if (password != null) {
      return MASK;
    }
    return null;
  }

  public static boolean isEmpty(final @Nullable String value) {
    return value == null || value.trim().isEmpty();
  }
}

