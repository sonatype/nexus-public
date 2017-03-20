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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.Arrays;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.browse.api.AssetXO;
import org.sonatype.nexus.repository.browse.internal.api.AssetXOID;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.base.Supplier;
import com.orientechnologies.orient.core.id.ORID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssetsResourceTest
    extends TestSupport
{
  AssetsResource underTest;

  @Mock
  BrowseService browseService;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Repository mavenReleases;

  @Mock
  Asset assetOne;

  @Mock
  ORID assetOneORID;

  @Mock
  EntityMetadata assetOneEntityMetadata;

  @Mock
  EntityId assetOneEntityId;

  @Mock
  Asset assetTwo;

  @Mock
  EntityMetadata assetTwoEntityMetadata;

  @Mock
  EntityId assetTwoEntityId;

  @Mock
  BrowseResult<Asset> browseResult;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  VariableSource variableSource;

  @Mock
  Format format;

  @Mock
  MaintenanceService maintenanceService;

  Supplier<StorageTx> storageTxSupplier;

  @Before
  public void setUp() throws Exception {
    configureMockedRepository(mavenReleases, "maven-releases", "http://localhost:8081/repository/maven-releases");

    when(format.toString()).thenReturn("maven2");

    storageTxSupplier = () -> storageTx;
    when(storageFacet.txSupplier()).thenReturn(storageTxSupplier);

    when(assetOne.name()).thenReturn("nameOne");
    when(assetOne.getEntityMetadata()).thenReturn(assetOneEntityMetadata);

    when(assetOneORID.toString()).thenReturn("assetOneORID");

    when(assetOneEntityMetadata.getId()).thenReturn(assetOneEntityId);
    when(assetOneEntityId.getValue()).thenReturn("assetOne");

    when(assetTwo.name()).thenReturn("nameTwo");
    when(assetTwo.getEntityMetadata()).thenReturn(assetTwoEntityMetadata);

    when(assetTwoEntityMetadata.getId()).thenReturn(assetTwoEntityId);
    when(assetTwoEntityId.getValue()).thenReturn("asset-two-continuation");

    underTest = new AssetsResource(browseService, repositoryManager, assetEntityAdapter, maintenanceService);
  }

  private void configureMockedRepository(Repository repository,
                                         String name,
                                         String url)
  {
    when(repositoryManager.get(name)).thenReturn(repository);
    when(repository.getUrl()).thenReturn(url);
    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(format);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
  }

  @Captor
  ArgumentCaptor<QueryOptions> queryOptionsCaptor;

  @Test
  public void testGetAssetsFirstPage() throws Exception {
    when(browseResult.getTotal()).thenReturn(10L);
    when(browseResult.getResults()).thenReturn(Arrays.asList(assetOne, assetTwo));

    when(browseService.browseAssets(eq(mavenReleases), queryOptionsCaptor.capture())).thenReturn(browseResult);

    Page<AssetXO> assetXOPage = underTest.getAssets(null, "maven-releases");
    assertThat(assetXOPage.getContinuationToken(), is("asset-two-continuation"));
    assertThat(assetXOPage.getItems(), hasSize(2));
  }

  @Test
  public void testGetAssetsLastPage() throws Exception {
    when(browseResult.getTotal()).thenReturn(2l);
    when(browseResult.getResults()).thenReturn(Arrays.asList(assetOne, assetTwo));

    when(browseService.browseAssets(eq(mavenReleases), queryOptionsCaptor.capture())).thenReturn(browseResult);

    Page<AssetXO> assetXOPage = underTest.getAssets(null, "maven-releases");
    assertThat(assetXOPage.getContinuationToken(), isEmptyOrNullString());
    assertThat(assetXOPage.getItems(), hasSize(2));
  }

  @Test(expected = WebApplicationException.class)
  public void testGetAssetsRepositoryRequired() {
    underTest.getAssets("continue", null);
  }

  @Test(expected = NotFoundException.class)
  public void testGetAssetsRepositoryNotFound() {
    underTest.getAssets("continue", "not-found");
  }

  @Test
  public void testFromAsset() {
    AssetXO assetXO = underTest.fromAsset(assetOne, mavenReleases);

    validateAssetOne(assetXO);
  }

  private void validateAssetOne(final AssetXO assetXO) {
    assertThat(assetXO.getId(), notNullValue());
    assertThat(assetXO.getCoordinates(), is("nameOne"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/repository/maven-releases/nameOne"));
  }

  @Test
  public void testGetAssetById() {
    AssetXOID assetXOID = new AssetXOID("maven-releases", "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(assetXOID.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    AssetXO assetXO = underTest.getAssetById(assetXOID.getValue());

    validateAssetOne(assetXO);
  }

  @Test
  public void testDeleteAsset() {
    AssetXOID assetXOID = new AssetXOID("maven-releases", "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    DetachedEntityId entityId = new DetachedEntityId(assetXOID.getId());
    when(assetEntityAdapter.recordIdentity(entityId)).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    underTest.deleteAsset(assetXOID.getValue());
    verify(maintenanceService).deleteAsset(mavenReleases, assetOne);

  }

  @Test(expected = NotFoundException.class)
  public void testGetAssetById_notFound() {
    AssetXOID assetXOID = new AssetXOID("maven-releases", "f10bd0593de3b5e4b377049bcaa80d3e");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(assetXOID.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(null);

    underTest.getAssetById(assetXOID.getValue());
  }

  @Test(expected = NotFoundException.class)
  public void testGetAssetById_notId_notFound() {
    String id = "not_an_id";

    underTest.getAssetById(id);
  }
}
