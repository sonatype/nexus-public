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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.ws.rs.NotFoundException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.rest.Page;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Base64.getUrlEncoder;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResource.PAGE_SIZE;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

public class AssetsResourceTest
    extends TestSupport
{
  private static final String A_CONTINUATION_TOKEN = "abcd-1234";

  private static final String AN_ASSET_PATH_FORMAT = "/junit/junit/4.%d/junit-4.%d.jar";

  private static final String ASSET_PATH = "/junit/junit/4.12/junit-4.12.jar";

  private static final String REPOSITORY_NAME = "repository1";

  private static final String REPOSITORY_URL = "http://localhost:8081/repository/" + REPOSITORY_NAME;

  private static final int AN_ASSET_ID = 1;

  @Mock
  private Format aFormat;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private Continuation<FluentAsset> assetContinuation;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  private MaintenanceService maintenanceService;

  @InjectMocks
  private AssetsResource underTest;

  @Before
  public void setup() {
    mockRepository();
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.browse(PAGE_SIZE, null)).thenReturn(assetContinuation);
  }

  @Test
  public void getAssetsShouldReturnAPageWithNullContinuationTokenWhenNoMoreAssets() {
    mockContinuation();

    Page<AssetXO> assets = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(assets.getContinuationToken(), is(nullValue()));
    assertThat(assets.getItems(), containsInAnyOrder(expectedAssetXOs().toArray()));
  }

  @Test
  public void getAssetsShouldReturnAPageWithContinuationTokenWhenMoreAssets() {
    mockContinuation();
    when(assetContinuation.size()).thenReturn(12);

    Page<AssetXO> assets = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(assets.getContinuationToken(), is(A_CONTINUATION_TOKEN));
    assertThat(assets.getItems(), containsInAnyOrder(expectedAssetXOs().toArray()));
  }

  @Test
  public void getAssetByIdShouldReturnAnAssetWhenFound() {
    when(fluentAssets.find(new DetachedEntityId(AN_ASSET_ID + "")))
        .thenReturn(Optional.of(aFluentAsset(AN_ASSET_ID, ASSET_PATH)));

    AssetXO assetXO = underTest.getAssetById(anEncodedAssetId());

    assertThat(assetXO, is(anAssetXO(ASSET_PATH, AN_ASSET_ID)));
  }

  @Test(expected = NotFoundException.class)
  public void getAssetByIdShouldThrowNotFoundExceptionWhenNotFound() {
    when(fluentAssets.find(new DetachedEntityId(AN_ASSET_ID + ""))).thenReturn(Optional.empty());

    AssetXO assetXO = underTest.getAssetById(anEncodedAssetId());

    assertThat(assetXO, is(anAssetXO(ASSET_PATH, AN_ASSET_ID)));
  }

  @Test
  public void deleteAssetShouldDeleteAsset() {
    FluentAssetImpl assetToDelete = aFluentAsset(AN_ASSET_ID, ASSET_PATH);
    when(fluentAssets.find(any())).thenReturn(Optional.of(assetToDelete));

    underTest.deleteAsset(anEncodedAssetId());

    verify(maintenanceService).deleteAsset(repository, assetToDelete);
  }

  @Test(expected = NotFoundException.class)
  public void deleteAssetShouldThrowNotFoundExceptionWhenNotFound() {
    when(fluentAssets.find(new DetachedEntityId(AN_ASSET_ID + ""))).thenReturn(Optional.empty());

    underTest.deleteAsset(anEncodedAssetId());
  }

  private void mockRepository() {
    when(repositoryManagerRESTAdapter.getRepository(REPOSITORY_NAME)).thenReturn(repository);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getUrl()).thenReturn(REPOSITORY_URL);
    when(repository.getFormat()).thenReturn(aFormat);
  }

  private void mockContinuation() {
    IntStream assetIds = IntStream.rangeClosed(1, 10);
    when(assetContinuation.nextContinuationToken()).thenReturn(A_CONTINUATION_TOKEN);
    when(assetContinuation.stream()).thenReturn(assetIds
        .mapToObj(index -> aFluentAsset(index, String.format(AN_ASSET_PATH_FORMAT, index, index))));
  }

  private FluentAssetImpl aFluentAsset(final int assetId, final String assetPath) {
    return new FluentAssetImpl(contentFacetSupport, anAsset(assetId, assetPath));
  }

  private AssetData anAsset(final int id, final String path) {
    AssetData asset = new AssetData();
    asset.setAssetId(id);
    asset.setPath(path);
    return asset;
  }

  private AssetXO anAssetXO(final String path, final int assetId) {
    return AssetXO.builder()
        .path(path)
        .downloadUrl(REPOSITORY_URL + path)
        .id(new RepositoryItemIDXO(REPOSITORY_NAME, toExternalId(assetId).getValue()).getValue())
        .repository(REPOSITORY_NAME)
        .checksum(emptyMap())
        .build();
  }

  private String anEncodedAssetId() {
    return getUrlEncoder().encodeToString((REPOSITORY_NAME + ":" + AN_ASSET_ID).getBytes());
  }

  private List<AssetXO> expectedAssetXOs() {
    return IntStream.rangeClosed(1, 10)
        .mapToObj(id -> anAssetXO(String.format(AN_ASSET_PATH_FORMAT, id, id), id))
        .collect(toList());
  }
}
