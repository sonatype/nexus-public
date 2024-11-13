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
package org.sonatype.nexus.repository.content.internal;

import java.time.OffsetDateTime;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.handlers.LastDownloadedAttributeHandler;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Update the asset last downloaded time in corresponding blob's '.properties' file.
 */
@Named
@Singleton
public class LastDownloadedAttributePropertyFileHandler
    extends ComponentSupport
    implements LastDownloadedAttributeHandler
{
  private final BlobStoreManager blobStoreManager;

  @Inject
  public LastDownloadedAttributePropertyFileHandler(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  public void writeLastDownloadedAttribute(final FluentAsset asset) {
    FluentAsset reloadedAsset = reloadAsset(asset);
    if (reloadedAsset != null && reloadedAsset.lastDownloaded().isPresent()) {
      updateLastDownloadedPropertyIfNeeded(reloadedAsset);
    }
  }

  @Nullable
  @Override
  public OffsetDateTime readLastDownloadedAttribute(final String blobstore, final Blob blob) {
    BlobStore blobStore = blobStoreManager.get(blobstore);
    if (blobStore == null) {
      log.warn("Blob store not loaded {}", blobStore);
      return null;
    }

    BlobAttributes blobAttributes = blobStore.getBlobAttributes(blob.getId());
    if (blobAttributes == null) {
      log.warn("Blob attributes not loaded for {}", blob.getId());
      return null;
    }
    return Optional.ofNullable(blobAttributes.getMetrics())
        .map(BlobMetrics::getLastDownloaded)
        .orElse(null);
  }

  private FluentAsset reloadAsset(final FluentAsset asset) {
    try {
      ContentFacet contentFacet = asset.repository().facet(ContentFacet.class);
      return contentFacet.assets()
          .path(asset.path())
          .find()
          .orElse(null);
    }
    catch (MissingFacetException e) {
      log.warn("There is no content facet for asset {} in repository {}", asset, asset.repository());
      return null;
    }
  }

  private void updateLastDownloadedPropertyIfNeeded(final FluentAsset fluentAsset) {
    fluentAsset.blob().ifPresent(assetBlob -> {
      BlobStore blobStore = getBlobStoreForAsset(assetBlob);
      if (blobStore == null) {
        log.warn("Could not find blobstore for {}", assetBlob);
        return;
      }

      BlobId blobId = assetBlob.blobRef().getBlobId();
      BlobAttributes blobAttributes = blobStore.getBlobAttributes(blobId);
      if (blobAttributes == null) {
        log.warn("Could not get blob attributes for {}", blobId);
        return;
      }

      fluentAsset.lastDownloaded().ifPresent(lastDownloaded -> {
        blobAttributes.getMetrics().setLastDownloaded(lastDownloaded);
        blobStore.setBlobAttributes(blobId, blobAttributes);
      });
    });
  }

  private BlobStore getBlobStoreForAsset(final AssetBlob assetBlob) {
    return blobStoreManager.get(assetBlob.blobRef().getStore());
  }
}
