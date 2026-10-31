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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

/**
 * UT for {@link AbstractMetadataUpdater}
 *
 * @since 3.0
 */
public class MetadataBuilderTest

{
  private final Maven2MavenPathParser mavenPathParser = new Maven2MavenPathParser();

  private MetadataBuilder testSubject;

  @Before
  public void prepare() {
    this.testSubject = new MetadataBuilder();
  }

  @Test(expected = IllegalStateException.class)
  public void wrongEnterA() {
    testSubject.onEnterArtifactId("foo");
  }

  @Test(expected = IllegalStateException.class)
  public void wrongEnterV() {
    testSubject.onEnterBaseVersion("foo");
  }

  @Test
  public void wrongEnterGV() {
    testSubject.onEnterGroupId("foo"); // good
    try {
      testSubject.onEnterBaseVersion("foo");
      fail("No A entered");
    }
    catch (IllegalStateException e) {
      // good
    }
  }

  @Test
  public void wrongEnterGANoV() {
    testSubject.onEnterGroupId("junit"); // good
    testSubject.onEnterArtifactId("junit"); // good
    try {
      testSubject.addArtifactVersion(mavenPathParser.parsePath("/junit/junit/4.12/junit-4.12.pom"));
      fail("Should fail: no V entered");
    }
    catch (IllegalStateException e) {
      // good
    }
  }

  @Test
  public void contextGAVMismatch() {
    testSubject.onEnterGroupId("foo"); // good
    testSubject.onEnterArtifactId("bar"); // good
    testSubject.onEnterBaseVersion("1.0"); // good
    try {
      testSubject.addArtifactVersion(mavenPathParser.parsePath("/junit/junit/4.12/junit-4.12.pom"));
      fail("Should fail: GAV mismatch of enters and path");
    }
    catch (IllegalStateException e) {
      // good
    }
  }

