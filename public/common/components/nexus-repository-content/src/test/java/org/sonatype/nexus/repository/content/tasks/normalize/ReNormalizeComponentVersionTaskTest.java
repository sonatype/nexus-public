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
package org.sonatype.nexus.repository.content.tasks.normalize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.CHECKPOINT_KEY_FORMAT;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.CURSOR_KEY;
import static org.sonatype.nexus.repository.content.tasks.normalize.ReNormalizeComponentVersionTask.GATE_KEY_FORMAT;

@ExtendWith(MockitoExtension.class)
class ReNormalizeComponentVersionTaskTest
{
  @Mock
  private VersionNormalizerService versionNormalizerService;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private FormatStoreManager npmFormatStoreManager;

  @Mock
  private ComponentStore<?> npmComponentStore;

  private MockedStatic<QualifierUtil> qualifierUtilMock;

  private ReNormalizeComponentVersionTask underTest;

  @BeforeEach
  void setUp() {
    qualifierUtilMock = mockStatic(QualifierUtil.class);
  }

  @AfterEach
  void tearDown() {
    qualifierUtilMock.close();
    CancelableHelper.remove();
  }

  private ReNormalizeComponentVersionTask create(
      final Map<String, FormatStoreManager> formatStoreManagers,
      final boolean disableTask,
      final int maxWriteRetries)
  {
    when(QualifierUtil.buildQualifierBeanMap(any(List.class))).thenReturn(formatStoreManagers);
    return new ReNormalizeComponentVersionTask(
        versionNormalizerService, globalKeyValueStore, repositoryManager,
        List.copyOf(formatStoreManagers.values()), disableTask, 5, maxWriteRetries);
  }

  private ReNormalizeComponentVersionTask createWithNpmOnly() {
    return createWithNpmOnly(5);
  }

  private ReNormalizeComponentVersionTask createWithNpmOnly(final int maxWriteRetries) {
    lenient().when(npmFormatStoreManager.componentStore(DEFAULT_DATASTORE_NAME)).thenReturn(npmComponentStore);
    return create(Map.of("npm", npmFormatStoreManager), false, maxWriteRetries);
  }

  @Test
  void testGetMessage() {
    underTest = createWithNpmOnly();
    assertThat(underTest.getMessage(), containsString("repair normalized_version column"));
  }

  @Test
  void testExecuteThrowsWhenDisabledAndNeverTouchesKeyValueStore() {
    underTest = create(Map.of("npm", npmFormatStoreManager), true, 5);

    assertThrows(TaskInterruptedException.class, underTest::call);

    verifyNoInteractions(globalKeyValueStore);
  }

  @Test
  void testExecuteScopesToFormatsPresentInFormatStoreManagers() throws Exception {
    underTest = createWithNpmOnly();
    when(repositoryManager.browse()).thenReturn(List.of());
    mockGateRaised("npm", true);

    underTest.call();

    // npm is present in formatStoreManagers, so its gate is cleared
    verify(globalKeyValueStore).removeKey(GATE_KEY_FORMAT.formatted("npm"));
    // every other RENORMALIZE_FORMATS entry is absent from formatStoreManagers and must never be touched
    for (String format : ReNormalizeComponentVersionTask.RENORMALIZE_FORMATS) {
      if (!format.equals("npm")) {
        verify(globalKeyValueStore, never()).removeKey(GATE_KEY_FORMAT.formatted(format));
      }
    }
  }

  @Test
  void testJobStreamSkipsFormatWhoseGateIsNoLongerRaised() {
    underTest = createWithNpmOnly();
    mockGateRaised("npm", false);

    long jobCount = underTest.jobStream(mock(ProgressLogIntervalHelper.class)).count();

    // on a resumed run, npm already finished (its gate was cleared), so it must not be re-scanned
    assertThat(jobCount, is(0L));
  }

  @Test
  void testJobStreamIncludesFormatWhoseGateIsStillRaised() {
    underTest = createWithNpmOnly();
    mockGateRaised("npm", true);

    long jobCount = underTest.jobStream(mock(ProgressLogIntervalHelper.class)).count();

    assertThat(jobCount, is(1L));
  }

  @Test
  void testExecuteSetsAndClearsRunCursor() throws Exception {
    underTest = createWithNpmOnly();

    underTest.call();

    verify(globalKeyValueStore).setBoolean(CURSOR_KEY, true);
    verify(globalKeyValueStore).removeKey(CURSOR_KEY);
  }

