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
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SemanticVersion} compliance with semantic versioning specification.
 */
@DisplayName("SemanticVersion semver compliance tests")
class SemanticVersionTest
{
  @Nested
  @DisplayName("Parsing valid semantic versions")
  class ParsingValidVersions
  {
    @Test
    @DisplayName("Parse simple version with major.minor.patch")
    void parseSimpleVersion() {
      SemanticVersion version = SemanticVersion.parse("1.2.3");
      assertEquals(1, version.major());
      assertEquals(2, version.minor());
      assertEquals(3, version.patch());
      assertFalse(version.isPrerelease());
    }

    @Test
    @DisplayName("Parse version with zero components")
    void parseVersionWithZeros() {
      SemanticVersion version = SemanticVersion.parse("0.0.0");
      assertEquals(0, version.major());
      assertEquals(0, version.minor());
      assertEquals(0, version.patch());
    }

    @Test
    @DisplayName("Parse version with large numbers")
    void parseVersionWithLargeNumbers() {
      SemanticVersion version = SemanticVersion.parse("12345.67890.1112131415");
      assertEquals(12345, version.major());
      assertEquals(67890, version.minor());
      assertEquals(1112131415, version.patch());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.0.0-alpha",
        "1.0.0-alpha.1",
        "1.0.0-0.3.7",
        "1.0.0-x.7.z.92",
        "1.0.0-x-y-z.--",
        "1.0.0-alpha.beta",
        "1.0.0-alpha.beta.1"
    })
    @DisplayName("Parse versions with prerelease identifiers")
    void parseVersionWithPrerelease(final String versionString) {
      SemanticVersion version = SemanticVersion.parse(versionString);
      assertTrue(version.isPrerelease());
      assertNotNull(version.prerelease());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.0.0+20130313144700",
        "1.0.0+exp.sha.5114f85",
        "1.0.0+21AF26D3"
    })
    @DisplayName("Parse versions with build metadata")
    void parseVersionWithBuildMetadata(final String versionString) {
      SemanticVersion version = SemanticVersion.parse(versionString);
      assertNotNull(version.buildMetadata());
    }

    @Test
    @DisplayName("Parse version with prerelease and build metadata")
    void parseVersionWithPrereleaseAndBuildMetadata() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-alpha+001");
      assertTrue(version.isPrerelease());
      assertEquals("alpha", version.prerelease());
      assertEquals("001", version.buildMetadata());
    }

    @Test
    @DisplayName("Parse version with numeric prerelease")
    void parseVersionWithNumericPrerelease() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-1");
      assertTrue(version.isPrerelease());
      assertEquals("1", version.prerelease());
    }

    @Test
    @DisplayName("Parse version with multiple prerelease identifiers")
    void parseVersionWithMultiplePrereleaseIdentifiers() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-alpha.1.2.3");
      assertTrue(version.isPrerelease());
      assertEquals("alpha.1.2.3", version.prerelease());
    }
  }

  @Nested
  @DisplayName("Parsing invalid semantic versions")
  class ParsingInvalidVersions
  {
    @Test
    @DisplayName("Parse throws exception for invalid version")
    void parseThrowsForInvalidVersion() {
      assertThrows(RuntimeException.class, () -> SemanticVersion.parse("not-a-version"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "1",
        "1.2",
        "1.2.3.4",
        "1.2.3-",
        "1.2.3+",
        "01.2.3",
        "1.02.3",
        "1.2.03",
        "1.2.3-01",
        "1.2.3-alpha..1",
        "v1.2.3",
        "-1.2.3"
    })
    @DisplayName("maybeParse returns empty for invalid versions")
    void maybeParseReturnsEmptyForInvalidVersions(final String invalidVersion) {
      Optional<SemanticVersion> result = SemanticVersion.maybeParse(invalidVersion);
      assertTrue(result.isEmpty(), "Expected empty for: " + invalidVersion);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2147483648.0.0",
        "0.2147483648.0",
        "0.0.2147483648",
        "99999999999999999999.0.0"
    })
    @DisplayName("maybeParse returns empty rather than throwing when a component exceeds an int")
    void maybeParseReturnsEmptyForOversizedComponents(final String oversizedVersion) {
      Optional<SemanticVersion> result = SemanticVersion.maybeParse(oversizedVersion);
      assertTrue(result.isEmpty(), "Expected empty for: " + oversizedVersion);
    }

    @Test
    @DisplayName("Int boundary still parses")
    void maxIntComponentsParse() {
      Optional<SemanticVersion> result = SemanticVersion.maybeParse("2147483647.2147483647.2147483647");
      assertTrue(result.isPresent());
      assertEquals(Integer.MAX_VALUE, result.get().major());
    }
  }

  @Nested
  @DisplayName("Version comparison")
  class VersionComparison
  {
    static Stream<Arguments> versionComparisons() {
      return Stream.of(
          // Major version comparisons
          Arguments.of("1.0.0", "2.0.0", -1),
          Arguments.of("2.0.0", "1.0.0", 1),
          Arguments.of("1.0.0", "1.0.0", 0),

          // Minor version comparisons
          Arguments.of("1.0.0", "1.1.0", -1),
          Arguments.of("1.1.0", "1.0.0", 1),
          Arguments.of("1.1.0", "1.1.0", 0),

          // Patch version comparisons
          Arguments.of("1.0.0", "1.0.1", -1),
          Arguments.of("1.0.1", "1.0.0", 1),
          Arguments.of("1.0.1", "1.0.1", 0),

          // Prerelease has lower precedence than normal version
          Arguments.of("1.0.0-alpha", "1.0.0", -1),
          Arguments.of("1.0.0", "1.0.0-alpha", 1),

          // Build metadata does not affect precedence
          Arguments.of("1.0.0+alpha", "1.0.0+beta", 0),
          Arguments.of("1.0.0-alpha+beta", "1.0.0-alpha+gamma", 0));
    }

    @ParameterizedTest
    @MethodSource("versionComparisons")
    @DisplayName("Compare versions according to semver precedence")
    void compareVersions(final String v1, final String v2, final int expected) {
      SemanticVersion version1 = SemanticVersion.parse(v1);
      SemanticVersion version2 = SemanticVersion.parse(v2);
      int result = version1.compareTo(version2);
      assertEquals(normalizeCompareResult(result), expected,
          () -> String.format("Expected %s compared to %s to be %d", v1, v2, expected));
    }

    private int normalizeCompareResult(final int result) {
      return Integer.compare(result, 0);
    }

    @Test
    @DisplayName("Prerelease version has lower precedence than normal version")
    void prereleaseHasLowerPrecedence() {
      SemanticVersion release = SemanticVersion.parse("1.0.0");
      SemanticVersion prerelease = SemanticVersion.parse("1.0.0-alpha");

      assertTrue(release.compareTo(prerelease) > 0,
          "Release version should have higher precedence than prerelease");
      assertTrue(prerelease.compareTo(release) < 0,
          "Prerelease version should have lower precedence than release");
    }

    @Test
    @DisplayName("Build metadata does not affect version precedence")
    void buildMetadataDoesNotAffectPrecedence() {
      SemanticVersion v1 = SemanticVersion.parse("1.0.0+build1");
      SemanticVersion v2 = SemanticVersion.parse("1.0.0+build2");

      assertEquals(0, v1.compareTo(v2),
          "Build metadata should not affect precedence");
    }
  }

  @Nested
  @DisplayName("Prerelease identifier comparison")
  class PrereleaseComparison
  {
    static Stream<Arguments> prereleaseComparisons() {
      return Stream.of(
          // Numeric identifiers compared as integers
          Arguments.of("1.0.0-1", "1.0.0-2", -1),
          Arguments.of("1.0.0-2", "1.0.0-1", 1),

          // Alphanumeric identifiers compared lexically
          Arguments.of("1.0.0-alpha", "1.0.0-beta", -1),
          Arguments.of("1.0.0-beta", "1.0.0-alpha", 1),

          // Numeric always lower than alphanumeric
          Arguments.of("1.0.0-1", "1.0.0-alpha", -1),
          Arguments.of("1.0.0-alpha", "1.0.0-1", 1),

          // Larger identifier set has higher precedence
          Arguments.of("1.0.0-alpha", "1.0.0-alpha.1", -1),
          Arguments.of("1.0.0-alpha.1", "1.0.0-alpha.beta", -1),
          Arguments.of("1.0.0-beta.2", "1.0.0-beta.11", -1),

          // Full semver spec example order
          Arguments.of("1.0.0-alpha", "1.0.0-alpha.1", -1),
          Arguments.of("1.0.0-alpha.1", "1.0.0-alpha.beta", -1),
          Arguments.of("1.0.0-alpha.beta", "1.0.0-beta", -1),
          Arguments.of("1.0.0-beta", "1.0.0-beta.2", -1),
          Arguments.of("1.0.0-beta.2", "1.0.0-beta.11", -1),
          Arguments.of("1.0.0-beta.11", "1.0.0-rc.1", -1),
          Arguments.of("1.0.0-rc.1", "1.0.0", -1));
    }

    @ParameterizedTest
    @MethodSource("prereleaseComparisons")
    @DisplayName("Compare prerelease identifiers according to semver spec")
    void comparePrereleaseIdentifiers(final String v1, final String v2, final int expected) {
      SemanticVersion version1 = SemanticVersion.parse(v1);
      SemanticVersion version2 = SemanticVersion.parse(v2);
      int result = version1.compareTo(version2);
      assertEquals(normalizeCompareResult(result), expected,
          () -> String.format("Expected %s compared to %s to be %d", v1, v2, expected));

      // Reverse
      result = version2.compareTo(version1);
      assertEquals(normalizeCompareResult(result), -expected,
          () -> String.format("Expected %s compared to %s to be %d", v1, v2, -expected));
    }

    private int normalizeCompareResult(final int result) {
      return Integer.compare(result, 0);
    }
  }

  @Nested
  @DisplayName("isPrerelease method")
  class IsPrerelease
  {
    @Test
    @DisplayName("Returns true when prerelease identifier present")
    void returnsTrueWhenPrereleasePresent() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-alpha");
      assertTrue(version.isPrerelease());
    }

    @Test
    @DisplayName("Returns false when no prerelease identifier")
    void returnsFalseWhenNoPrerelease() {
      SemanticVersion version = SemanticVersion.parse("1.0.0");
      assertFalse(version.isPrerelease());
    }

    @Test
    @DisplayName("Returns false when only build metadata present")
    void returnsFalseWhenOnlyBuildMetadata() {
      SemanticVersion version = SemanticVersion.parse("1.0.0+build123");
      assertFalse(version.isPrerelease());
    }

    @Test
    @DisplayName("Returns true when both prerelease and build metadata present")
    void returnsTrueWhenPrereleaseAndBuildMetadata() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-alpha+build123");
      assertTrue(version.isPrerelease());
    }
  }

  @Nested
  @DisplayName("Record component access")
  class RecordComponents
  {
    @Test
    @DisplayName("Access major version component")
    void accessMajorVersion() {
      SemanticVersion version = SemanticVersion.parse("3.2.1");
      assertEquals(3, version.major());
    }

    @Test
    @DisplayName("Access minor version component")
    void accessMinorVersion() {
      SemanticVersion version = SemanticVersion.parse("3.2.1");
      assertEquals(2, version.minor());
    }

    @Test
    @DisplayName("Access patch version component")
    void accessPatchVersion() {
      SemanticVersion version = SemanticVersion.parse("3.2.1");
      assertEquals(1, version.patch());
    }

    @Test
    @DisplayName("Access prerelease component")
    void accessPrereleaseComponent() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-alpha.1.beta.2");
      assertEquals("alpha.1.beta.2", version.prerelease());
    }

    @Test
    @DisplayName("Access build metadata component")
    void accessBuildMetadataComponent() {
      SemanticVersion version = SemanticVersion.parse("1.0.0+exp.sha.5114f85");
      assertEquals("exp.sha.5114f85", version.buildMetadata());
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases
  {
    @Test
    @DisplayName("Zero major version is valid (initial development)")
    void zeroMajorVersionIsValid() {
      SemanticVersion version = SemanticVersion.parse("0.1.0");
      assertEquals(0, version.major());
    }

    @Test
    @DisplayName("Zero minor version is valid")
    void zeroMinorVersionIsValid() {
      SemanticVersion version = SemanticVersion.parse("1.0.0");
      assertEquals(0, version.minor());
    }

    @Test
    @DisplayName("Zero patch version is valid")
    void zeroPatchVersionIsValid() {
      SemanticVersion version = SemanticVersion.parse("1.1.0");
      assertEquals(0, version.patch());
    }

    @Test
    @DisplayName("Hyphen allowed in prerelease identifiers")
    void hyphenAllowedInPrerelease() {
      SemanticVersion version = SemanticVersion.parse("1.0.0-alpha-beta");
      assertEquals("alpha-beta", version.prerelease());
    }

    @Test
    @DisplayName("Hyphen allowed in build metadata")
    void hyphenAllowedInBuildMetadata() {
      SemanticVersion version = SemanticVersion.parse("1.0.0+build-123");
      assertEquals("build-123", version.buildMetadata());
    }
  }
}