  @Test
  public void simpleRelease() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");
    testSubject.onEnterBaseVersion("1.0");
    testSubject.addArtifactVersion(mavenPathParser.parsePath("/group/artifact/1.0/artifact-1.0.pom"));
    testSubject.addPlugin("prefix", "artifact", "name");
    final Maven2Metadata vmd = testSubject.onExitBaseVersion();
    assertThat(vmd, nullValue());

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getGroupId(), equalTo("group"));
    assertThat(amd.getArtifactId(), equalTo("artifact"));
    assertThat(amd.getBaseVersions(), notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(1));

    final Maven2Metadata gmd = testSubject.onExitGroupId();
    assertThat(gmd, notNullValue());
    assertThat(gmd.getGroupId(), nullValue());
    assertThat(gmd.getArtifactId(), nullValue());
    assertThat(gmd.getPlugins(), hasSize(1));
  }

  @Test
  public void simpleSnapshot() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");
    testSubject.onEnterBaseVersion("1.0-SNAPSHOT");
    testSubject.addArtifactVersion(
        mavenPathParser.parsePath("/group/artifact/1.0-SNAPSHOT/artifact-1.0-20150430.121212-1.pom"));
    testSubject.addPlugin("prefix", "artifact", "name");
    final Maven2Metadata vmd = testSubject.onExitBaseVersion();
    assertThat(vmd, notNullValue());
    assertThat(vmd.getGroupId(), equalTo("group"));
    assertThat(vmd.getArtifactId(), equalTo("artifact"));
    assertThat(vmd.getVersion(), equalTo("1.0-SNAPSHOT"));
    assertThat(vmd.getSnapshots(), notNullValue());
    assertThat(vmd.getSnapshots().getSnapshotTimestamp(), equalTo(new DateTime("2015-04-30T12:12:12Z").getMillis()));
    assertThat(vmd.getSnapshots().getSnapshotBuildNumber(), equalTo(1));
    assertThat(vmd.getSnapshots().getSnapshots(), hasSize(1));

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getGroupId(), equalTo("group"));
    assertThat(amd.getArtifactId(), equalTo("artifact"));
    assertThat(amd.getBaseVersions(), notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(1));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0-SNAPSHOT"));
    assertThat(amd.getBaseVersions().getRelease(), nullValue());
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0-SNAPSHOT"));

    final Maven2Metadata gmd = testSubject.onExitGroupId();
    assertThat(gmd, notNullValue());
    assertThat(gmd.getGroupId(), nullValue());
    assertThat(gmd.getPlugins(), hasSize(1));
  }

  @Test
  public void wrongSimpleSnapshot() {
    String artifactId = "artifactId";
    String groupId = "groupId";
    String baseVersion = "baseVersion-SNAPSHOT-test-SNAPSHOT";
    String wrongVersion = "artifactId-baseVersion-20240910.132746-1-test-20240910.132746-1.jar";

    testSubject.onEnterGroupId(groupId);
    testSubject.onEnterArtifactId(artifactId);
    testSubject.onEnterBaseVersion(baseVersion);
    testSubject.addArtifactVersion(
        mavenPathParser.parsePath(String.join("/", groupId, artifactId, baseVersion, wrongVersion)));
    testSubject.addPlugin("prefix", "artifact", "name");

    final Maven2Metadata vmd = testSubject.onExitBaseVersion();
    assertThat(vmd, nullValue());

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getGroupId(), equalTo(groupId));
    assertThat(amd.getArtifactId(), equalTo(artifactId));
    assertThat(amd.getBaseVersions(), notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(1));
    assertThat(amd.getBaseVersions().getLatest(), equalTo(baseVersion));
    assertThat(amd.getBaseVersions().getRelease(), nullValue());
    assertThat(amd.getBaseVersions().getVersions(), contains(baseVersion));

    final Maven2Metadata gmd = testSubject.onExitGroupId();
    assertThat(gmd, notNullValue());
    assertThat(gmd.getGroupId(), nullValue());
    assertThat(gmd.getPlugins(), hasSize(1));
  }

  @Test
  public void nonUniqueSnapshot() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");
    testSubject.onEnterBaseVersion("1.0-SNAPSHOT");
    testSubject.addArtifactVersion(
        mavenPathParser.parsePath("/group/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.pom"));
    testSubject.addPlugin("prefix", "artifact", "name");
    final Maven2Metadata vmd = testSubject.onExitBaseVersion();
    assertThat(vmd, notNullValue());
    assertThat(vmd.getGroupId(), equalTo("group"));
    assertThat(vmd.getArtifactId(), equalTo("artifact"));
    assertThat(vmd.getVersion(), equalTo("1.0-SNAPSHOT"));
    assertThat(vmd.getSnapshots(), notNullValue());
    assertThat(vmd.getSnapshots().getSnapshotTimestamp(), nullValue());
    assertThat(vmd.getSnapshots().getSnapshotBuildNumber(), equalTo(1));
    assertThat(vmd.getSnapshots().getSnapshots(), hasSize(0));

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getGroupId(), equalTo("group"));
    assertThat(amd.getArtifactId(), equalTo("artifact"));
    assertThat(amd.getBaseVersions(), notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(1));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0-SNAPSHOT"));
    assertThat(amd.getBaseVersions().getRelease(), nullValue());
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0-SNAPSHOT"));

    final Maven2Metadata gmd = testSubject.onExitGroupId();
    assertThat(gmd, notNullValue());
    assertThat(gmd.getGroupId(), nullValue());
    assertThat(gmd.getPlugins(), hasSize(1));
  }

  @Test
  public void multipleReleasesOutOfOrderSelectsHighestAsLatestAndRelease() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // versions are supplied out of natural order; Version.compareTo must drive selection
    addArtifactPom("1.0");
    addArtifactPom("3.0");
    addArtifactPom("2.0");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(3));
    // TreeSet<Version> exposes the versions in ascending order
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0", "2.0", "3.0"));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("3.0"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("3.0"));
  }

  @Test
  public void semanticOrderingPrefersNumericallyHigherVersion() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // lexical ordering would place "1.10" before "1.9"; numeric ordering must win
    addArtifactPom("1.9");
    addArtifactPom("1.10");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(2));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.9", "1.10"));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.10"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.10"));
  }

  @Test
  public void qualifierOrderingPlacesPreReleaseBelowReleases() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // GenericVersionScheme orders the "alpha" qualifier below the bare release
    addArtifactPom("1.0-alpha");
    addArtifactPom("1.0");
    addArtifactPom("2.0");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(3));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0-alpha", "1.0", "2.0"));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("2.0"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("2.0"));
  }

  @Test
  public void latestMayBeSnapshotWhileReleaseFallsBackToHighestNonSnapshot() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // a release plus a higher snapshot: latest is the snapshot, release skips it
    addArtifactPom("1.0");
    addArtifactPom("2.0-SNAPSHOT");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(2));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0", "2.0-SNAPSHOT"));
    // latest is the highest version even though it is a snapshot
    assertThat(amd.getBaseVersions().getLatest(), equalTo("2.0-SNAPSHOT"));
    // release walks down past snapshots to the highest non-snapshot version
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.0"));
  }

  @Test
  public void multipleSnapshotsHaveNoReleaseAndLatestIsHighestSnapshot() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    addArtifactPom("1.0-SNAPSHOT");
    addArtifactPom("2.0-SNAPSHOT");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(2));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0-SNAPSHOT", "2.0-SNAPSHOT"));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("2.0-SNAPSHOT"));
    // every version is a snapshot, so there is no release
    assertThat(amd.getBaseVersions().getRelease(), nullValue());
  }

  @Test
  public void lenientVersionSchemeRetainsNonStandardVersions() {
    // NEXUS-53142: GenericVersionScheme.parseVersion(...) returns a GenericVersion for ANY
    // input and never throws InvalidVersionSpecificationException, so
    // MetadataBuilder.parseVersion never returns null and the "could not parse, omit it"
    // branch is unreachable with this scheme. Lock the actual lenient behavior instead:
    // non-standard version strings are still parsed, retained, and ordered as strings via
    // Version.compareTo without any exception being thrown.
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    addArtifactPom("foo");
    addArtifactPom("zar");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(2));
    // both non-standard versions are retained, ordered case-insensitively: foo < zar
    assertThat(amd.getBaseVersions().getVersions(), contains("foo", "zar"));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("zar"));
    // neither ends with SNAPSHOT, so release equals latest
    assertThat(amd.getBaseVersions().getRelease(), equalTo("zar"));
  }

  /**
   * NEXUS-53161: Equivalent RELEASE-bucket versions (1.0.0 vs 1.0.0-release vs 1.0.0-ga vs 1.0.0-final)
   * must be preserved in &lt;versions&gt; and the canonical/bare form must be selected as latest/release.
   */
  @Test
  public void equivalentReleaseVersionsArePreservedAndBareFormWins() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // Add ComparableVersion-equivalent versions in various orders
    addArtifactPom("1.0.0-release");
    addArtifactPom("1.0.0");
    addArtifactPom("1.0.0-ga");
    addArtifactPom("1.0.0-final");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    // All four distinct version strings must be preserved in <versions>
    assertThat(amd.getBaseVersions().getVersions(), hasSize(4));
    // Versions are sorted with bare form last (highest in ascending order)
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0.0-final", "1.0.0-ga", "1.0.0-release", "1.0.0"));
    // Bare/canonical form must be selected as latest AND release
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0.0"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.0.0"));
  }

  /**
   * NEXUS-53161: When only labelled equivalent versions exist (no bare form),
   * they must still be preserved and deterministically ordered.
   */
  @Test
  public void equivalentLabelledVersionsArePreservedAndOrderedDeterministically() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // Add only labelled forms (no bare/canonical form)
    addArtifactPom("1.0.0-final");
    addArtifactPom("1.0.0-ga");
    addArtifactPom("1.0.0-release");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    // All three distinct version strings must be preserved in lexicographic order
    assertThat(amd.getBaseVersions().getVersions(), hasSize(3));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0.0-final", "1.0.0-ga", "1.0.0-release"));
    // Latest/release should be the lexicographically highest (release)
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0.0-release"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.0.0-release"));
  }

  /**
   * NEXUS-53161: Release aliases with dot separator (e.g., "1.0.0.ga") should be treated
   * the same as hyphen separator.
   */
  @Test
  public void releaseAliasWithDotSeparatorIsPreserved() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // Add versions with dot separator (valid Maven notation)
    addArtifactPom("1.0.0");
    addArtifactPom("1.0.0.ga");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(2));
    // Bare form should win over dot-separated alias
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0.0"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.0.0"));
  }

  /**
   * NEXUS-53161: Snapshots and pre-release qualifiers should NOT be treated as release aliases.
   */
  @Test
  public void snapshotsAndPreReleasesAreNotTreatedAsReleaseAliases() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // These are NOT release aliases - they should sort by Maven's natural ordering
    addArtifactPom("1.0.0-alpha");
    addArtifactPom("1.0.0-beta");
    addArtifactPom("1.0.0-milestone");
    addArtifactPom("1.0.0-rc");
    addArtifactPom("1.0.0-SNAPSHOT");
    addArtifactPom("1.0.0");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(6));
    // Latest should be the bare release (highest in Maven ordering)
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0.0"));
    // Release should skip the SNAPSHOT and return the bare release
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.0.0"));
  }

  /**
   * NEXUS-53161: Complex scenario with snapshots, aliases, and regular versions.
   */
  @Test
  public void mixedVersionsWithSnapshotsAndAliases() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // Mix of different version types
    addArtifactPom("1.0.0-alpha");
    addArtifactPom("1.0.0-ga");
    addArtifactPom("1.0.0");
    addArtifactPom("1.0.0-SNAPSHOT");
    addArtifactPom("2.0.0-release");
    addArtifactPom("2.0.0");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(6));
    // Maven's ComparableVersion ordering: alpha < SNAPSHOT < release
    // Within release bucket: aliases < bare (tiebreaker)
    assertThat(amd.getBaseVersions().getVersions(),
        contains("1.0.0-alpha", "1.0.0-SNAPSHOT", "1.0.0-ga", "1.0.0", "2.0.0-release", "2.0.0"));
    // Latest is 2.0.0 (bare wins over release alias)
    assertThat(amd.getBaseVersions().getLatest(), equalTo("2.0.0"));
    // Release skips SNAPSHOT
    assertThat(amd.getBaseVersions().getRelease(), equalTo("2.0.0"));
  }

  /**
   * NEXUS-53161: Duplicate versions should not be added multiple times.
   */
  @Test
  public void duplicateVersionsAreDeduplicated() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // Add same version multiple times
    addArtifactPom("1.0.0");
    addArtifactPom("1.0.0");
    addArtifactPom("1.0.0-release");
    addArtifactPom("1.0.0-release");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    // Only 2 distinct versions should remain
    assertThat(amd.getBaseVersions().getVersions(), hasSize(2));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0.0-release", "1.0.0"));
  }

  /**
   * NEXUS-53161: Multiple bare versions should be sorted lexicographically.
   */
  @Test
  public void multipleBareVersionsAreSortedLexicographically() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // Multiple distinct bare versions (all in release bucket)
    addArtifactPom("1.0.0");
    addArtifactPom("2.0.0");
    addArtifactPom("1.5.0");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(3));
    assertThat(amd.getBaseVersions().getVersions(), contains("1.0.0", "1.5.0", "2.0.0"));
    assertThat(amd.getBaseVersions().getLatest(), equalTo("2.0.0"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("2.0.0"));
  }

  /**
   * NEXUS-53161: All release alias forms (ga, release, final) with both separators.
   */
  @Test
  public void allReleaseAliasFormsWithBothSeparators() {
    testSubject.onEnterGroupId("group");
    testSubject.onEnterArtifactId("artifact");

    // All variations of release aliases
    addArtifactPom("1.0.0-ga");
    addArtifactPom("1.0.0.ga");
    addArtifactPom("1.0.0-release");
    addArtifactPom("1.0.0.release");
    addArtifactPom("1.0.0-final");
    addArtifactPom("1.0.0.final");
    addArtifactPom("1.0.0");

    final Maven2Metadata amd = testSubject.onExitArtifactId();
    assertThat(amd, notNullValue());
    assertThat(amd.getBaseVersions().getVersions(), hasSize(7));
    // Bare form should win as latest/release
    assertThat(amd.getBaseVersions().getLatest(), equalTo("1.0.0"));
    assertThat(amd.getBaseVersions().getRelease(), equalTo("1.0.0"));
  }

  /**
   * Adds the main POM artifact for the given base version under {@code group:artifact}, mirroring
   * the single-version flow used by the other tests but for multi-version scenarios.
   */
  private void addArtifactPom(final String version) {
    testSubject.onEnterBaseVersion(version);
    testSubject.addArtifactVersion(
        mavenPathParser.parsePath(String.format("/group/artifact/%s/artifact-%s.pom", version, version)));
    testSubject.onExitBaseVersion();
  }
}
