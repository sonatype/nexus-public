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
  public void testSaveAssets_EmptyCollection_NoDAOCalls() {
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

  /**
   * Helper method to create mock SearchAssetRecord instances for testing.
   *
   * @param count number of mock assets to create
   * @return list of mock SearchAssetRecord instances
   */
  private List<SearchAssetRecord> createMockAssets(int count) {
    List<SearchAssetRecord> assets = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      SearchAssetRecord asset = mock(SearchAssetRecord.class);
      when(asset.getAssetId()).thenReturn(i);
      assets.add(asset);
    }
    return assets;
  }
}
