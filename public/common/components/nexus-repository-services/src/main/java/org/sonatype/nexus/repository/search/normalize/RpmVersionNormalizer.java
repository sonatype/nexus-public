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
 * Normalizes RPM (Yum) versions for correct lexicographic sorting.
 *
 * Format: [Epoch:]Version-Release
 * Epoch prepended as first sort field, default 0.
 * Tilde (~) sorts before everything (letter "a").
 * Release field "0.x" convention for pre-release (detect 0. prefix in release).
 */
@Component
@Qualifier("yum")
public class RpmVersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real RPM versions are well under this; rejecting
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

    // Parse epoch (N:)
    String epoch = "000000000";
    int colonIndex = trimmed.indexOf(':');
    if (colonIndex >= 0) {
      String epochStr = trimmed.substring(0, colonIndex);
      try {
        epoch = String.format("%09d", Long.parseLong(epochStr));
      }
      catch (NumberFormatException e) {
        // invalid epoch, keep default
      }
      trimmed = trimmed.substring(colonIndex + 1);
    }

    // Split Version-Release at the last hyphen
    String versionPart;
    String releasePart = "";
    int lastHyphen = trimmed.lastIndexOf('-');
    if (lastHyphen >= 0) {
      versionPart = trimmed.substring(0, lastHyphen);
      releasePart = trimmed.substring(lastHyphen + 1);
    }
    else {
      versionPart = trimmed;
    }

    // Handle tilde in version part
    String normalizedVersion = normalizeWithTilde(versionPart);

    // Build result
    StringBuilder result = new StringBuilder();
    result.append(epoch).append(".").append(normalizedVersion);

    // Append release part
    if (!releasePart.isEmpty()) {
      // "0." prefix in release field indicates pre-release by RPM convention
      if (releasePart.startsWith("0.")) {
        result.append(".").append(PRE_RELEASE_KEY).append(".").append(VersionNumberExpander.expand(releasePart));
      }
      else {
        result.append(".").append(RELEASE_KEY).append(".").append(VersionNumberExpander.expand(releasePart));
      }
    }

    return result.toString();
  }

  /**
   * Handle tilde (~) in version strings. Tilde sorts before everything.
   */
  private String normalizeWithTilde(final String version) {
    if (version.contains("~")) {
      int tildeIndex = version.indexOf('~');
      String basePart = version.substring(0, tildeIndex);
      String preReleasePart = version.substring(tildeIndex + 1);
      // Guard against a bare trailing tilde (e.g. "1.0~") where preReleasePart is empty.
      // VersionNumberExpander.expand("") returns "", which would leave a trailing "." in the
      // sort key. Substitute "0" so the key remains well-formed and the pre-release marker
      // still sorts before the corresponding release.
      String expandedPre = preReleasePart.isEmpty()
          ? VersionNumberExpander.expand("0")
          : VersionNumberExpander.expand(preReleasePart);
      return VersionNumberExpander.expand(basePart) + "." + PRE_RELEASE_KEY + "." + expandedPre;
    }
    return VersionNumberExpander.expand(version);
  }
}
