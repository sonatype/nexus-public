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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import java.io.IOException;
import java.util.Set;

import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataFacetSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * Apt maintenance facet
 *
 * @since 3.31
 */
@Qualifier(AptFormat.NAME)
@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AptLastAssetMaintenanceFacet
    extends LastAssetMaintenanceFacet
{
  @Override
  public Set<String> deleteAsset(final Asset asset) {
    // Remove metadata BEFORE deleting the asset so we can still access it
    removeAssetMetadata(asset);

    return super.deleteAsset(asset);
  }

  @Override
  public Set<String> deleteComponent(final Component component) {
    // Remove metadata for all assets in this component before deletion
    contentFacet().components().with(component).assets().forEach(this::removeAssetMetadata);

    return super.deleteComponent(component);
  }

  @Override
  public void afterComponentSoftDeleted(final Component component) {
    // Remove the key-value entries, then rebuild the stored Packages index so the soft-deleted package
    // stops being advertised. Without the rebuild the .deb 404s while Packages still lists it, breaking
    // apt clients (NEXUS-53523 fixed the restore side for APT but missed this soft-delete drop side).
    contentFacet().components().with(component).assets().forEach(this::removeAssetMetadata);
    rebuildMetadataQuietly();
  }

  @Override
  public void afterComponentRestored(final Component component) {
    try {
      // The Packages index is rebuilt from the key-value store, whose entries were removed on soft-delete.
      // Re-populate them from the (surviving) asset format attributes BEFORE rebuilding, otherwise the restored
      // package is physically present but invisible to apt clients (NEXUS-53523).
      contentFacet().components().with(component).assets().forEach(this::addAssetMetadata);
      metadata().rebuildMetadata();
    }
    catch (IOException e) {
      log.warn("Failed to rebuild APT metadata after component restore", e);
    }
  }

  @Override
  public void afterAssetSoftDeleted(final Asset asset) {
    removeAssetMetadata(asset);
    rebuildMetadataQuietly();
  }

  @Override
  public void afterAssetRestored(final Asset asset) {
    try {
      // Re-populate the key-value entry from the restored asset before rebuilding the Packages index (NEXUS-53523).
      addAssetMetadata(asset);
      metadata().rebuildMetadata();
    }
    catch (IOException e) {
      log.warn("Failed to rebuild APT metadata after asset restore", e);
    }
  }

  private void addAssetMetadata(final Asset asset) {
    FluentAsset fluentAsset = contentFacet().assets().with(asset);
    metadata().addPackageMetadata(fluentAsset);
  }

  private void rebuildMetadataQuietly() {
    try {
      metadata().rebuildMetadata();
    }
    catch (IOException e) {
      log.warn("Failed to rebuild APT metadata after soft-delete", e);
    }
  }

  private void removeAssetMetadata(final Asset asset) {
    FluentAsset fluentAsset = contentFacet().assets().with(asset);
    metadata().removePackageMetadata(fluentAsset);
  }

  private AptMetadataFacetSupport metadata() {
    return facet(AptMetadataFacetSupport.class);
  }
}
