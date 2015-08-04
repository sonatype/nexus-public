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
package org.sonatype.nexus.proxy.maven.metadata.mercury;

import java.util.Arrays;

import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.NexusMergeOperation;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MergeOperationTest
    extends TestSupport
{
  @Test
  public void testMergeNoLastUpdate()
      throws Exception
  {
    Metadata md1 = getSource(false);

    Metadata md2 = getTarget(false);

    NexusMergeOperation mergeOp = new NexusMergeOperation(new MetadataOperand(md1));
    mergeOp.perform(md2);

    validate(md2, false, false);
  }

  @Test
  public void testMergeTargetLastUpdate()
      throws Exception
  {
    Metadata md1 = getSource(false);

    Metadata md2 = getTarget(true);

    NexusMergeOperation mergeOp = new NexusMergeOperation(new MetadataOperand(md1));
    mergeOp.perform(md2);

    validate(md2, true, true);
  }

  @Test
  public void testMergeSourceLastUpdate()
      throws Exception
  {
    Metadata md1 = getSource(true);

    Metadata md2 = getTarget(false);

    NexusMergeOperation mergeOp = new NexusMergeOperation(new MetadataOperand(md1));
    mergeOp.perform(md2);

    validate(md2, true, false);
  }

  @Test
  public void testMergeBothLastUpdate()
      throws Exception
  {
    Metadata md1 = getSource(true);

    Metadata md2 = getTarget(true);

    NexusMergeOperation mergeOp = new NexusMergeOperation(new MetadataOperand(md1));
    mergeOp.perform(md2);

    validate(md2, true, true);
  }

  @Test
  public void testMergeReleaseAndSnapshot()
      throws Exception
  {
    Metadata release = getReleaseMetadata();
    Metadata snapshot = getSnapshotMetadata();
    NexusMergeOperation mergeOp = new NexusMergeOperation(new MetadataOperand(release));
    mergeOp.perform(snapshot);

    // check the snapshot metadata, which should now be merged
    assertThat(snapshot.getArtifactId(), equalTo("test"));
    assertThat(snapshot.getGroupId(), equalTo("test"));
    assertThat(snapshot.getPlugins(), empty());
    assertThat(snapshot.getVersion(), nullValue());
    assertThat(snapshot.getVersioning(), notNullValue());
    assertThat(snapshot.getVersioning().getLastUpdated(), equalTo("1234568"));
    assertThat(snapshot.getVersioning().getLatest(), equalTo("1.2-SNAPSHOT"));
    assertThat(snapshot.getVersioning().getRelease(), equalTo("1.1"));
    assertThat(snapshot.getVersioning().getSnapshot(), nullValue());
    assertThat(snapshot.getVersioning().getVersions(), notNullValue());
    assertThat(snapshot.getVersioning().getVersions(), containsInAnyOrder("1.1", "1.1-SNAPSHOT", "1.2-SNAPSHOT"));

    //now do the merge in reverse
    release = getReleaseMetadata();
    snapshot = getSnapshotMetadata();
    mergeOp = new NexusMergeOperation(new MetadataOperand(snapshot));
    mergeOp.perform(release);

    // check the release metadata, which should now be merged
    assertThat(release.getArtifactId(), equalTo("test"));
    assertThat(release.getGroupId(), equalTo("test"));
    assertThat(release.getPlugins(), empty());
    assertThat(release.getVersion(), nullValue());
    assertThat(release.getVersioning(), notNullValue());
    assertThat(release.getVersioning().getLastUpdated(), equalTo("1234568"));
    assertThat(release.getVersioning().getLatest(), equalTo("1.2-SNAPSHOT"));
    assertThat(release.getVersioning().getRelease(), equalTo("1.1"));
    assertThat(release.getVersioning().getSnapshot(), nullValue());
    assertThat(release.getVersioning().getVersions(), notNullValue());
    assertThat(release.getVersioning().getVersions(), containsInAnyOrder("1.1", "1.1-SNAPSHOT", "1.2-SNAPSHOT"));
  }

  private Metadata getReleaseMetadata() {
    Metadata releaseMetadata = new Metadata();
    releaseMetadata.setArtifactId("test");
    releaseMetadata.setGroupId("test");

    Versioning versioning = new Versioning();
    versioning.addVersion("1.1");
    versioning.setLatest("1.1");
    versioning.setRelease("1.1");
    versioning.setLastUpdated("1234567");

    releaseMetadata.setVersioning(versioning);

    return releaseMetadata;
  }

  private Metadata getSnapshotMetadata() {
    Metadata snapshotMetadata = new Metadata();
    snapshotMetadata.setArtifactId("test");
    snapshotMetadata.setGroupId("test");

    Versioning versioning = new Versioning();
    versioning.addVersion("1.1-SNAPSHOT");
    versioning.addVersion("1.2-SNAPSHOT");
    versioning.setLatest("1.2-SNAPSHOT");
    versioning.setRelease("");
    versioning.setLastUpdated("1234568");

    snapshotMetadata.setVersioning(versioning);

    return snapshotMetadata;
  }

  private Metadata getSource(boolean setLastUpdate) {
    Metadata md = new Metadata();
    md.setArtifactId("log4j");
    md.setGroupId("log4j");
    md.setVersion("1.1.3");

    Versioning versioning = new Versioning();
    versioning.setVersions(Arrays.asList("1.1.3"));

    if (setLastUpdate) {
      versioning.setLastUpdated("1234567");
    }

    md.setVersioning(versioning);

    return md;
  }

  private Metadata getTarget(boolean setLastUpdate) {
    Metadata md = new Metadata();
    md.setArtifactId("log4j");
    md.setGroupId("log4j");
    md.setVersion("1.1.3");

    Versioning versioning = new Versioning();
    versioning.setVersions(Arrays.asList("1.1.3", "1.2.4", "1.2.5", "1.2.6", "1.2.7", "1.2.8", "1.2.11", "1.2.9",
        "1.2.12", "1.2.13"));

    if (setLastUpdate) {
      versioning.setLastUpdated("7654321");
    }

    md.setVersioning(versioning);

    return md;
  }

  private void validate(Metadata md, boolean setLastUpdate, boolean targetLastUpdate) {
    assertThat(
        md.getVersioning().getVersions(),
        containsInAnyOrder("1.1.3", "1.2.4", "1.2.5", "1.2.6", "1.2.7", "1.2.8", "1.2.11", "1.2.9", "1.2.12",
            "1.2.13"));

    if (setLastUpdate) {
      if (targetLastUpdate) {
        assertThat(md.getVersioning().getLastUpdated(), equalTo("7654321"));
      }
      else {
        assertThat(md.getVersioning().getLastUpdated(), equalTo("1234567"));
      }
    }
    else {
      // it should contain "now", but not be blank
      assertThat(md.getVersioning().getLastUpdated(), not(equalTo("")));
    }
  }
}
