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

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.AssetDAO;

import org.apache.ibatis.annotations.Param;

/**
 * Test format which adds flagging of assets.
 */
public interface TestAssetDAO
    extends AssetDAO
{
  /**
   * Adds extra asset test columns to the owning data store.
   */
  void extendSchema();

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
  Continuation<Asset> browseFlaggedAssets(@Param("repositoryId") int repositoryId,
                                          @Param("limit") int limit,
                                          @Param("continuationToken") @Nullable String continuationToken);

  /**
   * Updates the test flag of the given asset in the content data store.
   *
   * @param asset the asset to update
   */
  void updateAssetFlag(TestAssetData asset);
}
