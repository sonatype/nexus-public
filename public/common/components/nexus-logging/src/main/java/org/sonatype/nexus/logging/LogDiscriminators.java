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
package org.sonatype.nexus.logging;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Makes MDC values safe to use as logback sift discriminators. Logback rejects values that are empty, longer than
 * {@link #MAX_LENGTH}, or contain path characters or {@code ..}, and falls back to the discriminator's default value.
 */
public final class LogDiscriminators
{
  public static final int MAX_LENGTH = 64;

  private static final String FORBIDDEN_CHARACTERS = "/\\${}[]()|?*+%@,";

  private static final int HASH_LENGTH = 8;

  private LogDiscriminators() {
  }

  @Nullable
  public static String safe(@Nullable final String value) {
    return safe(value, MAX_LENGTH);
  }

  /**
   * Returns {@code value} unchanged when logback accepts it as a discriminator within {@code maxLength}. Otherwise
   * returns a readable prefix of it with forbidden characters replaced, followed by a hash of the original value.
   */
  @Nullable
  public static String safe(@Nullable final String value, final int maxLength) {
    if (value == null || isValid(value, maxLength)) {
      return value;
    }
    StringBuilder prefix = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      prefix.append(FORBIDDEN_CHARACTERS.indexOf(c) >= 0 ? '_' : c);
    }
    String cleaned = prefix.toString().replace("..", "_");
    String suffix = "-" + hash(value);
    return cleaned.substring(0, Math.min(cleaned.length(), maxLength - suffix.length())) + suffix;
  }

  private static boolean isValid(final String value, final int maxLength) {
    if (value.isEmpty() || value.length() > maxLength || value.contains("..")) {
      return false;
    }
    return value.chars().noneMatch(c -> FORBIDDEN_CHARACTERS.indexOf(c) >= 0);
  }

  private static String hash(final String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
