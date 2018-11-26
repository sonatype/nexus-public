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
package org.sonatype.nexus.repository.raw.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

public class RawContentFacetImplTest
    extends TestSupport
{
  private static final String ASSET_NAME = "theAsset";

  private static final String GROUP_NAME = "/";

  private static final String EXISTING_ASSET_NAME = "anExistingAsset";

  private static final String NON_EXISTENT_ASSET_NAME = "thisOneDoesNotExist";

  private static final boolean EXISTS = true;

  private static final AttributesMap NO_CONTENT_ATTRIBUTES = null;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private Component component;

  @Mock
  private Asset asset;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private StorageTx tx;

  @Mock
  private ODatabaseDocumentTx db;

  @Mock
  private Bucket bucket;

  @Mock
  private AssetEntityAdapter assetEntityAdapter;

  private RawContentFacetImpl underTest;

  @Before
  public void setup() throws Exception {
    when(repository.getFormat()).thenReturn(format);

    when(component.group(any(String.class))).thenReturn(component);
    when(component.name(any(String.class))).thenReturn(component);

    when(asset.name(any(String.class))).thenReturn(asset);
    when(asset.attributes()).thenReturn(new NestedAttributesMap(P_ATTRIBUTES, newHashMap()));

    when(tx.getDb()).thenReturn(db);
    when(tx.findBucket(repository)).thenReturn(bucket);

    underTest = new RawContentFacetImpl(assetEntityAdapter);
    underTest.attach(repository);

    UnitOfWork.beginBatch(tx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void testPutWhenComponentAndAssetExist() {
    when(tx.findComponentWithProperty(P_NAME, ASSET_NAME, bucket)).thenReturn(component);
    when(tx.firstAsset(component)).thenReturn(asset);

    underTest.put(ASSET_NAME, assetBlob, NO_CONTENT_ATTRIBUTES);

    InOrder inOrder = inOrder(tx, asset);
    inOrder.verify(tx).attachBlob(asset, assetBlob);
    inOrder.verify(tx).saveAsset(asset);
    verify(tx, never()).createComponent(bucket, format);
    verify(tx, never()).saveComponent(component);
    verify(tx, never()).createAsset(bucket, component);
  }

  @Test
  public void testPutWhenComponentAndAssetDoNotExist() {
    when(tx.findComponentWithProperty(P_NAME, ASSET_NAME, bucket)).thenReturn(null);
    when(tx.createComponent(bucket, format)).thenReturn(component);
    when(tx.firstAsset(component)).thenReturn(null);
    when(tx.createAsset(bucket, component)).thenReturn(asset);

    underTest.put(ASSET_NAME, assetBlob, NO_CONTENT_ATTRIBUTES);

    InOrder inOrder = inOrder(tx, component, asset);
    inOrder.verify(tx).createComponent(bucket, format);
    inOrder.verify(component).group(GROUP_NAME);
    inOrder.verify(component).name(ASSET_NAME);
    inOrder.verify(tx).saveComponent(component);
    inOrder.verify(tx).createAsset(bucket, component);
    inOrder.verify(asset).name(ASSET_NAME);
    inOrder.verify(tx).attachBlob(asset, assetBlob);
    inOrder.verify(tx).saveAsset(asset);
  }

  @Test
  public void testPutWhenComponentExistsAndAssetDoesNot() {
    when(tx.findComponentWithProperty(P_NAME, ASSET_NAME, bucket)).thenReturn(component);
    when(tx.firstAsset(component)).thenReturn(null);
    when(tx.createAsset(bucket, component)).thenReturn(asset);

    underTest.put(ASSET_NAME, assetBlob, NO_CONTENT_ATTRIBUTES);

    InOrder inOrder = inOrder(tx, asset);
    inOrder.verify(tx).createAsset(bucket, component);
    inOrder.verify(asset).name(ASSET_NAME);
    inOrder.verify(tx).attachBlob(asset, assetBlob);
    inOrder.verify(tx).saveAsset(asset);
    verify(tx, never()).createComponent(bucket, format);
    verify(tx, never()).saveComponent(component);
  }

  @Test
  public void testAssetExists() {
    when(assetEntityAdapter.exists(db, EXISTING_ASSET_NAME, bucket)).thenReturn(EXISTS);
    when(assetEntityAdapter.exists(db, NON_EXISTENT_ASSET_NAME, bucket)).thenReturn(!EXISTS);

    assertTrue("Asset should exist", underTest.assetExists(EXISTING_ASSET_NAME));
    assertFalse("Asset should not exist", underTest.assetExists(NON_EXISTENT_ASSET_NAME));
  }
}
