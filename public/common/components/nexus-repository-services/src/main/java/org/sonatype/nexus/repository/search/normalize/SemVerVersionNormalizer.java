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
 * Normalizes SemVer 2.0 versions for correct lexicographic sorting.
 *
 * Handles the core SemVer rule: pre-release versions have LOWER precedence than
 * the associated normal version (1.0.0-alpha < 1.0.0).
 *
 * Uses a letter-encoding approach (same pattern as MavenVersionNormalizer):
 * - "a" = pre-release (anything with a hyphen-separated pre-release identifier)
 * - "c" = release (no pre-release identifier)
 *
 * Build metadata (+xyz) is stripped and ignored for sorting per SemVer spec.
 *
 * Covers: npm, Helm, Cargo, Pub, Terraform, Ansible Galaxy, Swift
 */
@Component
@Qualifier("npm")
public class SemVerVersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real SemVer versions are well under this; rejecting
   * pathologically long strings up front avoids unnecessary regex work and large intermediate
   * allocations during normalization.
   */
  private static final int MAX_VERSION_LENGTH = 256;

  private static final String PRE_RELEASE_KEY = "a";

  private static final String RELEASE_KEY = "c";

  @Override
  public String getNormalizedVersion(final String version) {
    if (isBlank(version)) {
      return "";
    }

    if (version.length() > MAX_VERSION_LENGTH) {
      return "";
    }

    String trimmed = version.trim();

    // Strip build metadata (+xyz) — ignored for sorting per SemVer spec §10
    int plusIndex = trimmed.indexOf('+');
    if (plusIndex >= 0) {
      trimmed = trimmed.substring(0, plusIndex);
    }

    // Split into version and pre-release at first hyphen after numeric part
    String versionPart;
    String preReleasePart;

    int hyphenIndex = findPreReleaseHyphen(trimmed);
    if (hyphenIndex >= 0) {
      versionPart = trimmed.substring(0, hyphenIndex);
      preReleasePart = trimmed.substring(hyphenIndex + 1);
    }
    else {
      versionPart = trimmed;
      preReleasePart = null;
    }

    // Normalize the numeric version part (pad numbers)
    String normalizedVersion = VersionNumberExpander.expand(versionPart);

    // Append qualifier letter + pre-release identifiers
    if (preReleasePart != null && !preReleasePart.isEmpty()) {
      String normalizedPreRelease = normalizePreRelease(preReleasePart);
      return normalizedVersion + "." + PRE_RELEASE_KEY + "." + normalizedPreRelease;
    }
    else {
      return normalizedVersion + "." + RELEASE_KEY;
    }
  }

  /**
   * Find the hyphen that separates version from pre-release.
   * Per SemVer, pre-release follows the PATCH version: MAJOR.MINOR.PATCH-prerelease
   * We look for the first hyphen that appears after at least one digit.
   */
  private int findPreReleaseHyphen(final String version) {
    boolean foundDigit = false;
    for (int i = 0; i < version.length(); i++) {
      char c = version.charAt(i);
      if (Character.isDigit(c)) {
        foundDigit = true;
      }
      else if (c == '-' && foundDigit) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Normalize pre-release identifiers for correct sorting.
   *
   * Per SemVer §11:
   * - Numeric identifiers: compared numerically (pad with zeros)
   * - Alphanumeric identifiers: compared as ASCII strings
   * - Numeric < alphanumeric (a numeric-only identifier sorts before alpha)
   * - Fewer fields < more fields when all preceding are equal
   *
   * We handle this by:
   * - Padding numeric identifiers with zeros (VersionNumberExpander handles this)
   * - Prefixing numeric-only identifiers with "0" and alpha with "1" to enforce numeric < alpha
   */
  private String normalizePreRelease(final String preRelease) {
    String[] identifiers = preRelease.split("\\.");
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < identifiers.length; i++) {
      if (i > 0) {
        result.append(".");
      }
      String id = identifiers[i];
      if (isNumeric(id)) {
        // Numeric identifiers: prefix with "0" (sorts before "1" prefix of alpha)
        result.append("0").append(String.format("%09d", Long.parseLong(id)));
      }
      else {
        // Alphanumeric identifiers: prefix with "1", expand any embedded numbers
        result.append("1").append(VersionNumberExpander.expand(id));
      }
    }

    return result.toString();
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
