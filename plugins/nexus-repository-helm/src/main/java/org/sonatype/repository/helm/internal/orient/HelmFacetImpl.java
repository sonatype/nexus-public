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
package org.sonatype.repository.helm.internal.orient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.Query.Builder;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.HelmFormat;

import com.google.common.base.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_COMPONENT;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;
import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;

/**
 * {@link HelmFacet} implementation.
 *
 * @since 3.next
 */
@Named
public class HelmFacetImpl
    extends FacetSupport
    implements HelmFacet
{
  @Override
  public Asset findOrCreateAsset(
      final StorageTx tx,
      final String assetPath,
      final AssetKind assetKind,
      final HelmAttributes helmAttributes)
  {
    Optional<Asset> assetOpt = findAsset(tx, assetPath);
    return assetOpt.orElseGet(() ->
        createAsset(tx, assetPath, helmAttributes, assetKind));
  }

  private Asset createAsset(
      final StorageTx tx,
      final String assetPath,
      final HelmAttributes helmAttributes,
      final AssetKind assetKind)
  {
    checkNotNull(assetKind);

    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = CacheControllerHolder.METADATA.equals(assetKind.getCacheType())
        ? tx.createAsset(bucket, getRepository().getFormat())
        : tx.createAsset(bucket, findOrCreateComponent(tx, bucket, helmAttributes.getName(), helmAttributes.getVersion()));
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
    helmAttributes.populate(asset.formatAttributes());
    asset.name(assetPath);
    tx.saveAsset(asset);
    return asset;
  }

  private Component findOrCreateComponent(final StorageTx tx,
                                          final Bucket bucket,
                                          final String name,
                                          final String version)
  {
    Optional<Component> componentOpt = findComponent(tx, name, version);
    if (!componentOpt.isPresent()) {
      Component component = tx.createComponent(bucket, getRepository().getFormat())
          .name(name)
          .version(version);
      tx.saveComponent(component);
      return component;
    }
    return componentOpt.get();
  }

  /**
   * Find a component by its name and tag (version)
   *
   * @return found Optional<component> or Optional.empty if not found
   */
  private Optional<Component> findComponent(final StorageTx tx, final String name, final String version)
  {
    Query query = builder()
        .where(P_NAME).eq(name)
        .and(P_VERSION).eq(version)
        .build();
    return StreamSupport.stream(tx.findComponents(query, singletonList(getRepository())).spliterator(), false)
        .findFirst();
  }

  /**
   * Find assets for Helm components by assetKind
   *
   * @return found assets or null if not found
   */
  @Nullable
  public Iterable<Asset> browseComponentAssets(final StorageTx tx, @Nullable final AssetKind assetKind)
  {
    Builder builder = builder()
        .where(P_COMPONENT).isNotNull();
    if (assetKind != null) {
      builder.and(P_ATTRIBUTES + "." + HelmFormat.NAME + "." + P_ASSET_KIND).eq(assetKind.name());
    }

    Query query = builder
        .build();
    Bucket bucket = tx.findBucket(getRepository());
    return tx.browseAssets(query, bucket);
  }

  /**
   * Find an asset by its name.
   *
   * @return found Optional<Asset> or Optional.empty if not found
   */
  @Override
  public Optional<Asset> findAsset(final StorageTx tx, final String assetName) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, assetName, bucket);
    return Optional.ofNullable(asset);
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  @Override
  @Nullable
  public Content saveAsset(final StorageTx tx,
                            final Asset asset,
                            final Supplier<InputStream> contentSupplier,
                            final Payload payload)
  {
    try {
      if (payload instanceof Content) {
        AttributesMap contentAttributes = ((Content) payload).getAttributes();
        String contentType = payload.getContentType();
        return saveAsset(tx, asset, contentSupplier, contentType, contentAttributes);
      }
      return saveAsset(tx, asset, contentSupplier, null, null);
    }
    catch (IOException ex) {
      log.warn("Could not set blob {}", ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  @Override
  public Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final Supplier<InputStream> contentSupplier,
                           @Nullable final String contentType,
                           @Nullable final AttributesMap contentAttributes) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    AssetBlob assetBlob = tx.setBlob(
        asset, asset.name(), contentSupplier, HASH_ALGORITHMS, null, contentType, false
    );
    asset.markAsDownloaded();
    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Convert an asset blob to {@link Content}.
   *
   * @return content of asset blob
   */
  public Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }
}
