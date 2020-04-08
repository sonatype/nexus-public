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
package org.sonatype.nexus.repository.content.store.example;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

/**
 * Enhanced test asset store.
 */
public class TestAssetStore
    extends AssetStore<TestAssetDAO>
{
  @Inject
  public TestAssetStore(final DataSessionSupplier sessionSupplier, @Assisted final String storeName) {
    super(sessionSupplier, storeName, TestAssetDAO.class);
  }

  /**
   * Browse all flagged assets in the given repository in a paged fashion.
   *
   * @param repositoryId the repository to browse
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from a previous request
   * @return collection of assets and the next continuation token
   *
   * @see Continuation#nextContinuationToken()
   */
  @Transactional
  public Continuation<Asset> browseFlaggedAssets(final int repositoryId,
                                                 final int limit,
                                                 @Nullable final String continuationToken)
  {
    return dao().browseFlaggedAssets(repositoryId, limit, continuationToken);
  }

  /**
   * Updates the test flag in the given asset.
   *
   * @param asset the asset to update
   */
  @Transactional
  public void updateAssetFlag(final TestAssetData asset) {
    dao().updateAssetFlag(asset);
  }
}
