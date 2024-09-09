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
package org.sonatype.nexus.repository.content.store;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.DetachedEntityId;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StoreDataTest
    extends TestSupport
{
  private AssetBlobData assetBlob;

  private AssetData asset;

  private ComponentData component;

  private ContentRepositoryData contentRepository;

  @Before
  public void setUp() throws Exception {
    BlobRef blobRef = new BlobRef("some-node", "some-store", "some-blob");
    assetBlob = new AssetBlobData();
    assetBlob.setAssetBlobId(1);
    assetBlob.setBlobRef(blobRef);
    assetBlob.setBlobSize(1L);
    assetBlob.setContentType("some-contentType");
    assetBlob.setChecksums(Collections.singletonMap("some-algo", "some-checksum"));
    assetBlob.setBlobCreated(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    assetBlob.setCreatedBy("some-user");
    assetBlob.setCreatedByIp("some-ip-address");
    asset = new AssetData();
    asset.setAssetId(1);
    asset.setPath("/some-path");
    asset.setKind("some-kind");
    asset.setAssetBlob(assetBlob);
    asset.setLastDownloaded(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    component = new ComponentData();
    component.setComponentId(1);
    component.setNamespace("some-namespace");
    component.setName("some-name");
    component.setKind("some-kind");
    component.setVersion("some-version");
    component.setNormalizedVersion("some-normalized-version");
    asset.setComponent(component);
    contentRepository = new ContentRepositoryData();
    contentRepository.setConfigRepositoryId(new DetachedEntityId("some-id"));
  }

  @Test
  public void shouldHaveMeaningfulToString() {
    assertEquals("AssetBlobData{assetBlobId=1, blobRef=some-store@some-blob, blobSize=1, contentType='some-contentType', checksums={some-algo=some-checksum}, blobCreated=1970-01-01T00:00Z, createdBy='some-user', createdByIp='some-ip-address'}", assetBlob.toString());
    assertEquals("AssetData{assetId=1, path='/some-path', kind='some-kind', componentId=1, component=ComponentData{componentId=1, namespace='some-namespace', name='some-name', kind='some-kind', version='some-version', normalizedVersion='some-normalized-version', entityVersion='null'} AbstractRepositoryContent{repositoryId=null, attributes=NestedAttributesMap{parent=null, key='attributes', backing={}}, created=null, lastUpdated=null}, assetBlobId=1, assetBlob=AssetBlobData{assetBlobId=1, blobRef=some-store@some-blob, blobSize=1, contentType='some-contentType', checksums={some-algo=some-checksum}, blobCreated=1970-01-01T00:00Z, createdBy='some-user', createdByIp='some-ip-address'}, lastDownloaded=1970-01-01T00:00Z, assetSize=0} AbstractRepositoryContent{repositoryId=null, attributes=NestedAttributesMap{parent=null, key='attributes', backing={}}, created=null, lastUpdated=null}", asset.toString());
    assertEquals("ComponentData{componentId=1, namespace='some-namespace', name='some-name', kind='some-kind', version='some-version', normalizedVersion='some-normalized-version', entityVersion='null'} AbstractRepositoryContent{repositoryId=null, attributes=NestedAttributesMap{parent=null, key='attributes', backing={}}, created=null, lastUpdated=null}", component.toString());
    assertEquals("ContentRepositoryData{configRepositoryId=DetachedEntityId{value='some-id'}} AbstractRepositoryContent{repositoryId=null, attributes=NestedAttributesMap{parent=null, key='attributes', backing={}}, created=null, lastUpdated=null}", contentRepository.toString());
  }
}
