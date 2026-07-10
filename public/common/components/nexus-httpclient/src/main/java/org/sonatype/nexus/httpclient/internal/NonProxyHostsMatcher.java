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
package org.sonatype.nexus.httpclient.internal;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compiles and matches Java {@code http.nonProxyHosts}-style glob patterns into a
 * single case-insensitive regex. Used by both the runtime route planner and the
 * SSRF validator so the two see identical bypass semantics.
 *
 * <p>
 * Patterns may contain a leading or trailing {@code *} wildcard. Dots, square
 * brackets, and the wildcard are escaped/translated; all other characters are
 * matched literally.
 */
public final class NonProxyHostsMatcher
{
  private static final Logger log = LoggerFactory.getLogger(NonProxyHostsMatcher.class);

  private NonProxyHostsMatcher() {
    // utility class
  }

  /**
   * Compiles a list of glob-style nonProxyHosts entries into a single case-insensitive
   * {@link Pattern}. Returns {@code null} when the input is null/empty, every entry is
   * blank, or the resulting expression fails to compile.
   */
  @Nullable
  public static Pattern compile(@Nullable final String[] globs) {
    if (globs == null || globs.length == 0) {
      return null;
    }

    String regex = Stream.of(globs)
        .filter(g -> g != null && !g.isBlank())
        .map(NonProxyHostsMatcher::globToRegex)
        .reduce((a, b) -> a + "|" + b)
        .orElse(null);

    if (regex == null) {
      return null;
    }

    try {
      return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }
    catch (PatternSyntaxException e) {
      log.warn("Invalid non-proxy host regex: {}, ignoring", regex, e);
      return null;
    }
  }

  /**
   * Returns {@code true} if {@code host} matches any of the supplied glob entries.
   */
  public static boolean matches(@Nullable final String host, @Nullable final String[] globs) {
    if (host == null || host.isBlank()) {
      return false;
    }
    Pattern pattern = compile(globs);
    return pattern != null && pattern.matcher(host).matches();
  }

  private static String globToRegex(final String glob) {
    return "(" +
        glob.toLowerCase(Locale.US)
            .replaceAll("\\.", "\\\\.")
            .replaceAll("\\*", ".*?")
            .replaceAll("\\[", "\\\\[")
            .replaceAll("\\]", "\\\\]")
        + ")";
  }
}
