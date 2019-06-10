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
package org.sonatype.nexus.repository.apt.internal;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.AptFacet;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
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
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.Query.builder;
import static org.sonatype.nexus.repository.apt.internal.debian.Utils.isDebPackageContentType;

/**
 * @since 3.next
 */
@Named
public class AptFacetImpl
    extends FacetSupport
    implements AptFacet
{
  @VisibleForTesting
  static final String CONFIG_KEY = "apt";

  @VisibleForTesting
  static class Config
  {
    @NotNull(groups = {HostedType.ValidationGroup.class, ProxyType.ValidationGroup.class})
    public String distribution;

    @NotNull(groups = {ProxyType.ValidationGroup.class})
    public boolean flat;
  }

  private Config config;

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(
        configuration,
        CONFIG_KEY,
        Config.class,
        Default.class,
        getRepository().getType().getValidationGroup());
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new AptWritePolicySelector());
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  public String getDistribution() {
    return config.distribution;
  }

  @Override
  public boolean isFlat() {
    return config.flat;
  }

  @Override
  @Nullable
  @TransactionalTouchBlob
  public Content get(final String path) throws IOException {
    final StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
    if (asset == null) {
      return null;
    }

    return FacetHelper.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  @TransactionalStoreBlob
  public Content put(final String path, final Payload content) throws IOException {
    return put(path, content, null);
  }

  @Override
  @TransactionalStoreBlob
  public Content put(final String path, final Payload content, final PackageInfo info) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (final TempBlob tempBlob = storageFacet.createTempBlob(content, FacetHelper.hashAlgorithms)) {
      StorageTx tx = UnitOfWork.currentTx();
      Asset asset = isDebPackageContentType(path)
          ? findOrCreateDebAsset(tx, path,
          info != null ? info : new PackageInfo(AptPackageParser.getDebControlFile(tempBlob.getBlob())))
          : findOrCreateMetadataAsset(tx, path);

      AttributesMap contentAttributes = null;
      if (content instanceof Content) {
        contentAttributes = ((Content) content).getAttributes();
      }
      Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
      AssetBlob blob = tx.setBlob(
          asset,
          path,
          tempBlob,
          FacetHelper.hashAlgorithms,
          null,
          content.getContentType(),
          false);
      tx.saveAsset(asset);
      return FacetHelper.toContent(asset, blob.getBlob());
    }
  }

  @Override
  public Asset findOrCreateDebAsset(final StorageTx tx, final String path, final PackageInfo packageInfo)
  {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, path, bucket);
    if (asset == null) {
      Component component = findOrCreateComponent(
          tx,
          bucket,
          packageInfo);
      asset = tx.createAsset(bucket, component).name(path);
    }

    return asset;
  }

  @Override
  public Asset findOrCreateMetadataAsset(final StorageTx tx, final String path) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, path, bucket);
    return asset != null
        ? asset
        : tx.createAsset(bucket, getRepository().getFormat()).name(path);
  }

  private Component findOrCreateComponent(final StorageTx tx, final Bucket bucket, final PackageInfo info) {
    String name = info.getPackageName();
    String version = info.getVersion();
    String architecture = info.getArchitecture();

    Iterable<Component> components = tx.findComponents(
        builder()
            .where(P_NAME).eq(name)
            .and(P_VERSION).eq(version)
            .and(P_GROUP).eq(architecture)
            .build(),
        singletonList(getRepository())
    );

    Component component = Iterables.getFirst(components, null);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(name)
          .version(version)
          .group(architecture);
      tx.saveComponent(component);
    }

    return component;
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, path, bucket);
    if (asset == null) {
      return false;
    }

    tx.deleteAsset(asset);
    return true;
  }
}
