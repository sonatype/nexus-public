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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.r.orient.OrientRFacet;
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.r.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.util.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.r.internal.util.RMetadataUtils.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.PACKAGES_GZ_FILENAME;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.buildPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.getBasePath;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.browseAllAssetsByKind;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.saveAsset;
import static org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils.toContent;

/**
 * {@link OrientRHostedFacet} implementation.
 * @since 3.28
 */
@Named
public class OrientRHostedFacetImpl
    extends FacetSupport
    implements OrientRHostedFacet
{
  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new OrientRWritePolicySelector());
  }

  @Override
  @TransactionalTouchBlob
  public Content getStoredContent(final String contentPath) {
    checkNotNull(contentPath);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), contentPath);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  public Asset upload(final String path, final Payload payload) throws IOException {
    checkNotNull(path);
    checkNotNull(payload);
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HASH_ALGORITHMS)) {
      return doPutArchive(path, tempBlob, payload);
    }
  }

  @TransactionalStoreBlob
  protected Asset doPutArchive(final String path,
                               final TempBlob archiveContent,
                               final Payload payload) throws IOException
  {

    StorageTx tx = UnitOfWork.currentTx();
    OrientRFacet rFacet = facet(OrientRFacet.class);

    Map<String, String> attributes;
    try (InputStream is = archiveContent.get()) {
      attributes = extractDescriptionFromArchive(path, is);
    }

    Component component = rFacet.findOrCreateComponent(tx, path, attributes);
    Asset asset = rFacet.findOrCreateAsset(tx, component, path, attributes);
    saveAsset(tx, asset, archiveContent, payload);

    return asset;
  }

  @Override
  @TransactionalTouchMetadata
  public Content buildAndPutPackagesGz(final String basePath) throws IOException {
    checkNotNull(basePath);
    StorageTx tx = UnitOfWork.currentTx();
    OrientRPackagesBuilder packagesBuilder = new OrientRPackagesBuilder();
    Iterable<Asset> archiveAssets = browseAllAssetsByKind(tx, tx.findBucket(getRepository()), ARCHIVE);
    StreamSupport.stream(archiveAssets.spliterator(), false) // packageInfoBuilder doesn't support multithreading
        .filter(asset -> basePath.equals(getBasePath(asset.name())))
        .forEach(packagesBuilder::append);
    byte[] packagesBytes = packagesBuilder.buildPackagesGz();
    StorageFacet storageFacet = getRepository().facet(StorageFacet.class);
    try (InputStream is = new ByteArrayInputStream(packagesBytes)) {
      TempBlob tempPackagesGz = storageFacet.createTempBlob(is, HASH_ALGORITHMS);
      return doPutPackagesGz(tx, basePath, tempPackagesGz);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutPackagesGz(final StorageTx tx, final String basePath, final TempBlob tempPackagesGz)
      throws IOException
  {
    OrientRFacet rFacet = facet(OrientRFacet.class);
    Asset asset = rFacet.findOrCreateAsset(tx, buildPath(basePath, PACKAGES_GZ_FILENAME));
    return saveAsset(tx, asset, tempPackagesGz, "", null);
  }
}
