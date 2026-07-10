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

import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Normalizes NuGet versions for correct lexicographic sorting.
 *
 * SemVer 2.0 + 4-part versions + case-insensitive labels.
 * Strips build metadata (+xyz).
 * Pre-release: letter "a", release: letter "c".
 * Normalizes to 4 parts (pads missing with 0).
 * Lowercases all pre-release identifiers before comparing.
 */
@Component
@Qualifier("nuget")
public class NuGetVersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real NuGet versions are well under this; rejecting
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

    // Strip build metadata (+xyz)
    int plusIndex = trimmed.indexOf('+');
    if (plusIndex >= 0) {
      trimmed = trimmed.substring(0, plusIndex);
    }

    // Split into version and pre-release at first hyphen
    String versionPart;
    String preReleasePart;

    int hyphenIndex = trimmed.indexOf('-');
    if (hyphenIndex >= 0) {
      versionPart = trimmed.substring(0, hyphenIndex);
      preReleasePart = trimmed.substring(hyphenIndex + 1);
    }
    else {
      versionPart = trimmed;
      preReleasePart = null;
    }

    // Normalize to 4 parts
    String normalizedVersion = normalizeTo4Parts(versionPart);

    // Append qualifier letter + pre-release identifiers
    if (preReleasePart != null && !preReleasePart.isEmpty()) {
      String normalizedPreRelease = normalizePreRelease(preReleasePart.toLowerCase(Locale.ROOT));
      return normalizedVersion + "." + PRE_RELEASE_KEY + "." + normalizedPreRelease;
    }
    else {
      return normalizedVersion + "." + RELEASE_KEY;
    }
  }

  /**
   * Normalize version to exactly 4 parts, padding missing parts with 0.
   */
  private String normalizeTo4Parts(final String versionPart) {
    String[] parts = versionPart.split("\\.");
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      if (i > 0) {
        result.append(".");
      }
      if (i < parts.length && !parts[i].isEmpty()) {
        try {
          result.append(String.format("%09d", Long.parseLong(parts[i])));
        }
        catch (NumberFormatException e) {
          result.append(VersionNumberExpander.expand(parts[i]));
        }
      }
      else {
        result.append("000000000");
      }
    }
    return result.toString();
  }

  /**
   * Normalize pre-release identifiers (already lowercased).
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
        result.append("0").append(String.format("%09d", Long.parseLong(id)));
      }
      else {
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
