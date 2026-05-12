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
package org.sonatype.nexus.cleanup.internal.content.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.sonatype.nexus.cleanup.content.search.CleanupBrowseServiceFactory;
import org.sonatype.nexus.cleanup.content.search.CleanupComponentBrowse;
import org.sonatype.nexus.cleanup.internal.storage.CleanupPolicyData;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyCriteria;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyPreviewXO;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentImpl;
import org.sonatype.nexus.repository.content.store.AssetBlobData;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.*;

@ExtendWith(MockitoExtension.class)
class CleanupPreviewHelperImplTest

{

  private static final OffsetDateTime NOW = OffsetDateTime.now();

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private Duration previewTimeout;

  @Mock
  private CleanupBrowseServiceFactory browseServiceFactory;

  @Mock
  private CleanupComponentBrowse cleanupComponentBrowse;

  @InjectMocks
  private CleanupPreviewHelperImpl underTest;

  @Captor
  private ArgumentCaptor<CleanupPolicy> cleanPolicyCaptor;

  @BeforeEach
  void setup() {
    when(cleanupPolicyStorage.newCleanupPolicy()).thenReturn(new CleanupPolicyData());
    when(browseServiceFactory.getPreviewService()).thenReturn(cleanupComponentBrowse);
  }

  @Test
  void testGetSearchResults() {
    when(previewTimeout.toMillis()).thenReturn(60L);

    Collection<Component> components = getComponentCollection();
    when(cleanupComponentBrowse.browseByPage(any(CleanupPolicy.class), any(Repository.class), any(QueryOptions.class)))
        .thenReturn(new PagedResponse<>(2, components));

    CleanupPolicyPreviewXO cleanupPolicyPreviewXO = createCleanupPolicyPreviewXO();
    Repository repository = createRepository();
    QueryOptions queryOptions = createQueryOptions();

    PagedResponse<ComponentXO> componentPagedResponse =
        underTest.getSearchResults(cleanupPolicyPreviewXO, repository, queryOptions);

    verify(previewTimeout).toMillis();
    verify(cleanupPolicyStorage).newCleanupPolicy();
    verify(browseServiceFactory).getPreviewService();
    verify(cleanupComponentBrowse).browseByPage(cleanPolicyCaptor.capture(), eq(repository), eq(queryOptions));

    assertCleanupPolicy(cleanPolicyCaptor.getValue());

    assertThat(componentPagedResponse.getTotal(), is(2L));
    assertComponentResultXO(componentPagedResponse.getData().stream(), false);
  }

  @Test
  void testGetSearchResultsStream() {
    Collection<Component> componentCollection = getComponentCollection();
    ContentFacetSupport contentFacetSupport = mock(ContentFacetSupport.class);
    ContentFacetStores contentFacetStores = mock(ContentFacetStores.class);
    AssetStore assetStore = mock(AssetStore.class);

    when(contentFacetSupport.stores()).thenReturn(contentFacetStores);
    when(contentFacetStores.assetStore()).thenReturn(assetStore);

    AssetBlobData assetBlob = new AssetBlobData();
    assetBlob.setAssetBlobId(1);
    assetBlob.setBlobCreated(NOW);

    AssetData asset = new AssetData();
    asset.setPath("/assetPath");
    asset.setBlobStoreName("blobStoreName");
    asset.setAssetBlobSize(100L);
    asset.setLastDownloaded(NOW);
    asset.setCreated(NOW);
    asset.setAssetBlob(assetBlob);

    when(assetStore.browseComponentAssets(any(Component.class))).thenReturn(Collections.singleton(asset));

    Stream<FluentComponent> fluentComponentStream = componentCollection.stream()
        .map(component -> new FluentComponentImpl(contentFacetSupport, component));

    when(cleanupComponentBrowse.browseIncludingAssets(any(CleanupPolicy.class), any(Repository.class)))
        .thenReturn(fluentComponentStream);

    CleanupPolicyPreviewXO cleanupPolicyPreviewXO = createCleanupPolicyPreviewXO();
    Repository repository = createRepository();

    Stream<ComponentXO> componentXOStream =
        underTest.getSearchResultsStream(cleanupPolicyPreviewXO, repository, createQueryOptions());

    verify(cleanupPolicyStorage).newCleanupPolicy();
    verify(browseServiceFactory).getPreviewService();
    verify(cleanupComponentBrowse).browseIncludingAssets(cleanPolicyCaptor.capture(), eq(repository));

    assertCleanupPolicy(cleanPolicyCaptor.getValue());
    assertComponentResultXO(componentXOStream, true);
  }

