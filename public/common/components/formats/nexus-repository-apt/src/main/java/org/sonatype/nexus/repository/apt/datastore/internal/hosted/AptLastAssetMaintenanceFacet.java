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

  private void removeAssetMetadata(final Asset asset) {
    FluentAsset fluentAsset = contentFacet().assets().with(asset);
    metadata().removePackageMetadata(fluentAsset);
  }

  private AptMetadataFacetSupport metadata() {
    return facet(AptMetadataFacetSupport.class);
  }
}
