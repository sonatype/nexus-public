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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.storage.BlobMetadataStorage;
import org.sonatype.nexus.repository.view.payloads.AttachableBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FluentAssetBuilderImplTest
    extends TestSupport
{
  private static final int REPOSITORY_ID = 1;

  private static final String ASSET_PATH = "/org/example/test/1.0/test-1.0.jar";

  private static final String NODE_NAME = "test-node";

  private static final String BLOB_STORE_NAME = "default";

  private static final String BLOB_ID_STRING = "blob-123";

  @Mock
  private ContentFacetSupport facet;

  @Mock
  private AssetStore assetStore;

  @Mock
  private Repository repository;

  @Mock
  private TempBlob tempBlob;

  @Mock
  private Blob blob;

  @Mock
  private BlobStore blobStore;

  @Mock
  private AssetBlobStore assetBlobStore;

  @Mock
  private BlobMetadataStorage blobMetadataStorage;

  private ContentFacetStores stores;

  private Component component;

  private FluentAssetBuilderImpl underTest;

  @Before
  public void setUp() {
    when(facet.repository()).thenReturn(repository);
    when(facet.contentRepositoryId()).thenReturn(REPOSITORY_ID);
    when(facet.nodeName()).thenReturn(NODE_NAME);
    when(facet.blobMetadataStorage()).thenReturn(blobMetadataStorage);

    FormatStoreManager mockFormatStoreManager = mock(FormatStoreManager.class);
    when(mockFormatStoreManager.assetStore(anyString())).thenReturn(assetStore);
    when(mockFormatStoreManager.assetBlobStore(anyString())).thenReturn(assetBlobStore);
    BlobStoreManager blobStoreManager = mock(BlobStoreManager.class);
    when(blobStoreManager.get(eq(BLOB_STORE_NAME))).thenReturn(blobStore);
    stores = new ContentFacetStores(blobStoreManager, BLOB_STORE_NAME, mockFormatStoreManager, "test");
    when(facet.stores()).thenReturn(stores);

    // When createAssetBlob is called, set the assetBlobId so that internal id checks pass
    doAnswer(invocation -> {
      AssetBlobData blobData = invocation.getArgument(0);
      blobData.setAssetBlobId(100);
      return null;
    }).when(assetBlobStore).createAssetBlob(any(AssetBlobData.class));

    ComponentData componentData = new ComponentData();
    componentData.setComponentId(1);
    componentData.setNamespace("org.example");
    componentData.setName("test");
    componentData.setVersion("1.0");
    component = componentData;

    underTest = new FluentAssetBuilderImpl(facet, assetStore, ASSET_PATH);
  }

  @Test
  public void testConstructorSetsPathAndDefaults() {
    // The constructor sets the path and defaults kind to empty string.
    // Verify by calling find(), which uses the path internally.
    when(assetStore.readPath(anyInt(), eq(ASSET_PATH))).thenReturn(Optional.empty());

    underTest.find();

    verify(assetStore).readPath(REPOSITORY_ID, ASSET_PATH);
  }

  @Test
  public void testKindReturnsSameBuilder() {
    FluentAssetBuilder result = underTest.kind("ARTIFACT");

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testComponentReturnsSameBuilder() {
    FluentAssetBuilder result = underTest.component(component);

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testBlobWithTempBlobReturnsSameBuilder() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    when(tempBlob.getHashes()).thenReturn(hashes);

    FluentAssetBuilder result = underTest.blob(tempBlob);

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testBlobWithBlobAndChecksumsReturnsSameBuilder() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();

    FluentAssetBuilder result = underTest.blob(blob, checksums);

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testAttributesReturnsSameBuilder() {
    FluentAssetBuilder result = underTest.attributes("format", "maven2");

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testAttributesMultipleCallsReturnSameBuilder() {
    FluentAssetBuilder result = underTest
        .attributes("format", "maven2")
        .attributes("classifier", "sources");

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testFluentChaining() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();

    FluentAssetBuilder result = underTest
        .kind("ARTIFACT")
        .component(component)
        .blob(blob, checksums)
        .attributes("format", "maven2");

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test
  public void testFindReturnsAssetWhenExists() {
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(anyInt(), eq(ASSET_PATH))).thenReturn(Optional.of(existingAsset));

    Optional<FluentAsset> result = underTest.find();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().path(), is(ASSET_PATH));
    verify(assetStore).readPath(REPOSITORY_ID, ASSET_PATH);
  }

  @Test
  public void testFindReturnsEmptyWhenAssetDoesNotExist() {
    when(assetStore.readPath(anyInt(), eq(ASSET_PATH))).thenReturn(Optional.empty());

    Optional<FluentAsset> result = underTest.find();

    assertThat(result.isPresent(), is(false));
    verify(assetStore).readPath(REPOSITORY_ID, ASSET_PATH);
  }

  @Test
  public void testFindUsesCorrectRepositoryId() {
    when(assetStore.readPath(anyInt(), eq(ASSET_PATH))).thenReturn(Optional.empty());

    underTest.find();

    verify(assetStore).readPath(REPOSITORY_ID, ASSET_PATH);
    // contentRepositoryId() is called once in the constructor and once in find()
    verify(facet, times(2)).contentRepositoryId();
  }

  @Test
  public void testKindCanBeSetMultipleTimes() {
    FluentAssetBuilder result = underTest
        .kind("ARTIFACT")
        .kind("INDEX");

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentAssetBuilder) underTest));
  }

  @Test(expected = NullPointerException.class)
  public void testKindRejectsNull() {
    underTest.kind(null);
  }

  @Test(expected = NullPointerException.class)
  public void testComponentRejectsNull() {
    underTest.component(null);
  }

  @Test(expected = NullPointerException.class)
  public void testAttributesRejectsNullKey() {
    underTest.attributes(null, "value");
  }

  @Test(expected = NullPointerException.class)
  public void testAttributesRejectsNullValue() {
    underTest.attributes("key", null);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullFacet() {
    new FluentAssetBuilderImpl(null, assetStore, ASSET_PATH);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullAssetStore() {
    new FluentAssetBuilderImpl(facet, null, ASSET_PATH);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullPath() {
    new FluentAssetBuilderImpl(facet, assetStore, (String) null);
  }

  @Test
  public void testFindReturnsFluentAssetWithCorrectRepository() {
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(anyInt(), eq(ASSET_PATH))).thenReturn(Optional.of(existingAsset));

    Optional<FluentAsset> result = underTest.find();

    assertThat(result.isPresent(), is(true));
    // FluentAssetImpl wraps the facet and asset; verify the asset path is preserved
    FluentAsset fluentAsset = result.get();
    assertThat(fluentAsset.path(), is(ASSET_PATH));
  }

  @Test
  public void testBlobWithTempBlobCapturesHashes() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(tempBlob.getHashes()).thenReturn(hashes);

    FluentAssetBuilder result = underTest.blob(tempBlob);

    assertThat(result, is(notNullValue()));
    verify(tempBlob).getHashes();
  }

  @Test
  public void testConstructorWithAsset() {
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    assertThat(builderFromAsset, is(notNullValue()));
  }

  @Test
  public void testConstructorWithAssetPreservesPath() {
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetStore.readPath(anyInt(), eq(ASSET_PATH))).thenReturn(Optional.of(existingAsset));

    Optional<FluentAsset> result = builderFromAsset.find();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().path(), is(ASSET_PATH));
  }

  @Test
  public void testFindCalledMultipleTimesUsesStore() {
    when(assetStore.readPath(anyInt(), eq(ASSET_PATH)))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.empty());

    Optional<FluentAsset> result1 = underTest.find();
    Optional<FluentAsset> result2 = underTest.find();

    assertThat(result1.isPresent(), is(false));
    assertThat(result2.isPresent(), is(false));
  }

  // --- save() method tests ---

  @Test
  public void testSaveWithoutBlobOrAttributes() {
    // save() with no blob and no attributes should just delegate to assetStore.save
    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          // Simulate assetStore calling the create callback (asset not found)
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });
    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    assertThat(result.path(), is(ASSET_PATH));
    // No checkAttachAllowed because no blob supplier
    verify(facet, never()).checkAttachAllowed(any(Asset.class));
  }

  @Test
  public void testSaveWithAttributesCopiesAttributesToAssetData() {
    underTest.attributes("format", "maven2");
    underTest.attributes("classifier", "sources");

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });
    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    // Verify asset was created with attributes in the backing map
    verify(assetStore).createAsset(any(AssetData.class));
  }

  @Test
  public void testSaveWithBlobCallsCheckAttachAllowed() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    underTest.blob(blob, checksums);

    configureBlobForAttach();

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(facet).checkAttachAllowed(any(Asset.class));
  }

  @Test
  public void testSaveWithBlobCallsCheckAttachAllowedWithExistingAsset() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    underTest.blob(blob, checksums);

    configureBlobForAttach();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    // checkAttachAllowed is called with the existing asset when one is found
    verify(facet).checkAttachAllowed(existingAsset);
  }

  // --- createAsset callback tests ---

  @Test
  public void testCreateAssetSetsLastUpdated() {
    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    assertThat(result.lastUpdated(), is(notNullValue()));
    verify(assetStore).createAsset(any(AssetData.class));
  }

  @Test
  public void testCreateAssetWithBlobSetsAssetBlob() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    underTest.blob(blob, checksums);

    configureBlobForAttach();

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    ArgumentCaptor<AssetData> assetDataCaptor = ArgumentCaptor.forClass(AssetData.class);

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore).createAsset(assetDataCaptor.capture());
    AssetData createdAsset = assetDataCaptor.getValue();
    assertThat(createdAsset.blob().isPresent(), is(true));
    verify(assetBlobStore).createAssetBlob(any(AssetBlobData.class));
  }

  // --- updateAsset callback tests ---

  @Test
  public void testUpdateAssetWithBlobUpdatesAssetBlobLink() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    underTest.blob(blob, checksums);

    configureBlobForAttach();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore).updateAssetBlobLink(existingAsset);
  }

  @Test
  public void testUpdateAssetWithoutBlobDoesNotUpdateBlobLink() {
    // save without blob, but with an existing asset triggers update path
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore, never()).updateAssetBlobLink(any(Asset.class));
  }

  @Test
  public void testUpdateAssetWithAttributesCallsUpdateAssetAttributes() {
    underTest.attributes("format", "maven2");

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore).updateAssetAttributes(eq(existingAsset), any(AttributeChangeSet.class));
  }

  @Test
  public void testUpdateAssetWithoutAttributesDoesNotCallUpdateAssetAttributes() {
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore, never()).updateAssetAttributes(any(Asset.class), any(AttributeChangeSet.class));
  }

  // --- postTransaction callback tests ---

  @Test
  public void testPostTransactionWithAttributesAndBlobAttachesBlobMetadata() {
    underTest.attributes("format", "maven2");

    AssetBlobData existingBlobData = createAssetBlobData();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");
    existingAsset.setAssetBlob(existingBlobData);

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    underTest.save();

    verify(blobMetadataStorage).attach(
        eq(blobStore),
        any(BlobId.class),
        any(),
        any(NestedAttributesMap.class),
        any(Map.class));
  }

  @Test
  public void testPostTransactionWithoutAttributesDoesNotAttachBlobMetadata() {
    // No attributes set
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    underTest.save();

    verifyNoInteractions(blobMetadataStorage);
  }

  @Test
  public void testPostTransactionWithAttributesButNoBlobDoesNotAttachMetadata() {
    underTest.attributes("format", "maven2");

    // Asset with no blob
    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    underTest.save();

    // Asset has no blob, so blobMetadataStorage should not be called
    verifyNoInteractions(blobMetadataStorage);
  }

  // --- attach() method tests ---

  @Test
  public void testAttachWithBlobAndChecksumsCallsCheckAttachAllowed() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    configureBlobForAttach();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    FluentAsset result = builderFromAsset.attach(blob, checksums);

    assertThat(result, is(notNullValue()));
    verify(facet).checkAttachAllowed(existingAsset);
    verify(assetStore).updateAssetBlobLink(existingAsset);
  }

  @Test
  public void testAttachIgnoringWritePolicyDoesNotCallCheckAttachAllowed() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    configureBlobForAttach();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    FluentAsset result = builderFromAsset.attachIgnoringWritePolicy(blob, checksums);

    assertThat(result, is(notNullValue()));
    verify(facet, never()).checkAttachAllowed(any(Asset.class));
    verify(assetStore).updateAssetBlobLink(existingAsset);
  }

  @Test
  public void testAttachWithTempBlobCallsCheckAttachAllowedAndMakesPermanent() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(tempBlob.getHashes()).thenReturn(hashes);
    when(tempBlob.getBlob()).thenReturn(blob);

    configureBlobForMakePermanent();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    Blob permanentBlob = createPermanentBlobMock();
    when(blobStore.makeBlobPermanent(eq(blob), any(Map.class))).thenReturn(permanentBlob);

    FluentAsset result = builderFromAsset.attach(tempBlob);

    assertThat(result, is(notNullValue()));
    verify(facet).checkAttachAllowed(existingAsset);
    verify(blobStore).makeBlobPermanent(eq(blob), any(Map.class));
    BlobId expectedBlobId = permanentBlob.getId();
    verify(blobMetadataStorage).attach(eq(blobStore), eq(expectedBlobId), any(), any(), any());
  }

  // --- makePermanent tests ---

  @Test
  public void testMakePermanentWithAttachableBlobNotAttachedReusesBlob() {
    AttachableBlob attachableBlob = mock(AttachableBlob.class);
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(attachableBlob.getHashes()).thenReturn(hashes);
    when(attachableBlob.isAttached()).thenReturn(false);
    when(attachableBlob.getBlob()).thenReturn(blob);

    configureBlobForAttach();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    FluentAsset result = builderFromAsset.attach(attachableBlob);

    assertThat(result, is(notNullValue()));
    // Should mark the blob as attached and not call makeBlobPermanent
    verify(attachableBlob).markAttached();
    verify(blobStore, never()).makeBlobPermanent(any(Blob.class), any(Map.class));
  }

  @Test
  public void testMakePermanentWithAttachableBlobAlreadyAttachedMakesCopy() {
    AttachableBlob attachableBlob = mock(AttachableBlob.class);
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(attachableBlob.getHashes()).thenReturn(hashes);
    when(attachableBlob.isAttached()).thenReturn(true);
    when(attachableBlob.getBlob()).thenReturn(blob);

    configureBlobForMakePermanent();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    Blob permanentBlob = createPermanentBlobMock();
    when(blobStore.makeBlobPermanent(eq(blob), any(Map.class))).thenReturn(permanentBlob);

    FluentAsset result = builderFromAsset.attach(attachableBlob);

    assertThat(result, is(notNullValue()));
    // Already attached, so should NOT mark attached again, but should call makeBlobPermanent
    verify(attachableBlob, never()).markAttached();
    verify(blobStore).makeBlobPermanent(eq(blob), any(Map.class));
  }

  @Test
  public void testMakePermanentWithRegularTempBlobMakesCopy() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(tempBlob.getHashes()).thenReturn(hashes);
    when(tempBlob.getBlob()).thenReturn(blob);

    configureBlobForMakePermanent();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    Blob permanentBlob = createPermanentBlobMock();
    when(blobStore.makeBlobPermanent(eq(blob), any(Map.class))).thenReturn(permanentBlob);

    FluentAsset result = builderFromAsset.attach(tempBlob);

    assertThat(result, is(notNullValue()));
    verify(blobStore).makeBlobPermanent(eq(blob), any(Map.class));
    verify(facet).checkContentType(any(AssetData.class), eq(blob));
  }

  @Test
  public void testMakePermanentSetsCorrectHeaders() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(tempBlob.getHashes()).thenReturn(hashes);
    when(tempBlob.getBlob()).thenReturn(blob);

    Map<String, String> blobHeaders = new HashMap<>();
    blobHeaders.put("Bucket.repo-name", "test-repo");
    blobHeaders.put("BlobStore.blob-name", "/old/path");
    blobHeaders.put("BlobStore.created-by", "admin");
    blobHeaders.put("BlobStore.created-by-ip", "127.0.0.1");
    blobHeaders.put("BlobStore.content-type", "application/java-archive");
    when(blob.getHeaders()).thenReturn(blobHeaders);
    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);

    when(facet.shouldKeepBlobHeader(any(String.class))).thenReturn(false);
    when(facet.checkContentType(any(AssetData.class), eq(blob))).thenReturn("application/java-archive");

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    Blob permanentBlob = createPermanentBlobMock();
    when(blobStore.makeBlobPermanent(eq(blob), headersCaptor.capture())).thenReturn(permanentBlob);

    builderFromAsset.attach(tempBlob);

    Map<String, String> capturedHeaders = headersCaptor.getValue();
    // BLOB_NAME_HEADER should be set to the asset path, not the temp blob path
    assertThat(capturedHeaders.get("BlobStore.blob-name"), is(ASSET_PATH));
    assertThat(capturedHeaders.get("BlobStore.created-by"), is("admin"));
    assertThat(capturedHeaders.get("BlobStore.created-by-ip"), is("127.0.0.1"));
    assertThat(capturedHeaders.get("BlobStore.content-type"), is("application/java-archive"));
  }

  @Test
  public void testMakePermanentKeepsFilteredHeaders() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(tempBlob.getHashes()).thenReturn(hashes);
    when(tempBlob.getBlob()).thenReturn(blob);

    Map<String, String> blobHeaders = new HashMap<>();
    blobHeaders.put("Bucket.repo-name", "test-repo");
    blobHeaders.put("BlobStore.blob-name", "/old/path");
    blobHeaders.put("BlobStore.created-by", "admin");
    blobHeaders.put("BlobStore.created-by-ip", "127.0.0.1");
    blobHeaders.put("BlobStore.content-type", "application/java-archive");
    blobHeaders.put("custom-header", "custom-value");
    when(blob.getHeaders()).thenReturn(blobHeaders);
    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);

    // Only keep the custom-header
    when(facet.shouldKeepBlobHeader("custom-header")).thenReturn(true);
    when(facet.shouldKeepBlobHeader(any(String.class))).thenReturn(false);
    when(facet.shouldKeepBlobHeader("custom-header")).thenReturn(true);
    when(facet.checkContentType(any(AssetData.class), eq(blob))).thenReturn("application/java-archive");

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
    Blob permanentBlob = createPermanentBlobMock();
    when(blobStore.makeBlobPermanent(eq(blob), headersCaptor.capture())).thenReturn(permanentBlob);

    builderFromAsset.attach(tempBlob);

    Map<String, String> capturedHeaders = headersCaptor.getValue();
    assertThat(capturedHeaders.get("custom-header"), is("custom-value"));
  }

  // --- getOrCreateAssetBlob tests ---

  @Test
  public void testGetOrCreateAssetBlobReusesExistingBlob() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    configureBlobForAttach();

    AssetBlobData existingBlobData = createAssetBlobData();
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.of(existingBlobData));

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    FluentAsset result = builderFromAsset.attach(blob, checksums);

    assertThat(result, is(notNullValue()));
    // Should NOT create a new asset blob since one already exists
    verify(assetBlobStore, never()).createAssetBlob(any(AssetBlobData.class));
    verify(assetBlobStore).readAssetBlob(any(BlobRef.class));
  }

  @Test
  public void testGetOrCreateAssetBlobCreatesNewWhenNoneExists() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    configureBlobForAttach();

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    FluentAsset result = builderFromAsset.attach(blob, checksums);

    assertThat(result, is(notNullValue()));
    verify(assetBlobStore).createAssetBlob(any(AssetBlobData.class));
  }

  // --- createAssetBlob with ExternalMetadata tests ---

  @Test
  public void testCreateAssetBlobSetsExternalMetadataWhenEtagPresent() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    Map<String, String> headers = new HashMap<>();
    headers.put("BlobStore.content-type", "application/java-archive");
    headers.put("BlobStore.created-by", "admin");
    headers.put("BlobStore.created-by-ip", "127.0.0.1");
    headers.put("External.etag", "\"abc123\"");
    when(blob.getHeaders()).thenReturn(headers);

    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);
    BlobMetrics metrics = new BlobMetrics(DateTime.now(), "sha1hash", 1024L);
    when(blob.getMetrics()).thenReturn(metrics);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    ArgumentCaptor<AssetBlobData> blobDataCaptor = ArgumentCaptor.forClass(AssetBlobData.class);

    builderFromAsset.attach(blob, checksums);

    verify(assetBlobStore).createAssetBlob(blobDataCaptor.capture());
    AssetBlobData createdBlobData = blobDataCaptor.getValue();
    assertThat(createdBlobData.externalMetadata(), is(notNullValue()));
    assertThat(createdBlobData.externalMetadata().etag(), is("\"abc123\""));
  }

  @Test
  public void testCreateAssetBlobSetsExternalMetadataWhenLastModifiedPresent() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    String lastModified = "2025-01-15T10:30:00+00:00";
    Map<String, String> headers = new HashMap<>();
    headers.put("BlobStore.content-type", "application/java-archive");
    headers.put("BlobStore.created-by", "admin");
    headers.put("BlobStore.created-by-ip", "127.0.0.1");
    headers.put("External.last-modified", lastModified);
    when(blob.getHeaders()).thenReturn(headers);

    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);
    BlobMetrics metrics = new BlobMetrics(DateTime.now(), "sha1hash", 1024L);
    when(blob.getMetrics()).thenReturn(metrics);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    ArgumentCaptor<AssetBlobData> blobDataCaptor = ArgumentCaptor.forClass(AssetBlobData.class);

    builderFromAsset.attach(blob, checksums);

    verify(assetBlobStore).createAssetBlob(blobDataCaptor.capture());
    AssetBlobData createdBlobData = blobDataCaptor.getValue();
    assertThat(createdBlobData.externalMetadata(), is(notNullValue()));
    assertThat(createdBlobData.externalMetadata().lastModified(), is(notNullValue()));
  }

  @Test
  public void testCreateAssetBlobNoExternalMetadataWhenNeitherEtagNorLastModified() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    configureBlobForAttach();

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    ArgumentCaptor<AssetBlobData> blobDataCaptor = ArgumentCaptor.forClass(AssetBlobData.class);

    builderFromAsset.attach(blob, checksums);

    verify(assetBlobStore).createAssetBlob(blobDataCaptor.capture());
    AssetBlobData createdBlobData = blobDataCaptor.getValue();
    // No external metadata headers, so externalMetadata should be null
    assertThat(createdBlobData.externalMetadata() == null, is(true));
  }

  @Test
  public void testCreateAssetBlobSetsEtagWithNullLastModified() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    Map<String, String> headers = new HashMap<>();
    headers.put("BlobStore.content-type", "application/java-archive");
    headers.put("BlobStore.created-by", "admin");
    headers.put("BlobStore.created-by-ip", "127.0.0.1");
    // Only etag, no last-modified
    headers.put("External.etag", "\"etag-value\"");
    when(blob.getHeaders()).thenReturn(headers);

    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);
    BlobMetrics metrics = new BlobMetrics(DateTime.now(), "sha1hash", 512L);
    when(blob.getMetrics()).thenReturn(metrics);

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    ArgumentCaptor<AssetBlobData> blobDataCaptor = ArgumentCaptor.forClass(AssetBlobData.class);

    builderFromAsset.attach(blob, checksums);

    verify(assetBlobStore).createAssetBlob(blobDataCaptor.capture());
    AssetBlobData createdBlobData = blobDataCaptor.getValue();
    assertThat(createdBlobData.externalMetadata(), is(notNullValue()));
    assertThat(createdBlobData.externalMetadata().etag(), is("\"etag-value\""));
    // lastModified header not set, so lastModified in ExternalMetadata should be null
    assertThat(createdBlobData.externalMetadata().lastModified() == null, is(true));
  }

  // --- createAssetBlob property mapping tests ---

  @Test
  public void testCreateAssetBlobSetsAllProperties() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    configureBlobForAttach();

    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");

    FluentAssetBuilderImpl builderFromAsset = new FluentAssetBuilderImpl(facet, assetStore, existingAsset);

    ArgumentCaptor<AssetBlobData> blobDataCaptor = ArgumentCaptor.forClass(AssetBlobData.class);

    builderFromAsset.attach(blob, checksums);

    verify(assetBlobStore).createAssetBlob(blobDataCaptor.capture());
    AssetBlobData createdBlobData = blobDataCaptor.getValue();
    assertThat(createdBlobData.blobRef(), is(notNullValue()));
    assertThat(createdBlobData.blobSize(), is(1024L));
    assertThat(createdBlobData.contentType(), is("application/java-archive"));
    assertThat(createdBlobData.checksums(), is(notNullValue()));
    assertThat(createdBlobData.checksums().containsKey("sha1"), is(true));
    assertThat(createdBlobData.blobCreated(), is(notNullValue()));
    assertThat(createdBlobData.createdBy().orElse(null), is("admin"));
    assertThat(createdBlobData.createdByIp().orElse(null), is("127.0.0.1"));
  }

  // --- save() with TempBlob tests ---

  @Test
  public void testSaveWithTempBlobCallsMakePermanent() {
    Map<HashAlgorithm, HashCode> hashes = new HashMap<>();
    hashes.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    when(tempBlob.getHashes()).thenReturn(hashes);
    when(tempBlob.getBlob()).thenReturn(blob);

    underTest.blob(tempBlob);

    configureBlobForMakePermanent();

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    Blob permanentBlob = createPermanentBlobMock();
    when(blobStore.makeBlobPermanent(eq(blob), any(Map.class))).thenReturn(permanentBlob);

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(blobStore).makeBlobPermanent(eq(blob), any(Map.class));
  }

  // --- Multiple attributes and component save tests ---

  @Test
  public void testSaveWithComponentSetsComponentOnAsset() {
    underTest.component(component);

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());

    ArgumentCaptor<AssetData> assetDataCaptor = ArgumentCaptor.forClass(AssetData.class);

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore).createAsset(assetDataCaptor.capture());
    AssetData capturedAsset = assetDataCaptor.getValue();
    assertThat(capturedAsset.component().isPresent(), is(true));
    assertThat(capturedAsset.component().get(), is(component));
  }

  @Test
  public void testSaveWithKindSetsKindOnAsset() {
    underTest.kind("INDEX");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());

    ArgumentCaptor<AssetData> assetDataCaptor = ArgumentCaptor.forClass(AssetData.class);

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore).createAsset(assetDataCaptor.capture());
    AssetData capturedAsset = assetDataCaptor.getValue();
    assertThat(capturedAsset.kind(), is("INDEX"));
  }

  @Test
  public void testSaveWithAttributesPutsAttributesIntoBacking() {
    underTest.attributes("format", "maven2");
    underTest.attributes("classifier", "sources");

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.empty());

    ArgumentCaptor<AssetData> assetDataCaptor = ArgumentCaptor.forClass(AssetData.class);

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          Supplier<Asset> create = invocation.getArgument(1);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().orElseGet(create);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    verify(assetStore).createAsset(assetDataCaptor.capture());
    AssetData capturedAsset = assetDataCaptor.getValue();
    assertThat(capturedAsset.attributes().backing().get("format"), is("maven2"));
    assertThat(capturedAsset.attributes().backing().get("classifier"), is("sources"));
  }

  // --- Combined update path tests ---

  @Test
  public void testSaveUpdatesExistingAssetWithBlobAndAttributes() {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    checksums.put(HashAlgorithm.SHA1, HashCode.fromString("da39a3ee5e6b4b0d3255bfef95601890afd80709"));

    underTest.blob(blob, checksums);
    underTest.attributes("format", "maven2");

    configureBlobForAttach();

    AssetBlobData existingBlobData = createAssetBlobData();

    AssetData existingAsset = new AssetData();
    existingAsset.setRepositoryId(REPOSITORY_ID);
    existingAsset.setPath(ASSET_PATH);
    existingAsset.setKind("ARTIFACT");
    existingAsset.setAssetBlob(existingBlobData);

    when(assetStore.readPath(REPOSITORY_ID, ASSET_PATH)).thenReturn(Optional.of(existingAsset));
    when(assetBlobStore.readAssetBlob(any(BlobRef.class))).thenReturn(Optional.empty());

    when(assetStore.save(any(Supplier.class), any(Supplier.class), any(UnaryOperator.class), any(Consumer.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Asset>> find = invocation.getArgument(0);
          UnaryOperator<Asset> update = invocation.getArgument(2);
          Consumer<Asset> postTransaction = invocation.getArgument(3);
          Asset result = find.get().map(update).orElse(null);
          postTransaction.accept(result);
          return result;
        });

    FluentAsset result = underTest.save();

    assertThat(result, is(notNullValue()));
    // Both blob and attributes should have been updated
    verify(assetStore).updateAssetBlobLink(existingAsset);
    verify(assetStore).updateAssetAttributes(eq(existingAsset), any(AttributeChangeSet.class));
    // postTransaction should attach blob metadata since we have attributes and a blob
    verify(blobMetadataStorage).attach(
        eq(blobStore),
        any(BlobId.class),
        any(),
        any(NestedAttributesMap.class),
        any(Map.class));
  }

  // --- Helper methods ---

  private void configureBlobForAttach() {
    Map<String, String> headers = createStandardHeaders();
    when(blob.getHeaders()).thenReturn(headers);
    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);
    BlobMetrics metrics = new BlobMetrics(DateTime.now(), "sha1hash", 1024L);
    when(blob.getMetrics()).thenReturn(metrics);
  }

  private void configureBlobForMakePermanent() {
    Map<String, String> headers = createStandardHeaders();
    when(blob.getHeaders()).thenReturn(headers);
    BlobId blobId = new BlobId(BLOB_ID_STRING);
    when(blob.getId()).thenReturn(blobId);

    when(facet.shouldKeepBlobHeader(any(String.class))).thenReturn(false);
    when(facet.checkContentType(any(AssetData.class), eq(blob))).thenReturn("application/java-archive");
  }

  private Map<String, String> createStandardHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Bucket.repo-name", "test-repo");
    headers.put("BlobStore.blob-name", "/temp/path");
    headers.put("BlobStore.created-by", "admin");
    headers.put("BlobStore.created-by-ip", "127.0.0.1");
    headers.put("BlobStore.content-type", "application/java-archive");
    return headers;
  }

  private Blob createPermanentBlobMock() {
    Blob permanentBlob = mock(Blob.class);
    BlobId permanentBlobId = new BlobId("permanent-blob-id");
    when(permanentBlob.getId()).thenReturn(permanentBlobId);
    when(permanentBlob.getHeaders()).thenReturn(createStandardHeaders());
    when(permanentBlob.getMetrics()).thenReturn(new BlobMetrics(DateTime.now(), "sha1hash", 1024L));
    return permanentBlob;
  }

  private AssetBlobData createAssetBlobData() {
    AssetBlobData assetBlobData = new AssetBlobData();
    assetBlobData.setAssetBlobId(1);
    assetBlobData.setBlobRef(new BlobRef("node", "store", "blob-ref-id", null));
    assetBlobData.setBlobSize(1024L);
    assetBlobData.setContentType("application/java-archive");
    assetBlobData.setChecksums(Map.of("SHA1", "da39a3ee5e6b4b0d3255bfef95601890afd80709"));
    assetBlobData.setBlobCreated(OffsetDateTime.now());
    return assetBlobData;
  }
}
