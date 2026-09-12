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
package org.sonatype.nexus.common.app;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

/**
 * Represents a semantic version.
 *
 * The specification is available at https://semver.org/
 */
public record SemanticVersion(String version, int major, int minor, int patch, String prerelease, String buildMetadata)
    implements Comparable<SemanticVersion>
{
  /*
   * Suggested pattern from 2.0.0 CC By 3.0.
   */
  private static final Pattern SEMVER = Pattern.compile(
      "^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)"
          + "(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

  private static final Predicate<String> ONLY_DIGITS = Pattern.compile("^\\d+$").asMatchPredicate();

  /**
   * @return true if this is considered a pre-release by semver
   */
  public boolean isPrerelease() {
    return prerelease != null;
  }

  @Override
  public int compareTo(final SemanticVersion o) {
    if (major != o.major) {
      return Integer.compare(major, o.major);
    }
    if (minor != o.minor) {
      return Integer.compare(minor, o.minor);
    }
    if (patch != o.patch) {
      return Integer.compare(patch, o.patch);
    }

    boolean thisIsPrerelease = isPrerelease();
    boolean otherIsPrerelease = o.isPrerelease();

    // When major, minor, and patch are equal, a pre-release version has
    // lower precedence than a normal version
    if (!thisIsPrerelease && otherIsPrerelease) {
      return 1;
    }
    if (thisIsPrerelease && !otherIsPrerelease) {
      return -1;
    }
    if (!thisIsPrerelease && !otherIsPrerelease) {
      return 0;
    }

    // Both are prerelease - compare prerelease identifiers
    return comparePrereleaseIdentifiers(prerelease, o.prerelease);
  }

  /*
   * Compares prerelease identifiers according to semver spec item 11.
   */
  private static int comparePrereleaseIdentifiers(final String thisPrerelease, final String otherPrerelease) {
    String[] thisIdentifiers = thisPrerelease.split("\\.");
    String[] otherIdentifiers = otherPrerelease.split("\\.");

    int minLength = Math.min(thisIdentifiers.length, otherIdentifiers.length);
    for (int i = 0; i < minLength; i++) {
      String thisId = thisIdentifiers[i];
      String otherId = otherIdentifiers[i];

      Integer thisInt = parseInt(thisId);
      Integer otherInt = parseInt(otherId);

      // Both are numeric - compare numerically
      if (thisInt != null && otherInt != null) {
        int result = Integer.compare(thisInt, otherInt);
        if (result != 0) {
          return result;
        }
        continue;
      }

      // Numeric identifiers have lower precedence than non-numeric
      if (thisInt != null) {
        return -1;
      }
      if (otherInt != null) {
        return 1;
      }

      // Both are non-numeric - compare lexically in ASCII sort order
      int result = thisId.compareTo(otherId);
      if (result != 0) {
        return result;
      }
    }

    // If all compared identifiers are equal, the one with more identifiers has higher precedence
    return Integer.compare(thisIdentifiers.length, otherIdentifiers.length);
  }

  @Nullable
  private static Integer parseInt(final String candidate) {
    if (ONLY_DIGITS.test(candidate)) {
      try {
        return Integer.parseInt(candidate);
      }
      catch (Exception e) {
        // fall through
      }
    }
    return null;
  }

  /**
   * Returns the parsed SemanticVersion or an exception will be thrown if this not a valid version string.
   */
  public static SemanticVersion parse(final String versionString) {
    return maybeParse(versionString)
        .orElseThrow(() -> new IllegalArgumentException("Not a valid semantic version: " + versionString));
  }

  /**
   * Returns the parsed SemanticVersion or an empty optional if not valid.
   */
  public static Optional<SemanticVersion> maybeParse(final String versionString) {
    return Optional.ofNullable(versionString)
        .map(SEMVER::matcher)
        .filter(Matcher::matches)
        .flatMap(matcher -> maybeCreate(versionString, matcher));
  }

  /*
   * Empty when a numeric component is larger than an int can hold, which the pattern permits.
   */
  private static Optional<SemanticVersion> maybeCreate(final String versionString, final Matcher matcher) {
    Integer major = parseInt(matcher.group("major"));
    Integer minor = parseInt(matcher.group("minor"));
    Integer patch = parseInt(matcher.group("patch"));

    if (major == null || minor == null || patch == null) {
      return Optional.empty();
    }

    return Optional.of(new SemanticVersion(versionString, major, minor, patch, matcher.group("prerelease"),
        matcher.group("buildmetadata")));
  }
}
