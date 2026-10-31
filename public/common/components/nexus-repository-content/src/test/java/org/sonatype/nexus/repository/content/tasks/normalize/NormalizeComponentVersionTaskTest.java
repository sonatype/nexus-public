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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTask.KEY_FORMAT;

@ExtendWith(MockitoExtension.class)
public class NormalizeComponentVersionTaskTest
{
  @Mock
  private NormalizationPriorityService priorityService;

  @Mock
  private VersionNormalizerService versionNormalizerService;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private EventManager eventManager;

  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private ComponentStore<?> componentStore;

  @Mock
  SecurityManager securityManager;

  private NormalizeComponentVersionTask underTest;

  @BeforeEach
  public void setUp() {
    ThreadContext.bind(securityManager);
    underTest = new NormalizeComponentVersionTask(priorityService, versionNormalizerService, globalKeyValueStore,
        eventManager, false, 5);
    lenient().when(formatStoreManager.componentStore(anyString())).thenReturn(componentStore);
  }

  @AfterEach
  public void teardown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  public void testGetMessage() {
    assertThat(underTest.getMessage(),
        containsString("populate normalized_version column on {format}_component tables"));
  }

  @Test
  public void testKeyFormatConstant() {
    assertThat(NormalizeComponentVersionTask.KEY_FORMAT, is("%s.normalized.version.available"));
  }

  @Test
  public void testConstructionWithDisabledFlag() {
    NormalizeComponentVersionTask disabled = new NormalizeComponentVersionTask(
        priorityService, versionNormalizerService, globalKeyValueStore, eventManager, true, 5);
    assertThat(disabled, is(notNullValue()));
  }

  @Test
  public void testExecuteThrowsWhenDisabled() throws Exception {
    NormalizeComponentVersionTask disabled = new NormalizeComponentVersionTask(
        priorityService, versionNormalizerService, globalKeyValueStore, eventManager, true, 5);
    assertThrows(TaskInterruptedException.class, disabled::call);
  }

