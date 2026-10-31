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
package org.sonatype.nexus.repository.search.normalize;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Normalizes RubyGems versions for correct lexicographic sorting.
 *
 * Rule: string segments sort before numeric segments (pre-release < release).
 * Split version by ".". Each segment:
 * - if all digits: prefix "1" + pad (sorts after strings)
 * - if contains letters: prefix "0" + as-is (sorts before numbers)
 *
 * This ensures string segments (pre-release markers) sort before numeric segments.
 */
@Component
@Qualifier("rubygems")
public class GemStyleVersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real RubyGems versions are well under this; rejecting
   * pathologically long strings up front avoids unnecessary regex work and large intermediate
   * allocations during normalization.
   */
  private static final int MAX_VERSION_LENGTH = 256;

  private static final String PRE_RELEASE_KEY = "0a";

  private static final String RELEASE_KEY = "0c";

  @Override
  public String getNormalizedVersion(final String version) {
    if (isBlank(version)) {
      return "";
    }

    if (version.length() > MAX_VERSION_LENGTH) {
      return "";
    }

    String trimmed = version.trim();
    String[] segments = trimmed.split("\\.");

    // RubyGems splits a version into its leading numeric run and the remainder, then drops trailing
    // zero segments from the numeric run before comparing. 1.0.beta3 therefore canonicalizes to
    // 1.beta3, and its release part must encode identically to a bare 1 — otherwise the extra 0
    // segment makes the pre-release outrank the release it precedes.
    int releaseEnd = 0;
    while (releaseEnd < segments.length && isNumeric(segments[releaseEnd])) {
      releaseEnd++;
    }
    int strippedEnd = releaseEnd;
    while (strippedEnd > 0 && isZero(segments[strippedEnd - 1])) {
      strippedEnd--;
    }
    boolean foundPreRelease = releaseEnd < segments.length;

    StringBuilder versionPart = new StringBuilder();
    for (int i = 0; i < strippedEnd; i++) {
      if (versionPart.length() > 0) {
        versionPart.append(".");
      }
      versionPart.append(normalizeSegment(segments[i]));
    }

    StringBuilder qualifierPart = new StringBuilder();
    for (int i = releaseEnd; i < segments.length; i++) {
      if (qualifierPart.length() > 0) {
        qualifierPart.append(".");
      }
      qualifierPart.append(segments[i].toLowerCase());
    }

    String normalizedVersion = versionPart.toString();
    if (normalizedVersion.isEmpty()) {
      normalizedVersion = "1000000000";
    }

    if (foundPreRelease) {
      return normalizedVersion + "." + PRE_RELEASE_KEY + "." + qualifierPart;
    }
    else {
      return normalizedVersion + "." + RELEASE_KEY;
    }
  }

  /**
   * Normalize a single version segment.
   * Numeric segments get prefix "1" + zero-padded number.
   * String segments get prefix "0" + the string as-is.
   */
  protected String normalizeSegment(final String segment) {
    if (segment == null || segment.isEmpty()) {
      return "1" + "000000000";
    }

    if (isNumeric(segment)) {
      try {
        return "1" + String.format("%09d", Long.parseLong(segment));
      }
      catch (NumberFormatException e) {
        return "1" + segment;
      }
    }
    else {
      return "0" + segment.toLowerCase();
    }
  }

  /**
   * A numeric segment whose value is zero, e.g. {@code 0} or {@code 000}.
   */
  private boolean isZero(final String str) {
    if (!isNumeric(str)) {
      return false;
    }
    for (char c : str.toCharArray()) {
      if (c != '0') {
        return false;
      }
    }
    return true;
  }

  private boolean isNumeric(final String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    for (char c : str.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }
}
