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
package org.sonatype.nexus.orient.raw;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.orient.raw.internal.RawContentFacetImpl;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.raw.RawCoordinatesHelper;
import org.sonatype.nexus.repository.raw.RawUploadHandlerTestSupport;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RawUploadHandlerTest
    extends RawUploadHandlerTestSupport
{
  @Mock
  Asset assetPayload;

  @Mock
  RawContentFacet rawFacet;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  private MimeSupport mimeSupport;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Override
  protected UploadHandler newRawUploadHandler(final ContentPermissionChecker contentPermissionChecker,
                                              final VariableResolverAdapter variableResolverAdapter,
                                              final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    return new RawUploadHandler(contentPermissionChecker, variableResolverAdapter, uploadDefinitionExtensions,
        mimeSupport);
  }

  @Before
  public void setup() throws IOException {
    when(repository.facet(RawContentFacet.class)).thenReturn(rawFacet);

    storageFacet = mock(StorageFacet.class);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload component = new ComponentUpload();

    component.getFields().put("directory", "org/apache/maven");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    asset = new AssetUpload();
    asset.getFields().put("filename", "bar.jar");
    asset.setPayload(sourcesPayload);
    component.getAssetUploads().add(asset);

    when(assetPayload.componentId()).thenReturn(new DetachedEntityId("foo"));
    when(attributesMap.get(Asset.class)).thenReturn(assetPayload);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(rawFacet.put(any(), any())).thenReturn(content);
    UploadResponse uploadResponse = underTest.handle(repository, component);
    assertThat(uploadResponse.getAssetPaths(), contains("org/apache/maven/foo.jar", "org/apache/maven/bar.jar"));
    assertThat(uploadResponse.getComponentId().getValue(), is("foo"));

    ArgumentCaptor<String> pathCapture = ArgumentCaptor.forClass(String.class);
    verify(rawFacet, times(2)).put(pathCapture.capture(), any(PartPayload.class));

    List<String> paths = pathCapture.getAllValues();

    assertThat(paths, hasSize(2));

    String path = paths.get(0);
    assertNotNull(path);
    assertThat(path, is("org/apache/maven/foo.jar"));

    path = paths.get(1);
    assertNotNull(path);
    assertThat(path, is("org/apache/maven/bar.jar"));
  }

  @Test
  public void testHandleHardLink() throws IOException {
    Path contentPath = temp.newFile("test.txt").toPath();
    String path = contentPath.toString();
    Asset asset = mock(Asset.class);
    Content content = mock(Content.class);
    TempBlob blob = mock(TempBlob.class);

    when(rawFacet.getOrCreateAsset(repository, path, RawCoordinatesHelper.getGroup(path), path)).thenReturn(asset);
    when(rawFacet.get(path)).thenReturn(content);
    when(mimeSupport.detectMimeType(any(InputStream.class), eq(path))).thenReturn("text/plain");
    when(storageFacet.createTempBlob(contentPath, RawContentFacetImpl.HASH_ALGORITHMS, true))
        .thenReturn(blob);

    Content importResponse =
        underTest.handle(new ImportFileConfiguration(repository, contentPath.toFile(), path, true));

    verify(rawFacet).put(eq(path), any());
    assertThat(importResponse, is(content));
  }

  @Override
  protected void testNormalizePath(
      final String directory,
      final String file,
      final String expectedPath) throws IOException
  {
    reset(rawFacet);
    ComponentUpload component = new ComponentUpload();

    component.getFields().put("directory", directory);

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", file);
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    when(assetPayload.componentId()).thenReturn(new DetachedEntityId("foo"));
    when(attributesMap.get(Asset.class)).thenReturn(assetPayload);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(rawFacet.put(any(), any())).thenReturn(content);
    underTest.handle(repository, component);

    ArgumentCaptor<String> pathCapture = ArgumentCaptor.forClass(String.class);
    verify(rawFacet).put(pathCapture.capture(), any(PartPayload.class));

    String path = pathCapture.getValue();
    assertNotNull(path);
    assertThat(path, is(expectedPath));
  }
}
