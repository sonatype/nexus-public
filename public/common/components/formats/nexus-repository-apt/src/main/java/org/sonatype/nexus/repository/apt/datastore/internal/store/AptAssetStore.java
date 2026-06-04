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
package org.sonatype.nexus.repository.apt.datastore.internal.store;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE;

/**
 * APT asset store.
 */
public class AptAssetStore
    extends AssetStore<AptAssetDAO>
{
  @Autowired
  public AptAssetStore(
      final DataSessionSupplier sessionSupplier,
      @Value(DATASTORE_CLUSTERED_ENABLED_NAMED_VALUE) final boolean clustered,
      final String storeName)
  {
    super(sessionSupplier, clustered, storeName, AptAssetDAO.class);
  }

  /**
   * Browse Package index metadata assets for a given repository.
   *
   * @param repositoryId the repository ID
   * @param limit maximum number of assets to return
   * @param continuationToken optional token to continue from previous query
   * @param pathPattern the path pattern to match (e.g., "/dists/bionic/main/binary-%")
   * @return continuation of assets matching the criteria
   */
  @Transactional
  public Continuation<Asset> browsePackageIndexAssets(
      final int repositoryId,
      final int limit,
      @Nullable final String continuationToken,
      final String pathPattern)
  {
    return dao().browsePackageIndexAssets(repositoryId, limit, continuationToken, pathPattern);
  }
}
