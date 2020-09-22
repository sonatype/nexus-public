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
package org.sonatype.nexus.repository.content.npm.internal;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.config.WritePolicy.ALLOW;
import static org.sonatype.nexus.repository.config.WritePolicy.ALLOW_ONCE;
import static org.sonatype.nexus.repository.content.npm.NpmContentFacet.metadataPath;
import static org.sonatype.nexus.repository.content.npm.NpmContentFacet.tarballPath;

/**
 * @since 3.next
 */
@Named(NpmFormat.NAME)
public class NpmContentFacetImpl
    extends ContentFacetSupport
    implements NpmContentFacet
{
  public static final List<HashAlgorithm> HASHING = ImmutableList.of(SHA1, MD5);

  private static final String REPOSITORY_ROOT_ASSET = "/-/all";

  private static final String REPOSITORY_SEARCH_ASSET = "/-/v1/search";

  @Inject
  public NpmContentFacetImpl(@Named(NpmFormat.NAME) final FormatStoreManager formatStoreManager) {
    super(formatStoreManager);
  }

  @Override
  public Optional<Content> get(final NpmPackageId packageId) throws IOException {
    return findAsset(metadataPath(packageId))
        .map(FluentAsset::download);
  }

  @Override
  public FluentAsset put(final NpmPackageId packageId, final Payload content) throws IOException {
    try (TempBlob blob = blobs().ingest(content, HASHING)) {
      String path = metadataPath(packageId);

      save(content, null, AssetKind.PACKAGE_ROOT, null, blob, path);
      return findAsset(path).get();
    }
  }

  @Override
  public boolean delete(final NpmPackageId packageId) throws IOException {
    return maybeDeleteAsset(metadataPath(packageId));
  }

  @Override
  public Optional<Content> get(final NpmPackageId packageId, final String version) throws IOException {
    return findAsset(tarballPath(packageId, version))
        .map(FluentAsset::download);
  }

  @Override
  public FluentAsset put(final NpmPackageId packageId, final TempBlob tempBlob) throws IOException {
    String path = metadataPath(packageId);
    save(null, null, AssetKind.PACKAGE_ROOT, null, tempBlob, path);
    return findAsset(path).get();
  }

  @Override
  public Content put(
      final NpmPackageId packageId,
      final String version,
      final Map<String, Object> npmAttributes,
      final Payload content) throws IOException
  {
    try (TempBlob blob = blobs().ingest(content, HASHING)) {
      FluentComponent component = getOrCreateComponent(packageId, version);
      String path = tarballPath(packageId, version);

      return save(content, component, AssetKind.TARBALL, npmAttributes, blob, path);
    }
  }

  @Override
  public Content put(
      final NpmPackageId packageId,
      final String version,
      final Map<String, Object> npmAttributes,
      final TempBlob blob) throws IOException
  {
    FluentComponent component = getOrCreateComponent(packageId, version);
    String path = tarballPath(packageId, version);

    return save(null, component, AssetKind.TARBALL, npmAttributes, blob, path);
  }

  @Override
  public Content putSearchIndex(final Content content) {
    try (TempBlob blob = blobs().ingest(content, HASHING)) {
      return save(content, null, AssetKind.REPOSITORY_ROOT, null, blob, REPOSITORY_SEARCH_ASSET);
    }
  }

  @Override
  public Content putRepositoryRoot(final Content content) {
    try (TempBlob blob = blobs().ingest(content, HASHING)) {
      return save(content, null, AssetKind.REPOSITORY_ROOT, null, blob, REPOSITORY_ROOT_ASSET);
    }
  }

  @Override
  public boolean delete(final NpmPackageId packageId, final String version) throws IOException {
    boolean assetDeleted = maybeDeleteAsset(metadataPath(packageId));

    if (assetDeleted) {
      maybeDeleteComponent(packageId, version);
    }

    return assetDeleted;
  }

  private FluentComponent getOrCreateComponent(final NpmPackageId packageId, final String version) {
    FluentComponentBuilder builder = components()
        .name(packageId.name())
        .version(version);

    if (packageId.scope() != null) {
      builder.namespace(packageId.scope());
    }

    return builder.getOrCreate();
  }

  private void maybeDeleteComponent(final NpmPackageId packageId, final String version) {
    FluentComponentBuilder component = components()
        .name(packageId.name())
        .version(version);

    if (packageId.scope() != null) {
      component.namespace(packageId.scope());
    }

    component.find().ifPresent(NpmContentFacetImpl::deleteIfNoAssetsLeft);
  }

  private Content save(
      @Nullable final Payload content,
      @Nullable final FluentComponent component,
      final AssetKind assetKind,
      @Nullable final Map<String, Object> npmAttributes,
      final TempBlob blob,
      final String path)
  {
    FluentAssetBuilder assetBuilder = assets().path(path).kind(assetKind.name());
    if (component != null) {
      assetBuilder = assetBuilder.component(component);
    }

    FluentAsset asset = assetBuilder.getOrCreate()
        .attach(blob)
        .markAsCached(content);

    if (npmAttributes != null && !npmAttributes.isEmpty()) {
      asset.withAttribute(NpmFormat.NAME, npmAttributes);
    }

    return asset.download();
  }

  private boolean maybeDeleteAsset(final String path) {
    return findAsset(path).map(FluentAsset::delete).orElse(false);
  }

  private Optional<FluentAsset> findAsset(final String path) {
    return assets().path(path).find();
  }

  private static void deleteIfNoAssetsLeft(final FluentComponent component) {
    if (component.assets().isEmpty()) {
      component.delete();
    }
  }

  @Override
  protected WritePolicy writePolicy(final Asset asset) {
    WritePolicy configuredWritePolicy = super.writePolicy(asset);
    if (ALLOW_ONCE == configuredWritePolicy) {
      String assetKind = asset.kind();
      if (StringUtils.equals(AssetKind.PACKAGE_ROOT.name(), assetKind)
          || StringUtils.equals(AssetKind.REPOSITORY_ROOT.name(), assetKind)) {
        return ALLOW;
      }
    }
    return configuredWritePolicy;
  }
}
