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
package org.sonatype.nexus.repository.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.mime.DefaultContentValidator;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.Asset.CHECKSUM;
import static org.sonatype.nexus.repository.storage.Asset.HASHES_NOT_VERIFIED;
import static org.sonatype.nexus.repository.storage.Asset.PROVENANCE;

/**
 * Tests for {@link StorageTxImpl}.
 */
public class StorageTxImplTest
    extends TestSupport
{
  @Mock
  private BlobTx blobTx;

  @Mock
  private ODatabaseDocumentTx db;

  @Mock
  private OTransaction tx;

  @Mock
  private BucketEntityAdapter bucketEntityAdapter;

  @Mock
  private ComponentEntityAdapter componentEntityAdapter;

  @Mock
  private AssetEntityAdapter assetEntityAdapter;

  @Mock
  private ComponentFactory componentFactory;

  @Mock
  private Provider<RepositoryMoveService> repositoryMoveStoreProvider;

  @Mock
  private Asset asset;

  @Mock
  private EntityMetadata entityMetadata;

  @Mock
  private EntityId entityId;

  @Mock
  private NodeAccess nodeAccess;

  private InputStreamSupplier supplier = () -> new ByteArrayInputStream("testContent".getBytes());

  private DefaultContentValidator defaultContentValidator = Mockito.mock(DefaultContentValidator.class);

  private Map<String, String> headers = new LinkedHashMap<String, String>();

  {
    LinkedHashMap<String, String> map = new LinkedHashMap<>(5);
    map.put(BlobStore.REPO_NAME_HEADER, "testRepo");
    map.put(BlobStore.BLOB_NAME_HEADER, "testBlob.txt");
    map.put(BlobStore.CREATED_BY_HEADER, "test");
    map.put(BlobStore.CREATED_BY_IP_HEADER, "127.0.0.1");
    map.put(BlobStore.CONTENT_TYPE_HEADER, "text/plain");
    expectedHeaders = map;
  }

  private Map<String, String> expectedHeaders;

  private Iterable<HashAlgorithm> hashAlgorithms = new ArrayList<>();

  @Before
  public void prepare() throws IOException {
    Mockito.when(asset.getEntityMetadata()).thenReturn(entityMetadata);
    Mockito.when(entityMetadata.getId()).thenReturn(entityId);

    Mockito.when(defaultContentValidator.determineContentType(ArgumentMatchers.anyBoolean(),
        ArgumentMatchers.any(InputStreamSupplier.class), ArgumentMatchers.eq(MimeRulesSource.NOOP),
        ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn("text/plain");
    Mockito.when(db.getTransaction()).thenReturn(tx);
  }

  /**
   * Given: - an asset with a blob - DENY write policy When: - asset is removed Then: - exception is thrown - blob is
   * not removed from db - asset is not removed from db
   */
  @Test
  public void deleting_assets_fails_when_DENY_write_policy() {
    Mockito.when(asset.blobRef()).thenReturn(Mockito.mock(BlobRef.class));
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.DENY, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    try {
      underTest.deleteAsset(asset);
      MatcherAssert.assertThat("Expected IllegalOperationException", false);
    }
    catch (IllegalOperationException e) {
    }

    Mockito.verify(blobTx, Mockito.never())
        .delete(ArgumentMatchers.any(BlobRef.class), ArgumentMatchers.any(String.class));
    Mockito.verify(assetEntityAdapter, Mockito.never()).deleteEntity(db, asset);
  }

  /**
   * Given: - an asset without a blob - DENY write policy When: - asset is removed Then: - asset is removed from db
   */
  @Test
  public void deleting_assets_pass_when_DENY_write_policy_without_blob() {
    new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.DENY, WritePolicySelector.DEFAULT,
        bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
        MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess).deleteAsset(asset);
    Mockito.verify(assetEntityAdapter, Mockito.times(1)).deleteEntity(db, asset);
  }

  /**
   * Given: - an asset with a blob - ALLOW write policy When: - asset is removed Then: - blob is removed from db - asset
   * is removed from db
   */
  @Test
  public void deleting_assets_pass_when_ALLOW_write_policy() {
    deleteAssetWhenWritePolicy(WritePolicy.ALLOW);
  }

  /**
   * Given: - an asset with a blob - ALLOW_ONCE write policy When: - asset is removed Then: - blob is removed from db -
   * asset is removed from db
   */
  @Test
  public void deleting_assets_pass_when_ALLOW_ONCE_write_policy() {
    deleteAssetWhenWritePolicy(WritePolicy.ALLOW_ONCE);
  }

  public void deleteAssetWhenWritePolicy(final WritePolicy writePolicy) {
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", writePolicy, WritePolicySelector.DEFAULT,
        bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
        MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess).deleteAsset(asset);
    Mockito.verify(blobTx, Mockito.times(1)).delete(ArgumentMatchers.eq(blobRef), ArgumentMatchers.any(String.class));
    Mockito.verify(assetEntityAdapter, Mockito.times(1)).deleteEntity(db, asset);
  }

  /**
   * Given: - an asset with a blob - DENY write policy When: - setting a blob on the asset Then: - exception is thrown -
   * existing blob is not removed from db - new blob is not created in db - asset blob reference is not changed
   */
  @Test
  public void setting_blob_fails_on_asset_with_blob_when_DENY_write_policy() {
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    AssetBlob asssetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(asssetBlob.getBlobRef()).thenReturn(blobRef);
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.DENY, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    try {
      underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, "text/plain", false);
      MatcherAssert.assertThat("Expected IllegalOperationException", false);
    }
    catch (IllegalOperationException | IOException e) {
    }

    Mockito.verify(blobTx, Mockito.never())
        .delete(ArgumentMatchers.any(BlobRef.class), ArgumentMatchers.any(String.class));
    Mockito.verify(blobTx, Mockito.never())
        .create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
            ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString());
    Mockito.verify(asset, Mockito.never()).blobRef(ArgumentMatchers.any(BlobRef.class));
  }

  /**
   * Given: - an asset without a blob - DENY write policy When: - setting a blob on the asset Then: - exception is
   * thrown - new blob is not created in db - asset blob reference is not changed
   */
  @Test
  public void setting_blob_fails_on_asset_without_blob_when_DENY_write_policy() {
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.DENY, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    try {
      underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, "text/plain", false);
      MatcherAssert.assertThat("Expected IllegalOperationException", false);
    }
    catch (IllegalOperationException | IOException e) {
    }

    Mockito.verify(blobTx, Mockito.never())
        .delete(ArgumentMatchers.any(BlobRef.class), ArgumentMatchers.any(String.class));
    Mockito.verify(blobTx, Mockito.never())
        .create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
            ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString());
    Mockito.verify(asset, Mockito.never()).blobRef(ArgumentMatchers.any(BlobRef.class));
  }

  /**
   * Given: - an asset with a blob - ALLOW_ONCE write policy When: - setting a blob on the asset Then: - exception is
   * thrown - existing blob is not removed from db - new blob is not created in db - asset blob reference is not
   * changed
   */
  @Test
  public void setting_blob_fails_on_asset_with_blob_when_ALLOW_ONCE_write_policy() {
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(assetBlob.getBlobRef()).thenReturn(blobRef);
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    Mockito.when(blobTx.create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
        ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString())).thenReturn(assetBlob);
    StorageTxImpl underTest = new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW_ONCE,
        WritePolicySelector.DEFAULT, bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false,
        defaultContentValidator, MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    try {
      underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, "text/plain", false);
      MatcherAssert.assertThat("Expected IllegalOperationException", false);
    }
    catch (IllegalOperationException | IOException e) {
    }

    Mockito.verify(blobTx, Mockito.never())
        .delete(ArgumentMatchers.any(BlobRef.class), ArgumentMatchers.any(String.class));
    Mockito.verify(blobTx, Mockito.never())
        .create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
            ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString());
    Mockito.verify(asset, Mockito.never()).blobRef(ArgumentMatchers.any(BlobRef.class));
  }

  /**
   * Given: - an asset without a blob - ALLOW_ONCE write policy When: - setting a blob on the asset Then: - new blob is
   * created in db - asset blob reference is changed
   */
  @Test
  public void setting_blob_pass_on_asset_without_blob_when_ALLOW_ONCE_write_policy() throws IOException {
    Mockito.when(asset.attributes())
        .thenReturn(new NestedAttributesMap("attributes", new LinkedHashMap<String, Object>()));
    BlobRef newBlobRef = Mockito.mock(BlobRef.class);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Blob blob = Mockito.mock(Blob.class);
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(2);
    map.put(BlobStore.CREATED_BY_IP_HEADER, "127.0.0.1");
    map.put(BlobStore.CREATED_BY_HEADER, "anonymous");
    LinkedHashMap<String, String> headerMap = map;
    Mockito.when(blob.getHeaders()).thenReturn(headerMap);
    Mockito.when(assetBlob.getBlob()).thenReturn(blob);
    Mockito.when(assetBlob.getBlobRef()).thenReturn(newBlobRef);
    Mockito.when(blobTx.create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
        ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString())).thenReturn(assetBlob);
    StorageTxImpl underTest = new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW_ONCE,
        WritePolicySelector.DEFAULT, bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false,
        defaultContentValidator, MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, "text/plain", false);
    Mockito.verify(blobTx, Mockito.times(1)).create(ArgumentMatchers.any(InputStream.class),
        ArgumentMatchers.eq((LinkedHashMap<String, String>) expectedHeaders),
        ArgumentMatchers.eq((ArrayList<HashAlgorithm>) hashAlgorithms), ArgumentMatchers.eq("text/plain"));
    Mockito.verify(asset, Mockito.times(1)).blobRef(newBlobRef);
  }

  /**
   * Given: - an asset with a blob - ALLOW write policy When: - setting a blob on the asset Then: - existing blob is
   * removed from db - new blob is created in db - asset blob reference is changed
   */
  @Test
  public void setting_blob_pass_on_asset_with_blob_when_ALLOW_write_policy() throws IOException {
    Mockito.when(asset.attributes())
        .thenReturn(new NestedAttributesMap("attributes", new LinkedHashMap<String, Object>()));
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Blob blob = Mockito.mock(Blob.class);
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(2);
    map.put(BlobStore.CREATED_BY_IP_HEADER, "127.0.0.1");
    map.put(BlobStore.CREATED_BY_HEADER, "anonymous");
    LinkedHashMap<String, String> headerMap = map;
    Mockito.when(blob.getHeaders()).thenReturn(headerMap);
    Mockito.when(assetBlob.getBlob()).thenReturn(blob);
    Mockito.when(assetBlob.getBlobRef()).thenReturn(blobRef);
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    BlobRef newBlobRef = Mockito.mock(BlobRef.class);
    AssetBlob newAssetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(newAssetBlob.getBlob()).thenReturn(blob);
    Mockito.when(newAssetBlob.getBlobRef()).thenReturn(newBlobRef);
    Mockito.when(blobTx.create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
        ArgumentMatchers.any(Iterable.class), ArgumentMatchers.eq(ContentTypes.TEXT_PLAIN))).thenReturn(newAssetBlob);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, "text/plain", false);
    Mockito.verify(blobTx, Mockito.times(1)).delete(ArgumentMatchers.eq(blobRef), ArgumentMatchers.any(String.class));
    Mockito.verify(blobTx, Mockito.times(1)).create(ArgumentMatchers.any(InputStream.class),
        ArgumentMatchers.eq((LinkedHashMap<String, String>) expectedHeaders), ArgumentMatchers.any(Iterable.class),
        ArgumentMatchers.eq(ContentTypes.TEXT_PLAIN));
    Mockito.verify(asset, Mockito.times(1)).blobRef(newBlobRef);
  }

  /**
   * Given: - an asset without a blob - ALLOW write policy When: - setting a blob on the asset Then: - new blob is
   * created in db - asset blob reference is changed
   */
  @Test
  public void setting_blob_pass_on_asset_without_blob_when_ALLOW_write_policy() throws IOException {
    Mockito.when(asset.attributes())
        .thenReturn(new NestedAttributesMap("attributes", new LinkedHashMap<String, Object>()));
    BlobRef newBlobRef = Mockito.mock(BlobRef.class);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Blob blob = Mockito.mock(Blob.class);
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(2);
    map.put(BlobStore.CREATED_BY_IP_HEADER, "127.0.0.1");
    map.put(BlobStore.CREATED_BY_HEADER, "anonymous");
    LinkedHashMap<String, String> headerMap = map;
    Mockito.when(blob.getHeaders()).thenReturn(headerMap);
    Mockito.when(assetBlob.getBlob()).thenReturn(blob);
    Mockito.when(assetBlob.getBlobRef()).thenReturn(newBlobRef);
    Mockito.when(blobTx.create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
        ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString())).thenReturn(assetBlob);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, "text/plain", false);
    Mockito.verify(blobTx, Mockito.times(1)).create(ArgumentMatchers.any(InputStream.class),
        ArgumentMatchers.eq((LinkedHashMap<String, String>) expectedHeaders),
        ArgumentMatchers.eq((ArrayList<HashAlgorithm>) hashAlgorithms), ArgumentMatchers.eq("text/plain"));
    Mockito.verify(asset, Mockito.times(1)).blobRef(newBlobRef);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invoking_createBlob_with_verified_by_no_contentType_supplied() throws IOException {
    Mockito.when(asset.attributes())
        .thenReturn(new NestedAttributesMap("attributes", new LinkedHashMap<String, Object>()));
    BlobRef newBlobRef = Mockito.mock(BlobRef.class);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(assetBlob.getBlobRef()).thenReturn(newBlobRef);
    Mockito.when(blobTx.create(ArgumentMatchers.any(InputStream.class), ArgumentMatchers.any(Map.class),
        ArgumentMatchers.any(Iterable.class), ArgumentMatchers.anyString())).thenReturn(assetBlob);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.setBlob(asset, "testBlob.txt", supplier, hashAlgorithms, headers, null, true);
  }

  /**
   * Given: - an asset without a blob - an unattached asset blob without verified checksums When: - attaching the blob
   * to the asset Then: - the asset checksum attributes will contain the checksum - the asset provenance attributes will
   * indicate the hashes were not verified
   */
  @Test
  public void attaching_blob_without_verified_hashes_to_asset() {
    attachBlobWithHashes(false);
  }

  /**
   * Given: - an asset without a blob - an unattached asset blob with verified checksums When: - attaching the blob to
   * the asset Then: - the asset checksum attributes will contain the checksum - the asset provenance attributes will
   * indicate the hashes were verified
   */
  @Test
  public void attaching_blob_with_verified_hashes_to_asset() {
    attachBlobWithHashes(true);
  }

  public void attachBlobWithHashes(boolean hashesVerified) {
    String hashCode = "6adfb183a4a2c94a2f92dab5ade762a47889a5a1";
    Map hashes = new HashMap();
    hashes.put(SHA1 , HashCode.fromString(hashCode));
    NestedAttributesMap attributesMap = Mockito.mock(NestedAttributesMap.class);
    NestedAttributesMap checksum = Mockito.mock(NestedAttributesMap.class);
    NestedAttributesMap provenance = Mockito.mock(NestedAttributesMap.class);
    Mockito.when(attributesMap.child(CHECKSUM)).thenReturn(checksum);
    Mockito.when(attributesMap.child(PROVENANCE)).thenReturn(provenance);
    Mockito.when(asset.attributes()).thenReturn(attributesMap);
    BlobRef newBlobRef = Mockito.mock(BlobRef.class);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Blob blob = Mockito.mock(Blob.class);
    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(2);
    map.put(BlobStore.CREATED_BY_IP_HEADER, "127.0.0.1");
    map.put(BlobStore.CREATED_BY_HEADER, "anonymous");
    LinkedHashMap<String, String> headerMap = map;
    Mockito.when(blob.getHeaders()).thenReturn(headerMap);
    Mockito.when(assetBlob.getBlob()).thenReturn(blob);
    Mockito.when(assetBlob.getBlobRef()).thenReturn(newBlobRef);
    Mockito.when(assetBlob.getHashes()).thenReturn(hashes);
    Mockito.when(assetBlob.getHashesVerified()).thenReturn(hashesVerified);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.attachBlob(asset, assetBlob);
    Mockito.verify(checksum, Mockito.times(1)).set(SHA1.name(), hashCode);
    Mockito.verify(provenance, Mockito.times(1)).set(HASHES_NOT_VERIFIED, !hashesVerified);
  }

  /**
   * Given: - a blob which is missing from the blobstore When: - requiring the blob Then: - exception is thrown
   */
  @Test(expected = MissingBlobException.class)
  public void requiring_blob_fails_when_blob_is_missing_from_blobstore() {
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    Mockito.when(blobTx.get(blobRef)).thenReturn(null);
    StorageTxImpl underTest = new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW_ONCE,
        WritePolicySelector.DEFAULT, bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false,
        defaultContentValidator, MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.requireBlob(blobRef);
  }

  /**
   * Given: - an asset with no attached blob When: - attaching a blob Then: - the blob created timestamp will be updated
   * - the blob updated timestamp will be updated
   */
  @Test
  public void attaching_a_blob_to_an_asset_without_a_blob_sets_the_blob_created_and_blob_updated_timestamps() {
    LinkedHashMap<String, Object> map =
        new LinkedHashMap<>(1);
    map.put("key", new LinkedHashMap<>());
    Mockito.when(asset.attributes()).thenReturn(new NestedAttributesMap("key", map));
    BlobMetrics blobMetrics = Mockito.mock(BlobMetrics.class);
    Mockito.when(blobMetrics.getSha1Hash()).thenReturn("sha1");
    Blob blob = Mockito.mock(Blob.class);
    Mockito.when(blob.getMetrics()).thenReturn(blobMetrics);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(assetBlob.getBlob()).thenReturn(blob);
    StorageTxImpl underTest = new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW_ONCE,
        WritePolicySelector.DEFAULT, bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false,
        defaultContentValidator, MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.attachBlob(asset, assetBlob);
    Mockito.verify(asset).blobCreated(ArgumentMatchers.any(DateTime.class));
    Mockito.verify(asset).blobUpdated(ArgumentMatchers.any(DateTime.class));
  }

  /**
   * Given: - an asset with an attached blob When: - attaching a blob with a different hash Then: - the blob created
   * timestamp will NOT be updated - the blob updated timestamp will be updated
   */
  @Test
  public void attaching_a_blob_with_different_hash_to_an_asset_with_a_blob_only_updates_the_blob_created_timestamp() {
    BlobMetrics oldBlobMetrics = Mockito.mock(BlobMetrics.class);
    Mockito.when(oldBlobMetrics.getSha1Hash()).thenReturn("old-sha1");
    Blob oldBlob = Mockito.mock(Blob.class);
    Mockito.when(oldBlob.getMetrics()).thenReturn(oldBlobMetrics);
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    Mockito.when(blobTx.get(blobRef)).thenReturn(oldBlob);
    LinkedHashMap<String, Object> map =
        new LinkedHashMap<>(1);
    map.put("key", new LinkedHashMap<>());
    Mockito.when(asset.attributes()).thenReturn(new NestedAttributesMap("key", map));
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    BlobMetrics newBlobMetrics = Mockito.mock(BlobMetrics.class);
    Mockito.when(newBlobMetrics.getSha1Hash()).thenReturn("new-sha1");
    Blob newBlob = Mockito.mock(Blob.class);
    Mockito.when(newBlob.getMetrics()).thenReturn(newBlobMetrics);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(assetBlob.getBlob()).thenReturn(newBlob);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.attachBlob(asset, assetBlob);
    Mockito.verify(asset, Mockito.never()).blobCreated(ArgumentMatchers.any(DateTime.class));
    Mockito.verify(asset).blobUpdated(ArgumentMatchers.any(DateTime.class));
  }

  /**
   * Given: - an asset with an attached blob When: - attaching a blob with the same hash Then: - the blob created
   * timestamp will NOT be updated - the blob updated timestamp will NOT be updated
   */
  public void attaching_a_blob_with_the_same_hash_to_an_asset_with_an_existing_blob_does_not_update_any_timestamps() {
    BlobMetrics oldBlobMetrics = Mockito.mock(BlobMetrics.class);
    Mockito.when(oldBlobMetrics.getSha1Hash()).thenReturn("sha1");
    Blob oldBlob = Mockito.mock(Blob.class);
    Mockito.when(oldBlob.getMetrics()).thenReturn(oldBlobMetrics);
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    Mockito.when(blobTx.get(blobRef)).thenReturn(oldBlob);
    LinkedHashMap<String, Object> map =
        new LinkedHashMap<>(1);
    map.put("key", new LinkedHashMap<>());
    Mockito.when(asset.attributes()).thenReturn(new NestedAttributesMap("key", map));
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    BlobMetrics newBlobMetrics = Mockito.mock(BlobMetrics.class);
    Mockito.when(newBlobMetrics.getSha1Hash()).thenReturn("sha1");
    Blob newBlob = Mockito.mock(Blob.class);
    Mockito.when(newBlob.getMetrics()).thenReturn(newBlobMetrics);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(assetBlob.getBlob()).thenReturn(newBlob);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.attachBlob(asset, assetBlob);
    Mockito.verify(asset, Mockito.never()).blobCreated(ArgumentMatchers.any(DateTime.class));
    Mockito.verify(asset, Mockito.never()).blobUpdated(ArgumentMatchers.any(DateTime.class));
  }

  /**
   * Given: - an asset with an attached blob that has disappeared from the blob store When: - attaching a blob with any
   * hash Then: - the blob created timestamp will NOT be updated - the blob updated timestamp will be updated
   */
  @Test
  public void attaching_a_blob_to_an_asset_with_a_missing_blob_only_updates_the_blob_created_timestamp() {
    BlobRef blobRef = Mockito.mock(BlobRef.class);
    Mockito.when(blobTx.get(blobRef)).thenReturn(null);
    LinkedHashMap<String, Object> map =
        new LinkedHashMap<>(1);
    map.put("key", new LinkedHashMap<>());
    Mockito.when(asset.attributes()).thenReturn(new NestedAttributesMap("key", map));
    Mockito.when(asset.blobRef()).thenReturn(blobRef);
    BlobMetrics newBlobMetrics = Mockito.mock(BlobMetrics.class);
    Mockito.when(newBlobMetrics.getSha1Hash()).thenReturn("new-sha1");
    Blob newBlob = Mockito.mock(Blob.class);
    Mockito.when(newBlob.getMetrics()).thenReturn(newBlobMetrics);
    AssetBlob assetBlob = Mockito.mock(AssetBlob.class);
    Mockito.when(assetBlob.getBlob()).thenReturn(newBlob);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);
    underTest.attachBlob(asset, assetBlob);
    Mockito.verify(asset, Mockito.never()).blobCreated(ArgumentMatchers.any(DateTime.class));
    Mockito.verify(asset).blobUpdated(ArgumentMatchers.any(DateTime.class));
  }

  @Test
  public void verifying_asset_lookup_by_id() {
    EntityId assetId = Mockito.mock(EntityId.class);
    Asset asset = Mockito.mock(Asset.class);
    Mockito.when(assetEntityAdapter.read(db, assetId)).thenReturn(asset);
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);

    MatcherAssert.assertThat(underTest.findAsset(assetId), Matchers.is(asset));
  }

  @Test
  public void test_asset_exists() {
    String repositoryName = "testRepo";
    Repository repository = Mockito.mock(Repository.class);

    StorageTxImpl underTest = new StorageTxImpl("test", "127.0.0.1", blobTx, db, repositoryName, WritePolicy.ALLOW,
        WritePolicySelector.DEFAULT, bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false,
        defaultContentValidator, MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);

    underTest.assetExists(repositoryName, repository);
    Mockito.verify(assetEntityAdapter)
        .exists(ArgumentMatchers.eq(db), ArgumentMatchers.eq(repositoryName), ArgumentMatchers.any());

    Mockito.doReturn(true).when(assetEntityAdapter)
        .exists(ArgumentMatchers.eq(db), ArgumentMatchers.eq(repositoryName), ArgumentMatchers.any());
    MatcherAssert.assertThat(underTest.assetExists(repositoryName, repository), Matchers.is(true));

    Mockito.doReturn(false).when(assetEntityAdapter)
        .exists(ArgumentMatchers.eq(db), ArgumentMatchers.eq(repositoryName), ArgumentMatchers.any());
    MatcherAssert.assertThat(underTest.assetExists(repositoryName, repository), Matchers.is(false));
  }

  @Test
  public void browse_assets_with_query() {
    Asset asset = Mockito.mock(Asset.class);

    Mockito.when(
        assetEntityAdapter.browseByQueryAsync(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Collections.singletonList(asset));

    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);

    MatcherAssert.assertThat(underTest.browseAssets(Mockito.mock(Query.class), Mockito.mock(Bucket.class)),
        Matchers.is(Collections.singletonList(asset)));
  }

  @Test
  public void browse_components_with_query() {
    Component component = Mockito.mock(Component.class);

    Mockito.when(componentEntityAdapter.browseByQueryAsync(ArgumentMatchers.any(), ArgumentMatchers.any(),
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Collections.singletonList(component));

    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);

    MatcherAssert.assertThat(underTest.browseComponents(Mockito.mock(Query.class), Mockito.mock(Bucket.class)),
        Matchers.is(Collections.singletonList(component)));
  }

  @Test
  public void createBlob_on_TempBlob_from_another_blobstore_falls_back_to_create_from_InputStream() throws IOException {
    StorageTxImpl underTest =
        new StorageTxImpl("test", "127.0.0.1", blobTx, db, "testRepo", WritePolicy.ALLOW, WritePolicySelector.DEFAULT,
            bucketEntityAdapter, componentEntityAdapter, assetEntityAdapter, false, defaultContentValidator,
            MimeRulesSource.NOOP, componentFactory, repositoryMoveStoreProvider, nodeAccess);

    BlobRef blobRef = Mockito.mock(BlobRef.class);
    TempBlob tempBlob = Mockito.mock(TempBlob.class);

    Mockito.doThrow(new MissingBlobException(blobRef)).when(blobTx)
        .createByCopying(ArgumentMatchers.any(), ArgumentMatchers.any(Map.class), ArgumentMatchers.any(Map.class),
            ArgumentMatchers.anyBoolean());

    underTest.createBlob("blob", tempBlob, headers, ContentTypes.TEXT_PLAIN, true);

    Mockito.verify(blobTx)
        .create(ArgumentMatchers.any(), ArgumentMatchers.any(Map.class), ArgumentMatchers.any(Iterable.class),
            ArgumentMatchers.any(String.class));
  }
}
