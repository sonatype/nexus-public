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
package org.sonatype.nexus.blobstore.restore.pypi.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.pypi.internal.orient.PyPiRestoreBlobData;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.datastore.PypiContentFacet;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PyPiRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  public static final String PACKAGE_PATH = "/packages/sampleproject/1.2.0/sampleproject-1.2.0.tar.gz";

  public static final String INDEX_PATH = "/simple/peppercorn/";

  PyPiRestoreBlobStrategy underTest;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Blob blob;

  @Mock
  Repository repository;

  @Mock
  PypiContentFacet pypiContentFacet;

  @Mock
  ContentFacet contentFacet;

  @Mock
  BlobStore blobStore;

  @Mock
  BlobStoreConfiguration blobStoreConfiguration;

  @Mock
  PyPiRestoreBlobData pyPiRestoreBlobData;

  @Mock
  private RestoreBlobData restoreBlobData;

  Properties packageProps = new Properties();

  Properties indexProps = new Properties();


  @Before
  public void setup() throws IOException {
    underTest = new PyPiRestoreBlobStrategy(new DryRunPrefix("dryrun"), repositoryManager);

    packageProps.setProperty("@BlobStore.created-by", "admin");
    packageProps.setProperty("size", "5674");
    packageProps.setProperty("@Bucket.repo-name", "pypi-hosted");
    packageProps.setProperty("creationTime", "1533220056556");
    packageProps.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    packageProps.setProperty("@BlobStore.content-type", "application/x-gzip");
    packageProps.setProperty("@BlobStore.blob-name", PACKAGE_PATH);
    packageProps.setProperty("sha1", "c402bbe79807576de00c1fa61fa037a27ee6b62b");

    indexProps.setProperty("@BlobStore.created-by", "anonymous");
    indexProps.setProperty("size", "1330");
    indexProps.setProperty("@Bucket.repo-name", "pypi-proxy");
    indexProps.setProperty("creationTime", "1533220387218");
    indexProps.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    indexProps.setProperty("@BlobStore.content-type", "text/html");
    indexProps.setProperty("@BlobStore.blob-name", INDEX_PATH);
    indexProps.setProperty("sha1", "0088eb478752a810f48f04d3cf9f46d2924e334a");

    when(repositoryManager.get(anyString())).thenReturn(repository);

    when(repository.facet(PypiContentFacet.class)).thenReturn(pypiContentFacet);
    when(repository.optionalFacet(PypiContentFacet.class)).thenReturn(Optional.of(pypiContentFacet));
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    byte[] blobBytes = Resources.toByteArray(Resources.getResource(getClass(),
        "pyglet-1.2.1.zip"));
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));

    when(blobStoreConfiguration.getName()).thenReturn(TEST_BLOB_STORE_NAME);

    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(pyPiRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);

    TempBlob tempBlobMock = mock(TempBlob.class);
    when(tempBlobMock.get()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(pypiContentFacet.getTempBlob(any(InputStream.class), anyString())).thenReturn(tempBlobMock);
  }

  @Test
  public void testPackageRestore() throws Exception {
    FluentComponent component = mock(FluentComponent.class);
    when(component.attributes()).thenReturn(new NestedAttributesMap());
    FluentAsset asset = mockAsset();
    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(pypiContentFacet.findOrCreateComponent(anyString(), anyString(), anyString())).thenReturn(component);
    when(pypiContentFacet.saveAsset(eq(PACKAGE_PATH), eq(component), anyString(), any(TempBlob.class))).thenReturn(asset);
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    underTest.restore(packageProps, blob, blobStore, false);

    verify(contentFacet.assets().path(PACKAGE_PATH)).find();
    verify(pypiContentFacet).findOrCreateComponent(anyString(), anyString(), anyString());
    verify(pypiContentFacet).saveAsset(eq(PACKAGE_PATH), eq(component), anyString(), any(TempBlob.class));
    verify(pypiContentFacet).getTempBlob(any(InputStream.class), anyString());

    verifyNoMoreInteractions(pypiContentFacet);
  }

  private FluentAsset mockAsset() {
    FluentAsset asset = mock(FluentAsset.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAssetBuilder builder = mock(FluentAssetBuilder.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(anyString())).thenReturn(builder);
    when(builder.find()).thenReturn(Optional.empty());
    return asset;
  }

  @Test
  public void testIndexRestore() throws Exception {
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(INDEX_PATH);
    mockAsset();

    underTest.restore(indexProps, blob, blobStore, false);

    verify(contentFacet.assets().path(INDEX_PATH)).find();
    verify(pypiContentFacet).saveAsset(eq(INDEX_PATH), anyString(), any(TempBlob.class));
    verify(pypiContentFacet).getTempBlob(any(InputStream.class), anyString());

    verifyNoMoreInteractions(pypiContentFacet);
  }

  @Test
  public void testRestoreSkipNotFacet() {
    when(repository.optionalFacet(PypiContentFacet.class)).thenReturn(Optional.empty());

    underTest.restore(indexProps, blob, blobStore, false);

    verifyNoMoreInteractions(pypiContentFacet);
  }

  @Test
  public void testRestoreSkipExistingPackage() {
    when(restoreBlobData.getRepository()).thenReturn(repository);
    when(pyPiRestoreBlobData.getBlobData().getBlobName()).thenReturn(PACKAGE_PATH);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAssetBuilder builder = mock(FluentAssetBuilder.class);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.path(anyString())).thenReturn(builder);
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.component()).thenReturn(Optional.of(mock(FluentComponent.class)));
    when(builder.find()).thenReturn(Optional.of(asset));

    underTest.restore(packageProps, blob, blobStore, false);

    verify(contentFacet.assets().path(PACKAGE_PATH)).find();

    verifyNoMoreInteractions(pypiContentFacet);
  }
}
