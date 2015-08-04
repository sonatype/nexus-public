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
package org.sonatype.nexus.plugins.p2.repository.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.plugins.p2.repository.P2MetadataGeneratorConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.p2.bridge.ArtifactRepository;
import org.sonatype.p2.bridge.MetadataRepository;
import org.sonatype.p2.bridge.Publisher;
import org.sonatype.p2.bridge.model.InstallableUnit;
import org.sonatype.p2.bridge.model.InstallableUnitArtifact;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultP2MetadataGeneratorTest
{

  RepositoryRegistry repositories = mock(RepositoryRegistry.class);

  MimeSupport mimeSupport = mock(MimeSupport.class);

  ArtifactRepository artifactRepository = mock(ArtifactRepository.class);

  MetadataRepository metadataRepository = mock(MetadataRepository.class);

  Publisher publisher = mock(Publisher.class);

  @Mock
  Logger logger;

  @InjectMocks
  DefaultP2MetadataGenerator generator = new DefaultP2MetadataGenerator(repositories, mimeSupport, artifactRepository,
      metadataRepository, publisher);

  private File tempFile;

  private File tempP2Repository;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    tempFile = File.createTempFile("feature", ".jar");
    ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tempFile));
    zip.putNextEntry(new ZipEntry("feature.xml"));
    FileInputStream fis = new FileInputStream(new File("src/test/resources", "metadata/feature/feature.xml"));
    byte[] b = new byte[10000];
    int read = fis.read(b);
    zip.write(b, 0, read);
    zip.close();
    fis.close();
    tempP2Repository = NexusUtils.createTemporaryP2Repository();

    Answer<Object> ans = new Answer<Object>()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        URI uri = (URI) invocation.getArguments()[0];
        File f = new File(uri);
        FileOutputStream fos = new FileOutputStream(new File(f, "artifacts.xml"));
        fos.close();
        fos = new FileOutputStream(new File(f, "content.xml"));
        fos.close();
        return null;
      }
    };
    doAnswer(ans).when(artifactRepository)
        .write(any(URI.class), any(Collection.class), any(String.class), any(Map.class), any(String[][].class));
    doAnswer(ans).when(metadataRepository)
        .write(any(URI.class), any(Collection.class), any(String.class), any(Map.class));
    when(mimeSupport.guessMimeTypeFromPath(any(String.class))).thenReturn("text/xml");
  }

  @After
  public void tearDown() throws IOException {
    tempFile.delete();
    FileUtils.deleteDirectory(tempP2Repository);
  }

  @Test
  public void Test_NEXUS_5995() throws IOException, NoSuchRepositoryException {
    P2MetadataGeneratorConfiguration config = mock(P2MetadataGeneratorConfiguration.class);
    when(config.repositoryId()).thenReturn("mockId");
    Repository repo = mock(Repository.class);
    DefaultFSLocalRepositoryStorage local = mock(DefaultFSLocalRepositoryStorage.class);
    when(local.getFileFromBase(any(Repository.class), any(ResourceStoreRequest.class))).thenReturn(tempFile);
    when(repo.getLocalStorage()).thenReturn(local);
    when(repo.getId()).thenReturn("mockId");
    when(repositories.getRepository("mockId")).thenReturn(repo);

    StorageItem item = mock(StorageItem.class);
    when(item.getPath()).thenReturn(new File(tempP2Repository, "feature.jar").getAbsolutePath());
    when(item.getRepositoryId()).thenReturn("mockId");

    InstallableUnit jarUnit = mock(InstallableUnit.class);
    when(jarUnit.getId()).thenReturn("feature.feature.jar");
    InstallableUnit groupUnit = mock(InstallableUnit.class);
    when(groupUnit.getId()).thenReturn("feature.feature.group");
    when(publisher.generateFeatureIUs(any(Boolean.class), any(Boolean.class), any(File[].class)))
        .thenReturn(Arrays.asList(jarUnit, groupUnit));

    generator.addConfiguration(config);
    generator.generateP2Metadata(item);

    verify(jarUnit).addArtifact(any(InstallableUnitArtifact.class));
    // The artifact should not be added to the feature group repo
    verify(groupUnit, never()).addArtifact(any(InstallableUnitArtifact.class));
  }

}
