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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FluentComponentImplTest
    extends TestSupport
{
  @Mock
  private ContentFacetSupport facet;

  @Mock
  private Repository repository;

  @Mock
  private ComponentStore<?> componentStore;

  @Mock
  private AssetStore<?> assetStore;

  @Mock
  private BlobStoreManager blobStoreManager;

  private ComponentData component;

  private FluentComponentImpl underTest;

  @Before
  public void setUp() {
    when(facet.repository()).thenReturn(repository);

    FormatStoreManager formatStoreManager = mock(FormatStoreManager.class);
    when(formatStoreManager.componentStore(any())).thenReturn(componentStore);
    when(formatStoreManager.assetStore(any())).thenReturn(assetStore);
    ContentFacetStores stores = new ContentFacetStores(blobStoreManager, "default", formatStoreManager, "nexus");
    when(facet.stores()).thenReturn(stores);

    component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(1);
    component.setNamespace("org.example");
    component.setName("my-artifact");
    component.setKind("jar");
    component.setVersion("1.0.0");
    component.setNormalizedVersion("001.000.000");

    underTest = new FluentComponentImpl(facet, component);
  }

  @Test
  public void testDelegatesNamespaceToComponent() {
    assertThat(underTest.namespace(), is("org.example"));
  }

  @Test
  public void testDelegatesNameToComponent() {
    assertThat(underTest.name(), is("my-artifact"));
  }

  @Test
  public void testDelegatesKindToComponent() {
    assertThat(underTest.kind(), is("jar"));
  }

  @Test
  public void testDelegatesVersionToComponent() {
    assertThat(underTest.version(), is("1.0.0"));
  }

  @Test
  public void testDelegatesNormalizedVersionToComponent() {
    assertThat(underTest.normalizedVersion(), is("001.000.000"));
  }

  @Test
  public void testDelegatesAttributesToComponent() {
    assertThat(underTest.attributes(), is(notNullValue()));
  }

  @Test
  public void testRepository() {
    assertThat(underTest.repository(), is(repository));
  }

  @Test
  public void testUnwrap() {
    assertThat(underTest.unwrap(), is(component));
  }

  @Test
  public void testAssetsWithPreloadedAssets() {
    FluentAsset mockAsset = mock(FluentAsset.class);
    Collection<FluentAsset> preloaded = Collections.singletonList(mockAsset);

    FluentComponentImpl withAssets = new FluentComponentImpl(facet, component, preloaded);
    Collection<FluentAsset> result = withAssets.assets();
    assertThat(result.size(), is(1));
  }

  @Test
  public void testAssetsWithCacheUsesComponentData() {
    Asset mockAsset = mock(Asset.class);
    component.setAssets(List.of(mockAsset));

    Collection<FluentAsset> result = underTest.assets(true);
    assertThat(result, is(notNullValue()));
    assertThat(result.size(), is(1));
  }

  @Test
  public void testToString() {
    assertThat(underTest.toString(), is(notNullValue()));
  }

  @Test
  public void testKindUpdatesWhenDifferent() {
    underTest.kind("pom");

    assertThat(component.kind(), is("pom"));
    verify(componentStore).updateComponentKind(component);
  }

  @Test
  public void testKindSkipsUpdateWhenSame() {
    underTest.kind("jar");

    assertThat(component.kind(), is("jar"));
    verify(componentStore, never()).updateComponentKind(any());
  }

  @Test
  public void testKindReturnsSelf() {
    assertThat(underTest.kind("pom"), is(sameInstance(underTest)));
  }

  @Test
  public void testDeleteDelegatesToStore() {
    when(componentStore.deleteComponent(component)).thenReturn(true);

    assertThat(underTest.delete(), is(true));
    verify(componentStore).deleteComponent(component);
  }

  @Test
  public void testDeleteReturnsFalseWhenNotFound() {
    when(componentStore.deleteComponent(component)).thenReturn(false);

    assertThat(underTest.delete(), is(false));
    verify(componentStore).deleteComponent(component);
  }

  @Test
  public void testAttributesOperationDelegatesToStore() {
    underTest.attributes(AttributeOperation.SET, "myKey", "myValue");

    verify(componentStore).updateComponentAttributes(component, AttributeOperation.SET, "myKey", "myValue");
  }

  @Test
  public void testAttributesOperationReturnsSelf() {
    assertThat(underTest.attributes(AttributeOperation.SET, "key", "val"), is(sameInstance(underTest)));
  }

  @Test
  public void testAssetReturnsFluentAssetBuilder() {
    FluentAssetBuilder builder = underTest.asset("/org/example/test-1.0.jar");

    assertThat(builder, is(notNullValue()));
  }

  @Test
  public void testAssetsWithoutPreloadedDelegatesToStore() {
    Asset mockAsset = mock(Asset.class);
    when(assetStore.browseComponentAssets(component)).thenReturn(List.of(mockAsset));

    Collection<FluentAsset> result = underTest.assets();
    assertThat(result, hasSize(1));
    verify(assetStore).browseComponentAssets(component);
  }

  @Test
  public void testAssetsWithoutPreloadedReturnsEmptyWhenNoAssets() {
    when(assetStore.browseComponentAssets(component)).thenReturn(Collections.emptyList());

    Collection<FluentAsset> result = underTest.assets();
    assertThat(result, hasSize(0));
  }

  @Test
  public void testAssetsWithCacheFalseIgnoresCache() {
    Asset mockAsset = mock(Asset.class);
    component.setAssets(List.of(mockAsset));

    when(assetStore.browseComponentAssets(component)).thenReturn(Collections.emptyList());

    Collection<FluentAsset> result = underTest.assets(false);

    // useCache=false should call the store, not use the cached assets
    verify(assetStore).browseComponentAssets(component);
  }

  @Test
  public void testAssetsWithCacheTrueButNullAssetsOnComponentData() {
    // ComponentData.getAssets() returns null by default - should fall back to store
    when(assetStore.browseComponentAssets(component)).thenReturn(Collections.emptyList());

    Collection<FluentAsset> result = underTest.assets(true);

    verify(assetStore).browseComponentAssets(component);
  }

  @Test
  public void testAssetsWithCacheUsesComponentDataWhenAvailable() {
    Asset mockAsset1 = mock(Asset.class);
    Asset mockAsset2 = mock(Asset.class);
    component.setAssets(List.of(mockAsset1, mockAsset2));

    Collection<FluentAsset> result = underTest.assets(true);

    assertThat(result, hasSize(2));
    // Should NOT call the store when using cache
    verify(assetStore, never()).browseComponentAssets(any());
  }

  @Test
  public void testPreloadedAssetsAreUnmodifiable() {
    FluentAsset mockAsset = mock(FluentAsset.class);
    Collection<FluentAsset> preloaded = Collections.singletonList(mockAsset);

    FluentComponentImpl withAssets = new FluentComponentImpl(facet, component, preloaded);

    Collection<FluentAsset> result = withAssets.assets();
    try {
      result.add(mock(FluentAsset.class));
      // Should not reach here
      assertThat("Expected UnsupportedOperationException", false);
    }
    catch (UnsupportedOperationException expected) {
      // expected - collection is unmodifiable
    }
  }

  @Test
  public void testNullPreloadedAssetsUsesStoreFallback() {
    // Explicitly pass null for assets - should delegate to store
    FluentComponentImpl withNullAssets = new FluentComponentImpl(facet, component, null);
    when(assetStore.browseComponentAssets(component)).thenReturn(Collections.emptyList());

    Collection<FluentAsset> result = withNullAssets.assets();

    verify(assetStore).browseComponentAssets(component);
  }

  @Test
  public void testAssetsWithCacheOnNonComponentDataFallsBack() {
    // Use a mock Component (not ComponentData) - cache check should fall through
    Component mockComponent = mock(Component.class);
    when(mockComponent.namespace()).thenReturn("org.example");
    when(mockComponent.name()).thenReturn("artifact");
    when(mockComponent.kind()).thenReturn("jar");
    when(mockComponent.version()).thenReturn("1.0");

    FluentComponentImpl impl = new FluentComponentImpl(facet, mockComponent);
    when(assetStore.browseComponentAssets(mockComponent)).thenReturn(Collections.emptyList());

    Collection<FluentAsset> result = impl.assets(true);

    // Since mockComponent is not ComponentData, cache should not be used
    verify(assetStore).browseComponentAssets(mockComponent);
  }
}
