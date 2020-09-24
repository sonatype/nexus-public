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
package org.sonatype.nexus.repository.maven.importtask;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.importtask.ImportPostProcessor;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageTx;

import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Provides some post processing capabilities for assets that could have hash files that need blobUpdated date to match
 * the imported asset blobUpdated Date
 *
 * @since 3.next
 */
@Singleton
@Named(Maven2Format.NAME)
public class MavenImportPostProcessor
    implements ImportPostProcessor
{
  private static final String MD_5_EXTENSION = ".md5";

  private static final String SHA_1_EXTENSION = ".sha1";

  @Override
  public void attributePostProcessing(
      final Asset asset, final StorageTx tx, final Repository repository)
  {
    updateHashFile(MD_5_EXTENSION, asset, tx, repository);
    updateHashFile(SHA_1_EXTENSION, asset, tx, repository);
  }

  private void updateHashFile(
      final String hashFileExtension,
      final Asset asset,
      final StorageTx tx,
      final Repository repository)
  {
    Asset hashAsset = tx.findAssetWithProperty(P_NAME, asset.name() + hashFileExtension, tx.findBucket(repository));

    if (hashAsset != null) {
      updateHashFileDates(hashAsset, asset, tx);
    }
  }

  private void updateHashFileDates(
      final Asset hashAsset, final Asset asset,
      final StorageTx tx)
  {
    boolean assetTouched = false;

    if (hashAsset.blobUpdated() == null ||
        (hashAsset.blobUpdated() != null && !hashAsset.blobUpdated().equals(asset.blobUpdated()))) {
      hashAsset.blobUpdated(asset.blobUpdated());
      assetTouched = true;
    }
    if (hashAsset.blobCreated() == null ||
        (hashAsset.blobCreated() != null && !hashAsset.blobCreated().equals(asset.blobCreated()))) {
      hashAsset.blobCreated(asset.blobCreated());
      assetTouched = true;
    }
    if (hashAsset.lastDownloaded() == null ||
        (hashAsset.lastDownloaded() != null && !hashAsset.lastDownloaded().equals(asset.lastDownloaded()))) {
      hashAsset.lastDownloaded(asset.lastDownloaded());
      assetTouched = true;
    }
    if (hashAsset.lastUpdated() == null ||
        (hashAsset.lastUpdated() != null && !hashAsset.lastUpdated().equals(asset.lastUpdated()))) {
      hashAsset.lastUpdated(asset.lastUpdated());
      assetTouched = true;
    }

    if (assetTouched) {
      tx.saveAsset(hashAsset);
    }
  }
}
