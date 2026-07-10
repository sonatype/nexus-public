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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Normalizes Go module versions for correct lexicographic sorting.
 *
 * Strips leading "v" prefix, then applies SemVer rules (pre-release < release).
 * Pseudo-versions (v0.0.0-YYYYMMDDHHMMSS-hash) are detected and use the timestamp for sorting.
 * For pseudo-versions: uses letter "a" + padded timestamp.
 */
@Component
@Qualifier("go")
public class GoVersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real Go module versions are well under this; rejecting
   * pathologically long strings up front avoids unnecessary regex work and large intermediate
   * allocations during normalization.
   */
  private static final int MAX_VERSION_LENGTH = 256;

  private static final String PRE_RELEASE_KEY = "a";

  private static final String RELEASE_KEY = "c";

  /**
   * Matches Go pseudo-version pattern: vX.Y.Z-YYYYMMDDHHMMSS-abcdefabcdef
   */
  private static final Pattern PSEUDO_VERSION_PATTERN =
      Pattern.compile("^(\\d+\\.\\d+\\.\\d+)-(\\d{14})-[0-9a-f]+$");

  private final SemVerVersionNormalizer semVerNormalizer;

  @Autowired
  public GoVersionNormalizer(@Qualifier("npm") final SemVerVersionNormalizer semVerNormalizer) {
    this.semVerNormalizer = semVerNormalizer;
  }

  @Override
  public String getNormalizedVersion(final String version) {
    if (isBlank(version)) {
      return "";
    }

    if (version.length() > MAX_VERSION_LENGTH) {
      return "";
    }

    String trimmed = version.trim();

    // Strip leading "v" prefix
    if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
      trimmed = trimmed.substring(1);
    }

    if (trimmed.isEmpty()) {
      return "";
    }

    // Check for pseudo-version pattern
    Matcher pseudoMatcher = PSEUDO_VERSION_PATTERN.matcher(trimmed);
    if (pseudoMatcher.matches()) {
      String baseVersion = pseudoMatcher.group(1);
      String timestamp = pseudoMatcher.group(2);
      // Pseudo-versions sort as pre-release of the base version with timestamp for ordering
      String normalizedBase = VersionNumberExpander.expand(baseVersion);
      return normalizedBase + "." + PRE_RELEASE_KEY + "." + timestamp;
    }

    // Otherwise delegate to SemVer normalization
    return semVerNormalizer.getNormalizedVersion(trimmed);
  }
}
