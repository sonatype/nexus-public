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
package org.sonatype.nexus.rest.indexng;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sonatype.nexus.rest.model.NexusNGArtifact;

import org.apache.maven.index.artifact.VersionUtils;

class GAHolder
{
  private final SortedMap<StringVersion, NexusNGArtifact> versionHits = new TreeMap<StringVersion, NexusNGArtifact>(
      Collections.reverseOrder());

  private NexusNGArtifact latestSnapshot = null;

  private StringVersion latestSnapshotVersion = null;

  private NexusNGArtifact latestRelease = null;

  private StringVersion latestReleaseVersion = null;

  public NexusNGArtifact getVersionHit(StringVersion version) {
    return versionHits.get(version);
  }

  public void putVersionHit(StringVersion version, NexusNGArtifact versionHit) {
    versionHits.put(version, versionHit);

    if (VersionUtils.isSnapshot(versionHit.getVersion())) {
      if (latestSnapshotVersion == null || latestSnapshotVersion.compareTo(version) < 0) {
        latestSnapshot = versionHit;
        latestSnapshotVersion = version;
      }
    }
    else {
      if (latestReleaseVersion == null || latestReleaseVersion.compareTo(version) < 0) {
        latestRelease = versionHit;
        latestReleaseVersion = version;
      }
    }
  }

  public NexusNGArtifact getLatestRelease() {
    return latestRelease;
  }

  public NexusNGArtifact getLatestSnapshot() {
    return latestSnapshot;
  }

  public Collection<NexusNGArtifact> getOrderedVersionHits() {
    return versionHits.values();
  }

  public NexusNGArtifact getLatestVersionHit() {
    return latestRelease != null ? latestRelease : latestSnapshot;
  }
}
