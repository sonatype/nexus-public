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
 * Normalizes Debian (APT) versions for correct lexicographic sorting.
 *
 * Format: [epoch:]upstream_version[-debian_revision]
 * Epoch (N:) is prepended as first sort field, default 0.
 * Tilde (~) means "sorts before everything" - replaced with letter "a" (pre-release).
 * Plus (+) means post-release - letter "d".
 * No tilde and no plus = release - letter "c".
 */
@Component
@Qualifier("apt")
public class DebianVersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real Debian versions are well under this; rejecting
   * pathologically long strings up front avoids unnecessary regex work and large intermediate
   * allocations during normalization.
   */
  private static final int MAX_VERSION_LENGTH = 256;

  private static final String PRE_RELEASE_KEY = "a";

  private static final String RELEASE_KEY = "c";

  private static final String POST_RELEASE_KEY = "d";

  @Override
  public String getNormalizedVersion(final String version) {
    if (isBlank(version)) {
      return "";
    }

    if (version.length() > MAX_VERSION_LENGTH) {
      return "";
    }

    String trimmed = version.trim();

    // Parse epoch
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

    // Parse debian revision (last hyphen)
    String upstreamVersion;
    String debianRevision = "";
    int lastHyphen = trimmed.lastIndexOf('-');
    if (lastHyphen >= 0) {
      upstreamVersion = trimmed.substring(0, lastHyphen);
      debianRevision = trimmed.substring(lastHyphen + 1);
    }
    else {
      upstreamVersion = trimmed;
    }

    // Normalize upstream version
    String normalizedUpstream = normalizeWithTildeAndPlus(upstreamVersion);

    // Build result: epoch.upstream.revision
    StringBuilder result = new StringBuilder();
    result.append(epoch).append(".").append(normalizedUpstream);

    if (!debianRevision.isEmpty()) {
      result.append(".").append(VersionNumberExpander.expand(debianRevision));
    }

    return result.toString();
  }

  /**
   * Normalize a version string handling tilde (~) and plus (+) markers.
   * Tilde means pre-release (sorts before the version without tilde).
   * Plus means post-release (sorts after).
   */
  private String normalizeWithTildeAndPlus(final String version) {
    if (version.contains("~")) {
      // Tilde: pre-release. Split at first tilde.
      int tildeIndex = version.indexOf('~');
      String basePart = version.substring(0, tildeIndex);
      String preReleasePart = version.substring(tildeIndex + 1);
      // Guard against a bare trailing tilde (e.g. "1.0~") where preReleasePart is empty.
      // VersionNumberExpander.expand("") returns "", which would leave a trailing "." in the
      // sort key. Substitute "0" so the key remains well-formed.
      String expandedPre = preReleasePart.isEmpty()
          ? VersionNumberExpander.expand("0")
          : VersionNumberExpander.expand(preReleasePart);
      return VersionNumberExpander.expand(basePart) + "." + PRE_RELEASE_KEY + "." + expandedPre;
    }
    else if (version.contains("+")) {
      // Plus: post-release. Split at first plus.
      int plusIndex = version.indexOf('+');
      String basePart = version.substring(0, plusIndex);
      String postReleasePart = version.substring(plusIndex + 1);
      // Same trailing-empty guard as the tilde branch above.
      String expandedPost = postReleasePart.isEmpty()
          ? VersionNumberExpander.expand("0")
          : VersionNumberExpander.expand(postReleasePart);
      return VersionNumberExpander.expand(basePart) + "." + POST_RELEASE_KEY + "." + expandedPost;
    }
    else {
      // Normal release
      return VersionNumberExpander.expand(version) + "." + RELEASE_KEY;
    }
  }
}
