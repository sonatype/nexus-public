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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.Arrays;

import javax.ws.rs.NotFoundException;

import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.internal.AssetContinuationTokenHelper;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.rest.Page;

import com.orientechnologies.orient.core.id.ORID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_ACCEPTABLE;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;

public class AssetsResourceTest
    extends RepositoryResourceTestSupport
{
  AssetsResource underTest;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  BrowseResult<Asset> browseResult;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  MaintenanceService maintenanceService;

  ContinuationTokenHelper continuationTokenHelper;

  @Mock
  ORID assetOneORID;

  Asset assetOne;

  Asset assetTwo;

  AssetXO assetOneXO;

  AssetXO assetTwoXO;

  @Before
  public void setUp() throws Exception {
    assetOne = getMockedAsset("nameOne", "asset");
    when(assetOneORID.toString()).thenReturn("assetORID");

    assetTwo = getMockedAsset("assetTwo", "asset-two-continuation");

    assetOneXO = buildAssetXO("asset", "nameOne", "http://localhost:8081/repository/maven-releases/nameOne");
    assetTwoXO = buildAssetXO("assetTwo", "nameTwo", "http://localhost:8081/repository/maven-releases/nameTwo");

    continuationTokenHelper = new AssetContinuationTokenHelper(assetEntityAdapter);

    underTest = new AssetsResource(browseService, repositoryManagerRESTAdapter, assetEntityAdapter, maintenanceService,
        continuationTokenHelper);
  }

  AssetXO buildAssetXO(String id, String path, String downloadUrl) {
    AssetXO assetXo = new AssetXO();
    assetXo.setId(id);
    assetXo.setPath(path);
    assetXo.setDownloadUrl(downloadUrl);
    return assetXo;
  }

  @Captor
  ArgumentCaptor<QueryOptions> queryOptionsCaptor;

  @Test
  public void checkPath() {
    assertThat(AssetsResource.RESOURCE_URI, is("/v1/assets"));
  }

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

  private void validateAssetOne(final AssetXO assetXO) {
    assertThat(assetXO.getId(), notNullValue());
    assertThat(assetXO.getPath(), is("nameOne"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/repository/maven-releases/nameOne"));
  }

  @Test
  public void testGetAssetById() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    AssetXO assetXO = underTest.getAssetById(repositoryItemIDXO.getValue());

    validateAssetOne(assetXO);
  }

  @Test
  public void testChecksumExists() throws Exception {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
            "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    AssetXO assetXO = underTest.getAssetById(repositoryItemIDXO.getValue());

    assertThat(assetXO.getChecksum().get("sha1"), is("87acec17cd9dcd20a716cc2cf67417b71c8a7016"));
  }

  @Test
  public void testGetAssetById_illegalArgumentException() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    //IllegalArgumentException is thrown when an id for a different entity type is supplied
    doThrow(new IllegalArgumentException()).when(assetEntityAdapter)
        .recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId()));

    thrown.expect(hasProperty("response", hasProperty("status", is(UNPROCESSABLE_ENTITY))));
    underTest.getAssetById(repositoryItemIDXO.getValue());
  }

  @Test
  public void testDeleteAsset() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "c0ac2ab6c5e93a4a3909f0830fdadfcd");

    DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
    when(assetEntityAdapter.recordIdentity(entityId)).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(assetOne);

    underTest.deleteAsset(repositoryItemIDXO.getValue());
    verify(maintenanceService).deleteAsset(mavenReleases, assetOne);

  }

  @Test(expected = NotFoundException.class)
  public void testGetAssetById_notFound() {
    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO("maven-releases",
        "f10bd0593de3b5e4b377049bcaa80d3e");

    when(assetEntityAdapter.recordIdentity(new DetachedEntityId(repositoryItemIDXO.getId()))).thenReturn(assetOneORID);
    when(browseService.getAssetById(assetOneORID, mavenReleases)).thenReturn(null);

    underTest.getAssetById(repositoryItemIDXO.getValue());
  }

  @Test
  public void testBadContinuationTokenThrowsNotAccepted() throws Exception {
    doThrow(new IllegalArgumentException()).when(assetEntityAdapter)
        .recordIdentity(any(EntityId.class));

    thrown.expect(hasProperty("response", hasProperty("status", is(NOT_ACCEPTABLE))));
    underTest.getAssets("whatever", "maven-releases");
  }
}
