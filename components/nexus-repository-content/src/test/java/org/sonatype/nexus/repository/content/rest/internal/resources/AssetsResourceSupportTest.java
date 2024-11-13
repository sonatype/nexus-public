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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetDependencies;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.PAGE_SIZE_LIMIT;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.toInternalToken;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.trim;

public class AssetsResourceSupportTest
    extends TestSupport
{
  private static final String A_FORMAT = "A_Format";

  // tests assume we've stored more assets than the page limit
  private static final int NUMBER_OF_ASSETS = PAGE_SIZE_LIMIT + 2; ;

  private static final String ASSET_PATH = "/junit/junit/4.12/junit-4.12.jar";

  private static final String REPOSITORY_NAME = "repository1";

  private static final String REPOSITORY_URL = "http://localhost:8081/repository/" + REPOSITORY_NAME;

  private static final int ASSET_ID = 1001;

  private static final int A_REPOSITORY_ID = 1;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentFacetDependencies dependencies;

  @Mock
  private ContentAuthHelper contentAuthHelper;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private Format aFormat;

  @Mock
  private Continuation<FluentAsset> assetContinuation;

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  private MaintenanceService maintenanceService;

  private AssetsResource underTest;

  @Before
  public void setup() {
    mockRepository();
    mockContentFacet();
    mockFluentAssets();
    underTest = new AssetsResource(repositoryManagerRESTAdapter, maintenanceService, contentAuthHelper, null);
  }

  @Test
  public void browseShouldBeEmptyWhenNoAssets() {
    when(assetContinuation.isEmpty()).thenReturn(true);

    List<FluentAsset> assets = underTest.browse(repository, null);

    assertThat(assets, empty());
    verify(fluentAssets).browse(PAGE_SIZE_LIMIT, null);
    verify(contentAuthHelper, never()).checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void browseShouldBeEmptyWhenNoPermittedAssets() {
    when(assetContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME)).thenReturn(false);

    List<FluentAsset> assets = underTest.browse(repository, null);

    assertThat(assets, empty());
    verify(fluentAssets, times(2)).browse(PAGE_SIZE_LIMIT, null);
    verify(contentAuthHelper, times(NUMBER_OF_ASSETS)).checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void browseShouldReturnPermittedAssets() {
    int numberOfAssetsNotPermitted = 4;
    int numberOfPermittedAssets = NUMBER_OF_ASSETS - numberOfAssetsNotPermitted;

    when(assetContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME))
        .thenReturn(false).thenReturn(false, false, false, true);

    List<FluentAsset> assets = underTest.browse(repository, null);

    assertThat(assets, hasSize(numberOfPermittedAssets));
    verify(fluentAssets, times(2)).browse(PAGE_SIZE_LIMIT, null);
    verify(contentAuthHelper, times(NUMBER_OF_ASSETS)).checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void shouldTrimNumberOfAssetsToLimit() {
    when(assetContinuation.isEmpty()).thenReturn(false);
    when(contentAuthHelper.checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME)).thenReturn(true);

    List<FluentAsset> assets = underTest.browse(repository, null);

    assertThat(assets, hasSize(AssetsResource.PAGE_SIZE_LIMIT));
    verify(contentAuthHelper, times(NUMBER_OF_ASSETS)).checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME);
  }

  @Test
  public void trimShouldTrimToSizeSpecified() {
    List<String> list = asList("foo", "bar", "foobar");

    assertThat(trim(list, 2), is(list.subList(0, 2)));
  }

  @Test
  public void trimShouldReturnSameList() {
    List<String> list = asList("foo", "bar", "foobar");

    assertThat(trim(list, 3), is(list));
  }

  @Test
  public void shouldBeReturnInternalToken() {

    assertThat(toInternalToken(null), nullValue());

    assertThat(toInternalToken("123"), notNullValue());
  }

  private void mockRepository() {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getUrl()).thenReturn(REPOSITORY_URL);
    when(repository.getFormat()).thenReturn(aFormat);
    when(repository.getType()).thenReturn(new HostedType());
    when(aFormat.getValue()).thenReturn(A_FORMAT);
  }

  private void mockContentFacet() {
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacetSupport.dependencies()).thenReturn(dependencies);
    when(dependencies.getMoveService()).thenReturn(Optional.of(mock(RepositoryMoveService.class)));
    when(contentFacet.contentRepositoryId()).thenReturn(A_REPOSITORY_ID);
  }

  private void mockFluentAssets() {
    when(fluentAssets.browse(PAGE_SIZE_LIMIT, null))
        .thenReturn(new FluentContinuation<>(assetContinuation, asset -> aFluentAsset()));

    List<FluentAsset> assets = range(0, NUMBER_OF_ASSETS).mapToObj(i -> aFluentAsset()).collect(toList());
    when(assetContinuation.iterator()).thenReturn(assets.iterator());
  }

  private FluentAsset aFluentAsset() {
    return new FluentAssetImpl(contentFacetSupport,anAsset());
  }

  private AssetData anAsset() {
    AssetData asset = new AssetData();
    asset.setAssetId(ASSET_ID);
    asset.setPath(ASSET_PATH);
    return asset;
  }
}
