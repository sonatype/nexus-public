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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Normalizes PEP 440 versions (Python/PyPI) for correct lexicographic sorting.
 *
 * Handles: .devN < aN (alpha) < bN (beta) < rcN < (release) < .postN
 * Handles epoch (N! prefix), strips local versions (+xyz).
 *
 * Two-level sort key scheme:
 * Position-1 (release tier): a=pre-release, c=release, d=post-release
 * Position-2 within pre-release (qualifier): a=dev, b=alpha, c=beta, d=rc
 */
@Component
@Qualifier("pypi")
public class PEP440VersionNormalizer
    implements VersionNormalizer
{
  /**
   * Defense-in-depth cap on input length. Real PEP 440 versions are well under this; rejecting
   * pathologically long strings up front avoids unnecessary regex work and large intermediate
   * allocations during normalization.
   */
  private static final int MAX_VERSION_LENGTH = 256;

  /**
   * Maximum number of decimal digits that fits safely in a {@code long}. Any captured numeric
   * run longer than this is clamped to its leading digits before parsing to avoid
   * {@link NumberFormatException} from overflow.
   */
  private static final int MAX_DIGIT_PARSE_LENGTH = 18;

  private static final String PRE_RELEASE_KEY = "0a";

  private static final String RELEASE_KEY = "0c";

  private static final String POST_RELEASE_KEY = "0d";

  private static final String DEV_SUB = "a";

  private static final String ALPHA_SUB = "b";

  private static final String BETA_SUB = "c";

  private static final String RC_SUB = "d";

  private static final Pattern EPOCH_PATTERN = Pattern.compile("^(\\d+)!(.+)$");

  private static final Pattern DEV_PATTERN = Pattern.compile("[._-]dev[._-]?(\\d*)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern POST_PATTERN =
      Pattern.compile("[._-]?(post|rev|r)[._-]?(\\d*)$", Pattern.CASE_INSENSITIVE);

  /**
   * PEP 440 implicit post-release form: a bare {@code -N} suffix means {@code .postN}. Without this
   * the hyphen is rewritten to a dot by {@link #normalizeRelease(String)} and the version collides
   * with an ordinary release of the same digits, e.g. {@code 1.2.3-1} and {@code 1.2.3.1}.
   */
  private static final Pattern POST_IMPLICIT_PATTERN = Pattern.compile("-(\\d+)$");

  private static final Pattern RC_PATTERN = Pattern.compile("[._-]?(rc|c|preview|pre)[._-]?(\\d*)$",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern BETA_PATTERN = Pattern.compile("[._-]?(beta|b)[._-]?(\\d*)$",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern ALPHA_PATTERN = Pattern.compile("[._-]?(alpha|a)[._-]?(\\d*)$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public String getNormalizedVersion(final String version) {
    if (isBlank(version)) {
      return "";
    }

    if (version.length() > MAX_VERSION_LENGTH) {
      return "";
    }

    String trimmed = version.trim();

    // Strip local version (+xyz)
    int plusIndex = trimmed.indexOf('+');
    if (plusIndex >= 0) {
      trimmed = trimmed.substring(0, plusIndex);
    }

    // Handle epoch (N!rest)
    String epoch = VersionNumberExpander.padPrefixed(0);
    Matcher epochMatcher = EPOCH_PATTERN.matcher(trimmed);
    if (epochMatcher.matches()) {
      epoch = VersionNumberExpander.padPrefixed(safeParseLong(epochMatcher.group(1)));
      trimmed = epochMatcher.group(2);
    }

    return epoch + "." + normalizeVersionAndQualifier(trimmed);
  }

  protected String normalizeVersionAndQualifier(final String version) {
    // Check for dev suffix FIRST — it may be attached to a pre-release (e.g., 1.0a1.dev1)
    Matcher devMatcher = DEV_PATTERN.matcher(version);
    if (devMatcher.find()) {
      String versionPart = version.substring(0, devMatcher.start());
      String devNum = devMatcher.group(1).isEmpty() ? "0" : devMatcher.group(1);
      String devSuffix = ".0dev." + VersionNumberExpander.padPrefixed(safeParseLong(devNum));

      // Check if remaining versionPart contains a pre-release qualifier (e.g., "1.0a1")
      if (hasPreReleaseQualifier(versionPart)) {
        // Recursively normalize the pre-release base, then append dev sub-sort
        // 1.0a1.dev1 → normalize("1.0a1") + ".0dev.000000001"
        // This sorts BELOW "1.0a1" because ".0dev" < ".z" (finalized marker)
        return normalizePreReleaseBase(versionPart) + devSuffix;
      }

      // Pure dev release (1.0.dev1) — no parent pre-release
      return VersionNumberExpander.expandPrefixed(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + DEV_SUB
          + "."
          + VersionNumberExpander.padPrefixed(safeParseLong(devNum));
    }

    // Check for alpha suffix
    Matcher alphaMatcher = ALPHA_PATTERN.matcher(version);
    if (alphaMatcher.find()) {
      String versionPart = version.substring(0, alphaMatcher.start());
      String num = alphaMatcher.group(2).isEmpty() ? "0" : alphaMatcher.group(2);
      return preReleaseKey(versionPart, ALPHA_SUB, num, true);
    }

    // Check for beta suffix
    Matcher betaMatcher = BETA_PATTERN.matcher(version);
    if (betaMatcher.find()) {
      String versionPart = version.substring(0, betaMatcher.start());
      String num = betaMatcher.group(2).isEmpty() ? "0" : betaMatcher.group(2);
      return preReleaseKey(versionPart, BETA_SUB, num, true);
    }

    // Check for rc suffix
    Matcher rcMatcher = RC_PATTERN.matcher(version);
    if (rcMatcher.find()) {
      String versionPart = version.substring(0, rcMatcher.start());
      String num = rcMatcher.group(2).isEmpty() ? "0" : rcMatcher.group(2);
      return preReleaseKey(versionPart, RC_SUB, num, true);
    }

    // Check for post suffix
    Matcher postMatcher = POST_PATTERN.matcher(version);
    if (postMatcher.find()) {
      String versionPart = version.substring(0, postMatcher.start());
      String num = postMatcher.group(2).isEmpty() ? "0" : postMatcher.group(2);
      return postReleaseKey(versionPart, num, true);
    }

    // Check for the implicit post suffix (-N), which PEP 440 treats as .postN
    Matcher postImplicitMatcher = POST_IMPLICIT_PATTERN.matcher(version);
    if (postImplicitMatcher.find()) {
      String versionPart = version.substring(0, postImplicitMatcher.start());
      return postReleaseKey(versionPart, postImplicitMatcher.group(1), true);
    }

    // Pure release
    return VersionNumberExpander.expandPrefixed(normalizeRelease(version)) + "." + RELEASE_KEY;
  }

  private boolean hasPreReleaseQualifier(final String version) {
    return ALPHA_PATTERN.matcher(version).find()
        || BETA_PATTERN.matcher(version).find()
        || RC_PATTERN.matcher(version).find()
        || POST_PATTERN.matcher(version).find();
  }

  private String normalizePreReleaseBase(final String version) {
    // Normalize without dev — will produce the base pre-release encoding
    Matcher alphaMatcher = ALPHA_PATTERN.matcher(version);
    if (alphaMatcher.find()) {
      String versionPart = version.substring(0, alphaMatcher.start());
      String num = alphaMatcher.group(2).isEmpty() ? "0" : alphaMatcher.group(2);
      return preReleaseKey(versionPart, ALPHA_SUB, num, false);
    }

    Matcher betaMatcher = BETA_PATTERN.matcher(version);
    if (betaMatcher.find()) {
      String versionPart = version.substring(0, betaMatcher.start());
      String num = betaMatcher.group(2).isEmpty() ? "0" : betaMatcher.group(2);
      return preReleaseKey(versionPart, BETA_SUB, num, false);
    }

    Matcher rcMatcher = RC_PATTERN.matcher(version);
    if (rcMatcher.find()) {
      String versionPart = version.substring(0, rcMatcher.start());
      String num = rcMatcher.group(2).isEmpty() ? "0" : rcMatcher.group(2);
      return preReleaseKey(versionPart, RC_SUB, num, false);
    }

    Matcher postMatcher = POST_PATTERN.matcher(version);
    if (postMatcher.find()) {
      String versionPart = version.substring(0, postMatcher.start());
      String num = postMatcher.group(2).isEmpty() ? "0" : postMatcher.group(2);
      return postReleaseKey(versionPart, num, false);
    }

    // Fallback — shouldn't reach here if hasPreReleaseQualifier was true
    return VersionNumberExpander.expandPrefixed(normalizeRelease(version));
  }

  /**
   * Builds the shared pre-release sort key used by both {@link #normalizeVersionAndQualifier} (which finalizes
   * the key with a trailing {@code .z} marker once no dev suffix follows) and {@link #normalizePreReleaseBase}
   * (which omits the marker because a {@code .devN} suffix is still to be appended by the caller).
   */
  private String preReleaseKey(final String versionPart, final String sub, final String num, final boolean finalized) {
    String key = VersionNumberExpander.expandPrefixed(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "."
        + sub + "." + VersionNumberExpander.padPrefixed(safeParseLong(num));
    return finalized ? key + ".z" : key;
  }

  /**
   * Builds the shared post-release sort key, mirroring {@link #preReleaseKey} for the post-release tier.
   */
  private String postReleaseKey(final String versionPart, final String num, final boolean finalized) {
    String key = VersionNumberExpander.expandPrefixed(normalizeRelease(versionPart)) + "." + POST_RELEASE_KEY + "."
        + VersionNumberExpander.padPrefixed(safeParseLong(num));
    return finalized ? key + ".z" : key;
  }

  /**
   * Normalize the release segment: replace underscores and hyphens with dots.
   */
  private String normalizeRelease(final String release) {
    if (release == null || release.isEmpty()) {
      return "0";
    }
    return stripTrailingZeros(release.replaceAll("[_-]", "."));
  }

  /**
   * PEP 440 treats trailing zero release segments as insignificant, so {@code 1.2.3.0} and
   * {@code 1.2.3} are the same version. They must therefore produce the same key, otherwise a
   * trailing-zero release outranks a post-release of the same version.
   */
  private String stripTrailingZeros(final String release) {
    String[] segments = release.split("\\.");
    int end = segments.length;
    while (end > 1 && segments[end - 1].matches("0+")) {
      end--;
    }
    return String.join(".", java.util.Arrays.copyOf(segments, end));
  }

  /**
   * Parse a captured digit run safely. Empty input returns {@code 0}. Inputs longer than
   * {@link #MAX_DIGIT_PARSE_LENGTH} are clamped to their leading digits to avoid overflow
   * from {@link Long#parseLong(String)} when fed pathologically long numeric strings.
   */
  private static long safeParseLong(final String digits) {
    if (digits == null || digits.isEmpty()) {
      return 0L;
    }
    String clamped = digits.length() > MAX_DIGIT_PARSE_LENGTH
        ? digits.substring(0, MAX_DIGIT_PARSE_LENGTH)
        : digits;
    return Long.parseLong(clamped);
  }
}
