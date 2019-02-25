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
package org.sonatype.nexus.repository.npm.internal.search.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.npm.internal.NpmFacetUtils;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetManager;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.joda.time.DateTime;

import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.findRepositoryRootAsset;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.saveRepositoryRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.toContent;

/**
 * npm search index facet for repository types that do their own caching of index document (currently all except
 * proxies).
 *
 * @since 3.0
 * @deprecated No longer actively used by npm upstream, replaced by v1 search api (NEXUS-13150).
 */
@Deprecated
public abstract class NpmSearchIndexFacetCaching
    extends FacetSupport
    implements NpmSearchIndexFacet
{
  private final EventManager eventManager;

  private final AssetManager assetManager;

  protected NpmSearchIndexFacetCaching(final EventManager eventManager,
                                       final AssetManager assetManager) {
    this.eventManager = eventManager;
    this.assetManager = assetManager;
  }

  /**
   * Fetches the cached index document, or, if not present, builds index document, caches it and sends it as response.
   */
  @Nonnull
  public Content searchIndex(@Nullable final DateTime since) throws IOException {
    // NOTE: This has been split up into separate calls to realize different transactional behavior:
    // First attempt restricted to cache, tolerating frozen database ...
    Content searchIndex = getCachedSearchIndex();
    if (searchIndex == null) {
      // ... second attempt allowed to create the index, requiring unfrozen database though
      searchIndex = getSearchIndex();
    }
    return NpmSearchIndexFilter.filterModifiedSince(searchIndex, since);
  }

  @Nullable
  @TransactionalTouchBlob
  protected Content getCachedSearchIndex() throws IOException {
    return getSearchIndex(true);
  }

  @TransactionalStoreBlob
  protected Content getSearchIndex() throws IOException {
    return getSearchIndex(false);
  }

  @Nullable
  private Content getSearchIndex(final boolean fromCacheOnly) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findRepositoryRootAsset(tx, bucket);
    if (asset == null) {
      if (fromCacheOnly) {
        return null;
      }
      log.debug("Building npm index for {}", getRepository().getName());
      asset = tx.createAsset(bucket, getRepository().getFormat())
          .name(NpmFacetUtils.REPOSITORY_ROOT_ASSET);
      final Path path = Files.createTempFile("npm-searchIndex", "json");
      try {
        Content content = buildIndex(tx, path);
        return saveRepositoryRoot(
            tx,
            asset,
            () -> {
              try {
                return content.openInputStream();
              }
              catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            content
        );
      }
      finally {
        Files.delete(path);
      }
    }
    else if (assetManager.maybeUpdateLastDownloaded(asset)) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  /**
   * Invalidates cached index document, by deleting it.
   */
  @Override
  public void invalidateCachedSearchIndex() {
    UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
    try {
      deleteAsset();
    }
    catch (IOException e) {
      log.warn("Could not invalidate cached search index for {}", getRepository().getName(), e);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @TransactionalDeleteBlob
  protected void deleteAsset() throws IOException {
    log.debug("Invalidating cached npm index of {}", getRepository().getName());
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = NpmFacetUtils.findRepositoryRootAsset(tx, tx.findBucket(getRepository()));
    if (asset == null) {
      return;
    }
    tx.deleteAsset(asset);
    eventManager.post(new NpmSearchIndexInvalidatedEvent(getRepository()));
  }

  /**
   * Builds the full index document of repository.
   */
  @Nonnull
  protected abstract Content buildIndex(final StorageTx tx, final Path path)
      throws IOException;
}
