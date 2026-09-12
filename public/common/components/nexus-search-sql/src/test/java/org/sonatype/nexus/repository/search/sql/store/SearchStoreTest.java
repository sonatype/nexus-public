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
package org.sonatype.nexus.repository.search.sql.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.search.sql.SearchAssetRecord;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link SearchStore} focusing on asset batching functionality.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchStoreTest
{
  @Mock
  private DataSessionSupplier sessionSupplier;

  @Mock
  private DataSession<Transaction> dataSession;

  @Mock
  private SearchTableDAO dao;

  @Mock
  private Transaction transaction;

  private SearchStore underTest;

  private static final int DEFAULT_BATCH_SIZE = 1500;

  @Before
  public void setup() {
    when(dataSession.access(SearchTableDAO.class)).thenReturn(dao);
    when(dataSession.getTransaction()).thenReturn(transaction);

    UnitOfWork.beginBatch(dataSession);
  }

  @After
  public void teardown() {
    UnitOfWork.end();
  }

  @Test
  public void testSaveAssets_SmallBatch_NoBatching() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = createMockAssets(100);

    underTest.saveAssets(assets);

    verify(dao, times(1)).saveAssets(any());
    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao).saveAssets(captor.capture());
    assertThat(captor.getValue().size(), is(100));
  }

  @Test
  public void testSaveAssets_ExactlyBatchSize_NoBatching() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = createMockAssets(DEFAULT_BATCH_SIZE);

    underTest.saveAssets(assets);

    verify(dao, times(1)).saveAssets(any());
    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao).saveAssets(captor.capture());
    assertThat(captor.getValue().size(), is(DEFAULT_BATCH_SIZE));
  }

  @Test
  public void testSaveAssets_LargeBatch_SplitsIntoTwoBatches() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = createMockAssets(2500);

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(2)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    assertThat(batches.get(0).size(), is(DEFAULT_BATCH_SIZE));
    assertThat(batches.get(1).size(), is(1000));
  }

  @Test
  public void testSaveAssets_VeryLargeBatch_SplitsIntoMultipleBatches() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = createMockAssets(4500);

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(3)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    assertThat(batches.get(0).size(), is(DEFAULT_BATCH_SIZE));
    assertThat(batches.get(1).size(), is(DEFAULT_BATCH_SIZE));
    assertThat(batches.get(2).size(), is(DEFAULT_BATCH_SIZE));
  }

  @Test
  public void testSaveAssets_NotEvenlyDivisible_HandlesRemainder() {
    underTest = new SearchStore(sessionSupplier, 1000, 2000);
    List<SearchAssetRecord> assets = createMockAssets(4500);

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(3)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    assertThat(batches.get(0).size(), is(2000));
    assertThat(batches.get(1).size(), is(2000));
    assertThat(batches.get(2).size(), is(500));
  }

  @Test
  public void testSaveAssets_CustomBatchSize_UsesConfiguredValue() {
    underTest = new SearchStore(sessionSupplier, 1000, 1000);
    List<SearchAssetRecord> assets = createMockAssets(2500);

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(3)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    batches.forEach(batch -> assertThat(batch.size(), lessThanOrEqualTo(1000)));
  }

  @Test
  public void testSaveAssets_PostgreSQLParameterLimit_StaysSafe() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = createMockAssets(4683);

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(4)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    batches.forEach(batch -> {
      int paramCount = batch.size() * 25;
      assertThat("Batch must stay under PostgreSQL parameter limit",
          paramCount, lessThanOrEqualTo(65535));
    });
  }

  @Test
  public void testSaveAssets_EmptyCollection_SingleDAOCall() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = new ArrayList<>();

    underTest.saveAssets(assets);

    verify(dao, times(1)).saveAssets(assets);
  }

  @Test
  public void testSaveAssets_SingleAsset_NoBatching() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchAssetRecord> assets = createMockAssets(1);

    underTest.saveAssets(assets);

    verify(dao, times(1)).saveAssets(any());
    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao).saveAssets(captor.capture());
    assertThat(captor.getValue().size(), is(1));
  }

  @Test
  public void testSaveAssets_SetInput_SmallBatch_NoBatching() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    Set<SearchAssetRecord> assets = new HashSet<>(createMockAssets(100));

    underTest.saveAssets(assets);

    verify(dao, times(1)).saveAssets(any());
    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao).saveAssets(captor.capture());
    assertThat(captor.getValue().size(), is(100));
  }

  @Test
  public void testSaveAssets_SetInput_LargeBatch_SplitsIntoBatches() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    Set<SearchAssetRecord> assets = new HashSet<>(createMockAssets(2500));

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(2)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    assertThat(batches.get(0).size(), is(DEFAULT_BATCH_SIZE));
    assertThat(batches.get(1).size(), is(1000));
  }

  @Test
  public void testSaveAssets_SetInput_VeryLargeBatch_SplitsIntoMultipleBatches() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    Set<SearchAssetRecord> assets = new HashSet<>(createMockAssets(4500));

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(3)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    assertThat(batches.get(0).size(), is(DEFAULT_BATCH_SIZE));
    assertThat(batches.get(1).size(), is(DEFAULT_BATCH_SIZE));
    assertThat(batches.get(2).size(), is(DEFAULT_BATCH_SIZE));
  }

  @Test
  public void testSaveAssets_SetInput_NotEvenlyDivisible_HandlesRemainder() {
    underTest = new SearchStore(sessionSupplier, 1000, 2000);
    Set<SearchAssetRecord> assets = new HashSet<>(createMockAssets(4500));

    underTest.saveAssets(assets);

    ArgumentCaptor<Collection<SearchAssetRecord>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(dao, times(3)).saveAssets(captor.capture());

    List<Collection<SearchAssetRecord>> batches = captor.getAllValues();
    assertThat(batches.get(0).size(), is(2000));
    assertThat(batches.get(1).size(), is(2000));
    assertThat(batches.get(2).size(), is(500));
  }

  @Test
  public void testDeleteAllForRepository_NothingToDelete_ReturnsFalse() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteAllForRepository(42, "maven2", 1000)).thenReturn(false);

    boolean result = underTest.deleteAllForRepository(42, "maven2");

    assertFalse(result);
    verify(dao).deleteAllForRepository(42, "maven2", 1000);
  }

  @Test
  public void testDeleteAllForRepository_MultipleBatches_ReturnsTrue() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteAllForRepository(42, "maven2", 1000))
        .thenReturn(true)
        .thenReturn(false);

    boolean result = underTest.deleteAllForRepository(42, "maven2");

    assertTrue(result);
    verify(dao, times(2)).deleteAllForRepository(42, "maven2", 1000);
  }

  @Test
  public void testDeleteAllSearchAssets_NothingToDelete_ReturnsFalse() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteAllSearchAssets(42, "maven2", 1000)).thenReturn(false);

    boolean result = underTest.deleteAllSearchAssets(42, "maven2");

    assertFalse(result);
    verify(dao).deleteAllSearchAssets(42, "maven2", 1000);
  }

  @Test
  public void testDeleteAllSearchAssets_MultipleBatches_ReturnsTrue() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteAllSearchAssets(42, "maven2", 1000))
        .thenReturn(true)
        .thenReturn(false);

    boolean result = underTest.deleteAllSearchAssets(42, "maven2");

    assertTrue(result);
    verify(dao, times(2)).deleteAllSearchAssets(42, "maven2", 1000);
  }

  @Test
  public void testRepositoryNeedsReindex_NoEntries_ReturnsTrue() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.hasRepositoryEntries("my-repo")).thenReturn(false);

    assertTrue(underTest.repositoryNeedsReindex("my-repo"));
  }

  @Test
  public void testRepositoryNeedsReindex_HasEntries_ReturnsFalse() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.hasRepositoryEntries("my-repo")).thenReturn(true);

    assertFalse(underTest.repositoryNeedsReindex("my-repo"));
  }

  @Test
  public void testGetSearchRepositories_ReturnsDaoResult() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchRepositoryData> expected = List.of(mock(SearchRepositoryData.class));
    when(dao.getSearchRepositories()).thenReturn(expected);

    assertThat(underTest.getSearchRepositories(), is(expected));
  }

  @Test
  public void testSave_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    SearchRecordData data = mock(SearchRecordData.class);

    underTest.save(data);

    verify(dao).save(data);
  }

  @Test
  public void testSaveBatch_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    List<SearchRecordData> batch = List.of(mock(SearchRecordData.class));

    underTest.saveBatch(batch);

    verify(dao).saveBatch(batch);
  }

  @Test
  public void testDelete_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);

    underTest.delete(42, 100, "maven2");

    verify(dao).delete(42, 100, "maven2");
  }

  @Test
  public void testDeleteComponentIds_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    Set<Integer> ids = Set.of(1, 2, 3);

    underTest.deleteComponentIds(42, ids, "maven2");

    verify(dao).deleteComponentIds(42, ids, "maven2");
  }

  @Test
  public void testDeleteSearchAssets_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    Set<Integer> ids = Set.of(1, 2, 3);

    underTest.deleteSearchAssets(42, ids, "maven2");

    verify(dao).deleteSearchAssets(42, ids, "maven2");
  }

  @Test
  public void testSaveAsset_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    SearchAssetRecord asset = mock(SearchAssetRecord.class);

    underTest.saveAsset(asset);

    verify(dao).saveAsset(asset);
  }

  @Test(expected = RuntimeException.class)
  public void testDelete_DaoThrows_PropagatesException() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    org.mockito.Mockito.doThrow(new RuntimeException("db error")).when(dao).delete(42, 100, "maven2");

    underTest.delete(42, 100, "maven2");
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteComponentIds_DaoThrows_PropagatesException() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    org.mockito.Mockito.doThrow(new RuntimeException("db error")).when(dao).deleteComponentIds(any(), any(), any());

    underTest.deleteComponentIds(42, Set.of(1), "maven2");
  }

  @Test(expected = RuntimeException.class)
  public void testDeleteSearchAssets_DaoThrows_PropagatesException() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    org.mockito.Mockito.doThrow(new RuntimeException("db error")).when(dao).deleteSearchAssets(any(), any(), any());

    underTest.deleteSearchAssets(42, Set.of(1), "maven2");
  }

  @Test(expected = RuntimeException.class)
  public void testSaveBatch_DaoThrows_PropagatesException() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    org.mockito.Mockito.doThrow(new RuntimeException("db error")).when(dao).saveBatch(any());

    underTest.saveBatch(java.util.Collections.emptyList());
  }

  @Test(expected = RuntimeException.class)
  public void testSaveAsset_DaoThrows_PropagatesException() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    org.mockito.Mockito.doThrow(new RuntimeException("db error")).when(dao).saveAsset(any());

    underTest.saveAsset(mock(SearchAssetRecord.class));
  }

  @Test(expected = RuntimeException.class)
  public void testGetSearchRepositories_DaoThrows_PropagatesException() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.getSearchRepositories()).thenThrow(new RuntimeException("db error"));

    underTest.getSearchRepositories();
  }

  @Test
  public void testSearchComponents_WithSortColumn_PassesSortToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.searchComponents(any())).thenReturn(java.util.Collections.emptyList());

    underTest.searchComponents(10, 0, null, "version", org.sonatype.nexus.repository.search.SortDirection.ASC, false);

    verify(dao).searchComponents(any());
  }

  @Test
  public void testDeleteOrphanedComponents_SingleBatch() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteOrphanedComponents(42, "maven2", 1000)).thenReturn(false);

    underTest.deleteOrphanedComponents(42, "maven2");

    verify(dao).deleteOrphanedComponents(42, "maven2", 1000);
  }

  @Test
  public void testDeleteOrphanedComponents_MultipleBatches() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteOrphanedComponents(42, "maven2", 1000))
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(false);

    underTest.deleteOrphanedComponents(42, "maven2");

    // DAO called 3 times: two batches returning true (triggering commitChangesSoFar),
    // then a termination check returning false to exit the loop.
    verify(dao, times(3)).deleteOrphanedComponents(42, "maven2", 1000);
  }

  @Test
  public void testDeleteOrphanedAssets_SingleBatch() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteOrphanedAssets(42, "maven2", 1000)).thenReturn(false);

    underTest.deleteOrphanedAssets(42, "maven2");

    verify(dao).deleteOrphanedAssets(42, "maven2", 1000);
  }

  @Test
  public void testDeleteOrphanedAssets_MultipleBatches() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    when(dao.deleteOrphanedAssets(42, "maven2", 1000))
        .thenReturn(true)
        .thenReturn(false);

    underTest.deleteOrphanedAssets(42, "maven2");

    // DAO called 2 times: one batch returning true (triggering commitChangesSoFar),
    // then a termination check returning false to exit the loop.
    verify(dao, times(2)).deleteOrphanedAssets(42, "maven2", 1000);
  }

  private List<SearchAssetRecord> createMockAssets(int count) {
    List<SearchAssetRecord> assets = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      SearchAssetRecord asset = mock(SearchAssetRecord.class);
      when(asset.getAssetId()).thenReturn(i);
      assets.add(asset);
    }
    return assets;
  }

  @Test
  public void browseComponentVersions_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder()
        .filter("cs.format = #{filterParams.format}")
        .filterParams(Map.of("format", "maven2"))
        .build();
    ComponentVersionData row = new ComponentVersionData();
    row.setVersion("1.0.0");
    when(dao.browseComponentVersions(request)).thenReturn(List.of(row));

    assertThat(underTest.browseComponentVersions(request), is(List.of(row)));
  }

  @Test
  public void countComponentVersions_DelegatesToDao() {
    underTest = new SearchStore(sessionSupplier, 1000, DEFAULT_BATCH_SIZE);
    ComponentVersionSearchRequest request = ComponentVersionSearchRequest.builder().build();
    when(dao.countComponentVersions(request)).thenReturn(7L);

    assertThat(underTest.countComponentVersions(request), is(7L));
  }
}
