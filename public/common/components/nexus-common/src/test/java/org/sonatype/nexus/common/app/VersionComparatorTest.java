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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.aether.version.Version;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class VersionComparatorTest
{
  @Test
  public void testStringVersionComparator() {
    List<String> sorted = Stream.of("1.1", "1.2", "1.0")
        .sorted(VersionComparator.INSTANCE)
        .collect(Collectors.toList());

    assertThat(sorted.get(0), equalTo("1.0"));
    assertThat(sorted.get(1), equalTo("1.1"));
    assertThat(sorted.get(2), equalTo("1.2"));
  }

  @Test
  public void testStringVersionComparator_Snapshot() {
    List<String> sorted = Stream
        .of("1.1-20170919.212404-2", "1.1-20170919.212405-3", "1.1-20170919.212403-1")
        .sorted(VersionComparator.INSTANCE)
        .collect(Collectors.toList());
    assertThat(sorted.get(0), equalTo("1.1-20170919.212403-1"));
    assertThat(sorted.get(1), equalTo("1.1-20170919.212404-2"));
    assertThat(sorted.get(2), equalTo("1.1-20170919.212405-3"));
  }

  @Test
  public void testStringVersionComparator_isTransitive() {
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.1"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.1", "1.2"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.2"), lessThan(0));

    assertThat(VersionComparator.INSTANCE.compare("1.1", "1.0"), greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.2", "1.1"), greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.2", "1.0"), greaterThan(0));
  }

  @Test
  public void testStringVersionComparator_mixedVersionAndNonVersionStringAreSortedTogether() {
    List<String> versions = Arrays.asList("2.0.0", "", "1.0-foo", "1.0-beta", "1.2", "1.1-SNAPSHOT", "foo", "2foo");
    versions.sort(VersionComparator.INSTANCE);
    assertThat(versions, is(Arrays.asList("", "2foo", "foo", "1.0-beta", "1.0-foo", "1.1-SNAPSHOT", "1.2", "2.0.0")));
  }

  @Test
  public void testStringVersionComparator_isNotThrowingExceptionOnMixedParsableAndNotParsableInput() {
    List<String> list = Arrays
        .asList("Z9BtVtVO", "11.0", "93eAzyqO", "saB5kQ64", "9.0", "2.0", "Mevi29bx", "10.0", "nPqYe0qc", "14.0",
            "AsQD7LvI", "7.0", "3.0", "13.0", "W1UmeoHQ", "8.0", "0eyq3xGh", "ADdMasr4", "KsepxKG4", "15.0", "LjEGUAU0",
            "Txu1bI2F", "16.0", "5.0", "1.0", "QmcAWDLQ", "6.0", "4.0", "apUQ6KHw", "ayFi1K6t", "CqJzJm5Z", "ckhC6xIH",
            "12.0");

    try {
      list.sort(VersionComparator.INSTANCE);
    }
    catch (IllegalArgumentException e) {
      fail("An exception was thrown when sorting a list: " + e.getMessage());
    }
  }

  @Test
  public void testStringVersionComparator_equalVersionsAreEqual() {
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.0"), is(0));
  }

  @Test
  public void testStringVersionComparator_orderingIsSemanticNotLexical() {
    // Lexical String.compareTo would sort "1.10" before "1.2"; semantic version ordering must not.
    assertThat(VersionComparator.INSTANCE.compare("1.10", "1.2"), greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.2", "1.10"), lessThan(0));
  }

  @Test
  public void testStringVersionComparator_qualifierOrderingFollowsGenericVersionScheme() {
    // GenericVersionScheme qualifier weights: alpha < beta < milestone < rc < snapshot < release < sp
    List<String> sorted = Stream
        .of("1.0-sp", "1.0", "1.0-snapshot", "1.0-rc", "1.0-milestone", "1.0-beta", "1.0-alpha")
        .sorted(VersionComparator.INSTANCE)
        .collect(Collectors.toList());

    assertThat(sorted,
        is(Arrays.asList("1.0-alpha", "1.0-beta", "1.0-milestone", "1.0-rc", "1.0-snapshot", "1.0", "1.0-sp")));
  }

  @Test
  public void testStringVersionComparator_qualifierChainIsStrictlyIncreasing() {
    assertThat(VersionComparator.INSTANCE.compare("1.0-alpha", "1.0-beta"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0-beta", "1.0-rc"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0-rc", "1.0"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.0-sp"), lessThan(0));
  }

  @Test
  public void testStringVersionComparator_snapshotIsOlderThanRelease() {
    assertThat(VersionComparator.INSTANCE.compare("1.0-SNAPSHOT", "1.0"), lessThan(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.0-SNAPSHOT"), greaterThan(0));
  }

  @Test
  public void testStringVersionComparator_qualifiersAreCaseInsensitive() {
    assertThat(VersionComparator.INSTANCE.compare("1.0-BETA", "1.0-beta"), is(0));
  }

  @Test
  public void testStringVersionComparator_versionLikeSortsAfterNonVersionLike() {
    assertThat(VersionComparator.INSTANCE.compare("1.0", "foo"), greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("foo", "1.0"), lessThan(0));
  }

  @Test
  public void testIsVersionLike_acceptsVersionLikeStrings() {
    VersionComparator comparator = new VersionComparator();
    assertThat(comparator.isVersionLike("1"), is(true));
    assertThat(comparator.isVersionLike("1.0"), is(true));
    assertThat(comparator.isVersionLike("1_0"), is(true));
    assertThat(comparator.isVersionLike("1-0"), is(true));
    assertThat(comparator.isVersionLike("1.0a"), is(true));
    assertThat(comparator.isVersionLike("1.0.beta"), is(true));
  }

  @Test
  public void testIsVersionLike_rejectsNonVersionLikeStrings() {
    VersionComparator comparator = new VersionComparator();
    assertThat(comparator.isVersionLike("foo"), is(false));
    assertThat(comparator.isVersionLike(""), is(false));
    assertThat(comparator.isVersionLike("v1.0"), is(false));
    assertThat(comparator.isVersionLike("1.0 "), is(false)); // trailing space
    assertThat(comparator.isVersionLike("1..0"), is(false)); // empty segment between delimiters
  }

  @Test
  public void testVersion_returnsParsedVersionForValidInput() {
    Version version = VersionComparator.version("1.2.3");

    assertThat(version, notNullValue());
    assertThat(version.toString(), equalTo("1.2.3"));
    assertThat(version.compareTo(VersionComparator.version("1.2.3")), is(0));
  }

  @Test
  public void testVersion_isLenientAndDoesNotThrowForNonVersionInput() {
    // GenericVersionScheme.parseVersion never throws InvalidVersionSpecificationException, so version()
    // does not throw IllegalArgumentException even for non-version-like input; it returns a Version.
    Version version = VersionComparator.version("not-a-real-version");

    assertThat(version, notNullValue());
    assertThat(version.toString(), equalTo("not-a-real-version"));
  }

  @Test
  public void testCompareNotVersionLikeStrings_usesStringCompareTo() {
    VersionComparator comparator = new VersionComparator();
    assertThat(comparator.compareNotVersionLikeStrings("foo", "bar"), equalTo("foo".compareTo("bar")));
    assertThat(comparator.compareNotVersionLikeStrings("bar", "foo"), equalTo("bar".compareTo("foo")));
    assertThat(comparator.compareNotVersionLikeStrings("foo", "foo"), is(0));
  }

  @Test
  public void testStringVersionComparator_qualifierAliasesMatchGenericVersionScheme() {
    // GenericVersionScheme normalizes several qualifier aliases: "ga"/"final" collapse to the bare
    // release, "a" -> "alpha", and "cr" -> "rc". Locking these guards against the swap silently
    // changing the alias table (NEXUS-53142).
    assertThat(VersionComparator.INSTANCE.compare("1.0-ga", "1.0"), is(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0-final", "1.0"), is(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0-a1", "1.0-alpha1"), is(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0-cr", "1.0-rc"), is(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0-cr1", "1.0-rc1"), is(0));
  }

  @Test
  public void testStringVersionComparator_literalReleaseQualifierEqualsBareRelease() {
    // NEXUS-53142: maven-resolver's GenericVersionScheme treats the literal "release" word as an alias for the bare
    // release (alongside "ga"/"final"), which matches Maven's canonical ComparableVersion. Eclipse Aether 1.1.0 was
    // inconsistent here -- it aliased "ga"/"final" but not "release", so it sorted "1.0-release" ABOVE "1.0". The swap
    // aligns Nexus with the rest of the Maven ecosystem, so the two versions now compare equal.
    assertThat(VersionComparator.INSTANCE.compare("1.0-release", "1.0"), is(0));
    assertThat(VersionComparator.INSTANCE.compare("1.0", "1.0-release"), is(0));
  }

  @Test
  public void testStringVersionComparator_dotAndHyphenSeparatorsAreEquivalent() {
    // '.' and '-' are interchangeable segment separators in GenericVersionScheme, so 1.0.0 == 1-0-0.
    assertThat(VersionComparator.INSTANCE.compare("1.0.0", "1-0-0"), is(0));
  }

  @Test
  public void testStringVersionComparator_largeNumericSegmentsComparedAsBigIntegers() {
    // Numeric segments are compared as arbitrary-precision integers, not lexically or by length,
    // so a 21-digit value outranks a 20-digit value.
    assertThat(VersionComparator.INSTANCE.compare("100000000000000000000.0", "99999999999999999999.0"),
        greaterThan(0));
    assertThat(VersionComparator.INSTANCE.compare("99999999999999999999.0", "100000000000000000000.0"),
        lessThan(0));
  }

  @Test
  public void testStringVersionComparator_buildMetadataTreatedAsNonVersionLike() {
    // Deliberate divergence from raw GenericVersionScheme: '+' is rejected by VERSION_RE, so build
    // metadata strings are NOT version-like here and fall back to lexical String.compareTo ordering,
    // unlike the Nuget/Docker code paths that feed '+' versions straight into GenericVersionScheme.
    VersionComparator comparator = new VersionComparator();
    assertThat(comparator.isVersionLike("1.0.0+build123"), is(false));
    assertThat(VersionComparator.INSTANCE.compare("1.0.0+build123", "1.0.0+build124"),
        equalTo("1.0.0+build123".compareTo("1.0.0+build124")));
  }

  @Test(expected = NullPointerException.class)
  public void testVersion_nullThrowsNullPointerException() {
    // version() only catches InvalidVersionSpecificationException; GenericVersionScheme.parseVersion(null)
    // throws NullPointerException, which propagates. Lock this contract so the maven-resolver swap is
    // verified to keep null hostile rather than silently accepting it.
    VersionComparator.version(null);
  }

  @Test(expected = NullPointerException.class)
  public void testCompare_nullArgumentThrowsNullPointerException() {
    // compare() is not null-safe: isVersionLike(null) runs the VERSION_RE matcher against null, which throws
    // NullPointerException. Pin the current (non-null-safe) boundary behavior.
    VersionComparator.INSTANCE.compare(null, "1.0");
  }
}
