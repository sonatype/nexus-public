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

import java.io.ByteArrayInputStream;
import java.util.List;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.M2GavCalculator;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UT for {@link ArtifactStoreHelper}
 */
public class ArtifactStoreHelperTest
    extends TestSupport
{
  /**
   * Test that verifies that classifier is not present in the artifact store request used to maintain metadata, even if
   * classified artifact is being deployed with POM.
   *
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-6731">NEXUS-6731</a>
   */
  @Test
  public void classifierNotPassedOnGeneratedPomAndClassifiedArtifactDeploy() throws Exception {
    final M2GavCalculator gavCalculator = new M2GavCalculator();
    final MetadataManager metadataManager = mock(MetadataManager.class);
    final MavenRepository mavenRepository = mock(MavenRepository.class);
    when(mavenRepository.getGavCalculator()).thenReturn(gavCalculator);
    when(mavenRepository.getMetadataManager()).thenReturn(metadataManager);
    when(mavenRepository.retrieveItem(any(Boolean.class), any(ResourceStoreRequest.class)))
        .thenThrow(ItemNotFoundException.class);
    final ArtifactStoreHelper artifactStoreHelper = new ArtifactStoreHelper(mavenRepository);

    final Gav gav = gavCalculator.pathToGav("/org/test/artifact/1.0/artifact-1.0-classifier.jar");
    assertThat(gav.getClassifier(), equalTo("classifier"));

    final ArtifactStoreRequest request = new ArtifactStoreRequest(mavenRepository, gav, false);
    artifactStoreHelper.storeArtifactWithGeneratedPom(
        request, "pom", new ByteArrayInputStream("Content".getBytes(Charsets.UTF_8)), null);

    final ArgumentCaptor<ArtifactStoreRequest> requestArgumentCaptor = ArgumentCaptor
        .forClass(ArtifactStoreRequest.class);
    verify(metadataManager).deployArtifact(requestArgumentCaptor.capture());

    final List<ArtifactStoreRequest> requestsPassed = requestArgumentCaptor.getAllValues();
    assertThat(requestsPassed, hasSize(1));
    assertThat(requestsPassed.get(0).getGav().getClassifier(), nullValue());
  }
}
