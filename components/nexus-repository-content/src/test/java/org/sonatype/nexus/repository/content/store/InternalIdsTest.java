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
package org.sonatype.nexus.repository.content.store;

import java.util.OptionalInt;
import java.util.function.IntSupplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetBlobId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalRepositoryId;

/**
 * Test {@link InternalIds}.
 */
public class InternalIdsTest
    extends TestSupport
{
  @Mock
  private ContentFacetSupport contentFacet;

  @Test
  public void testMissingInternalIds() {
    ContentRepository repository = new ContentRepositoryData();
    Component component = new ComponentData();
    Asset asset = new AssetData();
    AssetBlob assetBlob = new AssetBlobData();

    Component fluentComponent = new FluentComponentImpl(contentFacet, component);
    Asset fluentAsset = new FluentAssetImpl(contentFacet, asset);

    checkIllegalState(() -> internalRepositoryId(repository));
    checkIllegalState(() -> internalRepositoryId(component));
    checkIllegalState(() -> internalRepositoryId(fluentComponent));
    checkIllegalState(() -> internalComponentId(component));
    checkIllegalState(() -> internalComponentId(fluentComponent));
    checkIllegalState(() -> internalRepositoryId(asset));
    checkIllegalState(() -> internalRepositoryId(fluentAsset));
    checkIllegalState(() -> internalAssetId(asset));
    checkIllegalState(() -> internalAssetId(fluentAsset));
    checkIllegalState(() -> internalAssetBlobId(assetBlob));

    checkNotPresent(internalComponentId(asset));
    checkNotPresent(internalComponentId(fluentAsset));
    checkNotPresent(internalAssetBlobId(asset));
    checkNotPresent(internalAssetBlobId(fluentAsset));
  }

  @Test
  public void testInternalIds() {
    ContentRepositoryData repository = new ContentRepositoryData();
    repository.repositoryId = 1;
    ComponentData component = new ComponentData();
    component.componentId = 2;
    AssetData asset = new AssetData();
    asset.assetId = 3;
    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.assetBlobId = 4;

    component.repositoryId = 5;
    asset.repositoryId = 6;
    asset.componentId = 7;
    asset.assetBlobId = 8;

    Component fluentComponent = new FluentComponentImpl(contentFacet, component);
    Asset fluentAsset = new FluentAssetImpl(contentFacet, asset);

    assertThat(internalRepositoryId(repository), is(1));
    assertThat(internalRepositoryId(component), is(5));
    assertThat(internalRepositoryId(fluentComponent), is(5));
    assertThat(internalComponentId(component), is(2));
    assertThat(internalComponentId(fluentComponent), is(2));
    assertThat(internalRepositoryId(asset), is(6));
    assertThat(internalRepositoryId(fluentAsset), is(6));
    assertThat(internalAssetId(asset), is(3));
    assertThat(internalAssetId(fluentAsset), is(3));
    assertThat(internalAssetBlobId(assetBlob), is(4));

    assertThat(internalComponentId(asset).getAsInt(), is(7));
    assertThat(internalComponentId(fluentAsset).getAsInt(), is(7));
    assertThat(internalAssetBlobId(asset).getAsInt(), is(8));
    assertThat(internalAssetBlobId(fluentAsset).getAsInt(), is(8));
  }

  private static void checkIllegalState(final IntSupplier underTest) {
    try {
      underTest.getAsInt();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("Entity does not have an internal id; is it detached?"));
    }
  }

  private static void checkNotPresent(final OptionalInt underTest) {
    assertFalse(underTest.isPresent());
  }
}
