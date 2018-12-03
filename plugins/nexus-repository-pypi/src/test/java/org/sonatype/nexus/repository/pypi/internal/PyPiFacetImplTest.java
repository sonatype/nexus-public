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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.INDEX_PATH_PREFIX;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

public class PyPiFacetImplTest
    extends TestSupport
{
  static final String PACKAGE_NAME = "sample";

  static final String PACKAGE_VERSION = "1.2.0";

  static final String PACKAGE_ARCHIVE = PACKAGE_NAME + "-" + PACKAGE_VERSION + ".tar.gz";

  static final String PACKAGE_PATH = "packages/" + PACKAGE_NAME + "/" + PACKAGE_VERSION + "/" + PACKAGE_ARCHIVE;

  static final String PACKAGE_SUMMARY = "A sample Python project";

  static final String INDEX_PATH = "simple/peppercorn/";

  PyPiFacetImpl underTest;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  AssetBlob assetBlob;

  @Mock
  Repository repository;

  @Mock
  Format format;

  @Mock
  StorageTx tx;

  @Mock
  Blob blob;

  @Mock
  Bucket bucket;

  @Mock
  Component component;

  @Mock
  Asset asset;

  @Mock
  NestedAttributesMap componentFormatAttributes;

  @Mock
  NestedAttributesMap assetFormatAttributes;

  @Mock
  BlobMetrics blobMetrics;

  DateTime createdDateTime = new DateTime();

  @Before
  public void setup() throws Exception {
    underTest = new PyPiFacetImpl(assetEntityAdapter);
    underTest.attach(repository);

    UnitOfWork.beginBatch(tx);
  }

  @After
  public void teardown() {
    UnitOfWork.end();
  }

  @Test
  public void testPutPackage() throws Exception {
    when(assetBlob.getBlob()).thenReturn(blob);
    when(tx.findBucket(repository)).thenReturn(bucket);
    when(repository.getFormat()).thenReturn(format);
    when(blob.getInputStream()).thenReturn(this.getClass().getResourceAsStream("sample-1.2.0.tar.gz"));
    when(tx.findComponents(any(), eq(ImmutableList.of(repository)))).thenReturn(emptyList());
    when(tx.createComponent(bucket, format)).thenReturn(component);
    when(component.formatAttributes()).thenReturn(componentFormatAttributes);
    when(tx.createAsset(bucket, component)).thenReturn(asset);
    when(asset.formatAttributes()).thenReturn(assetFormatAttributes);
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blobMetrics.getCreationTime()).thenReturn(createdDateTime);

    underTest.put(PACKAGE_PATH, assetBlob);

    verify(tx).createComponent(bucket, format);
    verify(component).name(PACKAGE_NAME);
    verify(component).version(PACKAGE_VERSION);
    verify(componentFormatAttributes).set(P_SUMMARY, PACKAGE_SUMMARY);
    verify(tx).saveComponent(component);
    verify(asset).name(PACKAGE_PATH);
    verify(assetFormatAttributes).set(P_ASSET_KIND, AssetKind.PACKAGE);
    verify(assetFormatAttributes).set("name", PACKAGE_NAME);
    verify(assetFormatAttributes).set("version", PACKAGE_VERSION);
    verify(assetFormatAttributes).set("platform", "UNKNOWN");
    verify(assetFormatAttributes).set(eq("description"), startsWith("A sample Python project\n====="));
    verify(assetFormatAttributes).set("summary", PACKAGE_SUMMARY);
    verify(assetFormatAttributes).set("license", "MIT");
    verify(assetFormatAttributes).set("keywords", "sample setuptools development");
    verify(assetFormatAttributes).set("author", "The Python Packaging Authority");
    verify(assetFormatAttributes).set("author_email", "pypa-dev@googlegroups.com");
    verify(assetFormatAttributes).set("home_page", "https://github.com/pypa/sampleproject");
    verify(assetFormatAttributes).set(eq("classifiers"), startsWith("Development Status :: 3 - Alpha\n"));
    verify(tx).attachBlob(asset, assetBlob);
    verify(asset).blobCreated(createdDateTime);
    verify(tx).saveAsset(asset);

    verifyNoMoreInteractions(componentFormatAttributes, assetFormatAttributes);
  }


  @Test
  public void testPutIndex() throws Exception {
    when(assetBlob.getBlob()).thenReturn(blob);
    when(tx.findBucket(repository)).thenReturn(bucket);
    when(repository.getFormat()).thenReturn(format);
    when(tx.createAsset(bucket, format)).thenReturn(asset);
    when(asset.formatAttributes()).thenReturn(assetFormatAttributes);
    when(blob.getMetrics()).thenReturn(blobMetrics);
    when(blobMetrics.getCreationTime()).thenReturn(createdDateTime);

    underTest.put(INDEX_PATH, assetBlob);

    verify(asset).name(INDEX_PATH);
    verify(assetFormatAttributes).set(P_ASSET_KIND, AssetKind.INDEX);
    verify(tx).attachBlob(asset, assetBlob);
    verify(asset).blobCreated(createdDateTime);
    verify(tx).saveAsset(asset);
  }

  @Test
  public void testPutRootIndex() throws Exception {
    underTest.put(INDEX_PATH_PREFIX, assetBlob);

    verifyZeroInteractions(assetBlob, tx, repository);
  }
}
