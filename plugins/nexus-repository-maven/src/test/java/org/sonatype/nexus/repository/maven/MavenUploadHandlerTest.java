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
package org.sonatype.nexus.repository.maven;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.view.Payload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class MavenUploadHandlerTest
    extends TestSupport
{

  private MavenUploadHandler underTest = new MavenUploadHandler(new Maven2MavenPathParser());

  @Mock
  Repository repository;

  @Mock
  MavenFacet mavenFacet;

  @Mock
  Payload jarPayload;

  @Mock
  Payload sourcesPayload;

  @Mock
  StorageTx storageTx;

  @Before
  public void setup() {
    when(repository.getFormat()).thenReturn(new Maven2Format());
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);

    StorageFacet storageFacet = mock(StorageFacet.class);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

  }

  @Test
  public void testGetDefinition() {
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.isMultipleUpload(), is(true));
    // Order is important on fields as it affects the UI
    assertThat(def.getComponentFields(),
        contains(field("groupId", "Group ID", false, STRING), field("artifactId", "Artifact ID", false, STRING),
            field("version", "Version", false, STRING)));
    assertThat(def.getAssetFields(),
        contains(field("classifier", "Classifier", true, STRING), field("extension", "Extension", false, STRING)));
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    assetUpload = new AssetUpload();
    assetUpload.getFields().put("classifier", "sources");
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(sourcesPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    Collection<String> createdPaths = underTest.handle(repository, componentUpload);
    assertThat(createdPaths, contains("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar",
        "org/apache/maven/tomcat/5.0.28/tomcat-5.0.28-sources.jar"));

    ArgumentCaptor<MavenPath> pathCapture = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCapture.capture(), any(Payload.class));

    List<MavenPath> paths = pathCapture.getAllValues();

    assertThat(paths, hasSize(2));

    MavenPath path = paths.get(0);
    assertNotNull(path);
    assertThat(path.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar"));
    assertCoordinates(path.getCoordinates(), "org.apache.maven", "tomcat", "5.0.28", null, "jar");

    path = paths.get(1);
    assertNotNull(path);
    assertThat(path.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28-sources.jar"));
    assertCoordinates(path.getCoordinates(), "org.apache.maven", "tomcat", "5.0.28", "sources", "jar");
  }

  private static void assertCoordinates(final Coordinates actual,
                                        final String groupId,
                                        final String artifactId,
                                        final String version,
                                        final String classifier,
                                        final String extension)
  {
    assertThat(actual.getGroupId(), is(groupId));
    assertThat(actual.getArtifactId(), is(artifactId));
    assertThat(actual.getVersion(), is(version));
    assertThat(actual.getClassifier(), is(classifier));
    assertThat(actual.getExtension(), is(extension));

    assertNull(actual.getBuildNumber());
    assertNull(actual.getTimestamp());
  }

  private UploadFieldDefinition field(final String name,
                                      final String displayName,
                                      final boolean optional,
                                      final Type type)
  {
    return new UploadFieldDefinition(name, displayName, optional, type);
  }
}
