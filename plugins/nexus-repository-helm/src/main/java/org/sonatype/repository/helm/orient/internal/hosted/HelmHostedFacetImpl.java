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
package org.sonatype.repository.helm.orient.internal.hosted;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;
import org.sonatype.repository.helm.orient.internal.HelmFacet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PROVENANCE;
import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;
import static org.sonatype.repository.helm.internal.util.HelmAttributeParser.validateAttributes;

/**
 * {@link HelmHostedFacetImpl implementation}
 *
 * @since 3.28
 */
@Named
public class HelmHostedFacetImpl
    extends FacetSupport
    implements HelmHostedFacet
{
  private final HelmAttributeParser helmAttributeParser;

  private HelmFacet helmFacet;

  @Inject
  public HelmHostedFacetImpl(
      final HelmAttributeParser helmAttributeParser)
  {
    this.helmAttributeParser = helmAttributeParser;
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new HelmHostedWritePolicySelector());
    helmFacet = facet(HelmFacet.class);
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content get(final String path) {
    checkNotNull(path);
    StorageTx tx = UnitOfWork.currentTx();

    Optional<Asset> assetOpt = helmFacet.findAsset(tx, path);
    if (!assetOpt.isPresent()) {
      return null;
    }
    Asset asset = assetOpt.get();
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }

    return helmFacet.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  public void upload(
      final String path,
      final Payload payload,
      final AssetKind assetKind) throws IOException
  {
    checkNotNull(path);
    try (TempBlob tempBlob = facet(StorageFacet.class).createTempBlob(payload, HASH_ALGORITHMS)) {
      upload(path, tempBlob, payload, assetKind);
    }
  }

  @Override
  public String getPath(final TempBlob tempBlob, final AssetKind assetKind) throws IOException
  {
    if (assetKind != HELM_PACKAGE && assetKind != HELM_PROVENANCE) {
      throw new IllegalArgumentException("Unsupported assetKind: " + assetKind);
    }

    try (InputStream inputStream = tempBlob.get()) {
      HelmAttributes attributes = validateAttributes(helmAttributeParser.getAttributes(assetKind, inputStream));
      String extension = assetKind.getExtension();
      String name = attributes.getName();
      String version = attributes.getVersion();

      return String.format("%s-%s%s", name, version, extension);
    }
  }

  @Override
  @TransactionalStoreBlob
  public Asset upload(final String path, final TempBlob tempBlob, final Payload payload, final AssetKind assetKind) throws IOException {
    checkNotNull(path);
    checkNotNull(tempBlob);
    if (assetKind != HELM_PACKAGE && assetKind != HELM_PROVENANCE) {
      throw new IllegalArgumentException("Unsupported assetKind: " + assetKind);
    }

    StorageTx tx = UnitOfWork.currentTx();
    HelmAttributes attributes;
    try (InputStream inputStream = tempBlob.get()) {
      attributes = helmAttributeParser.getAttributes(assetKind, inputStream);
    }
    final Asset asset = helmFacet.findOrCreateAsset(tx, path, assetKind, attributes);
    helmFacet.saveAsset(tx, asset, tempBlob, payload);
    return asset;
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) {
    checkNotNull(path);

    StorageTx tx = UnitOfWork.currentTx();
    Optional<Asset> asset = helmFacet.findAsset(tx, path);
    if (!asset.isPresent()) {
      return false;
    }
    else {
      tx.deleteAsset(asset.get());
      return true;
    }
  }
}
