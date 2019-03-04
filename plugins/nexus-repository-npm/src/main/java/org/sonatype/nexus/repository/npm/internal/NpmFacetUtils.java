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
package org.sonatype.nexus.repository.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;
import org.sonatype.nexus.thread.io.StreamCopier;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.metadata.schema.OType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Maps.newHashMap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.TARBALL;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.npm.internal.NpmJsonUtils.serialize;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Shared code of npm facets.
 *
 * Payloads being stored and their mapping:
 *
 * npm package metadata (JSON)
 * Component: none
 * Asset: N = NpmPackageId.id()
 *
 * npm tarball (binary)
 * Component: G = NpmPackageId.scope(), N = NpmPackageId.name(), V = version that tarball belongs to
 * Asset: N = NpmPackage.id() + "/-/" + tarballName (see #tarballAssetName)
 *
 * @since 3.0
 */
public final class NpmFacetUtils
{
  private NpmFacetUtils() {
    // nop
  }

  private static final String SQL_FIND_ALL_PACKAGE_NAMES = String
      .format("SELECT DISTINCT(%s) AS %s FROM asset WHERE %s = :bucketRid AND %s.%s.%s = :kind ORDER BY name",
          P_NAME,
          P_NAME,
          P_BUCKET,
          P_ATTRIBUTES,
          NpmFormat.NAME,
          P_ASSET_KIND
      );

  public static final List<HashAlgorithm> HASH_ALGORITHMS = Lists.newArrayList(SHA1);

  public static final String REPOSITORY_ROOT_ASSET = "-/all";

  public static final String REPOSITORY_SEARCH_ASSET = "-/v1/search";

  /**
   * Parses JSON content into map.
   */
  @Nonnull
  static NestedAttributesMap parse(final Supplier<InputStream> streamSupplier) throws IOException {
    return NpmJsonUtils.parse(streamSupplier);
  }

  /**
   * Creates an {@link AssetBlob} out of passed in content and attaches it to passed in {@link Asset}.
   */
  @Nonnull
  static AssetBlob storeContent(final StorageTx tx,
                                final Asset asset,
                                final Supplier<InputStream> content,
                                final AssetKind assetKind) throws IOException
  {
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());

    final AssetBlob result = tx.createBlob(
        asset.name(),
        content,
        HASH_ALGORITHMS,
        null,
        assetKind.getContentType(),
        assetKind.isSkipContentVerification()
    );
    tx.attachBlob(asset, result);
    return result;
  }

  /**
   * Creates an {@link AssetBlob} out of passed in temporary blob and attaches it to passed in {@link Asset}.
   */
  @Nonnull
  static AssetBlob storeContent(final StorageTx tx,
                                final Asset asset,
                                final TempBlob tempBlob,
                                final AssetKind assetKind) throws IOException
  {
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
    AssetBlob result = tx.createBlob(
        asset.name(),
        tempBlob,
        null,
        assetKind.getContentType(),
        assetKind.isSkipContentVerification()
    );
    tx.attachBlob(asset, result);
    return result;
  }

  /**
   * Convert an asset blob to {@link Content}.
   *
   * @return content of asset blob
   */
  public static Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Convert an {@link Asset} representing a package root to a {@link Content} via a {@link StreamPayload}.
   *
   * @param repository              {@link Repository} to look up package root from.
   * @param packageRootAsset        {@link Asset} associated with blob holding package root.
   * @return Content of asset blob
   */
  public static NpmContent toContent(final Repository repository, final Asset packageRootAsset)
  {
    NpmContent content = new NpmContent(toPayload(repository, packageRootAsset));
    Content.extractFromAsset(packageRootAsset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Build a {@link NpmStreamPayload} out of the {@link InputStream} representing the package root.
   *
   * @param repository              {@link Repository} to look up package root from.
   * @param packageRootAsset        {@link Asset} associated with blob holding package root.
   */
  public static NpmStreamPayload toPayload(final Repository repository,
                                           final Asset packageRootAsset)
  {
    return new NpmStreamPayload(loadPackageRoot(repository, packageRootAsset));
  }

  /**
   * Save repository root asset & create blob from an input stream.
   *
   * @return blob content
   */
  @Nonnull
  public static Content saveRepositoryRoot(final StorageTx tx,
                                           final Asset asset,
                                           final Supplier<InputStream> contentSupplier,
                                           final Content content) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, content.getAttributes()));
    final AssetBlob assetBlob = storeContent(tx, asset, contentSupplier, AssetKind.REPOSITORY_ROOT);
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Save repository root asset & create blob from a temporary blob.
   *
   * @return blob content
   */
  @Nonnull
  static Content saveRepositoryRoot(final StorageTx tx,
                                    final Asset asset,
                                    final TempBlob tempBlob,
                                    final Content content) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, content.getAttributes()));
    AssetBlob assetBlob = storeContent(tx, asset, tempBlob, AssetKind.REPOSITORY_ROOT);
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Formats an asset name for a tarball out of package name and tarball filename.
   */
  @Nonnull
  static String tarballAssetName(final NpmPackageId packageId, final String tarballName) {
    return packageId.id() + "/-/" + tarballName;
  }

  /**
   * Builds query builder for {@link Component} based on passed in {@link NpmPackageId}.
   */
  @Nonnull
  private static Query.Builder query(final NpmPackageId packageId) {
    if (packageId.scope() != null) {
      return Query.builder().where(P_NAME).eq(packageId.name()).and(P_GROUP).eq(packageId.scope());
    }
    else {
      return Query.builder().where(P_NAME).eq(packageId.name()).and(P_GROUP).isNull();
    }
  }

  /**
   * Find all tarball component by package name in repository.
   */
  @Nonnull
  static Iterable<Component> findPackageTarballComponents(final StorageTx tx,
                                                          final Repository repository,
                                                          final NpmPackageId packageId)
  {
    return tx.findComponents(query(packageId).build(), singletonList(repository));
  }

  /**
   * Find a tarball component by package name and version in repository.
   */
  @Nullable
  static Component findPackageTarballComponent(final StorageTx tx,
                                               final Repository repository,
                                               final NpmPackageId packageId,
                                               final String version)
  {
    Iterable<Component> components = tx.findComponents(
        query(packageId)
            .and(P_VERSION).eq(version)
            .build(),
        singletonList(repository)
    );
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  /**
   * Find a repository root asset by package name in repository.
   */
  @Nullable
  public static Asset findRepositoryRootAsset(final StorageTx tx, final Bucket bucket) {
    return tx.findAssetWithProperty(P_NAME, REPOSITORY_ROOT_ASSET, bucket);
  }

  /**
   * Find a package root asset by package name in repository.
   */
  @Nullable
  public static Asset findPackageRootAsset(final StorageTx tx,
                                           final Bucket bucket,
                                           final NpmPackageId packageId)
  {
    return tx.findAssetWithProperty(P_NAME, packageId.id(), bucket);
  }

  /**
   * Find a tarball asset by package name and tarball filename in repository.
   */
  @Nullable
  static Asset findTarballAsset(final StorageTx tx,
                                final Bucket bucket,
                                final NpmPackageId packageId,
                                final String tarballName)
  {
    return tx.findAssetWithProperty(P_NAME, tarballAssetName(packageId, tarballName), bucket);
  }

  /**
   * Returns iterable that contains all the package names that exists in repository. Optional filtering possible with
   * nullable {@code modifiedSince} timestamp, that will return package names modified since.
   */
  @Nonnull
  public static Iterable<NpmPackageId> findAllPackageNames(final StorageTx tx,
                                                           final Bucket bucket)
  {
    Map<String, Object> sqlParams = new HashMap<>();
    sqlParams.put("bucketRid", AttachedEntityHelper.id(bucket));
    sqlParams.put("kind", AssetKind.PACKAGE_ROOT);
    return Iterables.transform(
        tx.browse(SQL_FIND_ALL_PACKAGE_NAMES, sqlParams),
        input -> NpmPackageId.parse(input.<String>field(P_NAME, OType.STRING))
    );
  }

  /**
   * Returns the package root JSON content by reading up package root asset's blob and parsing it. It also decorates
   * the JSON document with some fields.
   */
  public static NestedAttributesMap loadPackageRoot(final StorageTx tx,
                                                    final Asset packageRootAsset) throws IOException
  {
    final Blob blob = tx.requireBlob(packageRootAsset.requireBlobRef());
    NestedAttributesMap metadata = NpmJsonUtils.parse(() -> blob.getInputStream());
    // add _id
    metadata.set(NpmMetadataUtils.META_ID, packageRootAsset.name());
    return metadata;
  }

  /**
   * Returns a {@link Supplier} that will get the {@link InputStream} for the package root associated with the given
   * {@link Asset}.
   *
   * return {@link InputStreamSupplier}
   */
  public static InputStreamSupplier loadPackageRoot(final Repository repository,
                                                    final Asset packageRootAsset)
  {
    return () -> packageRootAssetToInputStream(repository, packageRootAsset);
  }

  /**
   * Returns a new {@link InputStream} that returns an error object. Mostly useful for NPM Responses that have already
   * been written with a successful status (like a 200) but just before streaming out content found an issue preventing
   * the intended content to be streamed out.
   *
   * @return InputStream
   */
  public static InputStream errorInputStream(final String message) {
    NestedAttributesMap errorObject = new NestedAttributesMap("error", newHashMap());
    errorObject.set("success", false);
    errorObject.set("error", "Failed to stream response due to: " + message);
    return new ByteArrayInputStream(NpmJsonUtils.bytes(errorObject));
  }

  /**
   * Saves the package root JSON content by persisting content into root asset's blob. It also removes some transient
   * fields from JSON document.
   */
  static void savePackageRoot(final StorageTx tx,
                              final Asset packageRootAsset,
                              final NestedAttributesMap packageRoot) throws IOException
  {
    packageRoot.remove(NpmMetadataUtils.META_ID);
    packageRoot.remove("_attachments");
    packageRootAsset.formatAttributes().set(
        NpmAttributes.P_NPM_LAST_MODIFIED, NpmMetadataUtils.maintainTime(packageRoot).toDate()
    );
    storeContent(
        tx,
        packageRootAsset,
        new StreamCopier<Supplier<InputStream>>(
            outputStream -> serialize(new OutputStreamWriter(outputStream, UTF_8), packageRoot),
            inputStream -> () -> inputStream).read(),
        AssetKind.PACKAGE_ROOT
    );
    tx.saveAsset(packageRootAsset);
  }

  /**
   * Deletes the package root and all related tarballs too.
   */
  static Set<String> deletePackageRoot(final StorageTx tx,
                                   final Repository repository,
                                   final NpmPackageId packageId,
                                   final boolean deleteBlobs)
  {
    // find package asset -> delete
    Asset packageRootAsset = findPackageRootAsset(tx, tx.findBucket(repository), packageId);
    if (packageRootAsset == null) {
      return Collections.emptySet();
    }
    tx.deleteAsset(packageRootAsset, deleteBlobs);
    // find all tarball components -> delete
    Iterable<Component> npmTarballs = findPackageTarballComponents(tx, repository, packageId);
    Set<String> deletedAssetNames = new HashSet<>();
    for (Component npmTarball : npmTarballs) {
      deletedAssetNames.addAll(tx.deleteComponent(npmTarball, deleteBlobs));
    }
    return deletedAssetNames;
  }

  /**
   * Returns the tarball content.
   */
  @Nullable
  static Content getTarballContent(final StorageTx tx,
                                   final Bucket bucket,
                                   final NpmPackageId packageId,
                                   final String tarballName)
  {
    Asset asset = findTarballAsset(tx, bucket, packageId, tarballName);
    if (asset == null) {
      return null;
    }

    Blob blob = tx.requireBlob(asset.requireBlobRef());
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Gets or creates a {@link Component} for npm package tarball.
   */
  @Nonnull
  static Component getOrCreateTarballComponent(final StorageTx tx,
                                               final Repository repository,
                                               final NpmPackageId packageId,
                                               final String version)
  {
    Component tarballComponent = findPackageTarballComponent(tx, repository, packageId, version);
    if (tarballComponent == null) {
      tarballComponent = tx.createComponent(tx.findBucket(repository), repository.getFormat())
          .group(packageId.scope())
          .name(packageId.name())
          .version(version);
      tx.saveComponent(tarballComponent);
    }
    return tarballComponent;
  }

  /**
   * Creates an {@code AssetBlob} from a tarball's {@code TempBlob}.
   *
   * @since 3.7
   */
  static AssetBlob createTarballAssetBlob(final StorageTx tx,
                                          final NpmPackageId packageId,
                                          final String tarballName,
                                          final TempBlob tempBlob) throws IOException
  {
    return tx.createBlob(
        tarballAssetName(packageId, tarballName),
        tempBlob,
        null,
        TARBALL.getContentType(),
        TARBALL.isSkipContentVerification()
    );
  }

  private static InputStream packageRootAssetToInputStream(final Repository repository, final Asset packageRootAsset) {
    BlobStore blobStore = repository.facet(StorageFacet.class).blobStore();
    if (isNull(blobStore)) {
      throw new MissingAssetBlobException(packageRootAsset);
    }

    BlobRef blobRef = packageRootAsset.requireBlobRef();
    Blob blob = blobStore.get(blobRef.getBlobId());
    if (isNull(blob)) {
      throw new MissingAssetBlobException(packageRootAsset);
    }

    try {
      return blob.getInputStream();
    } catch (BlobStoreException ignore) { // NOSONAR
      // we want any issue with the blob store stream to be caught during the getting of the input stream as throw the
      // the same type of exception as a missing asset blob, so that we can pass the associated asset around.
      throw new MissingAssetBlobException(packageRootAsset);
    }
  }
}