  @Test
  public void testExecuteWithNoFormats() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of());

    underTest.call();

    verify(priorityService).getPrioritizedFormats();
    verifyNoInteractions(formatStoreManager, componentStore);
  }

  @Test
  public void testExecuteSkipsAlreadyNormalizedFormat() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("maven2", formatStoreManager));

    mockNormalizationFlag("maven2", true);

    mockHasNoUnnormaliedVersions();

    underTest.call();

    verify(globalKeyValueStore).getKey("maven2.normalized.version.available");
    // browse should only be called as part of determining whether the format has any components missing normalization
    verify(componentStore).browseUnnormalized(1, null);
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testExecuteNormalizesFormatWhenNotPreviouslyNormalized() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("npm", formatStoreManager));

    // No previous normalization state
    when(globalKeyValueStore.getKey("npm.normalized.version.available")).thenReturn(Optional.empty());

    // Component store returns empty page (no unnormalized components)
    Continuation<ComponentData> emptyPage = mockEmptyPage();
    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.call();

    // Verify setNormalizationState is called twice: first false, then true
    ArgumentCaptor<NexusKeyValue> kvCaptor = ArgumentCaptor.forClass(NexusKeyValue.class);
    verify(globalKeyValueStore, times(2)).setKey(kvCaptor.capture());

    List<NexusKeyValue> capturedValues = kvCaptor.getAllValues();
    assertThat(capturedValues.get(0).key(), is("npm.normalized.version.available"));
    assertThat(capturedValues.get(0).getAsBoolean(), is(false));
    assertThat(capturedValues.get(1).key(), is("npm.normalized.version.available"));
    assertThat(capturedValues.get(1).getAsBoolean(), is(true));

    // Verify event is posted
    ArgumentCaptor<FormatVersionNormalizedEvent> eventCaptor =
        ArgumentCaptor.forClass(FormatVersionNormalizedEvent.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getFormat(), is("npm"));
  }

  @Test
  public void testExecuteNormalizesComponentsInSinglePage() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("docker", formatStoreManager));

    when(globalKeyValueStore.getKey("docker.normalized.version.available")).thenReturn(Optional.empty());

    ComponentData component1 = createComponentData(1, "1.0.0");
    ComponentData component2 = createComponentData(2, "2.0.0");

    // First page with components, nextContinuationToken returns a token
    Continuation<ComponentData> firstPage = mockPage(component1, component2);
    when(firstPage.nextContinuationToken()).thenReturn("2");

    // Second page empty
    Continuation<ComponentData> emptyPage = mockEmptyPage();

    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(firstPage);
    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, "2")).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(2);

    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "docker"))
        .thenReturn("000000001.000000000.000000000");
    when(versionNormalizerService.getNormalizedVersionByFormat("2.0.0", "docker"))
        .thenReturn("000000002.000000000.000000000");

    underTest.call();

    // Verify components were normalized
    verify(versionNormalizerService).getNormalizedVersionByFormat("1.0.0", "docker");
    verify(versionNormalizerService).getNormalizedVersionByFormat("2.0.0", "docker");
    verify(componentStore, times(2)).updateComponentNormalizedVersion(any(ComponentData.class));
  }

  @Test
  public void testExecuteNormalizesMultiplePages() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("raw", formatStoreManager));

    when(globalKeyValueStore.getKey("raw.normalized.version.available")).thenReturn(Optional.empty());

    ComponentData component1 = createComponentData(1, "1.0");
    ComponentData component2 = createComponentData(2, "2.0");

    // First page with one component
    Continuation<ComponentData> firstPage = mockPage(component1);
    when(firstPage.nextContinuationToken()).thenReturn("1");

    // Second page with one component
    Continuation<ComponentData> secondPage = mockPage(component2);
    when(secondPage.nextContinuationToken()).thenReturn("2");

    // Third page empty
    Continuation<ComponentData> emptyPage = mockEmptyPage();

    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(firstPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("1"))).thenReturn(secondPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("2"))).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(2);

    when(versionNormalizerService.getNormalizedVersionByFormat(anyString(), anyString()))
        .thenReturn("normalized");

    underTest.call();

    // Verify both components were processed across two pages
    verify(componentStore).browseUnnormalized(Continuations.BROWSE_LIMIT, null);
    verify(componentStore).browseUnnormalized(anyInt(), eq("1"));
    verify(componentStore).browseUnnormalized(anyInt(), eq("2"));
    verify(componentStore, times(2)).updateComponentNormalizedVersion(any(ComponentData.class));
  }

  @Test
  public void testExecuteProcessesMultipleFormats() throws Exception {
    FormatStoreManager mavenManager = mock(FormatStoreManager.class);
    FormatStoreManager npmManager = mock(FormatStoreManager.class);

    ComponentStore<?> mavenStore = mock(ComponentStore.class);
    ComponentStore<?> npmStore = mock(ComponentStore.class);

    when(mavenManager.componentStore(anyString())).thenReturn(mavenStore);
    when(npmManager.componentStore(anyString())).thenReturn(npmStore);

    Map<String, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put("maven2", mavenManager);
    formats.put("npm", npmManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    mockNormalizationFlag("maven2", true);

    // npm is not normalized
    when(globalKeyValueStore.getKey("npm.normalized.version.available")).thenReturn(Optional.empty());

    Continuation<ComponentData> emptyPage = mockEmptyPage();
    when(npmStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(emptyPage);
    when(npmStore.countUnnormalized()).thenReturn(0);

    mockHasNoUnnormaliedVersions(mavenStore);

    underTest.call();

    // Maven was skipped (already normalized) - browse once for pre-check
    verify(mavenStore).browseUnnormalized(anyInt(), any());

    // npm was processed
    verify(npmManager).componentStore(anyString());
    verify(npmStore, times(1)).browseUnnormalized(anyInt(), isNull());

    // Event only posted for npm
    ArgumentCaptor<FormatVersionNormalizedEvent> eventCaptor =
        ArgumentCaptor.forClass(FormatVersionNormalizedEvent.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getFormat(), is("npm"));
  }

  @Test
  public void testExecuteWithFormatNormalizationStateFalse() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("pypi", formatStoreManager));

    // Previous normalization was set to false (incomplete)
    mockNormalizationFlag("pypi", false);

    Continuation<ComponentData> emptyPage = mockEmptyPage();
    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.call();

    // Since the state is false, it should re-normalize
    verify(componentStore).browseUnnormalized(Continuations.BROWSE_LIMIT, null);
    verify(eventManager).post(any(FormatVersionNormalizedEvent.class));
  }

  @Test
  public void testExecuteFirstPageIsEmpty() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("go", formatStoreManager));

    when(globalKeyValueStore.getKey("go.normalized.version.available")).thenReturn(Optional.empty());

    Continuation<ComponentData> emptyPage = mockEmptyPage();

    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.call();

    // Empty first page should short-circuit the while loop
    verify(componentStore, never()).updateComponentNormalizedVersion(any());
    // Normalization state should still be set and event posted
    verify(globalKeyValueStore, times(2)).setKey(any(NexusKeyValue.class));
    verify(eventManager).post(any(FormatVersionNormalizedEvent.class));
  }

  @Test
  public void testNormalizationSetsCorrectKeyFormat() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("conan", formatStoreManager));

    when(globalKeyValueStore.getKey("conan.normalized.version.available")).thenReturn(Optional.empty());

    Continuation<ComponentData> emptyPage = mockEmptyPage();
    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.call();

    // Verify the key was checked with the correct format
    verify(globalKeyValueStore).getKey("conan.normalized.version.available");

    // Verify that setKey was called with correctly formatted keys
    ArgumentCaptor<NexusKeyValue> kvCaptor = ArgumentCaptor.forClass(NexusKeyValue.class);
    verify(globalKeyValueStore, times(2)).setKey(kvCaptor.capture());
    for (NexusKeyValue capturedKv : kvCaptor.getAllValues()) {
      assertThat(capturedKv.key(), is("conan.normalized.version.available"));
    }
  }

  @Test
  public void testNormalizedVersionIsSetOnComponent() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("helm", formatStoreManager));

    when(globalKeyValueStore.getKey("helm.normalized.version.available")).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "3.2.1");

    Continuation<ComponentData> firstPage = mockPage(component);
    when(firstPage.nextContinuationToken()).thenReturn("1");

    Continuation<ComponentData> emptyPage = mockEmptyPage();

    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(firstPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("1"))).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(1);

    when(versionNormalizerService.getNormalizedVersionByFormat("3.2.1", "helm"))
        .thenReturn("000000003.000000002.000000001");

    underTest.call();

    // Verify the normalized version was set on the component
    assertThat(component.normalizedVersion(), is("000000003.000000002.000000001"));
    verify(componentStore).updateComponentNormalizedVersion(component);
  }

  @Test
  public void testExecuteReturnsNull() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of());

    Object result = underTest.call();

    assertThat(result, is((Object) null));
  }

  @Test
  public void testAllFormatsAlreadyNormalized() throws Exception {
    FormatStoreManager mavenManager = mock(FormatStoreManager.class);
    FormatStoreManager npmManager = mock(FormatStoreManager.class);

    ComponentStore<?> mavenStore = mock(ComponentStore.class);
    ComponentStore<?> npmStore = mock(ComponentStore.class);
    when(mavenManager.componentStore(anyString())).thenReturn(mavenStore);
    when(npmManager.componentStore(anyString())).thenReturn(npmStore);

    Map<String, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put("maven2", mavenManager);
    formats.put("npm", npmManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    mockNormalizationFlag("maven2", true);
    mockNormalizationFlag("npm", true);

    mockHasNoUnnormaliedVersions(mavenStore);
    mockHasNoUnnormaliedVersions(npmStore);

    underTest.call();

    // componentStore() is called in processFormat, but no browse should happen for normalized formats
    verify(mavenStore).browseUnnormalized(1, null);
    verify(npmStore).browseUnnormalized(1, null);
    verifyNoMoreInteractions(mavenStore, npmStore);
    // No events should have been posted
    verifyNoInteractions(eventManager);
    // No normalization state set calls (only getKey calls)
    verify(globalKeyValueStore, never()).setKey(any(NexusKeyValue.class));
  }

  @Test
  public void testMixedNormalizedAndUnnormalizedFormats() throws Exception {
    FormatStoreManager normalizedManager = mock(FormatStoreManager.class);
    FormatStoreManager unnormalizedManager = mock(FormatStoreManager.class);

    ComponentStore<?> normalizedStore = mock();
    ComponentStore<?> unnormalizedStore = mock();
    when(normalizedManager.componentStore(anyString())).thenReturn(normalizedStore);
    when(unnormalizedManager.componentStore(anyString())).thenReturn(unnormalizedStore);

    Map<String, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put("maven2", normalizedManager);
    formats.put("docker", unnormalizedManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    mockNormalizationFlag("maven2", true);

    // docker is not normalized
    when(globalKeyValueStore.getKey("docker.normalized.version.available")).thenReturn(Optional.empty());

    Continuation<ComponentData> emptyPage = mockEmptyPage();
    when(unnormalizedStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(emptyPage);
    when(unnormalizedStore.countUnnormalized()).thenReturn(0);

    mockHasNoUnnormaliedVersions(normalizedStore);

    underTest.call();

    // maven2 was skipped - componentStore() is called but no browse
    verify(normalizedStore).browseUnnormalized(1, null);
    verifyNoMoreInteractions(normalizedStore);

    // docker was processed
    verify(unnormalizedManager).componentStore(anyString());

    // Only one event should be posted (for docker)
    verify(eventManager, times(1)).post(any(FormatVersionNormalizedEvent.class));
  }

  /*
   * Verify that normalization when component(s) exist in an unnormalized state even if the KV flag was set
   */
  @Test
  void testComponentMissingNormalizedVersionTriggersNormalization() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Map.of("docker", formatStoreManager));

    mockNormalizationFlag("docker", true);
    mockHasUnnormaliedVersions();

    ComponentData component1 = createComponentData(1, "1.0.0");
    ComponentData component2 = createComponentData(2, "2.0.0");

    // First page with components, nextContinuationToken returns a token
    Continuation<ComponentData> firstPage = mockPage(component1, component2);
    when(firstPage.nextContinuationToken()).thenReturn("next");
    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, null)).thenReturn(firstPage);

    Continuation<ComponentData> empty = mockEmptyPage();
    when(componentStore.browseUnnormalized(Continuations.BROWSE_LIMIT, "next")).thenReturn(empty);

    when(componentStore.countUnnormalized()).thenReturn(2);

    when(versionNormalizerService.getNormalizedVersionByFormat("1.0.0", "docker"))
        .thenReturn("000000001.000000000.000000000");
    when(versionNormalizerService.getNormalizedVersionByFormat("2.0.0", "docker"))
        .thenReturn("000000002.000000000.000000000");

    underTest.call();

    // Verify components were normalized
    verify(versionNormalizerService).getNormalizedVersionByFormat("1.0.0", "docker");
    verify(versionNormalizerService).getNormalizedVersionByFormat("2.0.0", "docker");
    verify(componentStore, times(2)).updateComponentNormalizedVersion(any(ComponentData.class));
  }

  private void mockNormalizationFlag(final String formatName, final boolean normalized) {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(KEY_FORMAT.formatted(formatName));
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(normalized);
    when(globalKeyValueStore.getKey(KEY_FORMAT.formatted(formatName))).thenReturn(Optional.of(kv));
  }

  private void mockHasNoUnnormaliedVersions() {
    mockHasNoUnnormaliedVersions(componentStore);
  }

  private void mockHasUnnormaliedVersions() {
    Continuation<ComponentData> continuation = mock();
    when(continuation.isEmpty()).thenReturn(false);
    when(componentStore.browseUnnormalized(1, null)).thenReturn(continuation);
  }

  private static void mockHasNoUnnormaliedVersions(final ComponentStore<?> componentStore) {
    Continuation<ComponentData> continuation = mock();
    when(continuation.isEmpty()).thenReturn(true);
    when(componentStore.browseUnnormalized(1, null)).thenReturn(continuation);
  }

  private static Continuation<ComponentData> mockPage(final ComponentData... components) {
    Continuation<ComponentData> page = mock();
    when(page.isEmpty()).thenReturn(components.length == 0);
    when(page.size()).thenReturn(components.length);

    // Enable forEach
    doAnswer(invocation -> {
      Consumer<ComponentData> action = invocation.getArgument(0);
      Stream.of(components).forEach(action::accept);
      return null;
    }).when(page).forEach(any());

    return page;
  }

  private static Continuation<ComponentData> mockEmptyPage() {
    Continuation<ComponentData> emptyPage = mock();
    when(emptyPage.isEmpty()).thenReturn(true);

    return emptyPage;
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
}
