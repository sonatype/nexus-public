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
package org.sonatype.nexus.repository.content.rest;

import java.time.OffsetDateTime;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.rest.api.AssetXO;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class AssetXOBuilderTest
    extends TestSupport
{
  @Mock
  Repository repository;

  @Mock
  EntityMetadata assetOneEntityMetadata;

  @Mock
  EntityId assetOneEntityId;

  private static final String ASSET_PATH = "/nameOne";

  private static final int AN_ASSET_ID = 1;


  @Before
  public void setup() {
    when(assetOneEntityMetadata.getId()).thenReturn(assetOneEntityId);
    when(assetOneEntityId.getValue()).thenReturn("assetOne");

    when(repository.getName()).thenReturn("maven-releases");
    when(repository.getUrl()).thenReturn("http://localhost:8081/repository/maven-releases");
    when(repository.getFormat()).thenReturn(new Format("maven2") { });
  }

  @Test
  public void blobCreatedExists() {
    AssetXO assetXO = AssetXOBuilder.fromAsset(anAsset(), repository, null);

    assertThat(assetXO.getId(), notNullValue());
    assertThat(assetXO.getPath(), is("/nameOne"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/repository/maven-releases/nameOne"));
    assertNotNull(assetXO.getBlobCreated());
  }

  private Asset anAsset() {
    AssetData asset = new AssetData();
    asset.setAssetId(AN_ASSET_ID);
    asset.setPath(ASSET_PATH);
    asset.setCreated(OffsetDateTime.now());
    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setAssetBlobId(1);
    asset.setAssetBlob(assetBlob);
    return asset;
  }
}
