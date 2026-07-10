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
package org.sonatype.nexus.repository.maven.internal.content;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for {@link MetadataRebuildWorker}'s {@code reverseSortByVersion} comparator and its
 * {@code parseVersion} helper, which delegate to {@code org.apache.maven.resolver} 1.9.27's
 * {@link org.eclipse.aether.util.version.GenericVersionScheme} / {@code GenericVersion#compareTo} (Maven Resolver
 * kept the original {@code org.eclipse.aether.*} package namespace).
 * <p>
 * NEXUS-53142 swapped {@code org.eclipse.aether} 1.1.0 for {@code org.apache.maven.resolver} 1.9.27. These tests
 * pin the (descending) ordering semantics so any behavioral drift is caught.
 * <p>
 * {@code reverseSortByVersion(a, b)} sorts in <strong>descending</strong> order:
 * <ul>
 * <li>if {@code parseVersion(b) == null} returns {@code -1};</li>
 * <li>else if {@code parseVersion(a) == null} returns {@code 1};</li>
 * <li>otherwise returns {@code parseVersion(b).compareTo(parseVersion(a))}.</li>
 * </ul>
 * <p>
 * Note on the {@code null} branches: {@code parseVersion} only returns {@code null} when
 * {@code GenericVersionScheme#parseVersion} throws {@code InvalidVersionSpecificationException}. That scheme constructs
 * a {@code GenericVersion} for <em>any</em> non-null string (even {@code ""}, {@code "..."}, {@code "$$$"},
 * {@code "1..2"}) and never throws that exception; a {@code null} argument throws {@code NullPointerException}, which
 * is
 * not caught by {@code parseVersion}. The {@code null} branches are therefore unreachable via
 * {@code GenericVersionScheme}
 * and cannot be exercised here without modifying production source (see {@link #nonNumericStringsAreParsedNotNulled()},
 * which proves the lenient parsing by asserting equal non-numeric strings compare to {@code 0} rather than hitting the
 * {@code -1}/{@code 1} null branches).
 */
@RunWith(MockitoJUnitRunner.class)
public class MetadataRebuildWorkerTest
{
  @Mock
  private Repository repository;

  @Mock
  private MavenContentFacet mavenContentFacet;

  @Mock
  private MavenPathParser mavenPathParser;

  private MetadataRebuildWorker worker;

  @Before
  public void setup() {
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(mavenContentFacet.getMavenPathParser()).thenReturn(mavenPathParser);

    worker = new MetadataRebuildWorker(repository, true, "group", "artifact", null, 20);
  }

  @Test
  public void lowerVersionSortsAfterHigherVersion() {
    // descending: "1.0" must sort after "2.0" -> positive
    assertThat(reverseSort("1.0", "2.0"), greaterThan(0));
  }

  @Test
  public void higherVersionSortsBeforeLowerVersion() {
    // descending: "2.0" must sort before "1.0" -> negative
    assertThat(reverseSort("2.0", "1.0"), lessThan(0));
  }

  @Test
  public void equalVersionsCompareToZero() {
    assertThat(reverseSort("1.0", "1.0"), is(0));
  }

  @Test
  public void numericSegmentsAreComparedNumericallyNotLexically() {
    // "1.10" > "1.2" numerically, so in descending order "1.2" (a) sorts after "1.10" (b) -> positive
    assertThat(reverseSort("1.2", "1.10"), greaterThan(0));
    assertThat(reverseSort("1.10", "1.2"), lessThan(0));
  }

  @Test
  public void snapshotSortsAfterRelease() {
    // a release is greater than its SNAPSHOT, so descending order keeps the release first
    assertThat(reverseSort("1.0-SNAPSHOT", "1.0"), greaterThan(0));
    assertThat(reverseSort("1.0", "1.0-SNAPSHOT"), lessThan(0));
  }

  @Test
  public void qualifierSortsAfterRelease() {
    // pre-release qualifiers (alpha) are older than the final release
    assertThat(reverseSort("1.0-alpha", "1.0"), greaterThan(0));
    assertThat(reverseSort("1.0", "1.0-alpha"), lessThan(0));
  }

  @Test
  public void literalReleaseQualifierEqualsBareRelease() {
    // NEXUS-53142: maven-resolver's GenericVersionScheme aliases the literal "release" word to the bare release (like
    // "ga"/"final"), matching Maven's canonical ComparableVersion. Eclipse Aether 1.1.0 sorted "1.0-release" ABOVE
    // "1.0"; after the swap the two compare equal, so this descending comparator reports no ordering between them.
    assertThat(reverseSort("1.0-release", "1.0"), is(0));
    assertThat(reverseSort("1.0", "1.0-release"), is(0));
  }

  @Test
  public void trailingZeroPaddingIsEquivalent() {
    // GenericVersion treats "1.0" and "1" as equivalent
    assertThat(reverseSort("1.0", "1"), is(0));
  }

  @Test
  public void emptyVersionIsEquivalentToZero() {
    // GenericVersion normalizes an empty version to "0"
    assertThat(reverseSort("", "0"), is(0));
    assertThat(reverseSort("", ""), is(0));
  }

  @Test
  public void nonNumericStringsAreParsedNotNulled() {
    // GenericVersionScheme parses any string (never throws InvalidVersionSpecificationException), so equal
    // non-numeric strings compare to 0. A 0 result proves neither null branch (-1 / 1) was taken.
    assertThat(reverseSort("abc", "abc"), is(0));
    assertThat(reverseSort("$$$", "$$$"), is(0));
    assertThat(reverseSort("1..2", "1..2"), is(0));
    assertThat(reverseSort("...", "..."), is(0));
  }

  @Test
  public void sortsComponentsInDescendingVersionOrder() {
    List<String> sortedVersions = Stream.of(
        component("1.0"),
        component("2.0"),
        component("1.10"),
        component("1.2"),
        component("1.0-SNAPSHOT"),
        component("10.0"),
        component("1.0.1"))
        .sorted(this::reverseSort)
        .map(Component::version)
        .toList();

    assertThat(sortedVersions,
        is(Arrays.asList("10.0", "2.0", "1.10", "1.2", "1.0.1", "1.0", "1.0-SNAPSHOT")));
  }

  @Test
  public void minByReverseComparatorSelectsHighestVersionMirroringProductionUsage() {
    // Production selects the newest component via Stream#min(this::reverseSortByVersion) (see
    // MetadataRebuildWorker#rebuildMetadataForBaseVersion). Because the comparator sorts descending,
    // min() must return the HIGHEST version. This exercises the real selection path rather than only
    // list ordering, so a sign flip introduced by the maven-resolver swap is caught here too.
    Optional<Component> newest = Stream.of(
        component("1.0"),
        component("2.0"),
        component("1.10"),
        component("1.0-SNAPSHOT"))
        .min(this::reverseSort);

    assertThat(newest.map(Component::version).orElse(null), is("2.0"));
  }

  private int reverseSort(final String aVersion, final String bVersion) {
    return reverseSort(component(aVersion), component(bVersion));
  }

  private int reverseSort(final Component a, final Component b) {
    return worker.reverseSortByVersion(a, b);
  }

  private Component component(final String version) {
    Component component = mock(Component.class);
    when(component.version()).thenReturn(version);
    return component;
  }
}
