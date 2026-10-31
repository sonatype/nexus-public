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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shared, thread-safe utility that masks sensitive HTTP {@code Set-Cookie} values in a log message
 * string at write-time.
 *
 * <p>
 * By default the following cookie names are masked (case-insensitive):
 * {@code KEYCLOAK_IDENTITY}, {@code KEYCLOAK_IDENTITY_LEGACY}, {@code KEYCLOAK_SESSION},
 * {@code KEYCLOAK_SESSION_LEGACY}, {@code AUTH_SESSION_ID}, {@code AUTH_SESSION_ID_LEGACY}.
 *
 * <p>
 * Additional cookie names may be supplied via the system property {@code nexus.log.mask.cookies}
 * as a comma-separated list. Each name is trimmed; blank entries are ignored. The property is read
 * once when the masker is constructed (at JVM/logging startup), so changing it requires a restart.
 *
 * <p>
 * The default cookie set is intentionally duplicated in two other places that must be kept in sync:
 * {@code LogCustomizer.SET_COOKIE_PATTERN} (the Support ZIP backstop) and the cloud
 * {@code logback.xml} JSON masking config. Those two locations mask the default Keycloak set only
 * and do not honor {@code nexus.log.mask.cookies}; that property applies to this write-time masker.
 */
public class SensitiveCookieMasker
{
  private static final String MASK = "****";

  private static final List<String> DEFAULT_COOKIE_NAMES = Arrays.asList(
      "KEYCLOAK_IDENTITY",
      "KEYCLOAK_IDENTITY_LEGACY",
      "KEYCLOAK_SESSION",
      "KEYCLOAK_SESSION_LEGACY",
      "AUTH_SESSION_ID",
      "AUTH_SESSION_ID_LEGACY");

  private final Pattern pattern;

  /**
   * Creates a masker that reads extra cookie names from the {@code nexus.log.mask.cookies} system property.
   */
  public SensitiveCookieMasker() {
    this(System.getProperty("nexus.log.mask.cookies"));
  }

  /**
   * Creates a masker with the given extra cookie names (comma-separated).
   * Intended for unit tests that need to inject cookie names without setting real system properties.
   *
   * @param extraCookiesCsv comma-separated list of additional cookie names to mask, or {@code null}
   */
  SensitiveCookieMasker(final String extraCookiesCsv) {
    List<String> names = new ArrayList<>(DEFAULT_COOKIE_NAMES);
    if (extraCookiesCsv != null && !extraCookiesCsv.isEmpty()) {
      for (String name : extraCookiesCsv.split(",")) {
        String trimmed = name.trim();
        if (!trimmed.isEmpty()) {
          names.add(trimmed);
        }
      }
    }
    StringBuilder sb = new StringBuilder("((?:Set-Cookie:\\s*)?(?:");
    for (int i = 0; i < names.size(); i++) {
      if (i > 0) {
        sb.append('|');
      }
      sb.append(Pattern.quote(names.get(i)));
    }
    // Value terminates at ; , whitespace, or " so a token embedded in a quoted/JSON-serialized
    // message (e.g. "...KEYCLOAK_IDENTITY=eyJ...") cannot leak a partial value past the closing quote.
    // JWT/base64 tokens never contain these characters, so excluding them never truncates a real value.
    sb.append(")\\s*=)[^;,\\s\"]+");
    this.pattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
  }

  /**
   * Masks sensitive cookie values in the given log message.
   *
   * <p>
   * Fast path: {@code null} messages and messages that contain no {@code =} character are
   * returned unchanged without regex evaluation, keeping overhead minimal on the hot logging path.
   *
   * @param message the log message, may be {@code null}
   * @return the message with sensitive cookie values replaced by {@code ****},
   *         or the original message if no masking was required
   */
  public String mask(final String message) {
    if (message == null) {
      return message;
    }
    if (message.indexOf('=') < 0) {
      return message;
    }
    return pattern.matcher(message).replaceAll(mr -> mr.group(1) + MASK);
  }
}
