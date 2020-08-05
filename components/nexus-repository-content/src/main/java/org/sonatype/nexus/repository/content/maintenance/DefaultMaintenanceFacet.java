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
package org.sonatype.nexus.repository.content.maintenance;

import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

import com.google.common.collect.ImmutableSet;

/**
 * Default {@link ContentMaintenanceFacet} for formats that don't need additional bookkeeping.
 *
 * @since 3.26
 */
@Named
public class DefaultMaintenanceFacet
    extends FacetSupport
    implements ContentMaintenanceFacet
{
  @Override
  public Set<String> deleteComponent(final Component component) {
    ImmutableSet.Builder<String> deletedPaths = ImmutableSet.builder();

    FluentComponent componentToDelete = contentFacet().components().with(component);

    componentToDelete.assets().forEach(assetToDelete -> {
      if (assetToDelete.delete()) {
        deletedPaths.add(assetToDelete.path()); // only add paths which were deleted by us
      }
    });

    componentToDelete.delete(); // the component itself has no path

    return deletedPaths.build();
  }

  @Override
  public Set<String> deleteAsset(final Asset asset) {
    ImmutableSet.Builder<String> deletedPaths = ImmutableSet.builder();

    FluentAsset assetToDelete = contentFacet().assets().with(asset);

    if (assetToDelete.delete()) {
      deletedPaths.add(assetToDelete.path());
    }

    return deletedPaths.build();
  }

  protected ContentFacet contentFacet() {
    return facet(ContentFacet.class);
  }
}
