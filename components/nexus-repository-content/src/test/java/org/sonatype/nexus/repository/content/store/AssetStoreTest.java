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
import java.util.Collection;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class AssetStoreTest
    extends ExampleContentTestSupport
{
  @Mock
  private ContentFacetFinder contentFacetFinder;

  @Mock
  private EventManager eventManager;

  private AssetStore<TestAssetDAO> underTest = null;

  private AssetBlobStore<TestAssetBlobDAO> assetBlobStore = null;

  private int repositoryId;

  @Before
  public void setup() {
    ContentRepositoryData contentRepository = randomContentRepository();
    createContentRepository(contentRepository);
    repositoryId = contentRepository.repositoryId;

    generateRandomNamespaces(4);
    generateRandomNames(4);
    generateRandomVersions(4);

    underTest = new AssetStore<>(sessionRule, "test", TestAssetDAO.class);
    underTest.setDependencies(contentFacetFinder, eventManager);

    assetBlobStore = new AssetBlobStore<>(sessionRule, "test", TestAssetBlobDAO.class);
  }

  private void createContentRepository(final ContentRepositoryData contentRepository) {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }
  }

  @Test
  public void testBrowseUpdatedAssetsDifferentDates() {
    OffsetDateTime time = OffsetDateTime.now();
    AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
    AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
    AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");
    AssetData asset4 = generateAsset(repositoryId, "/asset4/asset4.jar");
    AssetData asset5 = generateAsset(repositoryId, "/asset5/asset5.jar");
    AssetData asset6 = generateAsset(repositoryId, "/asset6/asset6.jar");

    AssetBlobData assetBlob1 = randomAssetBlob();
    AssetBlobData assetBlob2 = randomAssetBlob();
    AssetBlobData assetBlob3 = randomAssetBlob();
    AssetBlobData assetBlob4 = randomAssetBlob();
    AssetBlobData assetBlob5 = randomAssetBlob();
    AssetBlobData assetBlob6 = randomAssetBlob();

    assetBlob1.setBlobCreated(time);
    assetBlob2.setBlobCreated(time.plusSeconds(1));
    assetBlob3.setBlobCreated(time.plusSeconds(2));
    assetBlob4.setBlobCreated(time.plusSeconds(3));
    assetBlob5.setBlobCreated(time.plusSeconds(4));
    assetBlob6.setBlobCreated(time.plusSeconds(5));

    inTx(() -> {
      Collection<AssetInfo> assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), 2);
      assertThat(assets, is(empty()));

      assetBlobStore.createAssetBlob(assetBlob1);
      asset1.setAssetBlob(assetBlob1);
      underTest.createAsset(asset1);
      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(), 2);
      assertThat(assets.size(), is(1));

      assetBlobStore.createAssetBlob(assetBlob2);
      asset2.setAssetBlob(assetBlob2);
      underTest.createAsset(asset2);

      assetBlobStore.createAssetBlob(assetBlob3);
      asset3.setAssetBlob(assetBlob3);
      underTest.createAsset(asset3);

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(),2);
      assertThat(assets.size(), is(2));

      assetBlobStore.createAssetBlob(assetBlob4);
      asset4.setAssetBlob(assetBlob4);
      underTest.createAsset(asset4);

      assetBlobStore.createAssetBlob(assetBlob5);
      asset5.setAssetBlob(assetBlob5);
      underTest.createAsset(asset5);

      assetBlobStore.createAssetBlob(assetBlob6);
      asset6.setAssetBlob(assetBlob6);
      underTest.createAsset(asset6);

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of("asset5"),100);
      assertThat(assets.size(), is(1));

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of("asset4", "asset5"),100);
      assertThat(assets.size(), is(2));

      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of("/asset.?/a.*.jar"),100);
      assertThat(assets.size(), is(6));
    });
  }

  @Test
  public void testBrowseUpdatedAssetsIdenticalDates() {
    OffsetDateTime time1 = OffsetDateTime.now();
    AssetData asset1 = generateAsset(repositoryId, "/asset1/asset1.jar");
    AssetData asset2 = generateAsset(repositoryId, "/asset2/asset2.jar");
    AssetData asset3 = generateAsset(repositoryId, "/asset3/asset3.jar");
    AssetData asset4 = generateAsset(repositoryId, "/asset4/asset4.jar");

    AssetBlobData assetBlob1 = randomAssetBlob();
    AssetBlobData assetBlob2 = randomAssetBlob();
    AssetBlobData assetBlob3 = randomAssetBlob();
    AssetBlobData assetBlob4 = randomAssetBlob();

    // times are considered the same if they are at the same millisecond
    OffsetDateTime time2 = time1.plusNanos(100000);
    OffsetDateTime time3 = time2.plusNanos(100000);
    OffsetDateTime time4 = time3.plusNanos(100000);

    assetBlob1.setBlobCreated(time1);
    assetBlob2.setBlobCreated(time2);
    assetBlob3.setBlobCreated(time3);
    assetBlob4.setBlobCreated(time4);

    inTx(() -> {
      assetBlobStore.createAssetBlob(assetBlob1);
      asset1.setAssetBlob(assetBlob1);
      underTest.createAsset(asset1);
      assetBlobStore.createAssetBlob(assetBlob2);
      asset2.setAssetBlob(assetBlob2);
      underTest.createAsset(asset2);
      Collection<AssetInfo> assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(),2);
      assertThat(assets.size(), is(2));

      assetBlobStore.createAssetBlob(assetBlob3);
      asset3.setAssetBlob(assetBlob3);
      underTest.createAsset(asset3);
      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(),2);
      assertThat(assets.size(), is(3));

      assetBlobStore.createAssetBlob(assetBlob4);
      asset4.setAssetBlob(assetBlob4);
      underTest.createAsset(asset4);
      assets = underTest.findUpdatedAssets(repositoryId, null, ImmutableList.of(),2);
      assertThat(assets.size(), is(4));
    });
  }

  private void inTx(final Runnable action) {
    UnitOfWork.begin(() -> sessionRule.openSession(DEFAULT_DATASTORE_NAME));
    try {
      Transactional.operation.run(action::run);
    }
    finally {
      UnitOfWork.end();
    }
  }
}
