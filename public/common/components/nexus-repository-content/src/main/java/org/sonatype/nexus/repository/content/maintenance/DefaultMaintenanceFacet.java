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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.WrappedContent;

import com.google.common.collect.ImmutableSet;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Default {@link ContentMaintenanceFacet} for formats that don't need additional bookkeeping.
 *
 * @since 3.26
 */
@Scope(SCOPE_PROTOTYPE)
@org.springframework.stereotype.Component
@Primary
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

    // Purge any remaining asset rows, including Trashed ones, before removing the component. The read-filtered
    // assets() view above hides individually-Trashed assets whenever the Trash schema is present (even with the
    // feature flag off), and the asset->component foreign key would otherwise reject the component delete, leaving
    // an orphaned, file-less component behind (NEXUS-55410). browseAllComponentAssets is used (not the WithTrash
    // variant) so it never references the deleted_on/deleted_by columns: it still returns Trashed rows (no filter)
    // but stays safe on a pre-migration ZDU node where those columns do not yet exist. Every production ContentFacet
    // is a ContentFacetSupport (the stores() accessor lives there); the instanceof keeps the purge from forcing that
    // on unit tests that mock the ContentFacet interface.
    if (contentFacet() instanceof ContentFacetSupport support) {
      AssetStore<?> assetStore = support.stores().assetStore;
      // The REST component-delete route (ComponentsResource) passes a FluentComponent wrapper, but the AssetStore
      // mapper reads #{componentId} off the backing ComponentData -- a wrapper has no such getter and the query
      // throws. Unwrap first (the asset-cascade route already passes an unwrapped Component). NEXUS-55410.
      assetStore.browseAllComponentAssets(unwrap(component)).forEach(assetStore::deleteAsset);
    }

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

  /**
   * Unwrap a fluent component wrapper to its backing {@code ComponentData} so AssetStore mappers that read
   * {@code #{componentId}} work. A plain (already-unwrapped) Component passes through unchanged (NEXUS-55410).
   */
  protected static Component unwrap(final Component component) {
    return component instanceof WrappedContent<?> ? (Component) ((WrappedContent<?>) component).unwrap() : component;
  }

  @Override
  public int deleteComponents(final Stream<FluentComponent> components) {
    ContentFacetSupport contentFacet = (ContentFacetSupport) contentFacet();
    ComponentStore<?> componentStore = contentFacet.stores().componentStore;

    return componentStore.purge(contentFacet.contentRepositoryId(), components.collect(Collectors.toList()));
  }
}
