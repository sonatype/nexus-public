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
package org.sonatype.nexus.repository.p2.orient;

import java.io.IOException;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

/**
 * General P2 facet
 *
 * @since 3.28
 */
@Facet.Exposed
public interface P2Facet
    extends Facet
{
  /**
   * Find or Create Component
   *
   * @return Component
   */
  Component findOrCreateComponent(
      final StorageTx tx,
      final P2Attributes attributes);

  /**
   * Find or Create Asset
   *
   * @return Asset
   */
  Asset findOrCreateAsset(
      final StorageTx tx,
      final Component component,
      final String path,
      final P2Attributes attributes);

  /**
   * Find or Create Asset without Component
   *
   * @return Asset
   */
  Asset findOrCreateAsset(final StorageTx tx, final String path);

  /**
   * Return AssetKind for current asset path/name
   *
   * @return AssetKind
   */
  AssetKind getAssetKind(String path);

  /**
   * Find an asset by its name.
   *
   * @return found asset or null if not found
   */
  Asset findAsset(final StorageTx tx, final Bucket bucket, final String assetName);

  /**
   * Save an asset && create blob.
   *
   * @return blob content
   */
  Content saveAsset(
      final StorageTx tx,
      final Asset asset,
      final TempBlob contentSupplier,
      final Payload payload) throws IOException;

  /**
   * Create Component with Asset if it missed
   *
   * @return Content
   * @throws IOException
   */
  Content doCreateOrSaveComponent(
      final P2Attributes p2Attributes,
      final TempBlob componentContent,
      final Payload payload,
      final AssetKind assetKind) throws IOException;

  /**
   * Convert an asset blob to {@link Content}.
   *
   * @return content of asset blob
   */
  Content toContent(final Asset asset, final Blob blob);
}
