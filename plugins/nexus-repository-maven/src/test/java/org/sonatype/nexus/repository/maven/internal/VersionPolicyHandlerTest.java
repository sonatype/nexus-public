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
package org.sonatype.nexus.repository.maven.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;

/**
 * Tests {@link VersionPolicyHandler}
 */
public class VersionPolicyHandlerTest
    extends TestSupport
{

  @Mock
  private Context context;

  @Mock
  private Repository repository;

  @Mock
  private MavenFacet mavenFacet;

  @Mock
  private Response proceeded;

  private MavenPathParser mavenPathParser = new Maven2MavenPathParser();

  private VersionPolicyHandler underTest = new VersionPolicyHandler();

  private void testScenario(VersionPolicy policy, String path, boolean shouldProceed) throws Exception {
    when(context.getRepository()).thenReturn(repository);
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);
    when(mavenFacet.getVersionPolicy()).thenReturn(policy);
    AttributesMap attributes = new AttributesMap();
    attributes.set(MavenPath.class, mavenPathParser.parsePath(path));
    when(context.getAttributes()).thenReturn(attributes);
    if (shouldProceed) {
      when(context.proceed()).thenReturn(proceeded);
    }

    Response response = underTest.handle(context);
    if (shouldProceed) {
      assertThat(response, is(proceeded));
    }
    else {
      assertThat(response, not(proceeded));
      assertThat(response.getStatus().getCode(), is(BAD_REQUEST));
      assertThat(response.getStatus().isSuccessful(), is(false));
    }
  }

  @Test
  public void handleReleasedJarSnapshotRepoTest() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0/foo-1.0.0.jar", false);
  }

  @Test
  public void handleReleasedJarReleaseRepoTest() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0/foo-1.0.0.jar", true);
  }

  @Test
  public void handleReleasedJarMixedRepoTest() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0/foo-1.0.0.jar", true);
  }

  @Test
  public void handleSnapshotJarSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar", true);
  }

  @Test
  public void handleSnapshotJarReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar", false);
  }

  @Test
  public void handleSnapshotJarMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar", true);
  }

  @Test
  public void handleComponentMetadataSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/maven-metadata.xml", true);
  }

  @Test
  public void handleComponentMetadataReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/maven-metadata.xml", true);
  }

  @Test
  public void handleComponentMetadataMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/maven-metadata.xml", true);
  }

  @Test
  public void handleReleasedShaHashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0/foo-1.0.0.jar.sha1", false);
  }

  @Test
  public void handleReleasedShaHashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0/foo-1.0.0.jar.sha1", true);
  }

  @Test
  public void handleReleasedShaHashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0/foo-1.0.0.jar.sha1", true);
  }

  @Test
  public void handleSnapshotShaHashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar.sha1", true);
  }

  @Test
  public void handleSnapshotShaHashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar.sha1", false);
  }

  @Test
  public void handleSnapshotShaHashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar.sha1", true);
  }

  @Test
  public void handleReleasedMd5HashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0/foo-1.0.0.jar.md5", false);
  }

  @Test
  public void handleReleasedMd5HashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0/foo-1.0.0.jar.md5", true);
  }

  @Test
  public void handleReleasedMd5HashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0/foo-1.0.0.jar.md5", true);
  }

  @Test
  public void handleSnapshotMd5HashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar.md5", true);
  }

  @Test
  public void handleSnapshotMd5HashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar.md5", false);
  }

  @Test
  public void handleSnapshotMd5HashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0-SNAPSHOT/foo-1.0.0-20161204.003314-8.jar.md5", true);
  }

  @Test
  public void handleComponentMetadataShaHashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/maven-metadata.xml.sha1", true);
  }

  @Test
  public void handleComponentMetadataShaHashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/maven-metadata.xml.sha1", true);
  }

  @Test
  public void handleComponentMetadataMd5HashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/maven-metadata.xml.md5", true);
  }

  @Test
  public void handleComponentMetadataMd5HashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/maven-metadata.xml.md5", true);
  }

  @Test
  public void handleComponentMetadataMd5HashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/maven-metadata.xml.md5", true);
  }

  @Test
  public void handleSnapshotMetadataMd5HashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0-SNAPSHOT/maven-metadata.xml.md5", true);
  }

  @Test
  public void handleSnapshotMetadataMd5HashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0-SNAPSHOT/maven-metadata.xml.md5", false);
  }

  @Test
  public void handleSnapshotMetadataMd5HashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0-SNAPSHOT/maven-metadata.xml.md5", true);
  }

  @Test
  public void handleSnapshotMetadataShaHashSnapshotRepo() throws Exception {
    testScenario(VersionPolicy.SNAPSHOT, "org/sonatype/foo/1.0.0-SNAPSHOT/maven-metadata.xml.sha1", true);
  }

  @Test
  public void handleSnapshotMetadataShaHashReleaseRepo() throws Exception {
    testScenario(VersionPolicy.RELEASE, "org/sonatype/foo/1.0.0-SNAPSHOT/maven-metadata.xml.sha1", false);
  }

  @Test
  public void handleSnapshotMetadataShaHashMixedRepo() throws Exception {
    testScenario(VersionPolicy.MIXED, "org/sonatype/foo/1.0.0-SNAPSHOT/maven-metadata.xml.sha1", true);
  }

}
