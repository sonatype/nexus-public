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

  private static final String PRE_RELEASE_KEY = "a";

  private static final String RELEASE_KEY = "c";

  private static final String POST_RELEASE_KEY = "d";

  private static final String DEV_SUB = "a";

  private static final String ALPHA_SUB = "b";

  private static final String BETA_SUB = "c";

  private static final String RC_SUB = "d";

  private static final Pattern EPOCH_PATTERN = Pattern.compile("^(\\d+)!(.+)$");

  private static final Pattern DEV_PATTERN = Pattern.compile("[._-]dev[._-]?(\\d*)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern POST_PATTERN =
      Pattern.compile("[._-]?(post|rev|r)[._-]?(\\d*)$", Pattern.CASE_INSENSITIVE);

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
    String epoch = "000000000";
    Matcher epochMatcher = EPOCH_PATTERN.matcher(trimmed);
    if (epochMatcher.matches()) {
      epoch = String.format("%09d", safeParseLong(epochMatcher.group(1)));
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
      String devSuffix = ".0dev." + String.format("%09d", safeParseLong(devNum));

      // Check if remaining versionPart contains a pre-release qualifier (e.g., "1.0a1")
      if (hasPreReleaseQualifier(versionPart)) {
        // Recursively normalize the pre-release base, then append dev sub-sort
        // 1.0a1.dev1 → normalize("1.0a1") + ".0dev.000000001"
        // This sorts BELOW "1.0a1" because ".0dev" < ".z" (finalized marker)
        return normalizePreReleaseBase(versionPart) + devSuffix;
      }

      // Pure dev release (1.0.dev1) — no parent pre-release
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + DEV_SUB + "."
          + String.format("%09d", safeParseLong(devNum));
    }

    // Check for alpha suffix
    Matcher alphaMatcher = ALPHA_PATTERN.matcher(version);
    if (alphaMatcher.find()) {
      String versionPart = version.substring(0, alphaMatcher.start());
      String num = alphaMatcher.group(2).isEmpty() ? "0" : alphaMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + ALPHA_SUB + "."
          + String.format("%09d", safeParseLong(num)) + ".z";
    }

    // Check for beta suffix
    Matcher betaMatcher = BETA_PATTERN.matcher(version);
    if (betaMatcher.find()) {
      String versionPart = version.substring(0, betaMatcher.start());
      String num = betaMatcher.group(2).isEmpty() ? "0" : betaMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + BETA_SUB + "."
          + String.format("%09d", safeParseLong(num)) + ".z";
    }

    // Check for rc suffix
    Matcher rcMatcher = RC_PATTERN.matcher(version);
    if (rcMatcher.find()) {
      String versionPart = version.substring(0, rcMatcher.start());
      String num = rcMatcher.group(2).isEmpty() ? "0" : rcMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + RC_SUB + "."
          + String.format("%09d", safeParseLong(num)) + ".z";
    }

    // Check for post suffix
    Matcher postMatcher = POST_PATTERN.matcher(version);
    if (postMatcher.find()) {
      String versionPart = version.substring(0, postMatcher.start());
      String num = postMatcher.group(2).isEmpty() ? "0" : postMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + POST_RELEASE_KEY + "."
          + String.format("%09d", safeParseLong(num)) + ".z";
    }

    // Pure release
    return VersionNumberExpander.expand(normalizeRelease(version)) + "." + RELEASE_KEY;
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
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + ALPHA_SUB + "."
          + String.format("%09d", safeParseLong(num));
    }

    Matcher betaMatcher = BETA_PATTERN.matcher(version);
    if (betaMatcher.find()) {
      String versionPart = version.substring(0, betaMatcher.start());
      String num = betaMatcher.group(2).isEmpty() ? "0" : betaMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + BETA_SUB + "."
          + String.format("%09d", safeParseLong(num));
    }

    Matcher rcMatcher = RC_PATTERN.matcher(version);
    if (rcMatcher.find()) {
      String versionPart = version.substring(0, rcMatcher.start());
      String num = rcMatcher.group(2).isEmpty() ? "0" : rcMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + PRE_RELEASE_KEY + "." + RC_SUB + "."
          + String.format("%09d", safeParseLong(num));
    }

    Matcher postMatcher = POST_PATTERN.matcher(version);
    if (postMatcher.find()) {
      String versionPart = version.substring(0, postMatcher.start());
      String num = postMatcher.group(2).isEmpty() ? "0" : postMatcher.group(2);
      return VersionNumberExpander.expand(normalizeRelease(versionPart)) + "." + POST_RELEASE_KEY + "."
          + String.format("%09d", safeParseLong(num));
    }

    // Fallback — shouldn't reach here if hasPreReleaseQualifier was true
    return VersionNumberExpander.expand(normalizeRelease(version));
  }

  /**
   * Normalize the release segment: replace underscores and hyphens with dots.
   */
  private String normalizeRelease(final String release) {
    if (release == null || release.isEmpty()) {
      return "0";
    }
    return release.replaceAll("[_-]", ".");
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
