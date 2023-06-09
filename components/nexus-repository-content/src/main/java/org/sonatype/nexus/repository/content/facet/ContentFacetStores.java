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
package org.sonatype.nexus.repository.content.facet;

import javax.inject.Provider;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Persistence stores associated with a repository's {@link ContentFacet}.
 *
 * @since 3.24
 */
public class ContentFacetStores
{
  public final String blobStoreName;

  public final Provider<BlobStore> blobStoreProvider;

  public final String contentStoreName;

  public final ContentRepositoryStore<?> contentRepositoryStore;

  public final ComponentStore<?> componentStore;

  public final AssetStore<?> assetStore;

  public final AssetBlobStore<?> assetBlobStore;

  public ContentFacetStores(final BlobStoreManager blobStoreManager,
                            final String blobStoreName,
                            final FormatStoreManager formatStoreManager,
                            final String contentStoreName)
  {
    this.blobStoreName = checkNotNull(blobStoreName);
    // We have to use Provider here because the blobstore may be not initialized after conversion to group.
    this.blobStoreProvider = () -> blobStoreManager.get(blobStoreName);

    this.contentStoreName = checkNotNull(contentStoreName);
    this.contentRepositoryStore = formatStoreManager.contentRepositoryStore(contentStoreName);
    this.componentStore = formatStoreManager.componentStore(contentStoreName);
    this.assetStore = formatStoreManager.assetStore(contentStoreName);
    this.assetBlobStore = formatStoreManager.assetBlobStore(contentStoreName);
  }
}
