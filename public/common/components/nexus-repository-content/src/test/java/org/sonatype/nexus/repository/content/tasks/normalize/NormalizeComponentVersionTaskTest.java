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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
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

  private NormalizeComponentVersionTask underTest;

  @Before
  public void setUp() {
    underTest = new NormalizeComponentVersionTask(
        List.of(priorityService), versionNormalizerService, globalKeyValueStore, eventManager, false);
    when(formatStoreManager.componentStore(anyString())).thenReturn(componentStore);
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
        List.of(priorityService), versionNormalizerService, globalKeyValueStore, eventManager, true);
    assertThat(disabled, is(notNullValue()));
  }

  @Test
  public void testConstructionUsesLastPriorityService() {
    NormalizationPriorityService first = mock(NormalizationPriorityService.class);
    NormalizationPriorityService second = mock(NormalizationPriorityService.class);

    Map<Format, FormatStoreManager> secondFormats = new LinkedHashMap<>();
    when(second.getPrioritizedFormats()).thenReturn(secondFormats);

    NormalizeComponentVersionTask task = new NormalizeComponentVersionTask(
        List.of(first, second), versionNormalizerService, globalKeyValueStore, eventManager, false);
    assertThat(task, is(notNullValue()));
  }

  @Test(expected = TaskInterruptedException.class)
  public void testExecuteThrowsWhenDisabled() throws Exception {
    NormalizeComponentVersionTask disabled = new NormalizeComponentVersionTask(
        List.of(priorityService), versionNormalizerService, globalKeyValueStore, eventManager, true);
    disabled.execute();
  }

  @Test
  public void testExecuteWithNoFormats() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Collections.emptyMap());

    underTest.execute();

    verify(priorityService).getPrioritizedFormats();
    verifyNoInteractions(formatStoreManager, componentStore);
  }

  @Test
  public void testExecuteSkipsAlreadyNormalizedFormat() throws Exception {
    Format mavenFormat = createFormat("maven2");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(mavenFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("maven2.normalized.version.available");
    kv.setType(org.sonatype.nexus.kv.ValueType.BOOLEAN);
    kv.setValue(true);
    when(globalKeyValueStore.getKey("maven2.normalized.version.available")).thenReturn(Optional.of(kv));

    underTest.execute();

    verify(globalKeyValueStore).getKey("maven2.normalized.version.available");
    // componentStore is always called in processFormat, but browse should not happen
    verify(componentStore, never()).browseUnnormalized(anyInt(), any());
    verify(eventManager, never()).post(any());
  }

  @Test
  public void testExecuteNormalizesFormatWhenNotPreviouslyNormalized() throws Exception {
    Format npmFormat = createFormat("npm");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(npmFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    // No previous normalization state
    when(globalKeyValueStore.getKey("npm.normalized.version.available")).thenReturn(Optional.empty());

    // Component store returns empty page (no unnormalized components)
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);
    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.execute();

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
    Format dockerFormat = createFormat("docker");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(dockerFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    when(globalKeyValueStore.getKey("docker.normalized.version.available")).thenReturn(Optional.empty());

    ComponentData component1 = createComponentData(1, "1.0.0");
    ComponentData component2 = createComponentData(2, "2.0.0");

    // First page with components, nextContinuationToken returns a token
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.nextContinuationToken()).thenReturn("2");
    when(firstPage.size()).thenReturn(2);
    when(firstPage.iterator()).thenReturn(List.of(component1, component2).iterator());
    // Enable forEach
    when(firstPage.spliterator()).thenReturn(List.of(component1, component2).spliterator());
    org.mockito.Mockito.doAnswer(invocation -> {
      java.util.function.Consumer<ComponentData> action = invocation.getArgument(0);
      action.accept(component1);
      action.accept(component2);
      return null;
    }).when(firstPage).forEach(any());

    // Second page empty
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);

    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(firstPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("2"))).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(2);

    when(versionNormalizerService.getNormalizedVersionByFormat(eq("1.0.0"), any(Format.class)))
        .thenReturn("000000001.000000000.000000000");
    when(versionNormalizerService.getNormalizedVersionByFormat(eq("2.0.0"), any(Format.class)))
        .thenReturn("000000002.000000000.000000000");

    underTest.execute();

    // Verify components were normalized
    verify(versionNormalizerService).getNormalizedVersionByFormat("1.0.0", dockerFormat);
    verify(versionNormalizerService).getNormalizedVersionByFormat("2.0.0", dockerFormat);
    verify(componentStore, times(2)).updateComponentNormalizedVersion(any(ComponentData.class));
  }

  @Test
  public void testExecuteNormalizesMultiplePages() throws Exception {
    Format rawFormat = createFormat("raw");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(rawFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    when(globalKeyValueStore.getKey("raw.normalized.version.available")).thenReturn(Optional.empty());

    ComponentData component1 = createComponentData(1, "1.0");
    ComponentData component2 = createComponentData(2, "2.0");

    // First page with one component
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.nextContinuationToken()).thenReturn("1");
    when(firstPage.size()).thenReturn(1);
    org.mockito.Mockito.doAnswer(invocation -> {
      java.util.function.Consumer<ComponentData> action = invocation.getArgument(0);
      action.accept(component1);
      return null;
    }).when(firstPage).forEach(any());

    // Second page with one component
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> secondPage = mock(Continuation.class);
    when(secondPage.isEmpty()).thenReturn(false);
    when(secondPage.nextContinuationToken()).thenReturn("2");
    when(secondPage.size()).thenReturn(1);
    org.mockito.Mockito.doAnswer(invocation -> {
      java.util.function.Consumer<ComponentData> action = invocation.getArgument(0);
      action.accept(component2);
      return null;
    }).when(secondPage).forEach(any());

    // Third page empty
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);

    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(firstPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("1"))).thenReturn(secondPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("2"))).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(2);

    when(versionNormalizerService.getNormalizedVersionByFormat(anyString(), any(Format.class)))
        .thenReturn("normalized");

    underTest.execute();

    // Verify both components were processed across two pages
    verify(componentStore).browseUnnormalized(anyInt(), isNull());
    verify(componentStore).browseUnnormalized(anyInt(), eq("1"));
    verify(componentStore).browseUnnormalized(anyInt(), eq("2"));
    verify(componentStore, times(2)).updateComponentNormalizedVersion(any(ComponentData.class));
  }

  @Test
  public void testExecuteProcessesMultipleFormats() throws Exception {
    Format mavenFormat = createFormat("maven2");
    Format npmFormat = createFormat("npm");

    FormatStoreManager mavenManager = mock(FormatStoreManager.class);
    FormatStoreManager npmManager = mock(FormatStoreManager.class);

    @SuppressWarnings("unchecked")
    ComponentStore<?> mavenStore = mock(ComponentStore.class);
    @SuppressWarnings("unchecked")
    ComponentStore<?> npmStore = mock(ComponentStore.class);

    when(mavenManager.componentStore(anyString())).thenReturn(mavenStore);
    when(npmManager.componentStore(anyString())).thenReturn(npmStore);

    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(mavenFormat, mavenManager);
    formats.put(npmFormat, npmManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    // Maven is already normalized
    NexusKeyValue mavenKv = new NexusKeyValue();
    mavenKv.setKey("maven2.normalized.version.available");
    mavenKv.setType(org.sonatype.nexus.kv.ValueType.BOOLEAN);
    mavenKv.setValue(true);
    when(globalKeyValueStore.getKey("maven2.normalized.version.available")).thenReturn(Optional.of(mavenKv));

    // npm is not normalized
    when(globalKeyValueStore.getKey("npm.normalized.version.available")).thenReturn(Optional.empty());

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);
    when(npmStore.browseUnnormalized(anyInt(), isNull())).thenReturn(emptyPage);
    when(npmStore.countUnnormalized()).thenReturn(0);

    underTest.execute();

    // Maven was skipped (already normalized) - componentStore() is still called but no browse
    verify(mavenStore, never()).browseUnnormalized(anyInt(), any());

    // npm was processed
    verify(npmManager).componentStore(anyString());
    verify(npmStore).browseUnnormalized(anyInt(), isNull());

    // Event only posted for npm
    ArgumentCaptor<FormatVersionNormalizedEvent> eventCaptor =
        ArgumentCaptor.forClass(FormatVersionNormalizedEvent.class);
    verify(eventManager).post(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getFormat(), is("npm"));
  }

  @Test
  public void testExecuteWithFormatNormalizationStateFalse() throws Exception {
    Format pypiFormat = createFormat("pypi");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(pypiFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    // Previous normalization was set to false (incomplete)
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("pypi.normalized.version.available");
    kv.setType(org.sonatype.nexus.kv.ValueType.BOOLEAN);
    kv.setValue(false);
    when(globalKeyValueStore.getKey("pypi.normalized.version.available")).thenReturn(Optional.of(kv));

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);
    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.execute();

    // Since the state is false, it should re-normalize
    verify(componentStore).browseUnnormalized(anyInt(), isNull());
    verify(eventManager).post(any(FormatVersionNormalizedEvent.class));
  }

  @Test
  public void testExecuteFirstPageHasNullContinuationToken() throws Exception {
    Format nugetFormat = createFormat("nuget");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(nugetFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    when(globalKeyValueStore.getKey("nuget.normalized.version.available")).thenReturn(Optional.empty());

    // First page has components but nextContinuationToken returns null
    @SuppressWarnings("unchecked")
    Continuation<ComponentData> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.nextContinuationToken()).thenReturn(null);

    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(firstPage);
    when(componentStore.countUnnormalized()).thenReturn(5);

    underTest.execute();

    // While loop condition: !page.isEmpty() && page.nextContinuationToken() != null
    // With null token, the while loop body does not execute
    verify(componentStore, never()).updateComponentNormalizedVersion(any());
    verify(eventManager).post(any(FormatVersionNormalizedEvent.class));
  }

  @Test
  public void testExecuteFirstPageIsEmpty() throws Exception {
    Format goFormat = createFormat("go");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(goFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    when(globalKeyValueStore.getKey("go.normalized.version.available")).thenReturn(Optional.empty());

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);

    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.execute();

    // Empty first page should short-circuit the while loop
    verify(componentStore, never()).updateComponentNormalizedVersion(any());
    // Normalization state should still be set and event posted
    verify(globalKeyValueStore, times(2)).setKey(any(NexusKeyValue.class));
    verify(eventManager).post(any(FormatVersionNormalizedEvent.class));
  }

  @Test
  public void testConstructorSelectsLastPriorityServiceFromList() throws Exception {
    NormalizationPriorityService first = mock(NormalizationPriorityService.class);
    NormalizationPriorityService second = mock(NormalizationPriorityService.class);

    when(second.getPrioritizedFormats()).thenReturn(Collections.emptyMap());

    NormalizeComponentVersionTask task = new NormalizeComponentVersionTask(
        List.of(first, second), versionNormalizerService, globalKeyValueStore, eventManager, false);

    task.execute();

    // Only the last (second) priority service should be used
    verify(second).getPrioritizedFormats();
    verifyNoInteractions(first);
  }

  @Test
  public void testNormalizationSetsCorrectKeyFormat() throws Exception {
    Format conanFormat = createFormat("conan");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(conanFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    when(globalKeyValueStore.getKey("conan.normalized.version.available")).thenReturn(Optional.empty());

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(0);

    underTest.execute();

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
    Format helmFormat = createFormat("helm");
    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(helmFormat, formatStoreManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    when(globalKeyValueStore.getKey("helm.normalized.version.available")).thenReturn(Optional.empty());

    ComponentData component = createComponentData(1, "3.2.1");

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> firstPage = mock(Continuation.class);
    when(firstPage.isEmpty()).thenReturn(false);
    when(firstPage.nextContinuationToken()).thenReturn("1");
    when(firstPage.size()).thenReturn(1);
    org.mockito.Mockito.doAnswer(invocation -> {
      java.util.function.Consumer<ComponentData> action = invocation.getArgument(0);
      action.accept(component);
      return null;
    }).when(firstPage).forEach(any());

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);

    when(componentStore.browseUnnormalized(anyInt(), isNull())).thenReturn(firstPage);
    when(componentStore.browseUnnormalized(anyInt(), eq("1"))).thenReturn(emptyPage);
    when(componentStore.countUnnormalized()).thenReturn(1);

    when(versionNormalizerService.getNormalizedVersionByFormat("3.2.1", helmFormat))
        .thenReturn("000000003.000000002.000000001");

    underTest.execute();

    // Verify the normalized version was set on the component
    assertThat(component.normalizedVersion(), is("000000003.000000002.000000001"));
    verify(componentStore).updateComponentNormalizedVersion(component);
  }

  @Test
  public void testExecuteReturnsNull() throws Exception {
    when(priorityService.getPrioritizedFormats()).thenReturn(Collections.emptyMap());

    Object result = underTest.execute();

    assertThat(result, is((Object) null));
  }

  @Test
  public void testAllFormatsAlreadyNormalized() throws Exception {
    Format maven = createFormat("maven2");
    Format npm = createFormat("npm");

    FormatStoreManager mavenManager = mock(FormatStoreManager.class);
    FormatStoreManager npmManager = mock(FormatStoreManager.class);

    @SuppressWarnings("unchecked")
    ComponentStore<?> mavenStore = mock(ComponentStore.class);
    @SuppressWarnings("unchecked")
    ComponentStore<?> npmStore = mock(ComponentStore.class);
    when(mavenManager.componentStore(anyString())).thenReturn(mavenStore);
    when(npmManager.componentStore(anyString())).thenReturn(npmStore);

    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(maven, mavenManager);
    formats.put(npm, npmManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    NexusKeyValue mavenKv = new NexusKeyValue();
    mavenKv.setKey("maven2.normalized.version.available");
    mavenKv.setType(org.sonatype.nexus.kv.ValueType.BOOLEAN);
    mavenKv.setValue(true);
    when(globalKeyValueStore.getKey("maven2.normalized.version.available")).thenReturn(Optional.of(mavenKv));

    NexusKeyValue npmKv = new NexusKeyValue();
    npmKv.setKey("npm.normalized.version.available");
    npmKv.setType(org.sonatype.nexus.kv.ValueType.BOOLEAN);
    npmKv.setValue(true);
    when(globalKeyValueStore.getKey("npm.normalized.version.available")).thenReturn(Optional.of(npmKv));

    underTest.execute();

    // componentStore() is called in processFormat, but no browse should happen for normalized formats
    verify(mavenStore, never()).browseUnnormalized(anyInt(), any());
    verify(npmStore, never()).browseUnnormalized(anyInt(), any());
    // No events should have been posted
    verifyNoInteractions(eventManager);
    // No normalization state set calls (only getKey calls)
    verify(globalKeyValueStore, never()).setKey(any(NexusKeyValue.class));
  }

  @Test
  public void testMixedNormalizedAndUnnormalizedFormats() throws Exception {
    Format normalizedFormat = createFormat("maven2");
    Format unnormalizedFormat = createFormat("docker");

    FormatStoreManager normalizedManager = mock(FormatStoreManager.class);
    FormatStoreManager unnormalizedManager = mock(FormatStoreManager.class);

    @SuppressWarnings("unchecked")
    ComponentStore<?> normalizedStore = mock(ComponentStore.class);
    @SuppressWarnings("unchecked")
    ComponentStore<?> unnormalizedStore = mock(ComponentStore.class);
    when(normalizedManager.componentStore(anyString())).thenReturn(normalizedStore);
    when(unnormalizedManager.componentStore(anyString())).thenReturn(unnormalizedStore);

    Map<Format, FormatStoreManager> formats = new LinkedHashMap<>();
    formats.put(normalizedFormat, normalizedManager);
    formats.put(unnormalizedFormat, unnormalizedManager);
    when(priorityService.getPrioritizedFormats()).thenReturn(formats);

    // maven2 is already normalized
    NexusKeyValue mavenKv = new NexusKeyValue();
    mavenKv.setKey("maven2.normalized.version.available");
    mavenKv.setType(org.sonatype.nexus.kv.ValueType.BOOLEAN);
    mavenKv.setValue(true);
    when(globalKeyValueStore.getKey("maven2.normalized.version.available")).thenReturn(Optional.of(mavenKv));

    // docker is not normalized
    when(globalKeyValueStore.getKey("docker.normalized.version.available")).thenReturn(Optional.empty());

    @SuppressWarnings("unchecked")
    Continuation<ComponentData> emptyPage = mock(Continuation.class);
    when(emptyPage.isEmpty()).thenReturn(true);
    when(emptyPage.nextContinuationToken()).thenReturn(null);
    when(unnormalizedStore.browseUnnormalized(anyInt(), isNull())).thenReturn(emptyPage);
    when(unnormalizedStore.countUnnormalized()).thenReturn(0);

    underTest.execute();

    // maven2 was skipped - componentStore() is called but no browse
    verify(normalizedStore, never()).browseUnnormalized(anyInt(), any());

    // docker was processed
    verify(unnormalizedManager).componentStore(anyString());

    // Only one event should be posted (for docker)
    verify(eventManager, times(1)).post(any(FormatVersionNormalizedEvent.class));
  }

  private Format createFormat(final String value) {
    return new Format(value)
    {
    };
  }

  private ComponentData createComponentData(final int id, final String version) {
    ComponentData data = new ComponentData();
    data.setComponentId(id);
    data.setNamespace("test-namespace");
    data.setName("test-name");
    data.setKind("test-kind");
    data.setVersion(version);
    return data;
  }
}
