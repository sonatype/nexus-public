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
package org.sonatype.repository.helm.orient.internal;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;

/**
 * @since 3.28
 */
@Facet.Exposed
public interface HelmFacet
    extends Facet
{
  Iterable<Asset> browseComponentAssets(final StorageTx tx, @Nullable final AssetKind assetKind);

  Optional<Asset> findAsset(final StorageTx tx, final String assetName);

  Asset findOrCreateAsset(
      final StorageTx tx,
      final String assetPath,
      final AssetKind assetKind,
      final HelmAttributes helmAttributes);

  Content saveAsset(
      final StorageTx tx,
      final Asset asset,
      final TempBlob contentSupplier,
      final Payload payload);

  Content saveAsset(
      final StorageTx tx,
      final Asset asset,
      final TempBlob contentSupplier,
      @Nullable final String contentType,
      @Nullable final AttributesMap contentAttributes) throws IOException;

  Content toContent(final Asset asset, final Blob blob);
}
