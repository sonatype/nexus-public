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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.npm.NpmFacet;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.selectVersionByTarballName;
import static org.sonatype.nexus.repository.npm.internal.NpmPackageRootMetadataUtils.createFullPackageMetadata;

/**
 * {@link NpmHostedFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class NpmHostedFacetImpl
    extends FacetSupport
    implements NpmHostedFacet
{
  private final NpmRequestParser npmRequestParser;

  @Inject
  public NpmHostedFacetImpl(final NpmRequestParser npmRequestParser) {
    this.npmRequestParser = checkNotNull(npmRequestParser);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new NpmWritePolicySelector());
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content getPackage(final NpmPackageId packageId, final boolean edit) throws IOException {
    checkNotNull(packageId);
    log.debug("Getting package: {} (edit={})", packageId, edit);
    StorageTx tx = UnitOfWork.currentTx();
    Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
    if (packageRootAsset == null) {
      return null;
    }
    if (!edit && packageRootAsset.markAsDownloaded()) {
      tx.saveAsset(packageRootAsset);
    }
    NestedAttributesMap packageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);
    NpmMetadataUtils.rewriteTarballUrl(getRepository().getName(), packageRoot);
    return NpmFacetUtils.toContent(packageRootAsset, packageRoot);
  }

  @Override
  public void putPackage(final NpmPackageId packageId, @Nullable final String revision, final Payload payload)
      throws IOException
  {
    checkNotNull(packageId);
    checkNotNull(payload);
    try (NpmPublishRequest request = npmRequestParser.parsePublish(getRepository(), payload)) {
      putPublishRequest(packageId, revision, request);
    }
  }

  @Override
  public Asset putPackage(final Map<String, Object> packageJson, final TempBlob tempBlob) throws IOException {
    checkNotNull(packageJson);
    checkNotNull(tempBlob);

    log.debug("Storing package: {}", packageJson);

    checkNotNull(packageJson.get(NpmAttributes.P_NAME), "Uploaded package is invalid, or is missing package.json");

    NestedAttributesMap metadata = createFullPackageMetadata(new NestedAttributesMap("metadata", packageJson),
        getRepository().getName(), tempBlob.getHashes().get(HashAlgorithm.SHA1).toString());

    NpmPackageId packageId = NpmPackageId.parse((String) metadata.get(NpmAttributes.P_NAME));

    return putPackage(packageId, metadata, tempBlob);
  }

  @TransactionalStoreBlob
  protected Asset putPackage(final NpmPackageId packageId,
                             final NestedAttributesMap requestPackageRoot,
                             final TempBlob tarballTempBlob)
      throws IOException
  {
    checkNotNull(packageId);
    checkNotNull(requestPackageRoot);
    checkNotNull(tarballTempBlob);

    log.debug("Storing package: {}", packageId);

    StorageTx tx = UnitOfWork.currentTx();

    String tarballName = NpmMetadataUtils.extractTarballName(requestPackageRoot);
    AssetBlob assetBlob = NpmFacetUtils.createTarballAssetBlob(tx, packageId, tarballName, tarballTempBlob);

    NpmFacet npmFacet = facet(NpmFacet.class);
    Asset asset = npmFacet.putTarball(packageId.id(), tarballName, assetBlob, new AttributesMap());

    putPackageRoot(packageId, null, requestPackageRoot);

    return asset;
  }

  @TransactionalStoreBlob
  protected void putPublishRequest(final NpmPackageId packageId,
                                   @Nullable final String revision,
                                   final NpmPublishRequest request)
      throws IOException
  {
    log.debug("Storing package: {}", packageId);
    StorageTx tx = UnitOfWork.currentTx();

    NestedAttributesMap packageRoot = request.getPackageRoot();

    // process attachments, if any
    NestedAttributesMap attachments = packageRoot.child("_attachments");
    if (!attachments.isEmpty()) {
      for (String name : attachments.keys()) {
        NestedAttributesMap attachment = attachments.child(name);
        NestedAttributesMap packageVersion = selectVersionByTarballName(packageRoot, name);
        putTarball(tx, packageId, packageVersion, attachment, request);
      }
    }

    putPackageRoot(packageId, revision, packageRoot);
  }

  /**
   * Note: transactional method cannot be private, must be protected (as CGLIB will extend it).
   */
  @TransactionalStoreBlob
  public void putPackageRoot(final NpmPackageId packageId,
                                @Nullable final String revision,
                                final NestedAttributesMap newPackageRoot)
      throws IOException
  {
    log.debug("Storing package root: {}", packageId);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    boolean update = false;

    NestedAttributesMap packageRoot = newPackageRoot;
    Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(tx, bucket, packageId);
    if (packageRootAsset != null) {
      NestedAttributesMap oldPackageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);

      String rev = revision;
      if (rev == null) {
        rev = packageRoot.get(NpmMetadataUtils.META_REV, String.class);
      }
      // ensure revision is expected, client updates package that is in expected state
      if (rev != null) {
        // if revision is present, full document is being sent, no overlay must occur
        checkArgument(rev.equals(oldPackageRoot.get(NpmMetadataUtils.META_REV, String.class)));
        update = true;
      }
      else {
        // if no revision present, snippet is being sent, overlay it (if old exists)
        packageRoot = NpmMetadataUtils.overlay(oldPackageRoot, packageRoot);
      }
    }

    if (packageRootAsset == null) {
      packageRootAsset = tx.createAsset(bucket, getRepository().getFormat()).name(packageId.id());
    }

    NpmFacetUtils.savePackageRoot(tx, packageRootAsset, packageRoot);
    if (update) {
      updateDeprecationFlags(tx, packageId, packageRoot);
    }
  }

  /**
   * Updates all the tarball components that belong to given package, updating their deprecated flags. Only changed
   * {@link Component}s are modified and saved.
   */
  private void updateDeprecationFlags(final StorageTx tx,
                                      final NpmPackageId packageId,
                                      final NestedAttributesMap packageRoot)
  {
    final NestedAttributesMap versions = packageRoot.child(NpmMetadataUtils.VERSIONS);
    for (Component tarballComponent : NpmFacetUtils.findPackageTarballComponents(tx, getRepository(), packageId)) {
      // integrity check: package doc must contain the tarball version
      checkState(versions.contains(tarballComponent.version()), "Package %s lacks tarball version %s", packageId,
          tarballComponent.version());
      final NestedAttributesMap version = versions.child(tarballComponent.version());
      final String deprecationMessage = version.get(NpmMetadataUtils.DEPRECATED, String.class);
      // in npm JSON, deprecated with non-empty string means deprecated, with empty or not present is not deprecated
      final boolean deprecated = !Strings2.isBlank(deprecationMessage);
      if (deprecated && !deprecationMessage
          .equals(tarballComponent.formatAttributes().get(NpmAttributes.P_DEPRECATED, String.class))) {
        tarballComponent.formatAttributes().set(NpmAttributes.P_DEPRECATED, deprecationMessage);
        tx.saveComponent(tarballComponent);
      }
      else if (!deprecated && tarballComponent.formatAttributes().contains(NpmAttributes.P_DEPRECATED)) {
        tarballComponent.formatAttributes().remove(NpmAttributes.P_DEPRECATED);
        tx.saveComponent(tarballComponent);
      }
    }
  }

  private void putTarball(final StorageTx tx,
                          final NpmPackageId packageId,
                          final NestedAttributesMap packageVersion,
                          final NestedAttributesMap attachment,
                          final NpmPublishRequest request) throws IOException
  {
    String tarballName = NpmMetadataUtils.extractTarballName(attachment.getKey());
    log.debug("Storing tarball: {}@{} ({})",
        packageId,
        packageVersion.get(NpmMetadataUtils.VERSION, String.class),
        tarballName);

    TempBlob tempBlob = request.requireBlob(attachment.require("data", String.class));
    AssetBlob assetBlob = NpmFacetUtils.createTarballAssetBlob(tx, packageId, tarballName, tempBlob);

    NpmFacet npmFacet = facet(NpmFacet.class);
    npmFacet.putTarball(packageId.id(), tarballName, assetBlob, new AttributesMap());
  }

  @Override
  @TransactionalDeleteBlob
  public boolean deletePackage(final NpmPackageId packageId, @Nullable final String revision) throws IOException {
    return deletePackage(packageId, revision, true);
  }

  @Override
  @TransactionalDeleteBlob
  public boolean deletePackage(final NpmPackageId packageId,
                               @Nullable final String revision,
                               final boolean deleteBlobs)
      throws IOException
  {
    checkNotNull(packageId);
    StorageTx tx = UnitOfWork.currentTx();
    if (revision != null) {
      Asset packageRootAsset = NpmFacetUtils.findPackageRootAsset(tx, tx.findBucket(getRepository()), packageId);
      if (packageRootAsset != null) {
        NestedAttributesMap oldPackageRoot = NpmFacetUtils.loadPackageRoot(tx, packageRootAsset);
        checkArgument(revision.equals(oldPackageRoot.get(NpmMetadataUtils.META_REV, String.class)));
      }
    }

    return NpmFacetUtils.deletePackageRoot(tx, getRepository(), packageId, deleteBlobs);
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content getTarball(final NpmPackageId packageId, final String tarballName) throws IOException {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    StorageTx tx = UnitOfWork.currentTx();
    return NpmFacetUtils.getTarballContent(tx, tx.findBucket(getRepository()), packageId, tarballName);
  }

  @Override
  @TransactionalDeleteBlob
  public boolean deleteTarball(final NpmPackageId packageId, final String tarballName) {
    return deleteTarball(packageId, tarballName, true);
  }

  @Override
  @TransactionalDeleteBlob
  public boolean deleteTarball(final NpmPackageId packageId, final String tarballName, final boolean deleteBlob) {
    checkNotNull(packageId);
    checkNotNull(tarballName);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset tarballAsset = NpmFacetUtils.findTarballAsset(tx, bucket, packageId, tarballName);
    if (tarballAsset == null) {
      return false;
    }
    Component tarballComponent = tx.findComponentInBucket(tarballAsset.componentId(), bucket);
    if (tarballComponent == null) {
      return false;
    }
    tx.deleteComponent(tarballComponent, deleteBlob);
    return true;
  }
}
