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
import java.util.UUID;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;

import org.junit.jupiter.api.BeforeEach;
import org.sonatype.nexus.testdb.DatabaseTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Integration tests for {@link AssetData} continuation token generation with browseEager queries.
 * Tests the isFromEagerQuery flag mechanism to ensure proper continuation token format.
 */
public class AssetDataContinuationTokenIntegrationTest
    extends AssetDAOTestSupport
{
  private int repositoryId;

  @BeforeEach
  public void setup() {
    initialiseContent(false);

    generateConfiguration();
    EntityId entityId = generatedConfigurations().get(0).getRepositoryId();
    generateSingleRepository(UUID.fromString(entityId.getValue()));
    repositoryId = generatedRepositories().get(0).repositoryId;
  }

  @DatabaseTest
  public void testBrowseEagerAssets_producesCompositeTokens() throws InterruptedException {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      ComponentData component = generateComponent(repositoryId, "test", "component1", "1.0");
      createComponents(componentDao, false, component);

      // Create assets with different blob_created timestamps
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path1", now.minusDays(3));
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path2", now.minusDays(2));
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path3", now.minusDays(1));

      session.getTransaction().commit();

      // Browse with browseEagerAssetsInRepository
      Continuation<Asset> page1 = assetDao.browseEagerAssetsInRepository(repositoryId, null, 2, null, null);
      assertThat(page1.size(), is(2));

      String continuationToken = page1.nextContinuationToken();
      assertThat(continuationToken, notNullValue());

      // Verify the token is a composite token (can be decoded)
      AssetBlobCreatedContinuationToken decoded = AssetBlobCreatedContinuationToken.decode(continuationToken);
      assertThat(decoded, notNullValue());
      assertThat(decoded.getAssetId(), notNullValue());
      assertThat(decoded.getBlobCreatedMillis(), notNullValue());

      // Verify each asset from eager query produces composite tokens
      for (Asset asset : page1) {
        AssetData assetData = (AssetData) asset;
        String assetToken = assetData.nextContinuationToken();
        assertThat(assetToken, notNullValue());

        // Verify it produces a composite token (can be decoded)
        AssetBlobCreatedContinuationToken decodedAssetToken = AssetBlobCreatedContinuationToken.decode(assetToken);
        assertThat(decodedAssetToken, notNullValue());
      }
    }
  }

  @DatabaseTest
  public void testBrowseRegularAssets_producesSimpleTokens() throws InterruptedException {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      ComponentData component = generateComponent(repositoryId, "test", "component1", "1.0");
      createComponents(componentDao, false, component);

      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path1", now.minusDays(1));
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path2", now);

      session.getTransaction().commit();

      // Browse with regular browseAssets (non-eager)
      Continuation<Asset> assets = assetDao.browseAssets(repositoryId, 10, null, null, null, null);
      assertThat(assets.size(), is(2));

      // Verify each asset produces a simple token (just asset ID)
      for (Asset asset : assets) {
        AssetData assetData = (AssetData) asset;
        String token = assetData.nextContinuationToken();

        // Simple token should be just a number (asset ID)
        assertTrue(token.matches("\\d+"), "Token should be a simple numeric asset ID: " + token);

        // Should NOT be decodable as a composite token
        boolean decodeFailed = false;
        try {
          AssetBlobCreatedContinuationToken.decode(token);
        }
        catch (IllegalArgumentException e) {
          // Expected - simple token should not be decodable as composite
          decodeFailed = true;
        }
        assertTrue(decodeFailed, "Regular asset should not produce composite token");
      }
    }
  }

  @DatabaseTest
  public void testEagerQuery_continuationTokensAreUnique() throws InterruptedException {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      ComponentData component = generateComponent(repositoryId, "test", "component1", "1.0");
      createComponents(componentDao, false, component);

      // Create assets with different timestamps
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path1", now.minusDays(5));
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path2", now.minusDays(4));
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path3", now.minusDays(3));

      session.getTransaction().commit();

      Continuation<Asset> assets = assetDao.browseEagerAssetsInRepository(repositoryId, null, 10, null, null);
      assertThat(assets.size(), is(3));

      // Verify all tokens are unique
      Asset[] assetArray = assets.toArray(new Asset[0]);
      String token1 = ((AssetData) assetArray[0]).nextContinuationToken();
      String token2 = ((AssetData) assetArray[1]).nextContinuationToken();
      String token3 = ((AssetData) assetArray[2]).nextContinuationToken();

      assertThat(token1.equals(token2), is(false));
      assertThat(token2.equals(token3), is(false));
      assertThat(token1.equals(token3), is(false));
    }
  }

  @DatabaseTest
  public void testEagerQuery_sameTimestamp_producesUniqueTokens() throws InterruptedException {
    OffsetDateTime sameTime = OffsetDateTime.now(ZoneOffset.UTC);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      ComponentData component = generateComponent(repositoryId, "test", "component1", "1.0");
      createComponents(componentDao, false, component);

      // Create multiple assets with the same blob_created timestamp
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path1", sameTime);
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path2", sameTime);
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path3", sameTime);

      session.getTransaction().commit();

      Continuation<Asset> assets = assetDao.browseEagerAssetsInRepository(repositoryId, null, 10, null, null);
      assertThat(assets.size(), is(3));

      // Even with the same timestamp, tokens should be unique due to different asset IDs
      Asset[] assetArray = assets.toArray(new Asset[0]);
      String token1 = ((AssetData) assetArray[0]).nextContinuationToken();
      String token2 = ((AssetData) assetArray[1]).nextContinuationToken();
      String token3 = ((AssetData) assetArray[2]).nextContinuationToken();

      assertThat(token1.equals(token2), is(false));
      assertThat(token2.equals(token3), is(false));

      // Verify they're all composite tokens with the same timestamp but different asset IDs
      AssetBlobCreatedContinuationToken decoded1 = AssetBlobCreatedContinuationToken.decode(token1);
      AssetBlobCreatedContinuationToken decoded2 = AssetBlobCreatedContinuationToken.decode(token2);
      AssetBlobCreatedContinuationToken decoded3 = AssetBlobCreatedContinuationToken.decode(token3);

      assertThat(decoded1.getBlobCreatedMillis(), is(decoded2.getBlobCreatedMillis()));
      assertThat(decoded2.getBlobCreatedMillis(), is(decoded3.getBlobCreatedMillis()));

      // Asset IDs should be different
      assertThat(decoded1.getAssetId() == decoded2.getAssetId(), is(false));
      assertThat(decoded2.getAssetId() == decoded3.getAssetId(), is(false));
    }
  }

  @DatabaseTest
  public void testEagerQuery_pagination_maintainsCorrectTokenFormat() throws InterruptedException {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      ComponentData component = generateComponent(repositoryId, "test", "component1", "1.0");
      createComponents(componentDao, false, component);

      // Create 5 assets
      for (int i = 0; i < 5; i++) {
        createAssetWithBlobCreated(assetDao, blobDao, component, "/path" + i, now.minusDays(i));
      }

      session.getTransaction().commit();

      // Page 1
      Continuation<Asset> page1 = assetDao.browseEagerAssetsInRepository(repositoryId, null, 2, null, null);
      assertThat(page1.size(), is(2));

      String token1 = page1.nextContinuationToken();
      AssetBlobCreatedContinuationToken decodedToken1 = AssetBlobCreatedContinuationToken.decode(token1);
      assertThat(decodedToken1, notNullValue());

      // Page 2
      Continuation<Asset> page2 = assetDao.browseEagerAssetsInRepository(
          repositoryId, decodedToken1, 2, null, null);
      assertThat(page2.size(), is(2));

      String token2 = page2.nextContinuationToken();
      AssetBlobCreatedContinuationToken decodedToken2 = AssetBlobCreatedContinuationToken.decode(token2);
      assertThat(decodedToken2, notNullValue());

      // Page 3
      Continuation<Asset> page3 = assetDao.browseEagerAssetsInRepository(
          repositoryId, decodedToken2, 2, null, null);
      assertThat(page3.size(), is(1));

      // Verify all assets in all pages produce composite tokens
      for (Asset asset : page1) {
        AssetData assetData = (AssetData) asset;
        String assetToken = assetData.nextContinuationToken();
        assertThat(AssetBlobCreatedContinuationToken.decode(assetToken), notNullValue());
      }
      for (Asset asset : page2) {
        AssetData assetData = (AssetData) asset;
        String assetToken = assetData.nextContinuationToken();
        assertThat(AssetBlobCreatedContinuationToken.decode(assetToken), notNullValue());
      }
      for (Asset asset : page3) {
        AssetData assetData = (AssetData) asset;
        String assetToken = assetData.nextContinuationToken();
        assertThat(AssetBlobCreatedContinuationToken.decode(assetToken), notNullValue());
      }
    }
  }

  @DatabaseTest
  public void testMixedQueries_produceCorrectTokenFormats() throws InterruptedException {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      AssetBlobDAO blobDao = session.access(TestAssetBlobDAO.class);
      ComponentDAO componentDao = session.access(TestComponentDAO.class);

      ComponentData component = generateComponent(repositoryId, "test", "component1", "1.0");
      createComponents(componentDao, false, component);

      createAssetWithBlobCreated(assetDao, blobDao, component, "/path1", now.minusDays(1));
      createAssetWithBlobCreated(assetDao, blobDao, component, "/path2", now);

      session.getTransaction().commit();

      // Query the same assets with both methods
      Continuation<Asset> eagerAssets = assetDao.browseEagerAssetsInRepository(
          repositoryId, null, 10, null, null);
      Continuation<Asset> regularAssets = assetDao.browseAssets(repositoryId, 10, null, null, null, null);

      assertThat(eagerAssets.size(), is(2));
      assertThat(regularAssets.size(), is(2));

      // Eager query should produce composite tokens
      for (Asset asset : eagerAssets) {
        AssetData assetData = (AssetData) asset;
        String token = assetData.nextContinuationToken();
        AssetBlobCreatedContinuationToken decoded = AssetBlobCreatedContinuationToken.decode(token);
        assertThat(decoded, notNullValue());
      }

      // Regular query should produce simple tokens
      for (Asset asset : regularAssets) {
        AssetData assetData = (AssetData) asset;
        String token = assetData.nextContinuationToken();
        assertTrue(token.matches("\\d+"), "Regular query should produce simple token: " + token);
      }
    }
  }

  private AssetData createAssetWithBlobCreated(
      final AssetDAO assetDao,
      final AssetBlobDAO blobDao,
      final ComponentData component,
      final String path,
      final OffsetDateTime blobCreated) throws InterruptedException
  {
    Thread.sleep(1); // Small delay to ensure different asset_ids

    AssetBlobData blob = generateAssetBlob(blobCreated);
    blobDao.createAssetBlob(blob);

    AssetData asset = generateAsset(this.repositoryId, path);
    asset.setComponent(component);
    asset.setAssetBlob(blob);
    assetDao.createAsset(asset, false);

    return asset;
  }
}