  @Test
  void testRepositoryIdsForFormatFiltersByFormatAndSortsAscending() {
    underTest = createWithNpmOnly();

    Repository npmRepoHigh = mockRepository("npm", 20);
    Repository npmRepoLow = mockRepository("npm", 5);
    Repository otherFormatRepo = mockRepository("maven2", 1);
    Repository npmRepoNoContentFacet = mock(Repository.class);
    lenient().when(npmRepoNoContentFacet.getFormat()).thenReturn(new TestFormat("npm"));
    lenient().when(npmRepoNoContentFacet.optionalFacet(ContentFacet.class)).thenReturn(Optional.empty());

    when(repositoryManager.browse())
        .thenReturn(List.of(npmRepoHigh, otherFormatRepo, npmRepoNoContentFacet, npmRepoLow));

    assertThat(underTest.repositoryIdsForFormat("npm"), contains(5, 20));
  }

  @Test
  void testRenormalizeRepositoryResumesFromStoredCheckpoint() {
    underTest = createWithNpmOnly();
    mockCheckpoint("npm", 5, "resume-token");

    Continuation<Component> emptyPage = mockPage();
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, "resume-token", null, null, null))
        .thenReturn(emptyPage);

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(npmComponentStore).browseComponents(5, Continuations.BROWSE_LIMIT, "resume-token", null, null, null);
  }

  @Test
  void testRenormalizeRepositoryStartsFromScratchWhenNoCheckpoint() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    Continuation<Component> emptyPage = mockPage();
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(emptyPage);

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(npmComponentStore).browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null);
  }

  @Test
  void testRenormalizeRepositoryPersistsCheckpointBeforeFetchingNextPage() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "1.0.0");
    Continuation<Component> firstPage = mockPage(component);
    when(firstPage.nextContinuationToken()).thenReturn("next-token");
    Continuation<Component> secondPage = mockPage();

    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(firstPage);
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, "next-token", null, null, null))
        .thenReturn(secondPage);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized");

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(globalKeyValueStore).setString(CHECKPOINT_KEY_FORMAT.formatted("npm", 5), "next-token");
  }

  @Test
  void testRenormalizeRepositoryClearsCheckpointOnceLastPageIsReached() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    Continuation<Component> emptyPage = mockPage();

    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(emptyPage);

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(globalKeyValueStore).removeKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5));
  }

  @Test
  void testRenormalizeRepositoryLeavesCheckpointIntactWhenInterruptedMidPage() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "1.0.0");
    Continuation<Component> firstPage = mockPage(component);
    when(firstPage.nextContinuationToken()).thenReturn("next-token");

    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(firstPage);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized");

    // simulate cancellation firing right after the checkpoint for the next page is persisted
    CancelableHelper.set(new AtomicBoolean(true));

    assertThrows(TaskInterruptedException.class,
        () -> underTest.renormalizeRepository("npm", npmComponentStore, 5));

    verify(globalKeyValueStore).setString(CHECKPOINT_KEY_FORMAT.formatted("npm", 5), "next-token");
    verify(globalKeyValueStore, never()).removeKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5));
    verify(npmComponentStore, never()).browseComponents(5, Continuations.BROWSE_LIMIT, "next-token", null, null, null);
  }

  @Test
  void testRenormalizeFormatSkipsFinishFormatWhenARepositoryFails() {
    underTest = createWithNpmOnly();
    Repository repository = mockRepository("npm", 5);
    when(repositoryManager.browse()).thenReturn(List.of(repository));
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenThrow(new TaskInterruptedException("boom", true));

    assertThrows(TaskInterruptedException.class, () -> underTest.renormalizeFormat("npm"));

    verify(globalKeyValueStore, never()).removeKey(GATE_KEY_FORMAT.formatted("npm"));
  }

  @Test
  void testRenormalizeFormatClearsGateOnceAllRepositoriesComplete() {
    underTest = createWithNpmOnly();
    Repository repository = mockRepository("npm", 5);
    when(repositoryManager.browse()).thenReturn(List.of(repository));
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());
    Continuation<Component> emptyPage = mockPage();
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(emptyPage);
    primeProgressLogger();

    underTest.renormalizeFormat("npm");

    verify(globalKeyValueStore).removeKey(GATE_KEY_FORMAT.formatted("npm"));
  }

  @Test
  void testRenormalizeComponentRecomputesNormalizedVersionUnconditionally() {
    underTest = createWithNpmOnly();
    ComponentData component = createComponentData(1, "1.0.0");
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized-value");
    List<ComponentData> toUpdate = new ArrayList<>();

    underTest.renormalizeComponent(component, "npm", 5, toUpdate);

    assertThat(component.normalizedVersion(), is("normalized-value"));
    assertThat(toUpdate, contains(component));
  }

  @Test
  void testRenormalizeComponentIgnoresNonComponentDataImplementations() {
    underTest = createWithNpmOnly();
    Component component = mock(Component.class);
    List<ComponentData> toUpdate = new ArrayList<>();

    underTest.renormalizeComponent(component, "npm", 5, toUpdate);

    assertThat(toUpdate, is(empty()));
  }

  @Test
  void testRenormalizeComponentStopsFormatWhenNormalizerThrows() {
    underTest = createWithNpmOnly();
    ComponentData component = createComponentData(1, "not-a-valid-version");
    when(versionNormalizerService.getNormalizedVersionByFormat("not-a-valid-version", "npm"))
        .thenThrow(new IllegalArgumentException("bad version"));
    List<ComponentData> toUpdate = new ArrayList<>();

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> underTest.renormalizeComponent(component, "npm", 5, toUpdate));

    assertThat(thrown.getMessage(), containsString("npm"));
    assertThat(toUpdate, is(empty()));
  }

  @Test
  void testRenormalizeFormatAbortsImmediatelyWhenAComponentFailsToNormalize() {
    underTest = createWithNpmOnly();
    Repository repository = mockRepository("npm", 5);
    when(repositoryManager.browse()).thenReturn(List.of(repository));
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());
    ComponentData component = createComponentData(1, "not-a-valid-version");
    Continuation<Component> page = mockPage(component);
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(page);
    when(versionNormalizerService.getNormalizedVersionByFormat("not-a-valid-version", "npm"))
        .thenThrow(new IllegalArgumentException("bad version"));

    assertThrows(RuntimeException.class, () -> underTest.renormalizeFormat("npm"));

    verify(globalKeyValueStore, never()).removeKey(GATE_KEY_FORMAT.formatted("npm"));
    verify(npmComponentStore, never()).updateComponentsNormalizedVersion(any());
  }

  @Test
  void testRenormalizeRepositoryRetriesBatchWriteAndSucceedsWithinLimit() {
    underTest = createWithNpmOnly(2);
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "1.0.0");
    Continuation<Component> page = mockPage(component);
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(page);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized-1");
    doThrow(new RuntimeException("transient db error"))
        .doNothing()
        .when(npmComponentStore)
        .updateComponentsNormalizedVersion(List.of(component));

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(npmComponentStore, times(2)).updateComponentsNormalizedVersion(List.of(component));
    verify(globalKeyValueStore).removeKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5));
  }

  @Test
  void testRenormalizeRepositoryStopsFormatAndKeepsCheckpointWhenBatchWriteExhaustsRetries() {
    underTest = createWithNpmOnly(1);
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "1.0.0");
    Continuation<Component> page = mockPage(component);
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(page);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized-1");
    doThrow(new RuntimeException("db is down"))
        .when(npmComponentStore)
        .updateComponentsNormalizedVersion(List.of(component));

    RuntimeException thrown = assertThrows(RuntimeException.class,
        () -> underTest.renormalizeRepository("npm", npmComponentStore, 5));

    assertThat(thrown.getMessage(), containsString("npm"));
    verify(npmComponentStore, times(1)).updateComponentsNormalizedVersion(List.of(component));
    verify(globalKeyValueStore, never()).removeKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5));
    verify(globalKeyValueStore, never()).setString(eq(CHECKPOINT_KEY_FORMAT.formatted("npm", 5)), any());
  }

  @Test
  void testRenormalizeRepositoryHonorsCancellationDuringBackoffSleep() {
    underTest = createWithNpmOnly(3);
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "1.0.0");
    Continuation<Component> page = mockPage(component);
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(page);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized-1");
    doThrow(new RuntimeException("transient db error"))
        .when(npmComponentStore)
        .updateComponentsNormalizedVersion(List.of(component));

    CancelableHelper.set(new AtomicBoolean(true));

    assertThrows(TaskInterruptedException.class,
        () -> underTest.renormalizeRepository("npm", npmComponentStore, 5));

    verify(npmComponentStore, times(1)).updateComponentsNormalizedVersion(List.of(component));
  }

  @Test
  void testRenormalizeRepositoryBatchesUpdatesInOneCallPerPage() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData first = createComponentData(1, "1.0.0");
    ComponentData second = createComponentData(2, "2.0.0");
    Continuation<Component> page = mockPage(first, second);

    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(page);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized-1");
    when(versionNormalizerService.getNormalizedVersionByFormat("2.0.0", "npm")).thenReturn("normalized-2");

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(npmComponentStore).updateComponentsNormalizedVersion(List.of(first, second));
    verify(npmComponentStore, never()).updateComponentNormalizedVersion(any());
  }

  @Test
  void testRenormalizeRepositoryWritesBatchBeforeAdvancingCheckpoint() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "1.0.0");
    Continuation<Component> firstPage = mockPage(component);
    when(firstPage.nextContinuationToken()).thenReturn("next-token");
    Continuation<Component> secondPage = mockPage();

    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(firstPage);
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, "next-token", null, null, null))
        .thenReturn(secondPage);
    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "npm")).thenReturn("normalized-1");

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    InOrder inOrder = inOrder(npmComponentStore, globalKeyValueStore);
    inOrder.verify(npmComponentStore).updateComponentsNormalizedVersion(List.of(component));
    inOrder.verify(globalKeyValueStore).setString(CHECKPOINT_KEY_FORMAT.formatted("npm", 5), "next-token");
  }

  @Test
  void testRenormalizeRepositorySkipsBatchUpdateWhenPageHasNothingToUpdate() {
    underTest = createWithNpmOnly();
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted("npm", 5))).thenReturn(Optional.empty());

    Continuation<Component> emptyPage = mockPage();
    when(npmComponentStore.browseComponents(5, Continuations.BROWSE_LIMIT, null, null, null, null))
        .thenReturn(emptyPage);

    underTest.renormalizeRepository("npm", npmComponentStore, 5);

    verify(npmComponentStore, never()).updateComponentsNormalizedVersion(any());
  }

  private void primeProgressLogger() {
    // renormalizeFormat() logs via the field jobStream() normally sets; only jobStream() populates it
    underTest.jobStream(mock(ProgressLogIntervalHelper.class));
  }

  private void mockGateRaised(final String format, final boolean raised) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(GATE_KEY_FORMAT.formatted(format));
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(raised);
    when(globalKeyValueStore.getKey(GATE_KEY_FORMAT.formatted(format))).thenReturn(Optional.of(kv));
  }

  private void mockCheckpoint(final String format, final int repositoryId, final String token) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(CHECKPOINT_KEY_FORMAT.formatted(format, repositoryId));
    kv.setType(ValueType.CHARACTER);
    kv.setValue(token);
    when(globalKeyValueStore.getKey(CHECKPOINT_KEY_FORMAT.formatted(format, repositoryId)))
        .thenReturn(Optional.of(kv));
  }

  private static Repository mockRepository(final String format, final int repositoryId) {
    Repository repository = mock(Repository.class);
    ContentFacet contentFacet = mock(ContentFacet.class);
    lenient().when(contentFacet.contentRepositoryId()).thenReturn(repositoryId);
    lenient().when(repository.getFormat()).thenReturn(new TestFormat(format));
    lenient().when(repository.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(contentFacet));
    return repository;
  }

  private static ComponentData createComponentData(final int id, final String version) {
    ComponentData data = new ComponentData();
    data.setComponentId(id);
    data.setNamespace("test-namespace");
    data.setName("test-name");
    data.setKind("test-kind");
    data.setVersion(version);
    return data;
  }

  @SafeVarargs
  private static Continuation<Component> mockPage(final Component... components) {
    @SuppressWarnings("unchecked")
    Continuation<Component> page = mock(Continuation.class);
    lenient().when(page.isEmpty()).thenReturn(components.length == 0);
    lenient().doAnswer(invocation -> {
      Consumer<Component> action = invocation.getArgument(0);
      Stream.of(components).forEach(action::accept);
      return null;
    }).when(page).forEach(any());
    return page;
  }

  private static final class TestFormat
      extends Format
  {
    TestFormat(final String value) {
      super(value);
    }
  }
}
