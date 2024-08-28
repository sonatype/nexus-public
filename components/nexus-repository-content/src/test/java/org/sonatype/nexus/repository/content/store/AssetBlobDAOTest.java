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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.testcommon.matchers.NexusMatchers.time;

/**
 * Test {@link AssetBlobDAO}.
 */
public class AssetBlobDAOTest
    extends ExampleContentTestSupport
{
  private static final String NODE_ID = "ab761d55-5d9c22b6-3f38315a-75b3db34-0922a4d5";

  private static final String BLOB_ID = "a8f3f56f-e895-4b6e-984a-1cf1f5107d36";

  @Test
  public void testCrudOperations() {

    AssetBlobData assetBlob1 = randomAssetBlob();
    AssetBlobData assetBlob2 = randomAssetBlob();

    BlobRef blobRef1 = assetBlob1.blobRef();
    BlobRef blobRef2 = assetBlob2.blobRef();

    AssetBlob tempResult;

    // CREATE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      assertThat(dao.browseUnusedAssetBlobs(1, 60, null), emptyIterable());

      dao.createAssetBlob(assetBlob1);

      assertThat(dao.browseUnusedAssetBlobs(1, 60, null), contains(sameBlob(assetBlob1)));

      dao.createAssetBlob(assetBlob2);

      assertThat(dao.browseUnusedAssetBlobs(1, 60, null), contains(sameBlob(assetBlob1)));

      assertThat(dao.browseUnusedAssetBlobs(2, 60, null),
          contains(sameBlob(assetBlob1), sameBlob(assetBlob2)));

      session.getTransaction().commit();
    }

    // TRY CREATE AGAIN

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      AssetBlobData duplicate = new AssetBlobData();
      duplicate.setBlobRef(assetBlob1.blobRef());
      duplicate.setBlobSize(1234);
      duplicate.setContentType("text/html");
      duplicate.setChecksums(ImmutableMap.of());
      duplicate.setBlobCreated(UTC.now());
      dao.createAssetBlob(duplicate);

      session.getTransaction().commit();
      fail("Cannot create the same component twice");
    }
    catch (DuplicateKeyException e) {
      logger.debug("Got expected exception", e);
    }

    // READ

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      assertFalse(dao.readAssetBlob(new BlobRef(NODE_ID, "test-store", BLOB_ID)).isPresent());

      tempResult = dao.readAssetBlob(blobRef1).get();
      assertThat(tempResult, sameBlob(assetBlob1));

      tempResult = dao.readAssetBlob(blobRef2).get();
      assertThat(tempResult, sameBlob(assetBlob2));
    }

    // NO UPDATE METHODS (each blob is considered immutable)

    // DELETE

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      assertTrue(dao.deleteAssetBlob(blobRef1));

      assertThat(dao.browseUnusedAssetBlobs(1, 60, null), contains(sameBlob(assetBlob2)));

      assertTrue(dao.deleteAssetBlob(blobRef2));

      assertThat(dao.browseUnusedAssetBlobs(1, 60, null), emptyIterable());

      assertFalse(dao.deleteAssetBlob(new BlobRef(NODE_ID, "test-store", BLOB_ID)));
    }
  }

  @Test
  public void testBrowseAll() {
    AssetBlobData assetBlob1 = randomAssetBlob();
    AssetBlobData assetBlob2 = randomAssetBlob();

    BlobRef blobRef1 = assetBlob1.blobRef();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      dao.createAssetBlob(assetBlob1);

      assertThat(dao.browseAssetBlobs(1, null), contains(sameBlob(assetBlob1)));

      dao.createAssetBlob(assetBlob2);

      assertThat(dao.browseAssetBlobs(2, null), contains(sameBlob(assetBlob1), sameBlob(assetBlob2)));

      dao.deleteAssetBlob(blobRef1);

      assertThat(dao.browseAssetBlobs(2, null), contains(sameBlob(assetBlob2)));
    }
  }

  @Test
  public void testBlob() {
    AssetBlobData assetBlob1 = randomAssetBlob();
    BlobRef blobRef1 = assetBlob1.blobRef();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      dao.createAssetBlob(assetBlob1);

      OffsetDateTime blobCreated = OffsetDateTime.now().minusDays(1);
      dao.setBlobCreated(blobRef1, blobCreated);

      assertThat(dao.readAssetBlob(blobRef1).get().blobCreated().truncatedTo(ChronoUnit.SECONDS),
          time(blobCreated.truncatedTo(ChronoUnit.SECONDS)));
    }
  }

  @Test
  public void testCountLegacyAssetBlobs() throws Exception {
    final int TOTAL_ASSETS = 20;
    final int LEGACY_ASSETS = 15;
    prepareLegacyFormatAssetBlobs(TOTAL_ASSETS, LEGACY_ASSETS);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      int legacyCount = dao.countNotMigratedAssetBlobs();

      assertThat(legacyCount, is(equalTo(LEGACY_ASSETS)));
    }
  }

  @Test
  public void testBrowseLegacyAssetBlobs() throws Exception {
    final int TOTAL_ASSETS = 20;
    final int LEGACY_ASSETS = 15;
    prepareLegacyFormatAssetBlobs(TOTAL_ASSETS, LEGACY_ASSETS);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      Continuation<AssetBlob> assetBlobs = dao.browseAssetsWithLegacyBlobRef(100, null);

      assertThat(assetBlobs, hasSize(LEGACY_ASSETS));
    }
  }

  @Test
  public void testBrowseLegacyAssetBlobsPaging() throws Exception {
    final int TOTAL_ASSETS = 20;
    final int PAGE_SIZE = 5;
    prepareLegacyFormatAssetBlobs(TOTAL_ASSETS, TOTAL_ASSETS);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      Continuation<AssetBlob> assetBlobs = dao.browseAssetsWithLegacyBlobRef(PAGE_SIZE, null);
      assertThat(assetBlobs, hasSize(PAGE_SIZE));

      assetBlobs = dao.browseAssetsWithLegacyBlobRef(PAGE_SIZE, assetBlobs.nextContinuationToken());
      assertThat(assetBlobs, hasSize(PAGE_SIZE));

      assetBlobs = dao.browseAssetsWithLegacyBlobRef(PAGE_SIZE, assetBlobs.nextContinuationToken());
      assertThat(assetBlobs, hasSize(PAGE_SIZE));

      assetBlobs = dao.browseAssetsWithLegacyBlobRef(PAGE_SIZE, assetBlobs.nextContinuationToken());
      assertThat(assetBlobs, hasSize(PAGE_SIZE));

      assetBlobs = dao.browseAssetsWithLegacyBlobRef(PAGE_SIZE, assetBlobs.nextContinuationToken());
      assertThat(assetBlobs, empty());
    }
  }

  @Test
  public void testUpdateLegacyBlobRefs() throws Exception {
    final int TOTAL_ASSETS = 20;
    final int LEGACY_ASSETS = 15;
    prepareLegacyFormatAssetBlobs(TOTAL_ASSETS, LEGACY_ASSETS);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      Collection<AssetBlob> legacyAssetBlobs = dao.browseAssetsWithLegacyBlobRef(100, null);
      assertThat(dao.browseAssetsWithLegacyBlobRef(100, null), hasSize(LEGACY_ASSETS));

      dao.updateBlobRefs(legacyAssetBlobs);

      assertThat(dao.browseAssetsWithLegacyBlobRef(100, null), empty());
    }
  }

  @Test
  public void testUpdateSingleLegacyBlob() throws Exception {
    final int TOTAL_ASSETS = 2;
    final int LEGACY_ASSETS = 1;
    prepareLegacyFormatAssetBlobs(TOTAL_ASSETS, LEGACY_ASSETS);

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);

      assertThat(dao.browseAssetsWithLegacyBlobRef(100, null), hasSize(LEGACY_ASSETS));

      dao.browseAssetsWithLegacyBlobRef(100, null).forEach(dao::updateBlobRef);

      assertThat(dao.browseAssetsWithLegacyBlobRef(100, null), empty());
    }
  }

  @Test
  public void testGetRepositoryName() {
    generateConfiguration();
    ConfigurationData configurationData = generatedConfigurations().get(0);
    EntityId repositoryId = configurationData.getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateRandomNamespaces(1);
    generateRandomVersions(1);
    generateContent(1, false);

    BlobRef blobRef = generatedAssetBlobs().get(0).blobRef();
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      String repoName = dao.getRepositoryName(blobRef);
      System.out.println(repoName);
      assertThat(repoName, is("repo-name"));
    }
  }

  @Test
  public void testGetPath() {
    generateConfiguration();
    ConfigurationData configurationData = generatedConfigurations().get(0);
    EntityId repositoryId = configurationData.getRepositoryId();
    generateSingleRepository(UUID.fromString(repositoryId.getValue()));
    generateRandomNamespaces(1);
    generateRandomVersions(1);
    generateContent(1, false);

    BlobRef blobRef = generatedAssetBlobs().get(0).blobRef();
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      String path = dao.getPathByBlobRef(blobRef);
      assertNotNull(path);
      assertFalse(path.isEmpty());
    }
  }

  private void prepareLegacyFormatAssetBlobs(final int assetsCount, final int legacyAssetsCount) throws SQLException {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
         Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME)) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      for (int i = 0; i < assetsCount; i++) {
        dao.createAssetBlob(randomAssetBlob());
      }

      session.getTransaction().commit();

      dao.browseAssetBlobs(legacyAssetsCount, null).forEach(assetBlob -> {
        BlobRef blobRef = assetBlob.blobRef();
        String id = ((AssetBlobData) assetBlob).assetBlobId.toString();
        String legacyBlobRef = String.format("%s:%s@%s", blobRef.getStore(), blobRef.getBlob(), NODE_ID);
        String sql = String.format("UPDATE test_asset_blob SET blob_ref='%s' WHERE asset_blob_id=%s", legacyBlobRef, id);

        try {
          connection.prepareStatement(sql).executeUpdate();
          dao.browseAssetsWithLegacyBlobRef(100, null);
        }
        catch (SQLException e) {
          logger.info("Error updating in-memory asset blobs, {}", e.getMessage());
        }
      });
    }
  }
}