  @Test
  void testGetSearchResultsStream_withNullBlob() {
    Collection<Component> componentCollection = getComponentCollection();
    ContentFacetSupport contentFacetSupport = mock(ContentFacetSupport.class);
    ContentFacetStores contentFacetStores = mock(ContentFacetStores.class);
    AssetStore assetStore = mock(AssetStore.class);

    when(contentFacetSupport.stores()).thenReturn(contentFacetStores);
    when(contentFacetStores.assetStore()).thenReturn(assetStore);

    AssetData asset = new AssetData();
    asset.setPath("/assetPath");
    asset.setBlobStoreName("blobStoreName");
    asset.setAssetBlobSize(100L);
    asset.setLastDownloaded(NOW);
    asset.setCreated(NOW);
    asset.setAssetBlob(null);

    when(assetStore.browseComponentAssets(any(Component.class))).thenReturn(Collections.singleton(asset));

    Stream<FluentComponent> fluentComponentStream = componentCollection.stream()
        .map(component -> new FluentComponentImpl(contentFacetSupport, component));

    when(cleanupComponentBrowse.browseIncludingAssets(any(CleanupPolicy.class), any(Repository.class)))
        .thenReturn(fluentComponentStream);

    CleanupPolicyPreviewXO cleanupPolicyPreviewXO = createCleanupPolicyPreviewXO();
    Repository repository = createRepository();

    Stream<ComponentXO> componentXOStream =
        underTest.getSearchResultsStream(cleanupPolicyPreviewXO, repository, createQueryOptions());

    verify(cleanupPolicyStorage).newCleanupPolicy();
    verify(browseServiceFactory).getPreviewService();
    verify(cleanupComponentBrowse).browseIncludingAssets(cleanPolicyCaptor.capture(), eq(repository));

    assertCleanupPolicy(cleanPolicyCaptor.getValue());

    List<ComponentXO> componentsResult = componentXOStream.toList();
    assertThat(componentsResult, hasSize(2));

    componentsResult.forEach(componentXO -> {
      List<AssetXO> assets = componentXO.getAssets();
      assertThat(assets, hasSize(1));

      AssetXO assetXO = assets.get(0);
      assertThat(assetXO.getPath(), is("/assetPath"));
      assertThat(assetXO.getBlobStoreName(), is("blobStoreName"));
      assertThat(assetXO.getFileSize(), is(100L));

      Date nowDate = Date.from(NOW.toInstant());
      assertThat(assetXO.getLastDownloaded(), is(nowDate));
      assertThat(assetXO.getBlobCreated(), is((Date) null));
    });
  }

  private static void assertComponentResultXO(final Stream<ComponentXO> componentXOStream, final boolean withAssets) {
    List<ComponentXO> componentsResult = componentXOStream.toList();
    assertThat(componentsResult, hasSize(2));

    ComponentXO componentXO1 = componentsResult.get(0);
    assertThat(componentXO1.getRepository(), is("repositoryName"));
    assertThat(componentXO1.getGroup(), is("my-namespace-1"));
    assertThat(componentXO1.getName(), is("my-component-1"));
    assertThat(componentXO1.getVersion(), is("1.0"));
    assertThat(componentXO1.getFormat(), is("test-format"));

    ComponentXO componentXO2 = componentsResult.get(1);
    assertThat(componentXO2.getRepository(), is("repositoryName"));
    assertThat(componentXO2.getGroup(), is("my-namespace-2"));
    assertThat(componentXO2.getName(), is("my-component-2"));
    assertThat(componentXO2.getVersion(), is("2.0"));
    assertThat(componentXO2.getFormat(), is("test-format"));

    if (withAssets) {
      componentsResult.forEach(componentXO -> {
        List<AssetXO> assets = componentXO.getAssets();
        assertThat(assets, hasSize(1));

        AssetXO asset = assets.get(0);
        assertThat(asset.getPath(), is("/assetPath"));
        assertThat(asset.getBlobStoreName(), is("blobStoreName"));
        assertThat(asset.getFileSize(), is(100L));

        Date nowDate = Date.from(NOW.toInstant());
        assertThat(asset.getLastDownloaded(), is(nowDate));
        assertThat(asset.getBlobCreated(), is(nowDate));
      });
    }
  }

  private static void assertCleanupPolicy(final CleanupPolicy cleanupPolicy) {
    assertThat(cleanupPolicy.getCriteria(),
        allOf(hasEntry(LAST_BLOB_UPDATED_KEY, "86400"), hasEntry(LAST_DOWNLOADED_KEY, "86400"),
            hasEntry(IS_PRERELEASE_KEY, "false"), hasEntry(REGEX_KEY, "criteriaAssetRegex"),
            hasEntry(RETAIN_KEY, "1"), hasEntry(RETAIN_SORT_BY_KEY, "name")));
    assertThat(cleanupPolicy.getName(), is("preview"));
  }

  private static CleanupPolicyPreviewXO createCleanupPolicyPreviewXO() {
    CleanupPolicyPreviewXO cleanupPolicyPreviewXO = new CleanupPolicyPreviewXO();
    cleanupPolicyPreviewXO.setRepositoryName("repositoryName");
    CleanupPolicyCriteria criteria =
        new CleanupPolicyCriteria(1, 1, CleanupPolicyReleaseType.RELEASES, "criteriaAssetRegex", 1, "name");
    cleanupPolicyPreviewXO.setCriteria(criteria);
    return cleanupPolicyPreviewXO;
  }

  private static Collection<Component> getComponentCollection() {
    ComponentData component1 = new ComponentData();
    component1.setName("my-component-1");
    component1.setNamespace("my-namespace-1");
    component1.setVersion("1.0");
    ComponentData component2 = new ComponentData();
    component2.setName("my-component-2");
    component2.setNamespace("my-namespace-2");
    component2.setVersion("2.0");
    return List.of(component1, component2);
  }

  private static QueryOptions createQueryOptions() {
    return new QueryOptions("my-component-1", "name", "asc", 0, 100);
  }

  private static Repository createRepository() {
    Repository repository = mock(Repository.class);
    when(repository.getName()).thenReturn("repositoryName");
    Format formatMock = mock(Format.class);
    when(repository.getFormat()).thenReturn(formatMock);
    when(formatMock.getValue()).thenReturn("test-format");
    return repository;
  }
}
