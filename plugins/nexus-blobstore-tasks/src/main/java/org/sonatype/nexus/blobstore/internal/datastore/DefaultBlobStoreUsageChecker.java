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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.codahale.metrics.annotation.Timed;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.of;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * Check if a blob is referenced in the corresponding metadata for NewDB
 *
 * @since 3.29
 */
@Named
@Singleton
@FeatureFlag(name = DATASTORE_ENABLED)
public class DefaultBlobStoreUsageChecker
    implements BlobStoreUsageChecker
{
  private final RepositoryManager repositoryManager;

  @Inject
  public DefaultBlobStoreUsageChecker(final RepositoryManager repositoryManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  @Timed
  public boolean test(final BlobStore blobStore, final BlobId blobId, final String blobName) {
    return of(blobId)
        .map(blobStore::get)
        .map(Blob::getHeaders)
        .map(headers -> headers.get(REPO_NAME_HEADER))
        .map(repositoryManager::get)
        .map(repository -> (ContentFacetSupport) repository.facet(ContentFacet.class))
        .flatMap(contentFacetSupport -> {
          String blobStoreName = blobStore.getBlobStoreConfiguration().getName();
          BlobRef blobRef = new BlobRef(
              contentFacetSupport.nodeName(), blobStoreName, blobId.asUniqueString(), blobId.getBlobCreatedRef());
          return contentFacetSupport.stores().assetBlobStore.readAssetBlob(blobRef);
        })
        .isPresent();
  }
}
