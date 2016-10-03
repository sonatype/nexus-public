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
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.transaction.Transaction;

import com.google.common.base.Supplier;
import com.google.common.hash.HashCode;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * A storage transaction.
 *
 * @since 3.0
 */
public interface StorageTx
    extends Transaction
{
  /**
   * Begins the transaction.
   */
  void begin();

  /**
   * Commits the transaction.
   */
  void commit();

  /**
   * Rolls back the transaction.
   */
  void rollback();

  /**
   * Closes the transaction. Uncommitted changes will be lost, and the object will be ineligible for further use.
   */
  void close();

  /**
   * Provides the underlying document transaction.
   *
   * Note: The caller may use this to directly query or manipulate the Orient document DB, if needed, but should not
   * directly commit, rollback, or close the underlying transaction.
   */
  ODatabaseDocumentTx getDb();

  /**
   * A lower level but still safe access to SQL SELECT queries, that allows caller to "browse" potentially
   * huge result sets without worry about paging or OOMs. This method will NOT see uncommited changes performed
   * in this current TX, if any. The returned {@link Iterable} may throw {@link RuntimeException} if timeout to
   * receive new elements is breached.
   *
   * @param selectSql The SELECT SQL in full, optionally with named parameters.
   * @param params    The map holding named parameters of SELECT SQL.
   * @return Iterable of SELECTed {@link ODocument}s.
   * @see OrientAsyncHelper
   */
  Iterable<ODocument> browse(String selectSql, @Nullable Map<String, Object> params);

  /**
   * Finds a bucket based on the repository.
   */
  @Nullable
  Bucket findBucket(Repository repository);

  /**
   * Finds buckets based on the repositories.
   */
  @Nullable
  Iterable<Bucket> findBuckets(final Iterable<Repository> repositories);

  /**
   * Gets all buckets.
   */
  Iterable<Bucket> browseBuckets();

  /**
   * Gets all assets owned by the specified bucket. This method will NOT see unsommited changes performed in this
   * same TX, if any. The returned {@link Iterable} may throw {@link RuntimeException} if timeout to
   * receive new elements is breached.
   *
   * @see OrientAsyncHelper
   */
  Iterable<Asset> browseAssets(Bucket bucket);

  /**
   * Gets all assets owned by the specified component.
   */
  Iterable<Asset> browseAssets(Component component);

  /**
   * Gets first asset owned by the specified component.
   */
  Asset firstAsset(Component component);

  /**
   * Gets all components owned by the specified bucket. This method will NOT see unsommited changes performed in this
   * same TX, if any. The returned {@link Iterable} may throw {@link RuntimeException} if timeout to
   * receive new elements is breached.
   *
   * @see OrientAsyncHelper
   */
  Iterable<Component> browseComponents(Bucket bucket);

  /**
   * Gets an asset by id, owned by the specified bucket, or {@code null} if not found.
   */
  @Nullable
  Asset findAsset(EntityId id, Bucket bucket);

  /**
   * Gets an asset by some identifying property, owned by the specified bucket, or {@code null} if not found.
   */
  @Nullable
  Asset findAssetWithProperty(String propName, Object propValue, Bucket bucket);

  /**
   * Gets an asset by some identifying property, owned by the specified component, or {@code null} if not found.
   */
  @Nullable
  Asset findAssetWithProperty(String propName, Object propValue, Component component);

  /**
   * Gets all assets in the specified repositories that match the given where clause.
   *
   * @param whereClause  an OrientDB select query, minus the "select from X where " prefix. Rather than passing values
   *                     in directly, they should be specified as :labeled portions of the where clause (e.g. a =
   *                     :aVal).
   * @param parameters   the name-value pairs specifying the values for any :labeled portions of the where clause.
   * @param repositories the repositories to limit the results to. If null or empty, results won't be limited
   *                     by repository.
   * @param querySuffix  the part of the query after the where clause, which may by used for ordering and paging
   *                     as per the OrientDB select query syntax.
   * @see <a href="http://orientdb.com/docs/last/SQL-Query.html">OrientDB SELECT Query Documentation</a>
   */
  Iterable<Asset> findAssets(@Nullable String whereClause,
                             @Nullable Map<String, Object> parameters,
                             @Nullable Iterable<Repository> repositories,
                             @Nullable String querySuffix);

  /**
   * Gets all assets in the specified repositories that match the given where clause.
   *
   * @param query        an {@link Query} query.
   * @param repositories the repositories to limit the results to. If null or empty, results won't be limited
   *                     by repository (unless passed in query limits already).
   */
  Iterable<Asset> findAssets(Query query, @Nullable Iterable<Repository> repositories);

  /**
   * Gets the number of assets matching the given where clause.
   */
  long countAssets(@Nullable String whereClause,
                   @Nullable Map<String, Object> parameters,
                   @Nullable Iterable<Repository> repositories,
                   @Nullable String querySuffix);

  /**
   * Gets the number of assets matching the given {@link Query} clause.
   */
  long countAssets(Query query, @Nullable Iterable<Repository> repositories);

  /**
   * Gets a component by id, owned by the specified bucket, or {@code null} if not found.
   */
  @Nullable
  Component findComponentInBucket(EntityId id, Bucket bucket);

  /**
   * Gets a component by id, regardless of which bucket it resides in, or {@code null} if not found.
   */
  @Nullable
  Component findComponent(EntityId id);

  /**
   * Gets a component by some identifying property, or {@code null} if not found.
   */
  @Nullable
  Component findComponentWithProperty(String propName, Object propValue, Bucket bucket);

  /**
   * Gets all component in the specified repositories that match the given where clause.
   *
   * @param whereClause  an OrientDB query, minus the "select from X where " prefix. Rather than passing values
   *                     in directly, they should be specified as :labeled portions of the where clause (e.g. a =
   *                     :aVal).
   * @param parameters   the name-value pairs specifying the values for any :labeled portions of the where clause.
   * @param repositories the repositories to limit the results to. If null or empty, results won't be limited
   *                     by repository.
   * @param querySuffix  the part of the query after the where clause, which may by used for ordering and paging
   *                     as per the OrientDB select query syntax.
   * @see <a href="http://orientdb.com/docs/last/SQL-Query.html">OrientDB SELECT Query Documentation</a>
   */
  Iterable<Component> findComponents(@Nullable String whereClause,
                                     @Nullable Map<String, Object> parameters,
                                     @Nullable Iterable<Repository> repositories,
                                     @Nullable String querySuffix);

  /**
   * Gets all component in the specified repositories that match the given where clause.
   *
   * @param query        an {@link Query} query.
   * @param repositories the repositories to limit the results to. If null or empty, results won't be limited
   *                     by repository (unless passed in query limits already).
   */
  Iterable<Component> findComponents(Query query, @Nullable Iterable<Repository> repositories);

  /**
   * Gets the number of components matching the given where clause.
   */
  long countComponents(@Nullable String whereClause,
                       @Nullable Map<String, Object> parameters,
                       @Nullable Iterable<Repository> repositories,
                       @Nullable String querySuffix);

  /**
   * Gets the number of components matching the given {@link Query} clause.
   */
  long countComponents(Query query, @Nullable Iterable<Repository> repositories);

  /**
   * Creates a new standalone asset.
   */
  Asset createAsset(Bucket bucket, Format format);

  /**
   * Creates a new asset that belongs to a component.
   */
  Asset createAsset(Bucket bucket, Component component);

  /**
   * Creates a new component.
   */
  Component createComponent(Bucket bucket, Format format);

  /**
   * Updates an existing bucket.
   */
  void saveBucket(Bucket bucket);

  /**
   * Updates an existing component.
   */
  void saveComponent(Component component);

  /**
   * Updates an existing asset.
   */
  void saveAsset(Asset asset);

  /**
   * Deletes an existing component and all constituent assets.
   */
  void deleteComponent(Component component);

  /**
   * Deletes an existing asset and requests the blob to be deleted.
   */
  void deleteAsset(Asset asset);

  /**
   * Deletes an existing bucket and all components and assets within.
   *
   * NOTE: This is a potentially long-lived and non-atomic operation. Items within the bucket will be
   * sequentially deleted in batches in order to keep memory use within reason. This method will automatically
   * commit a transaction for each batch, and will return after committing the last batch.
   */
  void deleteBucket(Bucket bucket);

  /**
   * Creates a new Blob and updates the given asset with a reference to it, hash metadata, size, and content type.
   * The old blob, if any, will be deleted.
   *
   * @param asset                   the {@link Asset} that should reference newly created blob.
   * @param blobName                blob name (may not be unique), but it will be used also in content validation.
   *                                See {@link ContentValidator}.
   * @param streamSupplier          the content being stored as a blob
   * @param hashAlgorithms          {@link HashAlgorithm}s to be applied while streaming to blob store, will be set
   *                                in {@link Asset} attributes.
   * @param headers                 optional, custom headers for blob, if any.
   * @param declaredContentType     optional, the declared MIME type of the blob. See {@link ContentValidator}.
   * @param skipContentVerification if {@code false}, {@link ContentValidator} will be used to determine or verify
   *                                the content type of content. If {@code true}, the {@code declaredContentType}
   *                                will be used as-is and must be non-{@code null}. Use {@code true} in cases when
   *                                content to be saved is known to be what it is declared to, ie. caller already
   *                                parsed it or processed in any other way that proved it's format/type.
   * @return Attached instance of {@link AssetBlob} of the newly created blob. As side effect, passed in {@link Asset}
   * is modified too, but is unsaved. Caller must ensure {@link #saveAsset(Asset)} is invoked before this TX ends.
   */
  AssetBlob setBlob(Asset asset,
                    String blobName,
                    final Supplier<InputStream> streamSupplier,
                    Iterable<HashAlgorithm> hashAlgorithms,
                    @Nullable Map<String, String> headers,
                    @Nullable String declaredContentType,
                    boolean skipContentVerification) throws IOException;

  /**
   * Creates a new Blob by hard linking to {@code sourceFile} and updates the given asset with a reference to it,
   * hash metadata, size, and content type. The old blob, if any, will be deleted.
   *
   * @param asset                   the {@link Asset} that should reference newly created blob.
   * @param blobName                blob name (may not be unique), but it will be used also in content validation.
   *                                See {@link ContentValidator}.
   * @param sourceFile              the source file path of the content being stored as a blob
   * @param hashes                  precalculated {@link HashAlgorithm}s and {@link HashCode}s for {@link AssetBlob}.
   * @param headers                 optional, custom headers for blob, if any.
   * @param declaredContentType     the declared MIME type of the blob. See {@link ContentValidator}.
   * @param size                    precalculated size for the blob
   * @return Attached instance of {@link AssetBlob} of the newly created blob. As side effect, passed in {@link Asset}
   * is modified too, but is unsaved. Caller must ensure {@link #saveAsset(Asset)} is invoked before this TX ends.
   */
  AssetBlob setBlob(Asset asset,
                    String blobName,
                    Path sourceFile,
                    Map<HashAlgorithm, HashCode> hashes,
                    @Nullable Map<String, String> headers,
                    String declaredContentType,
                    long size) throws IOException;

  /**
   * Creates a new Blob and returns its {@link AssetBlob}. Blobs created but not attached in a scope of a TX to any
   * asset are considered as "orphans", and they will be deleted from blob store at the end of a TX.
   *
   * @param blobName                blob name (may not be unique), but it will be used also in content validation. See
   *                                {@link ContentValidator}.
   * @param streamSupplier          the content to be streamed into blob store.
   * @param hashAlgorithms          {@link HashAlgorithm}s to be applied while streaming to blob store, returned in
   *                                {@link
   *                                AssetBlob}.
   * @param headers                 optional, custom headers for blob, if any.
   * @param declaredContentType     optional, the declared MIME type of the blob. See {@link ContentValidator}.
   * @param skipContentVerification if {@code false}, {@link ContentValidator} will be used to determine or verify
   *                                the content type of content. If {@code true}, the {@code declaredContentType}
   *                                will be used as-is and must be non-{@code null}. Use {@code true} in cases when
   *                                content to be saved is known to be what it is declared to, ie. caller already
   *                                parsed it or processed in any other way that proved it's format/type.
   * @return Unattached instance of {@link AssetBlob} that should be attached to some {@link Asset} during this
   * transaction using {@link #attachBlob(Asset, AssetBlob)} method.
   */
  AssetBlob createBlob(String blobName,
                       final Supplier<InputStream> streamSupplier,
                       Iterable<HashAlgorithm> hashAlgorithms,
                       @Nullable Map<String, String> headers,
                       @Nullable String declaredContentType,
                       boolean skipContentVerification) throws IOException;

  /**
   * Creates a new Blob by hard linking to {@code sourceFile} and returns its {@link AssetBlob}. Blobs created but not
   * attached in a scope of a TX to any asset are considered as "orphans", and they will be deleted from blob store at
   * the end of a TX.
   *
   * @param blobName                blob name (may not be unique), but it will be used also in content validation. See
   *                                {@link ContentValidator}.
   * @param sourceFile              the source file path of the content to be included in blob store.
   * @param hashes                  precalculated {@link HashAlgorithm}s and {@link HashCode}s for {@link AssetBlob}.
   * @param headers                 optional, custom headers for blob, if any.
   * @param declaredContentType     the declared MIME type of the blob. See {@link ContentValidator}.
   * @param size                    precalculated size for the blob
   * @return Unattached instance of {@link AssetBlob} that should be attached to some {@link Asset} during this
   * transaction using {@link #attachBlob(Asset, AssetBlob)} method.
   */
  AssetBlob createBlob(String blobName,
                       Path sourceFile,
                       Map<HashAlgorithm, HashCode> hashes,
                       @Nullable Map<String, String> headers,
                       String declaredContentType,
                       long size) throws IOException;

  /**
   * Attaches a Blob to asset and updates the given asset with a reference to it, hash metadata, size, and content
   * type. The asset's old blob, if any, will be deleted.
   */
  void attachBlob(Asset asset, AssetBlob assetBlob);

  /**
   * Gets a Blob, or {@code null if not found}.
   */
  @Nullable
  Blob getBlob(BlobRef blobRef);

  /**
   * Gets a Blob, or throws an {@code IllegalStateException} if it doesn't exist.
   */
  Blob requireBlob(BlobRef blobRef);
}
