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
package org.sonatype.nexus.blobstore.restore.internal;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobAttributes;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RestoreMetadataServiceImplTest
    extends TestSupport
{
  RestoreMetadataServiceImpl underTest;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  RestoreBlobStrategy restoreBlobStrategy;

  @Mock
  Repository repository;

  @Mock
  FileBlobStore fileBlobStore;

  @Mock
  Blob blob;

  @Mock
  Format mavenFormat;

  @Before
  public void setup() {
    underTest = new RestoreMetadataServiceImpl(blobStoreManager, repositoryManager,
        ImmutableMap.of("maven2", restoreBlobStrategy));

    when(repositoryManager.get("maven-central")).thenReturn(repository);
    when(repository.getFormat()).thenReturn(mavenFormat);
    when(mavenFormat.getValue()).thenReturn("maven2");
  }

  @Test
  public void testRestoreMetadata() throws Exception {
    URL resource = Resources
        .getResource("test-restore/content/vol-1/chp-1/86e20baa-0bca-4915-a7dc-9a4f34e72321.properties");
    FileBlobAttributes blobAttributes = new FileBlobAttributes(Paths.get(resource.toURI()));
    blobAttributes.load();
    BlobId blobId = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72321");
    when(fileBlobStore.getBlobIdStream()).thenReturn(Stream.of(blobId));
    when(blobStoreManager.get("test")).thenReturn(fileBlobStore);

    ArgumentCaptor<Properties> propertiesArgumentCaptor = ArgumentCaptor.forClass(Properties.class);
    when(fileBlobStore.get(blobId)).thenReturn(blob);
    when(fileBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    underTest.restore("test");

    verify(restoreBlobStrategy).restore(propertiesArgumentCaptor.capture(), eq(blob), eq("test"));
    Properties properties = propertiesArgumentCaptor.getValue();

    assertThat(properties.getProperty("@BlobStore.blob-name"), is("org/codehaus/plexus/plexus/3.1/plexus-3.1.pom"));
  }
}
