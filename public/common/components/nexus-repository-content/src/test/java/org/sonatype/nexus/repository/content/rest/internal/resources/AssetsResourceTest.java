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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetDependencies;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static java.util.Base64.getUrlEncoder;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResourceSupport.PAGE_SIZE_LIMIT;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetsResourceTest
{
  private static final String ASSET_PATH = "/junit/junit/4.12/junit-4.12.jar";

  private static final String REPOSITORY_NAME = "repository1";

  private static final String REPOSITORY_URL = "http://localhost:8081/repository/" + REPOSITORY_NAME;

  private static final int AN_ASSET_ID = 1;

  private static final String A_FORMAT = "aFormatValue";

  private static final OffsetDateTime BLOB_CREATED = OffsetDateTime.now().minusDays(1);

  @Mock
  private Format aFormat;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private ContentFacetSupport contentFacetSupport;

  @Mock
  private ContentFacetDependencies dependencies;

  @Mock
  private RepositoryMoveService moveService;

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  private MaintenanceService maintenanceService;

  @Mock
  private Continuation<FluentAsset> assetContinuation;

  @Mock
  private ContentAuthHelper contentAuthHelper;

  private AssetsResource underTest;

  @BeforeEach
  void setup() {
    mockRepository();
    when(contentAuthHelper.checkPathPermissions(ASSET_PATH, A_FORMAT, repository.getName())).thenReturn(true);
    underTest = new AssetsResource(repositoryManagerRESTAdapter, maintenanceService, contentAuthHelper, emptyList());
  }

  // --- getAssets tests ---
  @WithUser(isAuthenticated = false)
  @Test
  void getAssetsShouldReturnAPageAssets() {
    Page<AssetXO> assets = underTest.getAssets(null, REPOSITORY_NAME);

    assertNotNull(assets);

    verify(fluentAssets).browse(PAGE_SIZE_LIMIT, null);
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_returnsEmptyPageWhenNoAssets() {
    when(assetContinuation.isEmpty()).thenReturn(true);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page, is(notNullValue()));
    assertThat(page.getItems(), is(empty()));
    assertThat(page.getContinuationToken(), is(nullValue()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_invalidContinuationToken() {
    BadRequestException e = assertThrows(BadRequestException.class, () -> underTest.getAssets("", REPOSITORY_NAME));
    assertThat(e.getMessage(), is("Invalid continuation token"));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_continuationTokenIsNullWhenFewerThanPageLimit() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    List<FluentAsset> assetList = List.of(aFluentAsset());
    when(assetContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(assetContinuation.stream()).thenReturn(assetList.stream());

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(1));
    assertThat(page.getContinuationToken(), is(nullValue()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_continuationTokenIsSetWhenExactlyPageLimit() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    List<FluentAsset> assetList = createFluentAssets(PAGE_SIZE_LIMIT);
    when(assetContinuation.isEmpty()).thenReturn(false);
    when(assetContinuation.stream()).thenReturn(assetList.stream());

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(PAGE_SIZE_LIMIT));
    assertThat(page.getContinuationToken(), is(notNullValue()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_filtersOutAssetsWithoutPermission() {
    // Deny permission for all assets
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(false);

    List<FluentAsset> assetList = List.of(aFluentAsset());
    when(assetContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(assetContinuation.stream()).thenReturn(assetList.stream());
    when(assetContinuation.nextContinuationToken()).thenReturn("next-token");

    // Second call returns empty to end the loop
    Continuation<FluentAsset> emptyContinuation = mock(Continuation.class);
    when(emptyContinuation.isEmpty()).thenReturn(true);
    when(fluentAssets.browse(eq(PAGE_SIZE_LIMIT), eq("next-token"))).thenReturn(emptyContinuation);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), is(empty()));
    assertThat(page.getContinuationToken(), is(nullValue()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_filtersPermittedAndNonPermittedAssets() {
    String permittedPath = "/allowed/path.jar";
    String deniedPath = "/denied/path.jar";

    when(contentAuthHelper.checkPathPermissions(permittedPath, A_FORMAT, REPOSITORY_NAME)).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(deniedPath, A_FORMAT, REPOSITORY_NAME)).thenReturn(false);

    FluentAssetImpl permittedAsset = aFluentAssetWithPath(10, permittedPath);
    FluentAssetImpl deniedAsset = aFluentAssetWithPath(11, deniedPath);

    List<FluentAsset> assetList = List.of(permittedAsset, deniedAsset);
    when(assetContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(assetContinuation.stream()).thenReturn(assetList.stream());
    when(assetContinuation.nextContinuationToken()).thenReturn("next-token");

    Continuation<FluentAsset> emptyContinuation = mock(Continuation.class);
    when(emptyContinuation.isEmpty()).thenReturn(true);
    when(fluentAssets.browse(eq(PAGE_SIZE_LIMIT), eq("next-token"))).thenReturn(emptyContinuation);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(1));
    assertThat(page.getItems().iterator().next().getPath(), is(permittedPath));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_paginatesUntilPageSizeLimitReached() {
    // All assets are permitted
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    // First page returns half of the limit
    int halfLimit = PAGE_SIZE_LIMIT / 2;
    List<FluentAsset> firstBatch = createFluentAssets(halfLimit);
    when(assetContinuation.isEmpty()).thenReturn(false);
    when(assetContinuation.stream()).thenReturn(firstBatch.stream());
    when(assetContinuation.nextContinuationToken()).thenReturn("page-2-token");

    // Second page returns remaining to fill page limit
    List<FluentAsset> secondBatch = createFluentAssetsStartingAt(halfLimit + 1, halfLimit);
    Continuation<FluentAsset> secondContinuation = mock(Continuation.class);
    when(secondContinuation.isEmpty()).thenReturn(false);
    when(secondContinuation.stream()).thenReturn(secondBatch.stream());
    when(secondContinuation.nextContinuationToken()).thenReturn("page-3-token");

    when(fluentAssets.browse(eq(PAGE_SIZE_LIMIT), eq("page-2-token"))).thenReturn(secondContinuation);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(PAGE_SIZE_LIMIT));
    assertThat(page.getContinuationToken(), is(notNullValue()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_groupRepositoryUsesGroupMemberContent() {
    when(repository.getType()).thenReturn(new GroupType());

    FluentQuery<FluentAsset> groupMemberQuery = mock(FluentQuery.class);
    when(fluentAssets.withOnlyGroupMemberContent()).thenReturn(groupMemberQuery);

    Continuation<FluentAsset> groupContinuation = mock(Continuation.class);
    when(groupContinuation.isEmpty()).thenReturn(true);
    when(groupMemberQuery.browse(PAGE_SIZE_LIMIT, null)).thenReturn(groupContinuation);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    verify(fluentAssets).withOnlyGroupMemberContent();
    verify(groupMemberQuery).browse(PAGE_SIZE_LIMIT, null);
    assertThat(page, is(notNullValue()));
    assertThat(page.getItems(), is(empty()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssets_hostedRepositoryDoesNotUseGroupMemberContent() {
    when(assetContinuation.isEmpty()).thenReturn(true);

    underTest.getAssets(null, REPOSITORY_NAME);

    verify(fluentAssets, never()).withOnlyGroupMemberContent();
    verify(fluentAssets).browse(PAGE_SIZE_LIMIT, null);
  }

  @Test
  void getAssets_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.getAssets(null, REPOSITORY_NAME));
    verifyNoInteractions(fluentAssets);
  }

  // --- getAssetById tests ---
  @WithUser(isAuthenticated = false)
  @Test
  void getAssetByIdShouldReturnAnAssetWhenFound() {
    FluentAssetImpl fluentAsset = aFluentAsset();

    when(fluentAssets.find(any(DetachedEntityId.class)))
        .thenReturn(Optional.of(fluentAsset));

    AssetXO assetXO = underTest.getAssetById(anEncodedAssetId());

    assertThat(assetXO, is(anAssetXO()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssetByIdShouldThrowNotFoundExceptionWhenNotFound() {
    when(fluentAssets.find(new DetachedEntityId(AN_ASSET_ID + ""))).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> underTest.getAssetById(anEncodedAssetId()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssetById_throwsNotFoundWhenAssetNotPermitted() {
    FluentAssetImpl fluentAsset = aFluentAsset();
    when(fluentAssets.find(any(DetachedEntityId.class))).thenReturn(Optional.of(fluentAsset));

    // Deny permission for this asset
    when(contentAuthHelper.checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> underTest.getAssetById(anEncodedAssetId()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssetById_throwsUnprocessableEntityOnIllegalArgument() {
    when(fluentAssets.find(any(DetachedEntityId.class)))
        .thenThrow(new IllegalArgumentException("bad id"));

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> underTest.getAssetById(anEncodedAssetId()));
    assertThat(e.getResponse().getStatus(), is(422));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void getAssetById_setsIdCorrectly() {
    FluentAssetImpl fluentAsset = aFluentAsset();
    when(fluentAssets.find(any(DetachedEntityId.class))).thenReturn(Optional.of(fluentAsset));

    AssetXO result = underTest.getAssetById(anEncodedAssetId());

    String expectedId = new RepositoryItemIDXO(REPOSITORY_NAME, toExternalId(AN_ASSET_ID).getValue()).getValue();
    assertThat(result.getId(), is(expectedId));
  }

  @Test
  void getAssetById_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.getAssetById(anEncodedAssetId()));
    verifyNoInteractions(fluentAssets);
  }

  // --- deleteAsset tests ---
  @WithUser(isAuthenticated = false)
  @Test
  void deleteAssetShouldDeleteAsset() {
    FluentAssetImpl assetToDelete = aFluentAsset();
    when(fluentAssets.find(any())).thenReturn(Optional.of(assetToDelete));

    underTest.deleteAsset(anEncodedAssetId());

    verify(maintenanceService).deleteAsset(repository, assetToDelete);
  }

  @WithUser(isAuthenticated = false)
  @Test
  void deleteAssetShouldThrowNotFoundExceptionWhenNotFound() {
    when(fluentAssets.find(new DetachedEntityId(AN_ASSET_ID + ""))).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> underTest.deleteAsset(anEncodedAssetId()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void deleteAsset_throwsNotFoundWhenAssetNotPermitted() {
    FluentAssetImpl fluentAsset = aFluentAsset();
    when(fluentAssets.find(any(DetachedEntityId.class))).thenReturn(Optional.of(fluentAsset));

    // Deny permission
    when(contentAuthHelper.checkPathPermissions(ASSET_PATH, A_FORMAT, REPOSITORY_NAME)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> underTest.deleteAsset(anEncodedAssetId()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void deleteAsset_throwsUnprocessableEntityOnIllegalArgument() {
    when(fluentAssets.find(any(DetachedEntityId.class)))
        .thenThrow(new IllegalArgumentException("bad id"));

    WebApplicationException e =
        assertThrows(WebApplicationException.class, () -> underTest.deleteAsset(anEncodedAssetId()));
    assertThat(e.getResponse().getStatus(), is(422));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void deleteAsset_doesNotDeleteWhenAssetNotFound() {
    when(fluentAssets.find(any(DetachedEntityId.class))).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> underTest.deleteAsset(anEncodedAssetId()));

    verify(maintenanceService, never()).deleteAsset(any(), any());
  }

  @Test
  void deleteAsset_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.deleteAsset(anEncodedAssetId()));
    verifyNoInteractions(fluentAssets, maintenanceService);
  }

  // --- browse (AssetsResourceSupport) pagination tests ---
  @WithUser(isAuthenticated = false)
  @Test
  void browse_trimsResultsWhenPermittedAssetsExceedLimit() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    // Return more than PAGE_SIZE_LIMIT assets in a single continuation
    int overLimit = PAGE_SIZE_LIMIT + 10;
    List<FluentAsset> assetList = createFluentAssets(overLimit);
    when(assetContinuation.isEmpty()).thenReturn(false);
    when(assetContinuation.stream()).thenReturn(assetList.stream());

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), hasSize(PAGE_SIZE_LIMIT));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void browse_continuesToFetchWhenPermissionFilterRemovesAllAssetsFromPage() {
    // First page: all assets denied
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(false);

    List<FluentAsset> firstBatch = List.of(aFluentAssetWithPath(10, "/denied1.jar"));
    when(assetContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(assetContinuation.stream()).thenReturn(firstBatch.stream());
    when(assetContinuation.nextContinuationToken()).thenReturn("token2");

    Continuation<FluentAsset> secondContinuation = mock(Continuation.class);
    when(secondContinuation.isEmpty()).thenReturn(true);
    when(fluentAssets.browse(eq(PAGE_SIZE_LIMIT), eq("token2"))).thenReturn(secondContinuation);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getItems(), is(empty()));
    // Verify it fetched the second page trying to find permitted assets
    verify(fluentAssets).browse(PAGE_SIZE_LIMIT, "token2");
  }

  // --- browseEager tests ---

  @Test
  void browseEager_returnsPermittedAssetsForHostedRepository() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    Continuation<FluentAsset> eagerContinuation = mock(Continuation.class);
    List<FluentAsset> assetList = List.of(aFluentAsset());
    when(eagerContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(eagerContinuation.stream()).thenReturn(assetList.stream());
    when(eagerContinuation.nextContinuationToken()).thenReturn("eager-token");

    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), isNull())).thenReturn(eagerContinuation);

    Continuation<FluentAsset> emptyEagerContinuation = mock(Continuation.class);
    when(emptyEagerContinuation.isEmpty()).thenReturn(true);
    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), eq("eager-token"))).thenReturn(emptyEagerContinuation);

    List<FluentAsset> result = underTest.browseEager(repository, null);

    assertThat(result, hasSize(1));
    verify(fluentAssets).browseEager(PAGE_SIZE_LIMIT, null);
  }

  @Test
  void browseEager_groupRepositoryUsesGroupMemberContent() {
    when(repository.getType()).thenReturn(new GroupType());
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    FluentQuery<FluentAsset> groupMemberQuery = mock(FluentQuery.class);
    when(fluentAssets.withOnlyGroupMemberContent()).thenReturn(groupMemberQuery);

    Continuation<FluentAsset> groupContinuation = mock(Continuation.class);
    when(groupContinuation.isEmpty()).thenReturn(true);
    when(groupMemberQuery.browseEager(eq(PAGE_SIZE_LIMIT), isNull())).thenReturn(groupContinuation);

    List<FluentAsset> result = underTest.browseEager(repository, null);

    verify(fluentAssets).withOnlyGroupMemberContent();
    verify(groupMemberQuery).browseEager(PAGE_SIZE_LIMIT, null);
    assertThat(result, is(empty()));
  }

  @Test
  void browseEager_filtersOutUnpermittedAssets() {
    String permittedPath = "/allowed.jar";
    String deniedPath = "/denied.jar";

    when(contentAuthHelper.checkPathPermissions(permittedPath, A_FORMAT, REPOSITORY_NAME)).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(deniedPath, A_FORMAT, REPOSITORY_NAME)).thenReturn(false);

    FluentAssetImpl permitted = aFluentAssetWithPath(10, permittedPath);
    FluentAssetImpl denied = aFluentAssetWithPath(11, deniedPath);

    Continuation<FluentAsset> eagerContinuation = mock(Continuation.class);
    List<FluentAsset> assetList = List.of(permitted, denied);
    when(eagerContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(eagerContinuation.stream()).thenReturn(assetList.stream());
    when(eagerContinuation.nextContinuationToken()).thenReturn("next");

    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), isNull())).thenReturn(eagerContinuation);

    Continuation<FluentAsset> emptyContinuation = mock(Continuation.class);
    when(emptyContinuation.isEmpty()).thenReturn(true);
    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), eq("next"))).thenReturn(emptyContinuation);

    List<FluentAsset> result = underTest.browseEager(repository, null);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).path(), is(permittedPath));
  }

  @Test
  void browseEager_trimsResultsToPageSizeLimit() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    int overLimit = PAGE_SIZE_LIMIT + 20;
    List<FluentAsset> assetList = createFluentAssets(overLimit);

    Continuation<FluentAsset> eagerContinuation = mock(Continuation.class);
    when(eagerContinuation.isEmpty()).thenReturn(false);
    when(eagerContinuation.stream()).thenReturn(assetList.stream());

    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), isNull())).thenReturn(eagerContinuation);

    List<FluentAsset> result = underTest.browseEager(repository, null);

    assertThat(result, hasSize(PAGE_SIZE_LIMIT));
  }

  @Test
  void browseEager_returnsEmptyListWhenNonePermitted() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(false);

    List<FluentAsset> assetList = List.of(aFluentAsset());

    Continuation<FluentAsset> eagerContinuation = mock(Continuation.class);
    when(eagerContinuation.isEmpty()).thenReturn(false).thenReturn(true);
    when(eagerContinuation.stream()).thenReturn(assetList.stream());
    when(eagerContinuation.nextContinuationToken()).thenReturn("next");

    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), isNull())).thenReturn(eagerContinuation);

    Continuation<FluentAsset> emptyContinuation = mock(Continuation.class);
    when(emptyContinuation.isEmpty()).thenReturn(true);
    when(fluentAssets.browseEager(eq(PAGE_SIZE_LIMIT), eq("next"))).thenReturn(emptyContinuation);

    List<FluentAsset> result = underTest.browseEager(repository, null);

    assertThat(result, is(empty()));
  }

  // --- toInternalToken tests ---

  @Test
  void toInternalToken_returnsNullForNullInput() {
    String result = AssetsResourceSupport.toInternalToken(null);
    assertThat(result, is(nullValue()));
  }

  @Test
  void toInternalToken_convertsExternalTokenToInternalToken() {
    // Use a known external ID
    String externalId = toExternalId(AN_ASSET_ID).getValue();
    String result = AssetsResourceSupport.toInternalToken(externalId);

    assertThat(result, is(notNullValue()));
    // The result should be the internal ID as a string (the integer value)
    assertThat(result, is(AN_ASSET_ID + ""));
  }

  // --- trim tests ---

  @Test
  void trim_doesNotModifyListSmallerThanLimit() {
    List<String> items = new ArrayList<>(List.of("a", "b", "c"));

    List<String> result = AssetsResourceSupport.trim(items, 5);

    assertThat(result, hasSize(3));
    assertThat(result, is(items));
  }

  @Test
  void trim_doesNotModifyListEqualToLimit() {
    List<String> items = new ArrayList<>(List.of("a", "b", "c"));

    List<String> result = AssetsResourceSupport.trim(items, 3);

    assertThat(result, hasSize(3));
  }

  @Test
  void trim_truncatesListLargerThanLimit() {
    List<String> items = new ArrayList<>(List.of("a", "b", "c", "d", "e"));

    List<String> result = AssetsResourceSupport.trim(items, 3);

    assertThat(result, hasSize(3));
    assertThat(result.get(0), is("a"));
    assertThat(result.get(1), is("b"));
    assertThat(result.get(2), is("c"));
  }

  @Test
  void trim_handlesEmptyList() {
    List<String> items = new ArrayList<>();

    List<String> result = AssetsResourceSupport.trim(items, 10);

    assertThat(result, is(empty()));
  }

  // --- toAssetXOs tests ---

  @Test
  void toAssetXOs_convertsFluentAssetsToAssetXOs() {
    List<FluentAsset> assets = List.of(aFluentAsset());

    List<AssetXO> result = underTest.toAssetXOs(repository, assets, emptyMap());

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getPath(), is(ASSET_PATH));
    assertThat(result.get(0).getRepository(), is(REPOSITORY_NAME));
  }

  @Test
  void toAssetXOs_returnsEmptyListForEmptyInput() {
    List<AssetXO> result = underTest.toAssetXOs(repository, emptyList(), emptyMap());

    assertThat(result, is(empty()));
  }

  @Test
  void toAssetXOs_convertsMultipleAssets() {
    FluentAssetImpl asset1 = aFluentAssetWithPath(1, "/path1.jar");
    FluentAssetImpl asset2 = aFluentAssetWithPath(2, "/path2.jar");

    List<AssetXO> result = underTest.toAssetXOs(repository, List.of(asset1, asset2), emptyMap());

    assertThat(result, hasSize(2));
    assertThat(result.get(0).getPath(), is("/path1.jar"));
    assertThat(result.get(1).getPath(), is("/path2.jar"));
  }

  // --- nextContinuationToken tests (indirectly through getAssets) ---
  @WithUser(isAuthenticated = false)
  @Test
  void nextContinuationToken_returnsNullWhenEmptyResult() {
    when(assetContinuation.isEmpty()).thenReturn(true);

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    assertThat(page.getContinuationToken(), is(nullValue()));
  }

  @WithUser(isAuthenticated = false)
  @Test
  void nextContinuationToken_returnsTokenBasedOnLastAssetId() {
    when(contentAuthHelper.checkPathPermissions(anyString(), eq(A_FORMAT), eq(REPOSITORY_NAME))).thenReturn(true);

    int lastAssetId = PAGE_SIZE_LIMIT;
    List<FluentAsset> assetList = createFluentAssets(PAGE_SIZE_LIMIT);
    when(assetContinuation.isEmpty()).thenReturn(false);
    when(assetContinuation.stream()).thenReturn(assetList.stream());

    Page<AssetXO> page = underTest.getAssets(null, REPOSITORY_NAME);

    // When exactly PAGE_SIZE_LIMIT results, continuation token should be the external ID of the last asset
    String expectedToken = toExternalId(lastAssetId).getValue();
    assertThat(page.getContinuationToken(), is(expectedToken));
  }

  // --- Helper methods ---

  private void mockRepository() {
    when(repositoryManagerRESTAdapter.getRepository(REPOSITORY_NAME)).thenReturn(repository);
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.assets()).thenReturn(fluentAssets);
    when(contentFacetSupport.dependencies()).thenReturn(dependencies);
    when(dependencies.getMoveService()).thenReturn(Optional.of(moveService));
    when(fluentAssets.browse(PAGE_SIZE_LIMIT, null)).thenReturn(assetContinuation);
    when(assetContinuation.isEmpty()).thenReturn(true);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getUrl()).thenReturn(REPOSITORY_URL);
    when(repository.getFormat()).thenReturn(aFormat);
    when(repository.getType()).thenReturn(new HostedType());
    when(aFormat.getValue()).thenReturn(A_FORMAT);
  }

  private FluentAssetImpl aFluentAsset() {
    return new FluentAssetImpl(contentFacetSupport, anAsset());
  }

  private FluentAssetImpl aFluentAssetWithPath(final int assetId, final String path) {
    AssetData asset = new AssetData();
    asset.setAssetId(assetId);
    asset.setPath(path);
    asset.setCreated(OffsetDateTime.now());
    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setAssetBlobId(assetId);
    assetBlob.setBlobCreated(BLOB_CREATED);
    asset.setAssetBlob(assetBlob);
    return new FluentAssetImpl(contentFacetSupport, asset);
  }

  private List<FluentAsset> createFluentAssets(final int count) {
    return createFluentAssetsStartingAt(1, count);
  }

  private List<FluentAsset> createFluentAssetsStartingAt(final int startId, final int count) {
    return IntStream.range(startId, startId + count)
        .mapToObj(i -> (FluentAsset) aFluentAssetWithPath(i, "/path/" + i + ".jar"))
        .collect(toList());
  }

  private AssetData anAsset() {
    AssetData asset = new AssetData();
    asset.setAssetId(AN_ASSET_ID);
    asset.setPath(ASSET_PATH);
    asset.setCreated(OffsetDateTime.now());
    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setAssetBlobId(1);
    assetBlob.setBlobCreated(BLOB_CREATED);
    asset.setAssetBlob(assetBlob);
    return asset;
  }

  private AssetXO anAssetXO() {
    return AssetXO.builder()
        .path(ASSET_PATH)
        .downloadUrl(REPOSITORY_URL + ASSET_PATH)
        .id(new RepositoryItemIDXO(REPOSITORY_NAME, toExternalId(AN_ASSET_ID).getValue()).getValue())
        .repository(REPOSITORY_NAME)
        .checksum(emptyMap())
        .lastModified(new Date(BLOB_CREATED.toInstant().toEpochMilli()))
        .build();
  }

  private String anEncodedAssetId() {
    return getUrlEncoder().encodeToString((REPOSITORY_NAME + ":" + AN_ASSET_ID).getBytes());
  }
}
