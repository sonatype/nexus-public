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
package org.sonatype.nexus.blobstore.compact.internal;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreConfiguration;
import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreStore;
import org.sonatype.nexus.scheduling.RecoveryModeService;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskUtils;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.common.BlobStoreParallelTaskSupport.ALL;
import static org.sonatype.nexus.blobstore.common.BlobStoreParallelTaskSupport.BLOBSTORE_NAME_FIELD_ID;
import static org.sonatype.nexus.blobstore.compact.internal.CompactBlobStoreTaskDescriptor.TYPE_ID;

public class CompactBlobStoreTaskTest
    extends TestSupport
{
  private final String BLOBSTORE_NAME = "test";

  private final String TASK_NAME = "test-task";

  @Mock
  ChangeRepositoryBlobStoreStore changeBlobstoreStore;

  @Mock
  BlobStoreUsageChecker blobStoreUsageChecker;

  @Mock
  TaskUtils taskUtils;

  @Mock
  RecoveryModeService recoveryModeService;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  SecurityManager securityManager;

  TaskConfiguration configuration;

  CompactBlobStoreTask underTest;

  @Before
  public void setUp() {
    ThreadContext.bind(securityManager);

    configuration = new TaskConfiguration();
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, BLOBSTORE_NAME);
    configuration.setString(".name", TASK_NAME);
    configuration.setTypeId(TYPE_ID);
    configuration.setId(TASK_NAME);

    underTest =
        new CompactBlobStoreTask(changeBlobstoreStore, blobStoreUsageChecker, taskUtils, recoveryModeService, 5, 20);
    underTest.install(blobStoreManager);
  }

  @After
  public void tearDown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void checkForConflictsThrowsExceptionIfConflictingTaskIsRunning() {
    underTest.configure(configuration);

    BlobStore blobStore = mock(BlobStore.class);
    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    when(config.getName()).thenReturn(BLOBSTORE_NAME);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);

    doThrow(new IllegalStateException("conflicting task"))
        .when(taskUtils)
        .checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    when(changeBlobstoreStore.findByBlobStoreName(anyString())).thenReturn(Collections.emptyList());

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> underTest.checkForConflicts(BLOBSTORE_NAME));

    assertThat(exception.getMessage(), is("conflicting task"));
    verify(taskUtils, times(1)).checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
  }

  @Test
  public void checkForConflictsThrowsExceptionIfMoveTaskIsUnfinished() {
    ChangeRepositoryBlobStoreConfiguration record = getRecord("test", BLOBSTORE_NAME, "target-blobstore");

    underTest.configure(configuration);

    BlobStore blobStore = mock(BlobStore.class);
    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    when(config.getName()).thenReturn(BLOBSTORE_NAME);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(config);

    doNothing()
        .when(taskUtils)
        .checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    when(changeBlobstoreStore.findByBlobStoreName(anyString())).thenReturn(Collections.singletonList(record));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> underTest.checkForConflicts(BLOBSTORE_NAME));

    assertThat(exception.getMessage(),
        is(String.format("found unfinished move task(s) using blobstore '%s', task can't be executed",
            BLOBSTORE_NAME)));
    verify(taskUtils, times(1)).checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    verify(changeBlobstoreStore, times(1)).findByBlobStoreName(eq(BLOBSTORE_NAME));
  }

  @Test
  public void testAllBlobStores() throws Exception {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, ALL);

    List<BlobStore> blobStores = runCompact("blobstore-one", "blobstore-two");

    assertThat("All blob stores should be processed", underTest.result(), is(2));
    verify(blobStores.get(0), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    verify(blobStores.get(1), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
  }

  @Test
  public void testSingleBlobStore() throws Exception {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, "blobstore-one");

    List<BlobStore> blobStores = runCompact("blobstore-one", "blobstore-two");

    assertThat("Only specified blob store should be processed", underTest.result(), is(1));
    verify(blobStores.get(0), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    verify(blobStores.get(1), times(0)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
  }

  @Test
  public void testMultipleBlobStores() throws Exception {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, "blobstore-one,blobstore-three");

    List<BlobStore> blobStores = runCompact("blobstore-one", "blobstore-two", "blobstore-three");

    assertThat("Multiple specified blob stores should be processed", underTest.result(), is(2));
    verify(blobStores.get(0), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    verify(blobStores.get(1), times(0)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    verify(blobStores.get(2), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
  }

  @Test
  public void testGetMessageForAllBlobStores() {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, ALL);
    underTest.configure(configuration);

    assertThat(underTest.getMessage(), is("Compacting all blob stores"));
  }

  @Test
  public void testGetMessageForSingleBlobStore() {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, "my-blobstore");
    underTest.configure(configuration);

    assertThat(underTest.getMessage(), is("Compacting [my-blobstore]"));
  }

  @Test
  public void testResultReturnsProcessedCount() throws Exception {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, "blobstore-one,blobstore-two");

    List<BlobStore> blobStores = runCompact("blobstore-one", "blobstore-two");

    assertThat("Result should return number of processed blob stores", underTest.result(), is(2));
    verify(blobStores.get(0), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    verify(blobStores.get(1), times(1)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
  }

  @Test
  public void testCompact_failsWhenRecoveryModeActive() throws Exception {
    configuration.setString(BLOBSTORE_NAME_FIELD_ID, "blobstore-one");
    doThrow(
        new IllegalStateException("expected exception"))
            .when(recoveryModeService)
            .ensureNotInRecoveryMode(any());

    List<BlobStore> blobStores = runCompact("blobstore-one");

    verify(recoveryModeService).ensureNotInRecoveryMode(any());
    verify(blobStores.get(0), times(0)).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    assertThat("No blob stores should be processed when recovery mode is active", underTest.result(), is(0));
  }

  @Test
  public void testCheckForConflicts_withNullRecoveryModeService() {
    // Create task with null RecoveryModeService (CORE edition scenario)
    CompactBlobStoreTask taskWithNullRecoveryMode =
        new CompactBlobStoreTask(changeBlobstoreStore, blobStoreUsageChecker, taskUtils, null, 5, 20);
    taskWithNullRecoveryMode.install(blobStoreManager);
    taskWithNullRecoveryMode.configure(configuration);

    doNothing()
        .when(taskUtils)
        .checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    when(changeBlobstoreStore.findByBlobStoreName(anyString())).thenReturn(Collections.emptyList());

    // checkForConflicts should complete without throwing NullPointerException
    taskWithNullRecoveryMode.checkForConflicts(BLOBSTORE_NAME);

    // Verify that taskUtils.checkForConflictingTasks was still called
    verify(taskUtils, times(1)).checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    verify(changeBlobstoreStore, times(1)).findByBlobStoreName(eq(BLOBSTORE_NAME));
  }

  private List<BlobStore> runCompact(final String... names) throws Exception {
    List<BlobStore> blobStores = mockBlobStores(names);

    // Mock compact behavior
    for (BlobStore blobStore : blobStores) {
      doNothing().when(blobStore).compact(any(BlobStoreUsageChecker.class), any(Duration.class));
    }

    doNothing()
        .when(taskUtils)
        .checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    when(changeBlobstoreStore.findByBlobStoreName(anyString())).thenReturn(Collections.emptyList());

    underTest.configure(configuration);
    underTest.call();

    return blobStores;
  }

  private List<BlobStore> mockBlobStores(final String... names) {
    List<BlobStore> blobStores = Stream.of(names)
        .map(name -> {
          BlobStore blobStore = mock(BlobStore.class);
          BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
          lenient().when(config.getName()).thenReturn(name);
          lenient().when(blobStore.getBlobStoreConfiguration()).thenReturn(config);
          lenient().when(blobStoreManager.get(name)).thenReturn(blobStore);
          return blobStore;
        })
        .toList();

    lenient().when(blobStoreManager.browse()).thenReturn(blobStores);

    return blobStores;
  }

  private ChangeRepositoryBlobStoreConfiguration getRecord(
      final String name,
      final String sourceBlobStoreName,
      final String targetBlobStoreName)
  {
    return new ChangeRepositoryBlobStoreConfiguration()
    {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public void setName(final String name) {

      }

      @Override
      public String getTargetBlobStoreName() {
        return targetBlobStoreName;
      }

      @Override
      public void setTargetBlobStoreName(final String targetBlobStoreName) {

      }

      @Override
      public String getSourceBlobStoreName() {
        return sourceBlobStoreName;
      }

      @Override
      public void setSourceBlobStoreName(final String sourceBlobStoreName) {

      }

      @Override
      public OffsetDateTime getStarted() {
        return null;
      }

      @Override
      public void setStarted(final OffsetDateTime processStartDate) {

      }
    };
  }
}
