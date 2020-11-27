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
package org.sonatype.nexus.blobstore.internal.datastore;

import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Check if a blob is referenced in the corresponding metadata for NewDB
 *
 * @since 3.29
 */
@Named
@Singleton
@FeatureFlag(name = "nexus.datastore.enabled")
public class DefaultBlobStoreUsageChecker
    implements BlobStoreUsageChecker
{
  private static final String ANY_NODE = "%";

  private final RepositoryManager repositoryManager;

  private final Function<Repository, AssetBlobStore<?>> getAssetBlobStore;

  @Inject
  public DefaultBlobStoreUsageChecker(final RepositoryManager repositoryManager)
  {
    this(repositoryManager, DefaultBlobStoreUsageChecker::getAssetBlobStore);
  }

  @VisibleForTesting
  DefaultBlobStoreUsageChecker(
      final RepositoryManager repositoryManager,
      Function<Repository, AssetBlobStore<?>> getAssetBlobStore)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.getAssetBlobStore = checkNotNull(getAssetBlobStore);
  }

  private static AssetBlobStore<?> getAssetBlobStore(final Repository repository) {
    return ((ContentFacetSupport) repository.facet(ContentFacet.class)).stores().assetBlobStore;
  }

  @Override
  @Timed
  public boolean test(final BlobStore blobStore, final BlobId blobId, final String blobName)
  {
    String blobStoreId = blobStore.getBlobStoreConfiguration().getName();
    BlobRef blobRef = new BlobRef(ANY_NODE, blobStoreId, blobId.asUniqueString());

    return stream(repositoryManager.browseForBlobStore(blobStoreId).spliterator(), false)
        .filter(repository -> repository.facet(ContentFacet.class).assets().path(blobName).find().isPresent())
        .map(getAssetBlobStore)
        .anyMatch(assetBlobStore -> assetBlobStore.readAssetBlob(blobRef).isPresent());
  }
}
