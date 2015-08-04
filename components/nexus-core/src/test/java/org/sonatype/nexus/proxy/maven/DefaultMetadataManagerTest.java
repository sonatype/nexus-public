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
package org.sonatype.nexus.proxy.maven;

import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M2GavCalculator;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultMetadataManagerTest
{

  private MavenRepository repo;

  private MetadataLocator locator;

  private DefaultMetadataUpdater updater;

  @Test
  public void getTimeForMetadataTimestampMaven2Normal() {
    assertThat(DefaultMetadataManager.getTimeFromMetadataTimestampMaven2("??"), nullValue());
    assertThat(DefaultMetadataManager.getTimeFromMetadataTimestampMaven2("20101224.124422"), is(1293194662000L));
  }

  @Test(expected = NullPointerException.class)
  public void getTimeForMetadataTimestampMaven2Arg1Null() {
    assertThat(DefaultMetadataManager.getTimeFromMetadataTimestampMaven2(null), is(1293207262000L));
  }

  @Test
  public void getTimeForMetadataTimestampMaven3UpdatedNormal() {
    assertThat(DefaultMetadataManager.getTimeFromMetadataTimestampMaven3Updated("??"), nullValue());
    assertThat(DefaultMetadataManager.getTimeFromMetadataTimestampMaven3Updated("20101224124422"), is(1293194662000L));
  }

  @Test
  public void getTimeForMetadataTimestampMaven3UpdatedUsingMaven2TimestampFailsParse() {
    assertThat(DefaultMetadataManager.getTimeFromMetadataTimestampMaven3Updated("20101224.124422"), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void getTimeForMetadataTimestampMaven3UpdatedArg1Null() {
    DefaultMetadataManager.getTimeFromMetadataTimestampMaven3Updated(null);
  }

  //================

  @Test(expected = NullPointerException.class)
  public void getBuildNumberForMetadataMaven3ValueArg1Null() {
    DefaultMetadataManager.getBuildNumberForMetadataMaven3Value(null);
  }

  @Test
  public void getBuildNumberForMetadataMaven3ValueNoDashIsZero() {
    assertThat(DefaultMetadataManager.getBuildNumberForMetadataMaven3Value("foo"), is(0));
  }

  @Test
  public void getBuildNumberForMetadataMaven3ValueDashAtEndBogusSanity() {
    assertThat(DefaultMetadataManager.getBuildNumberForMetadataMaven3Value("1.2.1-20110719.092007-"), nullValue());
  }

  @Test
  public void getBuildNumberForMetadataMaven3ValueNormal() {
    assertThat(DefaultMetadataManager.getBuildNumberForMetadataMaven3Value("1.2.1-20110719.092007-17"), is(17));
  }

  @Before
  public void setup() {
    this.locator = mock(MetadataLocator.class);
    this.updater = new DefaultMetadataUpdater(locator);

    this.repo = mock(MavenRepository.class);
    GavCalculator calculator = new M2GavCalculator();
    when(repo.getGavCalculator()).thenReturn(calculator);
  }

  @Test
  public void deployHash()
      throws Exception
  {
    ArtifactStoreRequest request = new ArtifactStoreRequest(
        repo, pathToGav("/nexus4918/artifact/1.2.1-SNAPSHOT/artifact-1.2.1-20110719.134341-19.pom.sha1"), true
    );
    assertFalse(updater.doesImpactMavenMetadata(request.getGav()));
  }

  @Test
  public void deploySignature()
      throws Exception
  {
    ArtifactStoreRequest request = new ArtifactStoreRequest(
        repo, pathToGav("/nexus4918/artifact/1.0/artifact-1.0.pom.asc"), true
    );
    assertFalse(updater.doesImpactMavenMetadata(request.getGav()));
  }

  @Test
  public void deployReleaseWithClassifier()
      throws Exception
  {
    ArtifactStoreRequest request = new ArtifactStoreRequest(
        repo, pathToGav("/nexus4918/artifact/1.0/artifact-1.0-classifier.jar"), true
    );
    assertFalse(updater.doesImpactMavenMetadata(request.getGav()));
  }

  @Test
  public void deployRelease()
      throws Exception
  {
    ArtifactStoreRequest request = new ArtifactStoreRequest(
        repo, pathToGav("/nexus4918/artifact/1.0/artifact-1.0.jar"), true
    );
    assertTrue(updater.doesImpactMavenMetadata(request.getGav()));
  }

  @Test
  public void deploySnapshot()
      throws Exception
  {
    ArtifactStoreRequest request = new ArtifactStoreRequest(
        repo, pathToGav("/nexus4918/artifact/1.1-SNAPSHOT/artifact-1.1-SNAPSHOT.jar"), true
    );
    assertTrue(updater.doesImpactMavenMetadata(request.getGav()));
  }

  @Test
  public void deploySnapshotWithClassifier()
      throws Exception
  {
    ArtifactStoreRequest request = new ArtifactStoreRequest(
        repo, pathToGav("/nexus4918/artifact/1.2.1-SNAPSHOT/artifact-1.2.1-20110719.134341-19-classifier.jar"), true
    );
    assertTrue(updater.doesImpactMavenMetadata(request.getGav()));
  }

  private Gav pathToGav(final String path) {
    return repo.getGavCalculator().pathToGav(path);
  }

}
