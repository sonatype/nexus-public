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
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.npm.NpmContentFacet;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;

import static java.util.Collections.*;

/**
 * This handles the deletes of Npm components and assets. A key piece of functionality is removing the dist tags for
 * version of packages from the root when they are removed.
 *
 * @since 3.29
 */
@Named
public class NpmHostedMaintenanceFacet
    extends NpmFacetSupport
    implements ContentMaintenanceFacet
{
  @Inject
  public NpmHostedMaintenanceFacet(final NpmPackageParser npmPackageParser) {
    super(npmPackageParser);
  }

  @Override
  public Set<String> deleteComponent(final Component component) {
    log.debug("Deleting npm component: {}", component);

    ImmutableSet.Builder<String> deletedAssets = ImmutableSet.builder();

    FluentComponent fluentComponent = getRepository()
        .facet(ContentFacet.class)
        .components()
        .with(component);

    fluentComponent.assets().forEach(asset -> deletedAssets.addAll(deleteAsset(asset)));

    return deletedAssets.build();
  }

  @Override
  public Set<String> deleteAsset(final Asset asset) {
    log.debug("Deleting NPM asset: {}", asset);

    AssetKind assetKind = AssetKind.valueOf(asset.kind());
    ImmutableSet.Builder<String> deletedAssets = ImmutableSet.builder();

    switch (assetKind) {
      case PACKAGE_ROOT:
        deletedAssets.addAll(deletePackageRoot(asset));
        break;
      case TARBALL:
        try {
          deletedAssets.addAll(deleteTarball(asset));
        }
        catch (IOException e) {
          log.error("Exception deleting tarball for asset: {}", asset, e);
          return emptySet();
        }
        break;
      default:
        break;
    }
    return deletedAssets.build();
  }

  @Override
  public int deleteComponents(final int[] componentIds) {
    return (int) Arrays
        .stream(componentIds)
        .boxed()
        .map(InternalIds::toExternalId)
        .map(componentId -> getRepository().facet(ContentFacet.class).components().find(componentId))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::deleteComponent)
        .count();
  }

  /**
   * Deletes package and all related tarballs too.
   */
  private Set<String> deletePackageRoot(final Asset asset) {
    NpmPackageId packageId = NpmPackageId.parse(removeStartingSlash(asset.path()));
    try {
      return getRepository().facet(NpmHostedFacet.class).deletePackage(packageId, null);
    }
    catch (IOException e) {
      log.error("Exception delete package root for asset: {}", asset, e);
      return emptySet();
    }
  }

  private Set<String> deleteTarball(final Asset asset) throws IOException
  {
    log.debug("Deleting tarball for asset: {}", asset);

    String assetNameWithoutStartingSlash = removeStartingSlash(asset.path());
    NpmPackageId packageId =
        NpmPackageId.parse(assetNameWithoutStartingSlash.substring(0, assetNameWithoutStartingSlash.indexOf("/-/")));
    String tarballName = NpmMetadataUtils.extractTarballName(assetNameWithoutStartingSlash);

    Optional<FluentAsset> packageRootAsset = findPackageRootAsset(packageId);
    if (!packageRootAsset.isPresent()) {
      log.debug("No package root present for asset with path: {}", asset.path());
      return emptySet();
    }

    Optional<NestedAttributesMap> nestedAttributesMapOptional =
        loadPackageRoot(packageId, getRepository().facet(NpmContentFacet.class));

    if (!nestedAttributesMapOptional.isPresent()) {
      log.debug("No attributes for package root present for asset with path: {}", asset.path());
      return emptySet();
    }
    NestedAttributesMap packageRoot = nestedAttributesMapOptional.get();

    NestedAttributesMap version = NpmMetadataUtils.selectVersionByTarballName(packageRoot, tarballName);

    if (version == null) {
      return emptySet();
    }

    packageRoot.child(NpmMetadataUtils.VERSIONS).remove(version.getKey());
    if (packageRoot.child(NpmMetadataUtils.VERSIONS).isEmpty()) {
      return getRepository().facet(NpmHostedFacet.class).deletePackage(packageId, null);
    }
    else {
      log.debug("Removing dist tags for asset {} with version {}", asset.path(), version.getKey());
      removeDistTagsFromTagsWithVersion(packageRoot, version.getKey());

      packageRoot.child(NpmMetadataUtils.TIME).remove(version.getKey());
      NpmMetadataUtils.maintainTime(packageRoot);
      savePackageRoot(packageId, packageRoot);
      return getRepository().facet(NpmHostedFacet.class)
          .deleteTarball(packageId, tarballName)
          .map(Collections::singleton)
          .orElseGet(Collections::emptySet);
    }
  }

  private String removeStartingSlash(final String toRemoveFrom) {
    return StringUtils.removeStart(toRemoveFrom, "/");
  }
}
