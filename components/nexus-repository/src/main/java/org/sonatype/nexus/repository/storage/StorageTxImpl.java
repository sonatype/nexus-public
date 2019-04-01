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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuard;
import org.sonatype.nexus.common.stateguard.StateGuardAware;
import org.sonatype.nexus.common.stateguard.Transitions;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.transaction.RetryController;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.isDownloading;
import static org.sonatype.nexus.repository.storage.Asset.CHECKSUM;
import static org.sonatype.nexus.repository.storage.Asset.HASHES_NOT_VERIFIED;
import static org.sonatype.nexus.repository.storage.Asset.PROVENANCE;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageTxImpl.State.ACTIVE;
import static org.sonatype.nexus.repository.storage.StorageTxImpl.State.CLOSED;
import static org.sonatype.nexus.repository.storage.StorageTxImpl.State.OPEN;

/**
 * Default {@link StorageTx} implementation.
 *
 * @since 3.0
 */
public class StorageTxImpl
    implements StorageTx, StateGuardAware
{
  private static final Logger log = LoggerFactory.getLogger(StorageTxImpl.class);

  private final String createdBy;

  private final String createdByIp;

  private final BlobTx blobTx;

  private final ODatabaseDocumentTx db;

  private final String repositoryName;

  private final WritePolicy writePolicy;

  private final WritePolicySelector writePolicySelector;

  private final StateGuard stateGuard = new StateGuard.Builder().initial(OPEN).logger(log).create();

  private final BucketEntityAdapter bucketEntityAdapter;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final boolean strictContentValidation;

  private final ContentValidator contentValidator;

  private final MimeRulesSource mimeRulesSource;

  private final ComponentFactory componentFactory;

  private int retries = 0;

  public StorageTxImpl(final String createdBy,
                       final String createdByIp,
                       final BlobTx blobTx,
                       final ODatabaseDocumentTx db,
                       final String repositoryName,
                       final WritePolicy writePolicy,
                       final WritePolicySelector writePolicySelector,
                       final BucketEntityAdapter bucketEntityAdapter,
                       final ComponentEntityAdapter componentEntityAdapter,
                       final AssetEntityAdapter assetEntityAdapter,
                       final boolean strictContentValidation,
                       final ContentValidator contentValidator,
                       final MimeRulesSource mimeRulesSource,
                       final ComponentFactory componentFactory)
  {
    this.createdBy = checkNotNull(createdBy);
    this.createdByIp = checkNotNull(createdByIp);
    this.blobTx = checkNotNull(blobTx);
    this.db = checkNotNull(db);
    this.repositoryName = checkNotNull(repositoryName);
    this.writePolicy = checkNotNull(writePolicy);
    this.writePolicySelector = checkNotNull(writePolicySelector);
    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.strictContentValidation = strictContentValidation;
    this.contentValidator = checkNotNull(contentValidator);
    this.mimeRulesSource = checkNotNull(mimeRulesSource);
    this.componentFactory = checkNotNull(componentFactory);

    // This is only here for now to yell in case of nested TX
    // To be discussed in future, or at the point when we will have need for nested TX
    // Note: orient DB sports some rudimentary support for nested TXes
    checkArgument(!db.getTransaction().isActive(), "Nested DB TX!");
  }

  public static final class State
  {
    public static final String OPEN = "OPEN";

    public static final String ACTIVE = "ACTIVE";

    public static final String CLOSED = "CLOSED";
  }

  @Override
  @Nonnull
  public StateGuard getStateGuard() {
    return stateGuard;
  }

  @Override
  @Transitions(from = OPEN, to = ACTIVE)
  public void begin() {
    db.begin(TXTYPE.OPTIMISTIC);
  }

  @Override
  @Transitions(from = ACTIVE, to = OPEN, silent = true)
  public void commit() {
    db.commit(); // this must happen _before_ we commit any blob changes
    blobTx.commit();
    retries = 0;
  }

  @Override
  @Transitions(from = ACTIVE, to = OPEN)
  public void rollback() {
    db.rollback();
    blobTx.rollback();
  }

  @Override
  public boolean isActive() {
    return ACTIVE.equals(stateGuard.getCurrent());
  }

  /**
   * Custom retry strategy that throws {@link RetryDeniedException} when retry limit is breached.
   */
  @Override
  public boolean allowRetry(final Exception cause) throws RetryDeniedException {
    if (RetryController.INSTANCE.allowRetry(retries, cause)) {
      retries++;
      return true;
    }
    else {
      throw new RetryDeniedException("Exceeded retry limit", cause);
    }
  }

  @Override
  @Transitions(from = {OPEN, ACTIVE}, to = CLOSED)
  public void close() {
    // If the transaction has not been committed, then we roll back.
    if (ACTIVE.equals(stateGuard.getCurrent())) {
      rollback();
    }

    db.close(); // rolls back and releases ODatabaseDocumentTx to pool
  }

  @Override
  @Guarded(by = {ACTIVE})
  public ODatabaseDocumentTx getDb() {
    return db;
  }

  @Override
  @Guarded(by = {ACTIVE})
  public Iterable<ODocument> browse(final String selectSql, @Nullable final Map<String, Object> params) {
    if (Strings2.isBlank(selectSql)) {
      return Collections.emptyList();
    }
    else {
      return OrientAsyncHelper.asyncIterable(db, selectSql, params);
    }
  }

  @Override
  @Guarded(by = {ACTIVE})
  public Iterable<ODocument> browse(final String selectSql,
                                    @Nullable final Map<String, Object> params,
                                    final int bufferSize,
                                    final long timeoutSeconds)
  {
    if (Strings2.isBlank(selectSql)) {
      return Collections.emptyList();
    }
    else {
      return OrientAsyncHelper.asyncIterable(db, selectSql, params, bufferSize, timeoutSeconds);
    }
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Bucket findBucket(final Repository repository) {
    return bucketOf(repository.getName());
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Bucket> findBuckets(final Iterable<Repository> repositories) {
    return bucketsOf(repositories);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Bucket> browseBuckets() {
    return bucketEntityAdapter.browse(db);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Asset> browseAssets(final Bucket bucket) {
    return assetEntityAdapter.browseByBucket(db, bucket);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Asset> browseAssets(final Component component) {
    return assetEntityAdapter.browseByComponent(db, component);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Asset> browseAssets(final Query query, final Bucket bucket) {
    return assetEntityAdapter.browseByQueryAsync(db, query.getWhere(), query.getParameters(), ImmutableList.of(bucket), 
        query.getQuerySuffix());
  }

  @Override
  @Guarded(by = ACTIVE)
  public Asset firstAsset(final Component component) {
    return Iterables.getFirst(browseAssets(component), null);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Component> browseComponents(final Bucket bucket) {
    return componentEntityAdapter.browseByBucket(db, bucket);
  }

  @Override
  public Iterable<Component> browseComponents(final Query query, final Bucket bucket) {
    return componentEntityAdapter.browseByQueryAsync(db, query.getWhere(), query.getParameters(), 
        ImmutableList.of(bucket), query.getQuerySuffix());
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Asset findAsset(final EntityId id, final Bucket bucket) {
    checkNotNull(id);
    checkNotNull(bucket);
    Asset asset = assetEntityAdapter.read(db, id);
    return bucketOwns(bucket, asset) ? asset : null;
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Asset findAsset(final EntityId id) {
    checkNotNull(id);
    return assetEntityAdapter.read(db, id);
  }

  private boolean bucketOwns(final Bucket bucket, @Nullable final MetadataNode<?> item) {
    return item != null && Objects.equals(id(bucket), item.bucketId());
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Asset findAssetWithProperty(final String propName, final Object propValue, final Bucket bucket) {
    return assetEntityAdapter.findByProperty(db, propName, propValue, bucket);
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Asset findAssetWithProperty(final String propName, final Object propValue) {
    return assetEntityAdapter.findByProperty(db, propName, propValue);
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Asset findAssetWithProperty(final String propName, final Object propValue, final Component component) {
    return assetEntityAdapter.findByProperty(db, propName, propValue, component);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Asset> findAssets(@Nullable String whereClause,
                                    @Nullable Map<String, Object> parameters,
                                    @Nullable Iterable<Repository> repositories,
                                    @Nullable String querySuffix)
  {
    return assetEntityAdapter.browseByQuery(db, whereClause, parameters, bucketsOf(repositories), querySuffix);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Asset> findAssets(final Query query, @Nullable final Iterable<Repository> repositories) {
    return findAssets(query.getWhere(), query.getParameters(), repositories, query.getQuerySuffix());
  }

  @Override
  @Guarded(by = ACTIVE)
  public long countAssets(@Nullable String whereClause,
                          @Nullable Map<String, Object> parameters,
                          @Nullable Iterable<Repository> repositories,
                          @Nullable String querySuffix)
  {
    return assetEntityAdapter.countByQuery(db, whereClause, parameters, bucketsOf(repositories), querySuffix);
  }

  @Override
  @Guarded(by = ACTIVE)
  public long countAssets(final Query query, @Nullable final Iterable<Repository> repositories) {
    return countAssets(query.getWhere(), query.getParameters(), repositories, query.getQuerySuffix());
  }

  @Override
  @Guarded(by = ACTIVE)
  public boolean componentExists(@Nullable final String group,
                                 final String name,
                                 @Nullable final String version,
                                 final Repository repository)
  {
    return componentEntityAdapter.exists(db, group, name, version, bucketOf(repository.getName()));
  }

  @Override
  @Guarded(by = ACTIVE)
  public boolean assetExists(final String name,
                             final Repository repository)
  {
    checkNotNull(name);
    checkNotNull(repository);

    return assetEntityAdapter.exists(db, name, bucketOf(repository.getName()));
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Component findComponentInBucket(final EntityId id, final Bucket bucket) {
    checkNotNull(id);
    checkNotNull(bucket);
    Component component = findComponent(id);
    return bucketOwns(bucket, component) ? component : null;
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Component findComponent(final EntityId id) {
    checkNotNull(id);
    return componentEntityAdapter.read(db, id);
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Component findComponentWithProperty(final String propName, final Object propValue, final Bucket bucket) {
    return componentEntityAdapter.findByProperty(db, propName, propValue, bucket);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Component> findComponents(@Nullable String whereClause,
                                            @Nullable Map<String, Object> parameters,
                                            @Nullable Iterable<Repository> repositories,
                                            @Nullable String querySuffix)
  {
    return componentEntityAdapter.browseByQuery(db, whereClause, parameters, bucketsOf(repositories), querySuffix);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Component> findComponents(final Query query, @Nullable final Iterable<Repository> repositories) {
    return findComponents(query.getWhere(), query.getParameters(), repositories, query.getQuerySuffix());
  }

  @Override
  @Guarded(by = ACTIVE)
  public Iterable<Component> findComponentsByNameCaseInsensitive(final String name,
                                                                 @Nullable final Iterable<Repository> repositories,
                                                                 @Nullable final String querySuffix)
  {
    return componentEntityAdapter.browseByNameCaseInsensitive(db, name, bucketsOf(repositories), querySuffix);
  }

  @Override
  @Guarded(by = ACTIVE)
  public long countComponents(@Nullable String whereClause,
                              @Nullable Map<String, Object> parameters,
                              @Nullable Iterable<Repository> repositories,
                              @Nullable String querySuffix)
  {
    return componentEntityAdapter.countByQuery(db, whereClause, parameters, bucketsOf(repositories), querySuffix);
  }

  @Override
  @Guarded(by = ACTIVE)
  public long countComponents(final Query query, @Nullable final Iterable<Repository> repositories) {
    return countComponents(query.getWhere(), query.getParameters(), repositories, query.getQuerySuffix());
  }

  @Override
  @Guarded(by = ACTIVE)
  public Asset createAsset(final Bucket bucket, final Format format) {
    checkNotNull(format);
    return createAsset(bucket, format.toString());
  }

  private Asset createAsset(final Bucket bucket, final String format) {
    checkNotNull(bucket);
    Asset asset = new Asset();
    asset.bucketId(id(bucket));
    asset.format(format);
    asset.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    if (isDownloading()) {
      asset.lastDownloaded(DateTime.now());
    }
    return asset;
  }

  @Override
  @Guarded(by = ACTIVE)
  public Asset createAsset(final Bucket bucket, final Component component) {
    checkNotNull(component);
    Asset asset = createAsset(bucket, component.format());
    asset.componentId(id(component));
    return asset;
  }

  @Override
  @Guarded(by = ACTIVE)
  public Component createComponent(final Bucket bucket, final Format format) {
    checkNotNull(bucket);
    checkNotNull(format);

    Component component = componentFactory.createComponent();
    component.bucketId(id(bucket));
    component.format(format.toString());
    component.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    return component;
  }

  @Override
  @Guarded(by = ACTIVE)
  public void saveBucket(final Bucket bucket) {
    bucketEntityAdapter.editEntity(db, bucket);
  }

  @Override
  @Guarded(by = ACTIVE)
  public void saveComponent(final Component component) {
    if (EntityHelper.hasMetadata(component)) {
      componentEntityAdapter.editEntity(db, component);
    }
    else {
      componentEntityAdapter.addEntity(db, component);
    }
  }

  @Override
  @Guarded(by = ACTIVE)
  public void saveAsset(final Asset asset) {
    if (EntityHelper.hasMetadata(asset)) {
      assetEntityAdapter.editEntity(db, asset);
    }
    else {
      assetEntityAdapter.addEntity(db, asset);
    }
  }

  @Override
  @Guarded(by = ACTIVE)
  public Set<String> deleteComponent(final Component component) {
    return deleteComponent(component, true);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Set<String> deleteComponent(final Component component, final boolean deleteBlobs) {
    return deleteComponent(component, true, deleteBlobs);
  }

  private Set<String> deleteComponent(final Component component, final boolean checkWritePolicy, final boolean deleteBlobs) {
    checkNotNull(component);

    Set<String> deletedAssets = new HashSet<>();

    for (Asset asset : browseAssets(component)) {
      deleteAsset(asset, checkWritePolicy ? writePolicySelector.select(asset, writePolicy) : null, deleteBlobs);
      deletedAssets.add(asset.name());
    }
    componentEntityAdapter.deleteEntity(db, component);
    return deletedAssets;
  }

  @Override
  @Guarded(by = ACTIVE)
  public void deleteAsset(final Asset asset) {
    deleteAsset(asset, true);
  }

  @Override
  @Guarded(by = ACTIVE)
  public void deleteAsset(final Asset asset, final boolean deleteBlob) {
    deleteAsset(asset, writePolicySelector.select(asset, writePolicy), deleteBlob);
  }

  private void deleteAsset(final Asset asset,
                           @Nullable final WritePolicy effectiveWritePolicy,
                           final boolean deleteBlob)
  {
    checkNotNull(asset);

    if (deleteBlob) {
      BlobRef blobRef = asset.blobRef();
      if (blobRef != null) {
        deleteBlob(blobRef, effectiveWritePolicy, format("Deleting asset %s", EntityHelper.id(asset)));
      }
    }
    assetEntityAdapter.deleteEntity(db, asset);
  }

  @Override
  @Guarded(by = ACTIVE)
  public AssetBlob createBlob(final String blobName,
                              final Supplier<InputStream> streamSupplier,
                              final Iterable<HashAlgorithm> hashAlgorithms,
                              @Nullable final Map<String, String> headers,
                              @Nullable final String declaredContentType,
                              final boolean skipContentVerification) throws IOException
  {
    checkNotNull(blobName);
    checkNotNull(streamSupplier);
    checkNotNull(hashAlgorithms);

    if (!writePolicy.checkCreateAllowed()) {
      throw new IllegalOperationException("Repository is read only: " + repositoryName);
    }

    Map<String, String> storageHeadersMap = buildStorageHeaders(blobName, streamSupplier, headers, declaredContentType,
        skipContentVerification);
    return blobTx.create(
        streamSupplier.get(),
        storageHeadersMap,
        hashAlgorithms,
        storageHeadersMap.get(BlobStore.CONTENT_TYPE_HEADER)
    );
  }

  @Override
  @Guarded(by = ACTIVE)
  public AssetBlob createBlob(final String blobName,
                              final Path sourceFile,
                              final Map<HashAlgorithm, HashCode> hashes,
                              @Nullable final Map<String, String> headers,
                              final String declaredContentType,
                              final long size) throws IOException
  {
    checkNotNull(blobName);
    checkNotNull(sourceFile);
    checkNotNull(hashes);
    checkArgument(!Strings2.isBlank(declaredContentType), "no declaredContentType provided");

    if (!writePolicy.checkCreateAllowed()) {
      throw new IllegalOperationException("Repository is read only: " + repositoryName);
    }

    Map<String, String> storageHeaders = buildStorageHeaders(blobName, null, headers, declaredContentType, true);
    return blobTx.createByHardLinking(
        sourceFile,
        storageHeaders,
        hashes,
        declaredContentType,
        size
    );
  }

  @Override
  public AssetBlob createBlob(final String blobName,
                              final TempBlob originalBlob,
                              @Nullable final Map<String, String> headers,
                              @Nullable final String declaredContentType,
                              boolean skipContentVerification)
      throws IOException
  {
    checkNotNull(blobName);
    checkNotNull(originalBlob);

    if (!writePolicy.checkCreateAllowed()) {
      throw new IllegalOperationException("Repository is read only: " + repositoryName);
    }

    Map<String, String> storageHeadersMap = buildStorageHeaders(blobName, originalBlob, headers, declaredContentType,
        skipContentVerification);
    return blobTx.createByCopying(
        originalBlob.getBlob().getId(),
        storageHeadersMap,
        originalBlob.getHashes(),
        originalBlob.getHashesVerified()
    );
  }

  private Map<String, String> buildStorageHeaders(final String blobName,
                                                  @Nullable final Supplier<InputStream> streamSupplier,
                                                  @Nullable final Map<String, String> headers,
                                                  @Nullable final String declaredContentType,
                                                  final boolean skipContentVerification) throws IOException
  {
    checkArgument(
        !skipContentVerification || !Strings2.isBlank(declaredContentType),
        "skipContentVerification set true but no declaredContentType provided"
    );
    Builder<String, String> storageHeaders = ImmutableMap.builder();
    storageHeaders.put(Bucket.REPO_NAME_HEADER, repositoryName);
    storageHeaders.put(BlobStore.BLOB_NAME_HEADER, blobName);
    storageHeaders.put(BlobStore.CREATED_BY_HEADER, createdBy);
    storageHeaders.put(BlobStore.CREATED_BY_IP_HEADER, createdByIp);
    if (!skipContentVerification) {
      storageHeaders.put(
          BlobStore.CONTENT_TYPE_HEADER,
          determineContentType(streamSupplier, blobName, declaredContentType)
      );
    }
    else {
      storageHeaders.put(BlobStore.CONTENT_TYPE_HEADER, declaredContentType);
    }
    if (headers != null) {
      storageHeaders.putAll(headers);
    }
    return storageHeaders.build();
  }

  @Override
  @Guarded(by = ACTIVE)
  public void attachBlob(final Asset asset, final AssetBlob assetBlob)
  {
    checkNotNull(asset);
    checkNotNull(assetBlob);
    checkArgument(!assetBlob.isAttached(), "Blob is already attached to an asset");

    final WritePolicy effectiveWritePolicy = writePolicySelector.select(asset, writePolicy);
    if (!effectiveWritePolicy.checkCreateAllowed()) {
      throw new IllegalOperationException("Repository is read only: " + repositoryName);
    }

    NestedAttributesMap checksums = asset.attributes().child(CHECKSUM);

    if (!isDuplicateBlob(asset, assetBlob, effectiveWritePolicy, checksums)) {
      maybeDeleteBlob(asset, effectiveWritePolicy);

      asset.blobRef(assetBlob.getBlobRef());
      asset.size(assetBlob.getSize());
      asset.contentType(assetBlob.getContentType());

      // Set attributes map to contain computed checksum metadata
      for (Entry<HashAlgorithm, HashCode> entry : assetBlob.getHashes().entrySet()) {
        HashAlgorithm algorithm = entry.getKey();
        HashCode checksum = entry.getValue();
        checksums.set(algorithm.name(), checksum.toString());
      }

      // Mark assets whose checksums were not verified locally, for possible later verification
      NestedAttributesMap provenance = asset.attributes().child(PROVENANCE);
      provenance.set(HASHES_NOT_VERIFIED, !assetBlob.getHashesVerified());

      Map<String, String> blobHeaders = assetBlob.getBlob().getHeaders();
      if (blobHeaders.containsKey(BlobStore.CREATED_BY_HEADER)) {
        asset.createdBy(blobHeaders.get(BlobStore.CREATED_BY_HEADER));
      }
      if (blobHeaders.containsKey(BlobStore.CREATED_BY_IP_HEADER)) {
        asset.createdByIp(blobHeaders.get(BlobStore.CREATED_BY_IP_HEADER));
      }

      assetBlob.setAttached(true);
    }
  }

  /**
   * Returns {@code true} when at least one incoming hash has the same algorithm as an existing checksum.
   */
  private boolean checksumExists(final NestedAttributesMap checksums, final AssetBlob assetBlob) {
    if (!checksums.isEmpty()) {
      for (HashAlgorithm algorithm : assetBlob.getHashes().keySet()) {
        if (checksums.contains(algorithm.name())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns {@code true} when incoming hashes all match existing checksums.
   */
  private boolean compareChecksums(final NestedAttributesMap checksums, final AssetBlob assetBlob) {
    for (Entry<HashAlgorithm, HashCode> entry : assetBlob.getHashes().entrySet()) {
      HashAlgorithm algorithm = entry.getKey();
      HashCode checksum = entry.getValue();
      if (!checksum.toString().equals(checksums.get(algorithm.name()))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether incoming blob is a duplicate of existing asset blob; if so it re-uses the old blob.
   */
  private boolean isDuplicateBlob(final Asset asset,
                                  final AssetBlob assetBlob,
                                  final WritePolicy effectiveWritePolicy,
                                  final NestedAttributesMap checksums)
  {
    if (asset.blobRef() != null) {
      Blob oldBlob = blobTx.get(asset.blobRef());
      if (oldBlob != null) {
        boolean checksumsMatch;
        if (assetBlob.getHashesVerified() && checksumExists(checksums, assetBlob)) {
          // we have verified hashes, use those to avoid touching blob metrics
          checksumsMatch = compareChecksums(checksums, assetBlob);
        }
        else {
          // fall back to blob metrics, which involves fetching the new blob
          String oldBlobSha1 = oldBlob.getMetrics().getSha1Hash();
          String newBlobSha1 = assetBlob.getBlob().getMetrics().getSha1Hash();
          checksumsMatch = oldBlobSha1.equalsIgnoreCase(newBlobSha1);
        }
        if (checksumsMatch) {
          // still respect write policy even when de-duplicating
          if (!effectiveWritePolicy.checkUpdateAllowed()) {
            throw new IllegalOperationException("Repository does not allow updating assets: " + repositoryName);
          }

          assetBlob.setDuplicate(oldBlob);
          
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   * Deletes the existing blob for the asset if one exists, updating the blob updated field if necessary. The
   * write policy will be enforced for this operation and will throw an exception if updates are not supported.
   */
  private void maybeDeleteBlob(final Asset asset, final WritePolicy effectiveWritePolicy)
  {
    DateTime now = DateTime.now();
    if (asset.blobRef() != null) {
      // updating old blob
      if (!effectiveWritePolicy.checkUpdateAllowed()) {
        throw new IllegalOperationException("Repository does not allow updating assets: " + repositoryName);
      }
      asset.blobUpdated(now);
      deleteBlob(asset.blobRef(), effectiveWritePolicy, format("Updating asset %s", EntityHelper.id(asset)));
    }
    else {
      // creating new blob
      asset.blobCreated(now);
      asset.blobUpdated(now);
    }
  }

  @Override
  @Guarded(by = ACTIVE)
  public AssetBlob setBlob(final Asset asset,
                           final String blobName,
                           final Supplier<InputStream> streamSupplier,
                           final Iterable<HashAlgorithm> hashAlgorithms,
                           @Nullable final Map<String, String> headers,
                           @Nullable final String declaredContentType,
                           final boolean skipContentVerification) throws IOException
  {
    checkNotNull(asset);

    // Enforce write policy ahead, as we have asset here
    BlobRef oldBlobRef = asset.blobRef();
    if (oldBlobRef != null) {
      if (!writePolicySelector.select(asset, writePolicy).checkUpdateAllowed()) {
        throw new IllegalOperationException("Repository does not allow updating assets: " + repositoryName);
      }
    }
    final AssetBlob assetBlob = createBlob(
        blobName,
        streamSupplier,
        hashAlgorithms,
        headers,
        declaredContentType,
        skipContentVerification
    );
    attachBlob(asset, assetBlob);
    return assetBlob;
  }

  @Override
  @Guarded(by = ACTIVE)
  public AssetBlob setBlob(final Asset asset,
                           final String blobName,
                           final Path sourceFile,
                           final Map<HashAlgorithm, HashCode> hashes,
                           @Nullable final Map<String, String> headers,
                           final String declaredContentType,
                           final long size) throws IOException
  {
    checkNotNull(asset);
    checkArgument(!Strings2.isBlank(declaredContentType), "no declaredContentType provided");

    // Enforce write policy ahead, as we have asset here
    BlobRef oldBlobRef = asset.blobRef();
    if (oldBlobRef != null) {
      if (!writePolicySelector.select(asset, writePolicy).checkUpdateAllowed()) {
        throw new IllegalOperationException( "Repository does not allow updating assets: " + repositoryName);
      }
    }
    final AssetBlob assetBlob = createBlob(
        blobName,
        sourceFile,
        hashes,
        headers,
        declaredContentType,
        size
    );
    attachBlob(asset, assetBlob);
    return assetBlob;
  }

  @Override
  public AssetBlob setBlob(final Asset asset,
                           final String blobName,
                           final TempBlob originalBlob,
                           @Nullable final Map<String, String> headers,
                           @Nullable String declaredContentType,
                           boolean skipContentVerification)
      throws IOException
  {
    checkNotNull(blobName);
    checkNotNull(originalBlob);

    // Enforce write policy ahead, as we have asset here
    BlobRef oldBlobRef = asset.blobRef();
    if (oldBlobRef != null && !writePolicySelector.select(asset, writePolicy).checkUpdateAllowed()) {
      throw new IllegalOperationException("Repository does not allow updating assets: " + repositoryName);
    }
    AssetBlob assetBlob = createBlob(blobName, originalBlob, headers, declaredContentType, skipContentVerification);
    attachBlob(asset, assetBlob);
    return assetBlob;
  }

  @Nullable
  @Override
  @Guarded(by = ACTIVE)
  public Blob getBlob(final BlobRef blobRef) {
    checkNotNull(blobRef);

    return blobTx.get(blobRef);
  }

  @Override
  @Guarded(by = ACTIVE)
  public Blob requireBlob(final BlobRef blobRef) {
    Blob blob = getBlob(blobRef);
    if (blob == null) {
      throw new MissingBlobException(blobRef);
    }
    return blob;
  }

  @Nonnull
  private String determineContentType(final Supplier<InputStream> inputStreamSupplier,
                                      final String blobName,
                                      @Nullable final String declaredContentType)
      throws IOException
  {
    try {
      return contentValidator.determineContentType(
          strictContentValidation,
          inputStreamSupplier,
          mimeRulesSource,
          blobName,
          declaredContentType
      );
    }
    catch (InvalidContentException e) {
      log.warn(
          "An exception occurred determining the content type of asset {} in repository {}", blobName, repositoryName);
      throw e;
    }
  }

  /**
   * Deletes a blob w/ enforcing {@link WritePolicy} if not {@code null}. otherwise write policy will NOT be checked.
   */
  private void deleteBlob(final BlobRef blobRef, @Nullable WritePolicy effectiveWritePolicy, final String reason) {
    checkNotNull(blobRef);
    if (effectiveWritePolicy != null && !effectiveWritePolicy.checkDeleteAllowed()) {
      throw new IllegalOperationException("Repository does not allow deleting assets: " + repositoryName);
    }
    blobTx.delete(blobRef, reason);
  }

  /**
   * Returns the {@link Bucket} associated with given repository.
   */
  @Nullable
  private Bucket bucketOf(final String repositoryName) {
    return bucketEntityAdapter.read(db, repositoryName);
  }

  /**
   * Returns the {@link Bucket}s associated with the given repositories.
   */
  @Nullable
  private Iterable<Bucket> bucketsOf(@Nullable final Iterable<Repository> repositories) {
    if (repositories == null) {
      return null;
    }
    ImmutableList.Builder<Bucket> bucketsBuilder = ImmutableList.builder();
    for (Repository repository : repositories) {
      bucketsBuilder.add(bucketOf(repository.getName()));
    }
    return bucketsBuilder.build();
  }
}
