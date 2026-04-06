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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LastAssetMaintenanceFacetTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacetSupport contentFacet;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private ComponentStore<?> componentStore;

  private LastAssetMaintenanceFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(contentFacet.assets()).thenReturn(fluentAssets);

    FormatStoreManager formatStoreManager = mock(FormatStoreManager.class);
    when(formatStoreManager.componentStore(any())).thenReturn(componentStore);
    ContentFacetStores stores = new ContentFacetStores(blobStoreManager, "default", formatStoreManager, "nexus");
    when(contentFacet.stores()).thenReturn(stores);

    underTest = new LastAssetMaintenanceFacet();
    underTest.attach(repository);
  }

  @Test
  public void deleteAsset_deletesComponentWhenLastAsset() {
    Asset asset = mock(Asset.class);
    Component component = mock(Component.class);
    when(asset.component()).thenReturn(Optional.of(component));

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    // deleteComponent will call contentFacet().components().with(fluentComponent) again
    when(fluentComponents.with(fluentComponent)).thenReturn(fluentComponent);

    // Component has only 1 asset (the one being deleted) - this is the "last asset" case
    FluentAsset lastAsset = mock(FluentAsset.class);
    when(lastAsset.delete()).thenReturn(true);
    when(lastAsset.path()).thenReturn("/path/last-artifact.jar");
    when(fluentComponent.assets()).thenReturn(Collections.singletonList(lastAsset));

    Set<String> deletedPaths = underTest.deleteAsset(asset);

    assertThat(deletedPaths, hasSize(1));
    assertThat(deletedPaths, containsInAnyOrder("/path/last-artifact.jar"));
    verify(fluentComponent).delete();
  }

  @Test
  public void deleteAsset_deletesOnlyAssetWhenComponentHasMultipleAssets() {
    Asset asset = mock(Asset.class);
    Component component = mock(Component.class);
    when(asset.component()).thenReturn(Optional.of(component));

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);

    // Component has 2 assets - not the last asset, so only delete the asset not the component
    FluentAsset asset1 = mock(FluentAsset.class);
    FluentAsset asset2 = mock(FluentAsset.class);
    when(fluentComponent.assets()).thenReturn(Arrays.asList(asset1, asset2));

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);
    when(fluentAsset.delete()).thenReturn(true);
    when(fluentAsset.path()).thenReturn("/path/one-of-many.jar");

    Set<String> deletedPaths = underTest.deleteAsset(asset);

    assertThat(deletedPaths, hasSize(1));
    assertThat(deletedPaths, containsInAnyOrder("/path/one-of-many.jar"));
    // Component should NOT be deleted since there are other assets
    verify(fluentComponent, never()).delete();
  }

  @Test
  public void deleteAsset_deletesOnlyAssetWhenNoComponent() {
    Asset asset = mock(Asset.class);
    when(asset.component()).thenReturn(Optional.empty());

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);
    when(fluentAsset.delete()).thenReturn(true);
    when(fluentAsset.path()).thenReturn("/path/orphan.jar");

    Set<String> deletedPaths = underTest.deleteAsset(asset);

    assertThat(deletedPaths, hasSize(1));
    assertThat(deletedPaths, containsInAnyOrder("/path/orphan.jar"));
    verify(fluentComponents, never()).with(any(Component.class));
  }

  @Test
  public void deleteAsset_returnsEmptyWhenAssetDeleteFails() {
    Asset asset = mock(Asset.class);
    when(asset.component()).thenReturn(Optional.empty());

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);
    when(fluentAsset.delete()).thenReturn(false);

    Set<String> deletedPaths = underTest.deleteAsset(asset);

    assertThat(deletedPaths, is(empty()));
  }

  @Test
  public void deleteAsset_deletesComponentWhenZeroAssets() {
    Asset asset = mock(Asset.class);
    Component component = mock(Component.class);
    when(asset.component()).thenReturn(Optional.of(component));

    FluentComponent fluentComponent = mock(FluentComponent.class);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    // deleteComponent will call contentFacet().components().with(fluentComponent) again
    when(fluentComponents.with(fluentComponent)).thenReturn(fluentComponent);

    // Component has 0 assets — should delete component
    when(fluentComponent.assets()).thenReturn(Collections.emptyList());

    Set<String> deletedPaths = underTest.deleteAsset(asset);

    assertThat(deletedPaths, is(empty()));
    verify(fluentComponent).delete();
  }
}
