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
package org.sonatype.nexus.repository.golang.internal.hosted;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.util.CompressedContentExtractor;
import org.sonatype.nexus.repository.golang.internal.util.GolangDataAccess;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import org.apache.commons.io.Charsets;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.golang.AssetKind.MODULE;
import static org.sonatype.nexus.repository.golang.AssetKind.PACKAGE;

public class GolangHostedFacetImplTest
    extends TestSupport
{
  private static final String SOME_PATH = "some_path";

  private static final String MODULE_INFO = "module/@v/v1.0.0.info";

  private static final String GO_MOD = "go.mod";

  @Mock
  private GolangDataAccess dataAccess;

  @Mock
  private StorageTx tx;

  @Mock
  private Repository repository;

  @Mock
  private Asset asset1, asset2;

  @Mock
  private Payload payload;

  @Mock
  private Bucket bucket;

  @Mock
  private CompressedContentExtractor compressExtractor;

  private GolangHostedFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    UnitOfWork.beginBatch(tx);
    underTest = new GolangHostedFacetImpl(dataAccess, compressExtractor);
    underTest.attach(repository);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void getZipFailsToFindAsset() {
    when(dataAccess.findAsset(any(), any(), any())).thenReturn(null);

    assertThat(underTest.getZip(SOME_PATH), is(nullValue()));

    verify(dataAccess).findAsset(any(), any(), any());
    verifyNoMoreInteractions(dataAccess);
  }

  @Test
  public void getZipFindsAsset() {
    when(dataAccess.findAsset(any(), any(), any())).thenReturn(asset1);
    when(dataAccess.getBlobAsPayload(tx, asset1)).thenReturn(payload);

    assertThat(underTest.getZip(SOME_PATH), is(payload));

    verify(dataAccess).findAsset(any(), any(), any());
    verify(dataAccess).getBlobAsPayload(tx, asset1);
    verifyNoMoreInteractions(dataAccess);
  }

  @Test
  public void getInfo() throws IOException {
    DateTime blobCreated = DateTime.now();
    String expected = String.format("{\"Version\":\"v1.0.0\",\"Time\":\"%s\"}", blobCreated.toString());
    when(dataAccess.findAsset(any(), any(), any())).thenReturn(asset1);
    when(asset1.blobCreated()).thenReturn(blobCreated);

    GolangAttributes goAttributes = new GolangAttributes();
    goAttributes.setModule("module");
    goAttributes.setVersion("v1.0.0");

    Content content = underTest.getInfo(MODULE_INFO, goAttributes);
    String response = CharStreams.toString(new InputStreamReader(content.openInputStream(), Charsets.UTF_8));

    assertThat(response, is(equalTo(expected)));
  }

  @Test
  public void getMod() {
    String path = MODULE_INFO;
    verifyGet(path);
  }

  @Test
  public void getPackage() {
    String path = "module/@v/v1.0.0.zip";
    verifyGet(path);
  }

  @Test
  public void getList() throws IOException {
    String expected = "v1.0.0\nv1.0.1";
    String name = "module/@v/v1.0.0.list";

    NestedAttributesMap attributesMap = mock(NestedAttributesMap.class);
    when(attributesMap.get("asset_kind")).thenReturn(PACKAGE.name());
    when(asset1.name()).thenReturn("modulename/@v/v1.0.0.zip");
    when(asset1.formatAttributes()).thenReturn(attributesMap);
    when(asset2.name()).thenReturn("modulename/@v/v1.0.1.zip");
    when(asset2.formatAttributes()).thenReturn(attributesMap);

    when(dataAccess.findAssetsForModule(tx, repository, name)).thenReturn(
        ImmutableList.of(asset1, asset2)
    );

    Content content = underTest.getList(name);

    String response = CharStreams.toString(new InputStreamReader(content.openInputStream(), Charsets.UTF_8));

    assertThat(response, is(equalTo(expected)));
  }

  @Test
  public void upload() throws IOException {
    String path = "modulename/@v/v1.0.0.zip";
    String gomod_path = "modulename/@v/v1.0.0.mod";
    GolangAttributes goAttributes = new GolangAttributes();
    goAttributes.setModule("modulename");
    goAttributes.setVersion("v1.0.0");
    StorageFacet storageFacet = mock(StorageFacet.class);
    TempBlob tempBlob = mock(TempBlob.class);

    InputStream payloadStream = mock(InputStream.class);
    when(payload.openInputStream()).thenReturn(payloadStream);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.createTempBlob(payloadStream, GolangDataAccess.HASH_ALGORITHMS)).thenReturn(tempBlob);

    when(dataAccess.findAsset(any(), any(), any())).thenReturn(asset1);
    when(dataAccess.getBlobAsPayload(tx, asset1)).thenReturn(payload);


    when(compressExtractor.fileExists(payload, path, GO_MOD)).thenReturn(true);
    when(compressExtractor.extractFile(payloadStream, GO_MOD)).thenReturn(payloadStream);

    underTest.upload(path, goAttributes, payload, PACKAGE);

    verify(dataAccess).maybeCreateAndSaveComponent(repository, goAttributes, path, tempBlob, payload, PACKAGE);
    verify(dataAccess).maybeCreateAndSaveComponent(eq(repository), eq(goAttributes), eq(gomod_path), eq(tempBlob), any(), eq(MODULE));
  }

  private void verifyGet(final String path) {
    BlobRef blobRef = mock(BlobRef.class);
    Blob blob = mock(Blob.class);
    Content content = mock(Content.class);

    when(tx.findBucket(repository)).thenReturn(bucket);
    when(dataAccess.findAsset(tx, bucket, path)).thenReturn(asset1);
    when(asset1.requireBlobRef()).thenReturn(blobRef);
    when(tx.requireBlob(blobRef)).thenReturn(blob);
    when(dataAccess.toContent(asset1, blob)).thenReturn(content);

    Content response = underTest.getMod(path);

    assertThat(response, is(content));
    verify(dataAccess).findAsset(any(), any(), any());
    verify(dataAccess).toContent(any(), any());
    verifyNoMoreInteractions(dataAccess);
  }
}
