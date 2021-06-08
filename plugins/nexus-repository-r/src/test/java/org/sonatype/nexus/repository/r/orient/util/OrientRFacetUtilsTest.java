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
package org.sonatype.nexus.repository.r.orient.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.findComponent;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.saveAsset;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.toContent;

public class OrientRFacetUtilsTest
    extends TestSupport
{

  static final String ASSET_NAME = "asset";

  List<Component> components;

  @Mock
  Asset asset;

  @Mock
  Blob blob;

  @Mock
  NestedAttributesMap nestedAttributesMap;

  @Mock
  StorageTx tx;

  @Mock
  Repository repository;

  @Mock
  Component component;

  @Mock
  Bucket bucket;

  @Mock
  InputStreamSupplier supplier;

  @Mock
  Payload payload;

  @Mock
  AssetBlob assetBlob;

  @Mock
  Content content;

  @Before
  public void setup() throws Exception {
    components = new ArrayList<>();
    components.add(component);

    when(asset.attributes()).thenReturn(nestedAttributesMap);
    when(nestedAttributesMap.child("content")).thenReturn(nestedAttributesMap);
    when(nestedAttributesMap.child("cache")).thenReturn(nestedAttributesMap);
    when(nestedAttributesMap.get("last_modified", Date.class)).thenReturn(new Date());
    when(tx.findComponents(any(Query.class), any(List.class))).thenReturn(components);
    when(tx.setBlob(any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(assetBlob);
    when(assetBlob.getBlob()).thenReturn(blob);
  }

  @Test
  public void content() throws Exception {
    Content content = toContent(asset, blob);
    assertThat(content.getAttributes().get("last_modified"), is(notNullValue()));
  }

  @Test
  public void returnFirstComponent() throws Exception {
    Component component = findComponent(tx, repository, "name", "version", "group");
    assertThat(component, is(notNullValue()));
  }

  @Test
  public void findNoComponents() throws Exception {
    components.clear();
    Component component = findComponent(tx, repository, "name", "version", "group");
    assertThat(component, is(equalTo(component)));
  }

  @Test
  public void findAssetWithProperty() throws Exception {
    findAsset(tx, bucket, ASSET_NAME);
    verify(tx).findAssetWithProperty("name", ASSET_NAME, bucket);
  }

  @Test
  public void findAssetReturnValue() throws Exception {
    when(tx.findAssetWithProperty("name", ASSET_NAME, bucket)).thenReturn(asset);
    assertThat(findAsset(tx, bucket, ASSET_NAME), is(equalTo(this.asset)));
  }

  @Test
  public void returnContentOnSaveAsset() throws Exception {
    assertThat(saveAsset(tx, asset, supplier, payload), is(notNullValue()));
  }

  @Test
  public void markAssetAsDownloadedOnSave() throws Exception {
    saveAsset(tx, asset, supplier, payload);
    verify(asset).markAsDownloaded();
  }

  @Test
  public void passAssetToTxOnSave() throws Exception {
    saveAsset(tx, asset, supplier, payload);
    verify(tx).saveAsset(asset);
  }

  @Test
  public void applyContentToAssetOnSave() throws Exception {
    saveAsset(tx, asset, supplier, payload);
    verify(nestedAttributesMap).set(eq("last_modified"),any());
  }

  @Test
  public void extractContentTypeForContentPayloadOnSave() throws Exception {
    saveAsset(tx, asset, supplier, content);
    verify(content).getContentType();
    verify(content).getAttributes();
  }
}
