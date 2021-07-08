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
package org.sonatype.nexus.repository.p2.orient.internal;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.p2.internal.util.P2PathUtils;
import org.sonatype.nexus.repository.p2.orient.P2Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.p2.orient.internal.util.OrientP2PathUtils.PLUGIN_NAME;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * {@link P2Facet} implementation
 *
 * @since 3.28
 */
@Named
@Priority(Integer.MAX_VALUE)
public class OrientP2Facet
    extends FacetSupport
    implements P2Facet
{
  public static final Collection<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1);

  @Override
  public Component findOrCreateComponent(final StorageTx tx, final P2Attributes attributes) {
    String name = attributes.getComponentName();
    String version = attributes.getComponentVersion();

    Component component = findComponent(tx, getRepository(), name, version);
    if (component == null) {
      Bucket bucket = tx.findBucket(getRepository());
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(name)
          .version(version);
      if (attributes.getPluginName() != null) {
        component.formatAttributes().set(PLUGIN_NAME, attributes.getPluginName());
      }

      tx.saveComponent(component);
    }

    return component;
  }

  @TransactionalStoreBlob
  @Override
  public Content doCreateOrSaveComponent(final P2Attributes p2Attributes,
                                         final TempBlob componentContent,
                                         final Payload payload,
                                         final AssetKind assetKind) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Component component = findOrCreateComponent(tx, p2Attributes);

    Asset asset = findAsset(tx, bucket, p2Attributes.getPath());
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(p2Attributes.getPath());
      //add human readable plugin or feature name in asset attributes
      asset.formatAttributes().set(PLUGIN_NAME,  p2Attributes.getPluginName());
      asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
    }
    return saveAsset(tx, asset, componentContent, payload);
  }

  @Override
  public Asset findOrCreateAsset(final StorageTx tx,
                                 final Component component,
                                 final String path,
                                 final P2Attributes attributes)
  {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);

      asset.formatAttributes().set(PLUGIN_NAME, attributes.getPluginName());
      asset.formatAttributes().set(P_ASSET_KIND, attributes.getAssetKind());
      tx.saveAsset(asset);
    }

    return asset;
  }

  @Override
  public Asset findOrCreateAsset(final StorageTx tx, final String path) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(path);
      asset.formatAttributes().set(P_ASSET_KIND, getAssetKind(path).name());
      tx.saveAsset(asset);
    }

    return asset;
  }

  /**
   * Find a component by its name and tag (version)
   *
   * @return found component of null if not found
   */
  @Nullable
  public Component findComponent(final StorageTx tx,
                                 final Repository repository,
                                 final String name,
                                 final String version)
  {
    Iterable<Component> components = tx.findComponents(
        Query.builder()
            .where(P_NAME).eq(name)
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
   * Find an asset by its name.
   *
   * @return found asset or null if not found
   */
  @Nullable
  @Override
  public Asset findAsset(final StorageTx tx, final Bucket bucket, final String assetName) {
    return tx.findAssetWithProperty(MetadataNodeEntityAdapter.P_NAME, assetName, bucket);
  }

  /**
   * Save an asset && create blob.
   *
   * @return blob content
   */
  @Override
  public Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final TempBlob contentSupplier,
                           final Payload payload) throws IOException
  {
    AttributesMap contentAttributes = null;
    String contentType = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
      contentType = payload.getContentType();
    }
    return saveAsset(tx, asset, contentSupplier, contentType, contentAttributes);
  }

  /**
   * Save an asset && create blob.
   *
   * @return blob content
   */
  public Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final InputStreamSupplier contentSupplier,
                           final String contentType,
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
  @Override
  public Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  @Override
  public AssetKind getAssetKind(final String path) {
    return P2PathUtils.getAssetKind(path);
  }
}
