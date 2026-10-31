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
import java.util.Arrays;
import java.util.Collections;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataFacetSupport;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AptLastAssetMaintenanceFacetSoftDeleteTest
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private AptMetadataFacetSupport metadataFacet;

  @Mock
  private Asset asset;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private Component component;

  @Mock
  private FluentComponent fluentComponent;

  private AptLastAssetMaintenanceFacet underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(AptMetadataFacetSupport.class)).thenReturn(metadataFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);

    underTest = new AptLastAssetMaintenanceFacet();
    underTest.attach(repository);
  }

  @Test
  public void afterAssetSoftDeleted_removesPackageMetadataThenRebuilds() throws Exception {
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);

    underTest.afterAssetSoftDeleted(asset);

    // Soft-delete must remove the KV entry AND rebuild the stored Packages index; otherwise the
    // soft-deleted .deb 404s while Packages still advertises it, breaking apt clients.
    InOrder inOrder = inOrder(metadataFacet);
    inOrder.verify(metadataFacet).removePackageMetadata(fluentAsset);
    inOrder.verify(metadataFacet).rebuildMetadata();
  }

  @Test
  public void afterAssetSoftDeleted_ioException_doesNotThrow() throws Exception {
    doThrow(new IOException("test")).when(metadataFacet).rebuildMetadata();

    underTest.afterAssetSoftDeleted(asset);
  }

  @Test
  public void afterAssetRestored_addsMetadataThenRebuilds() throws Exception {
    underTest.afterAssetRestored(asset);

    // NEXUS-53523: the KV entry (removed on soft-delete) must be re-populated from the surviving asset attributes
    // BEFORE the Packages index is rebuilt, otherwise the restored package stays invisible to apt clients.
    InOrder inOrder = inOrder(metadataFacet);
    inOrder.verify(metadataFacet).addPackageMetadata(fluentAsset);
    inOrder.verify(metadataFacet).rebuildMetadata();
  }

  @Test
  public void afterAssetRestored_ioException_doesNotThrow() throws Exception {
    doThrow(new IOException("test")).when(metadataFacet).rebuildMetadata();

    underTest.afterAssetRestored(asset);
  }

  @Test
  public void afterComponentSoftDeleted_removesMetadataForAllAssetsThenRebuilds() throws Exception {
    FluentAsset asset1 = org.mockito.Mockito.mock(FluentAsset.class);
    FluentAsset asset2 = org.mockito.Mockito.mock(FluentAsset.class);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(Arrays.asList(asset1, asset2));
    when(fluentAssets.with(asset1)).thenReturn(asset1);
    when(fluentAssets.with(asset2)).thenReturn(asset2);

    underTest.afterComponentSoftDeleted(component);

    // Remove every asset's KV entry, then rebuild the Packages index so the soft-deleted component
    // stops being advertised to apt clients.
    verify(metadataFacet).removePackageMetadata(asset1);
    verify(metadataFacet).removePackageMetadata(asset2);
    verify(metadataFacet).rebuildMetadata();
  }

  @Test
  public void afterComponentRestored_addsMetadataForAllAssetsThenRebuilds() throws Exception {
    FluentAsset asset1 = org.mockito.Mockito.mock(FluentAsset.class);
    FluentAsset asset2 = org.mockito.Mockito.mock(FluentAsset.class);
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(Arrays.asList(asset1, asset2));
    when(fluentAssets.with(asset1)).thenReturn(asset1);
    when(fluentAssets.with(asset2)).thenReturn(asset2);

    underTest.afterComponentRestored(component);

    // NEXUS-53523: re-populate the KV store for every restored asset before rebuilding the Packages index.
    verify(metadataFacet).addPackageMetadata(asset1);
    verify(metadataFacet).addPackageMetadata(asset2);
    verify(metadataFacet).rebuildMetadata();
  }

  @Test
  public void afterComponentRestored_ioException_doesNotThrow() throws Exception {
    when(fluentComponents.with(component)).thenReturn(fluentComponent);
    when(fluentComponent.assets()).thenReturn(Collections.emptyList());
    doThrow(new IOException("test")).when(metadataFacet).rebuildMetadata();

    underTest.afterComponentRestored(component);
  }
}
