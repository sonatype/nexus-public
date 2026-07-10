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
