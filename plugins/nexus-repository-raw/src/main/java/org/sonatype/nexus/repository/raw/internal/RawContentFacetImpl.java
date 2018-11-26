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
package org.sonatype.nexus.repository.raw.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.raw.RawContentFacet;
import org.sonatype.nexus.repository.raw.RawCoordinatesHelper;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * A {@link RawContentFacet} that persists to a {@link StorageFacet}.
 *
 * @since 3.0
 */
@Named
public class RawContentFacetImpl
    extends FacetSupport
    implements RawContentFacet
{
  private static final List<HashAlgorithm> hashAlgorithms = Arrays.asList(MD5, SHA1);

  private final AssetEntityAdapter assetEntityAdapter;

  @Inject
  public RawContentFacetImpl(final AssetEntityAdapter assetEntityAdapter) {
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
  }

  // TODO: raw does not have config, this method is here only to have this bundle do Import-Package org.sonatype.nexus.repository.config
  // TODO: as FacetSupport subclass depends on it. Actually, this facet does not need any kind of configuration
  // TODO: it's here only to circumvent this OSGi/maven-bundle-plugin issue.
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    // empty
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content get(final String path) {
    StorageTx tx = UnitOfWork.currentTx();

    final Asset asset = findAsset(tx, path);
    if (asset == null) {
      return null;
    }

    final Blob blob = tx.requireBlob(asset.requireBlobRef());
    return toContent(asset, blob);
  }

  @Override
  public Content put(final String path, final Payload content) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content, hashAlgorithms)) {
      return doPutContent(path, tempBlob, content);
    }
  }

  @Override
  @TransactionalStoreBlob
  public Asset put(final String path, final AssetBlob assetBlob, @Nullable final AttributesMap contentAttributes) {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = getOrCreateAsset(getRepository(), path, RawCoordinatesHelper.getGroup(path), path);
    tx.attachBlob(asset, assetBlob);
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    tx.saveAsset(asset);
    return asset;
  }

  @TransactionalStoreBlob
  protected Content doPutContent(final String path, final TempBlob tempBlob, final Payload payload)
      throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = getOrCreateAsset(getRepository(), path, RawCoordinatesHelper.getGroup(path), path);

    AttributesMap contentAttributes = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
    }
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    AssetBlob assetBlob = tx.setBlob(
        asset,
        path,
        tempBlob,
        null,
        payload.getContentType(),
        false
    );

    tx.saveAsset(asset);

    return toContent(asset, assetBlob.getBlob());
  }

  @TransactionalStoreMetadata
  public Asset getOrCreateAsset(final Repository repository, final String componentName, final String componentGroup,
                                final String assetName) {
    final StorageTx tx = UnitOfWork.currentTx();

    final Bucket bucket = tx.findBucket(getRepository());
    Component component = tx.findComponentWithProperty(P_NAME, componentName, bucket);
    Asset asset;
    if (component == null) {
      // CREATE
      component = tx.createComponent(bucket, getRepository().getFormat())
          .group(componentGroup)
          .name(componentName);

      tx.saveComponent(component);

      asset = tx.createAsset(bucket, component);
      asset.name(assetName);
    }
    else {
      // UPDATE
      asset = tx.firstAsset(component);
      asset = asset != null ? asset : tx.createAsset(bucket, component).name(assetName);
    }

    return asset;
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    final Component component = findComponent(tx, tx.findBucket(getRepository()), path);
    if (component == null) {
      return false;
    }

    tx.deleteComponent(component);
    return true;
  }

  @Override
  @TransactionalTouchMetadata
  public void setCacheInfo(final String path, final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    // by EntityId
    Asset asset = Content.findAsset(tx, bucket, content);
    if (asset == null) {
      // by format coordinates
      Component component = tx.findComponentWithProperty(P_NAME, path, bucket);
      if (component != null) {
        asset = tx.firstAsset(component);
      }
    }
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent raw component {}", path);
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", path, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  @Transactional
  public boolean assetExists(final String name) {
    StorageTx tx = UnitOfWork.currentTx();
    return assetEntityAdapter.exists(tx.getDb(), name, tx.findBucket(getRepository()));
  }

  private Component findComponent(StorageTx tx, Bucket bucket, String path) {
    return tx.findComponentWithProperty(P_NAME, path, bucket);
  }

  private Asset findAsset(StorageTx tx, String path) {
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
  }

  private Content toContent(final Asset asset, final Blob blob) {
    final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, hashAlgorithms, content.getAttributes());
    return content;
  }
}
