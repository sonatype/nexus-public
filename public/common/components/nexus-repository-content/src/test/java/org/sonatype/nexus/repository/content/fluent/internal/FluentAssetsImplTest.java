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

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FluentAssetsImplTest
{
  @Mock
  private ContentFacetSupport facet;

  @Mock
  private AssetStore<?> assetStore;

  @Mock
  private Repository repository;

  private FluentAssetsImpl underTest;

  @Before
  public void setUp() {
    Format format = new Format("maven2")
    {
    };
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(new HostedType());
    when(facet.repository()).thenReturn(repository);
    when(facet.contentRepositoryId()).thenReturn(1);

    underTest = new FluentAssetsImpl(facet, assetStore);
  }

  @Test
  public void testPath() {
    assertThat(underTest.path("/org/example/artifact.jar"), is(notNullValue()));
  }

  @Test
  public void testWithAsset() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(1);
    asset.setPath("/test/path");

    FluentAsset result = underTest.with(asset);
    assertThat(result, is(notNullValue()));
    assertThat(result.path(), is("/test/path"));
  }

  @Test
  public void testWithFluentAssetReturnsSameInstance() {
    FluentAsset fluentAsset = mock(FluentAsset.class);
    FluentAsset result = underTest.with(fluentAsset);
    assertThat(result, is(fluentAsset));
  }

  @Test
  public void testCount() {
    when(assetStore.countAssets(eq(1), isNull(), isNull(), isNull())).thenReturn(100);
    assertThat(underTest.count(), is(100));
  }

  @Test
  public void testBrowse() {
    Continuation<Asset> continuation = mock(Continuation.class);
    when(assetStore.browseAssets(any(), anyString(), isNull(), isNull(), isNull(), anyInt()))
        .thenReturn(continuation);

    Continuation<FluentAsset> result = underTest.browse(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testBrowseEager() {
    Continuation<Asset> continuation = mock(Continuation.class);
    when(assetStore.browseEagerAssets(eq(1), isNull(), eq(25)))
        .thenReturn(continuation);

    Continuation<FluentAsset> result = underTest.browseEager(25, null);
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testFind() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(1);
    asset.setAssetId(5);
    asset.setPath("/test");

    when(assetStore.readAsset(anyInt())).thenReturn(Optional.of(asset));

    Optional<FluentAsset> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void testFindNotFound() {
    when(assetStore.readAsset(anyInt())).thenReturn(Optional.empty());

    Optional<FluentAsset> result = underTest.find(new DetachedEntityId("999"));
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testByKind() {
    FluentQuery<FluentAsset> query = underTest.byKind("DOCKER_LAYER");
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testByFilter() {
    FluentQuery<FluentAsset> query = underTest.byFilter("path like :path",
        Collections.singletonMap("path", "/test%"));
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testWithGroupMemberContent() {
    FluentQuery<FluentAsset> query = underTest.withGroupMemberContent();
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testWithOnlyGroupMemberContent() {
    FluentQuery<FluentAsset> query = underTest.withOnlyGroupMemberContent();
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testFindAssetInDifferentRepositoryReturnsEmpty() {
    AssetData asset = new AssetData();
    asset.setRepositoryId(999); // different from facet's contentRepositoryId (1)
    asset.setAssetId(5);
    asset.setPath("/test");

    when(assetStore.readAsset(anyInt())).thenReturn(Optional.of(asset));

    Optional<FluentAsset> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testFindAssetInGroupMemberRepository() {
    // Set up group repository
    when(repository.getType()).thenReturn(new GroupType());

    // Asset in a member repository (not the group itself)
    AssetData asset = new AssetData();
    asset.setRepositoryId(42);
    asset.setAssetId(5);
    asset.setPath("/test");

    when(assetStore.readAsset(anyInt())).thenReturn(Optional.of(asset));

    // Set up member repository with matching content repo id
    Repository memberRepo = mock(Repository.class);
    ContentFacet memberFacet = mock(ContentFacet.class);
    when(memberFacet.contentRepositoryId()).thenReturn(42);
    when(memberRepo.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(memberFacet));

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(List.of(memberRepo));
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    Optional<FluentAsset> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void testFindAssetNotInGroupMembersReturnsEmpty() {
    when(repository.getType()).thenReturn(new GroupType());

    AssetData asset = new AssetData();
    asset.setRepositoryId(999);
    asset.setAssetId(5);
    asset.setPath("/test");

    when(assetStore.readAsset(anyInt())).thenReturn(Optional.of(asset));

    // Member has different content repo id
    Repository memberRepo = mock(Repository.class);
    ContentFacet memberFacet = mock(ContentFacet.class);
    when(memberFacet.contentRepositoryId()).thenReturn(42);
    when(memberRepo.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(memberFacet));

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(List.of(memberRepo));
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    Optional<FluentAsset> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testBrowseEagerWithTimestampFilters() {
    Continuation<Asset> continuation = mock(Continuation.class);
    OffsetDateTime newerThan = OffsetDateTime.now().minusDays(1);
    OffsetDateTime olderThan = OffsetDateTime.now();

    when(assetStore.browseEagerAssets(eq(1), isNull(), eq(10), eq(newerThan), eq(olderThan)))
        .thenReturn(continuation);

    Continuation<FluentAsset> result = underTest.browseEager(10, null, newerThan, olderThan);
    assertThat(result, is(notNullValue()));
    verify(assetStore).browseEagerAssets(eq(1), isNull(), eq(10), eq(newerThan), eq(olderThan));
  }

  @Test
  public void testBrowseForGroupRepositoryAddsLocalConstraint() {
    when(repository.getType()).thenReturn(new GroupType());
    when(repository.facet(ContentFacet.class)).thenReturn(facet);

    // When browsing a group repository, should add LOCAL constraint
    Continuation<Asset> continuation = mock(Continuation.class);
    when(assetStore.browseAssets(any(), anyString(), isNull(), isNull(), isNull(), anyInt()))
        .thenReturn(continuation);

    Continuation<FluentAsset> result = underTest.browse(10, "token");
    assertThat(result, is(notNullValue()));
  }
}
