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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet;
import org.sonatype.nexus.repository.r.internal.hosted.RMetadataInvalidationEvent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.r.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

public class OrientRPackagesBuilderFacetImplTest
    extends TestSupport
{
  static final String REPOSITORY_NAME = "repository-name";

  static final String BASE_PATH = "/some/path";

  static final String PACKAGES_GZ_PATH = BASE_PATH + "/" + "PACKAGES.gz";

  static final String ASSET_PATH = BASE_PATH + "/" + "asset.gz";

  @Mock
  Repository repository;

  @Mock
  EventManager eventManager;

  @Mock
  OrientRHostedFacet hostedFacet;

  @Mock
  StorageFacet storageFacet;

  @Mock
  AssetCreatedEvent assetCreatedEvent;

  @Mock
  AssetDeletedEvent assetDeletedEvent;

  @Mock
  AssetUpdatedEvent assetUpdatedEvent;

  @Mock
  RMetadataInvalidationEvent invalidationEvent;

  @Mock
  Asset asset;

  @Mock
  NestedAttributesMap formatAttributes;

  @Mock
  TempBlob tempBlob;

  @Mock
  StorageTx storageTx;

  OrientRPackagesBuilderFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.facet(OrientRHostedFacet.class)).thenReturn(hostedFacet);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);

    when(asset.formatAttributes()).thenReturn(formatAttributes);

    underTest = new OrientRPackagesBuilderFacetImpl(eventManager, 1L);
    underTest.attach(repository);
  }

  @Test
  public void testAssetDeletedEventHandledCorrectly() {
    when(assetDeletedEvent.isLocal()).thenReturn(true);
    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetDeletedEvent);

    ArgumentCaptor<RMetadataInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(RMetadataInvalidationEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    RMetadataInvalidationEvent event = eventCaptor.getValue();
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(event.getBasePath(), is(BASE_PATH));
  }

  @Test
  public void testAssetDeletedEventIgnoredForDifferentNode() {
    when(assetDeletedEvent.isLocal()).thenReturn(false);
    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetDeletedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetDeletedEventIgnoredForDifferentRepository() {
    when(assetDeletedEvent.isLocal()).thenReturn(true);
    when(assetDeletedEvent.getRepositoryName()).thenReturn("foo");
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetDeletedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetDeletedEventIgnoredForPackagesGzAsset() {
    when(assetDeletedEvent.isLocal()).thenReturn(true);
    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGES.name());

    underTest.on(assetDeletedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetCreatedEventHandledCorrectly() {
    when(assetCreatedEvent.isLocal()).thenReturn(true);
    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetCreatedEvent);

    ArgumentCaptor<RMetadataInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(RMetadataInvalidationEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    RMetadataInvalidationEvent event = eventCaptor.getValue();
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(event.getBasePath(), is(BASE_PATH));
  }

  @Test
  public void testAssetCreatedEventIgnoredForDifferentNode() {
    when(assetCreatedEvent.isLocal()).thenReturn(false);
    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetCreatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetCreatedEventIgnoredForDifferentRepository() {
    when(assetCreatedEvent.isLocal()).thenReturn(true);
    when(assetCreatedEvent.getRepositoryName()).thenReturn("foo");
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetCreatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetCreatedEventIgnoredForPackagesGzAsset() {
    when(assetCreatedEvent.isLocal()).thenReturn(true);
    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGES.name());

    underTest.on(assetCreatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetUpdatedEventHandledCorrectly() {
    when(assetUpdatedEvent.isLocal()).thenReturn(true);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetUpdatedEvent);

    ArgumentCaptor<RMetadataInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(RMetadataInvalidationEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    RMetadataInvalidationEvent event = eventCaptor.getValue();
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(event.getBasePath(), is(BASE_PATH));
  }

  @Test
  public void testAssetUpdatedEventIgnoredForDifferentNode() {
    when(assetUpdatedEvent.isLocal()).thenReturn(false);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetUpdatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetUpdatedEventIgnoredForDifferentRepository() {
    when(assetUpdatedEvent.isLocal()).thenReturn(true);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn("foo");
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(ARCHIVE.name());

    underTest.on(assetUpdatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetUpdatedEventIgnoredForPackagesGzAsset() {
    when(assetUpdatedEvent.isLocal()).thenReturn(true);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);
    when(formatAttributes.get(P_ASSET_KIND, String.class)).thenReturn(PACKAGES.name());

    underTest.on(assetUpdatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testRebuildMetadataOnEvent() throws Exception {
    when(invalidationEvent.getBasePath()).thenReturn(BASE_PATH);
    when(invalidationEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);

    underTest.on(invalidationEvent);

    verify(hostedFacet).buildAndPutPackagesGz(BASE_PATH);
  }
}
