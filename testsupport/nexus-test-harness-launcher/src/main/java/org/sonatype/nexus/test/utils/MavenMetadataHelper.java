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
package org.sonatype.nexus.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.codehaus.plexus.util.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MavenMetadataHelper
{

  public static void assertEquals(Metadata m1, Metadata m2) {
    assertThat(m1, is(notNullValue()));
    assertThat(m2, is(notNullValue()));

    assertThat(m1.getArtifactId(), equalTo(m2.getArtifactId()));
    assertThat(m1.getGroupId(), equalTo(m2.getGroupId()));
    assertThat(m1.getVersion(), equalTo(m2.getVersion()));

    assertThat(m1.getVersioning(), is(notNullValue()));
    assertThat(m2.getVersioning(), is(notNullValue()));

    assertThat(m1.getVersioning().getLatest(), equalTo(m2.getVersioning().getLatest()));
    assertThat(m1.getVersioning().getRelease(), equalTo(m2.getVersioning().getRelease()));

    if (m1.getVersioning().getSnapshot() != null || m1.getVersioning().getSnapshot() != null) {
      assertThat(m1.getVersioning().getSnapshot().getBuildNumber(),
          equalTo(m2.getVersioning().getSnapshot().getBuildNumber()));
      assertThat(m1.getVersioning().getSnapshot().getTimestamp(),
          equalTo(m2.getVersioning().getSnapshot().getTimestamp()));
    }

    assertThat(m1.getVersioning().getSnapshotVersions().size(), equalTo(
        m2.getVersioning().getSnapshotVersions().size()));

    for (int i = 0; i < m1.getVersioning().getSnapshotVersions().size(); i++) {
      SnapshotVersion s1 = m1.getVersioning().getSnapshotVersions().get(i);
      SnapshotVersion s2 = get(s1, m2.getVersioning().getSnapshotVersions());

      assertThat(s1, is(notNullValue()));
      assertThat(s2, is(notNullValue()));
      assertThat(s1.getClassifier(), equalTo(s2.getClassifier()));
      assertThat(s1.getExtension(), equalTo(s2.getExtension()));
      assertThat(s1.getUpdated(), equalTo(s2.getUpdated()));
      assertThat(s1.getVersion(), equalTo(s2.getVersion()));
    }
  }

  private static SnapshotVersion get(SnapshotVersion s1, List<SnapshotVersion> snapshotVersions) {
    for (SnapshotVersion s2 : snapshotVersions) {
      if (StringUtils.equals(s1.getClassifier(), s2.getClassifier())
          && StringUtils.equals(s1.getExtension(), s2.getExtension())) {
        return s2;
      }
    }
    return null;
  }

  public static Metadata getMetadata(File metadata)
      throws Exception
  {
    try (FileInputStream in = new FileInputStream(metadata)) {
      return MetadataBuilder.read(in);
    }
  }
}
