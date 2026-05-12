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
package org.sonatype.nexus.repository.content.store.internal;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.component.ComponentAttributesEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentsPurgedAuditEvent;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ContentStoreEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ComponentAuditorTest
{
  private static final String REPO_NAME = "test-repo";

  private static final String COMPONENT_NAME = "test-component";

  private static final String COMPONENT_NAMESPACE = "test-namespace";

  private static final String COMPONENT_VERSION = "1.0.0";

  private static final String COMPONENT_KIND = "test-kind";

  private static final int CONTENT_REPOSITORY_ID = 1;

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private Repository repository;

  private ComponentAuditor underTest;

  @Before
  public void setUp() {
    underTest = Mockito.spy(new ComponentAuditor());
    when(auditRecorder.isEnabled()).thenReturn(true);
    underTest.setAuditRecorder(auditRecorder);
    when(repository.getName()).thenReturn(REPO_NAME);
  }

  @Test
  public void testConstruction() {
    assertThat(underTest, is(notNullValue()));
  }

  @Test
  public void testDomainConstant() {
    assertThat(ComponentAuditor.DOMAIN, is("repository.component"));
  }

  @Test
  public void testOnComponentPurgedEvent_recording() {
    int[] componentIds = {1, 2, 3};
    ComponentPurgedEvent event = new ComponentPurgedEvent(CONTENT_REPOSITORY_ID, componentIds);
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.component"));
    assertThat(auditData.getType(), is("purged"));
    assertThat(auditData.getContext(), is(REPO_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes.get("repository.name"), is(REPO_NAME));
    assertThat(attributes.get("componentIds"), is(Arrays.toString(componentIds)));
  }

  @Test
  public void testOnComponentPurgedEvent_notRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    int[] componentIds = {1, 2, 3};
    ComponentPurgedEvent event = new ComponentPurgedEvent(CONTENT_REPOSITORY_ID, componentIds);
    setRepositorySupplier(event, () -> Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any());
  }

  @Test
  public void testOnComponentsPurgedAuditEvent_recording() {
    Component component1 = createMockComponent("comp1", "ns1", "1.0", "kind1");
    Component component2 = createMockComponent("comp2", "ns2", "2.0", "kind2");
    List<Component> components = Arrays.asList(component1, component2);

    ComponentsPurgedAuditEvent event = new ComponentsPurgedAuditEvent(CONTENT_REPOSITORY_ID, components);
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder, times(2)).record(captor.capture());

    List<AuditData> capturedData = captor.getAllValues();

    AuditData data1 = capturedData.get(0);
    assertThat(data1.getDomain(), is("repository.component"));
    assertThat(data1.getType(), is("purged"));
    assertThat(data1.getContext(), is("comp1"));
    assertThat(data1.getAttributes().get("repository.name"), is(REPO_NAME));
    assertThat(data1.getAttributes().get("name"), is("comp1"));
    assertThat(data1.getAttributes().get("namespace"), is("ns1"));
    assertThat(data1.getAttributes().get("version"), is("1.0"));
    assertThat(data1.getAttributes().get("kind"), is("kind1"));

    AuditData data2 = capturedData.get(1);
    assertThat(data2.getDomain(), is("repository.component"));
    assertThat(data2.getType(), is("purged"));
    assertThat(data2.getContext(), is("comp2"));
    assertThat(data2.getAttributes().get("repository.name"), is(REPO_NAME));
    assertThat(data2.getAttributes().get("name"), is("comp2"));
    assertThat(data2.getAttributes().get("namespace"), is("ns2"));
    assertThat(data2.getAttributes().get("version"), is("2.0"));
    assertThat(data2.getAttributes().get("kind"), is("kind2"));
  }

  @Test
  public void testOnComponentsPurgedAuditEvent_notRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    Component component = createMockComponent("comp1", "ns1", "1.0", "kind1");
    ComponentsPurgedAuditEvent event = new ComponentsPurgedAuditEvent(CONTENT_REPOSITORY_ID, List.of(component));
    setRepositorySupplier(event, () -> Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any());
  }

  @Test
  public void testOnComponentCreatedEvent() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentCreatedEvent event = new ComponentCreatedEvent(componentData);
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.component"));
    assertThat(auditData.getType(), is("created"));
    assertThat(auditData.getContext(), is(COMPONENT_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes.get("repository.name"), is(REPO_NAME));
    assertThat(attributes.get("name"), is(COMPONENT_NAME));
    assertThat(attributes.get("namespace"), is(COMPONENT_NAMESPACE));
    assertThat(attributes.get("version"), is(COMPONENT_VERSION));
    assertThat(attributes.get("kind"), is(COMPONENT_KIND));
  }

  @Test
  public void testOnComponentCreatedEvent_notRecording() {
    when(auditRecorder.isEnabled()).thenReturn(false);

    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentCreatedEvent event = new ComponentCreatedEvent(componentData);
    setRepositorySupplier(event, () -> Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder, never()).record(any());
  }

  @Test
  public void testOnComponentDeletedEvent() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentDeletedEvent event = new ComponentDeletedEvent(componentData);
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.component"));
    assertThat(auditData.getType(), is("deleted"));
    assertThat(auditData.getContext(), is(COMPONENT_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes.get("repository.name"), is(REPO_NAME));
    assertThat(attributes.get("name"), is(COMPONENT_NAME));
    assertThat(attributes.get("namespace"), is(COMPONENT_NAMESPACE));
    assertThat(attributes.get("version"), is(COMPONENT_VERSION));
    assertThat(attributes.get("kind"), is(COMPONENT_KIND));
  }

  @Test
  public void testOnComponentKindEvent() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentKindEvent event = new ComponentKindEvent(componentData);
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.component"));
    assertThat(auditData.getType(), is("updated-kind"));
    assertThat(auditData.getContext(), is(COMPONENT_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes.get("repository.name"), is(REPO_NAME));
    assertThat(attributes.get("name"), is(COMPONENT_NAME));
    assertThat(attributes.get("namespace"), is(COMPONENT_NAMESPACE));
    assertThat(attributes.get("version"), is(COMPONENT_VERSION));
    assertThat(attributes.get("kind"), is(COMPONENT_KIND));
  }

  @Test
  public void testOnComponentAttributesEvent() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentAttributesEvent event = new ComponentAttributesEvent(
        componentData, AttributeOperation.SET, "testKey", "testValue");
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.component"));
    assertThat(auditData.getType(), is("updated-attribute"));
    assertThat(auditData.getContext(), is(COMPONENT_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes.get("repository.name"), is(REPO_NAME));
    assertThat(attributes.get("name"), is(COMPONENT_NAME));
    assertThat(attributes.get("namespace"), is(COMPONENT_NAMESPACE));
    assertThat(attributes.get("version"), is(COMPONENT_VERSION));
    assertThat(attributes.get("kind"), is(COMPONENT_KIND));
    assertThat(attributes.get("attribute.change"), is(AttributeOperation.SET));
    assertThat(attributes.get("attribute.key"), is("testKey"));
    assertThat(attributes.get("attribute.value"), is("testValue"));
  }

  @Test
  public void testOnComponentAttributesEvent_withNullValue() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentAttributesEvent event = new ComponentAttributesEvent(
        componentData, AttributeOperation.REMOVE, "removedKey", null);
    setRepositorySupplier(event, () -> Optional.of(repository));

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is("repository.component"));
    assertThat(auditData.getType(), is("updated-attribute"));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes.get("attribute.change"), is(AttributeOperation.REMOVE));
    assertThat(attributes.get("attribute.key"), is("removedKey"));
    assertThat(attributes.containsKey("attribute.value"), is(false));
  }

  @Test
  public void testOnComponentPurgedEvent_emptyRepository() {
    int[] componentIds = {10};
    ComponentPurgedEvent event = new ComponentPurgedEvent(CONTENT_REPOSITORY_ID, componentIds);
    setRepositorySupplier(event, Optional::empty);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getContext(), is("Unknown"));
    assertThat(auditData.getAttributes().get("repository.name"), is("Unknown"));
  }

  @Test
  public void testOnComponentsPurgedAuditEvent_emptyRepository() {
    Component component = createMockComponent("comp1", "ns1", "1.0", "kind1");
    ComponentsPurgedAuditEvent event = new ComponentsPurgedAuditEvent(CONTENT_REPOSITORY_ID, List.of(component));
    setRepositorySupplier(event, Optional::empty);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on(event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getAttributes().get("repository.name"), is("Unknown"));
  }

  @Test
  public void testOnComponentEvent_emptyRepository() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentCreatedEvent event = new ComponentCreatedEvent(componentData);
    setRepositorySupplier(event, Optional::empty);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(false);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getAttributes().get("repository.name"), is("Unknown"));
  }

  @Test
  public void testOnComponentEvent_notRecordingWhenReplicating() {
    ComponentData componentData = createComponentData(COMPONENT_NAME, COMPONENT_NAMESPACE, COMPONENT_VERSION,
        COMPONENT_KIND);
    ComponentCreatedEvent event = new ComponentCreatedEvent(componentData);
    setRepositorySupplier(event, () -> Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(true);
      underTest.on((ComponentEvent) event);
    }

    verify(auditRecorder, never()).record(any());
  }

  @Test
  public void testOnComponentPurgedEvent_notRecordingWhenReplicating() {
    int[] componentIds = {1};
    ComponentPurgedEvent event = new ComponentPurgedEvent(CONTENT_REPOSITORY_ID, componentIds);
    setRepositorySupplier(event, () -> Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(true);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any());
  }

  @Test
  public void testOnComponentsPurgedAuditEvent_notRecordingWhenReplicating() {
    Component component = createMockComponent("comp1", "ns1", "1.0", "kind1");
    ComponentsPurgedAuditEvent event = new ComponentsPurgedAuditEvent(CONTENT_REPOSITORY_ID, List.of(component));
    setRepositorySupplier(event, () -> Optional.of(repository));

    try (MockedStatic<EventHelper> mockedStatic = mockStatic(EventHelper.class)) {
      mockedStatic.when(EventHelper::isReplicating).thenReturn(true);
      underTest.on(event);
    }

    verify(auditRecorder, never()).record(any());
  }

  /**
   * Sets the repository supplier on a {@link ContentStoreEvent} via reflection,
   * since the method has package-private access in {@code org.sonatype.nexus.repository.content.store}.
   */
  private static void setRepositorySupplier(
      final ContentStoreEvent event,
      final Supplier<Optional<Repository>> supplier)
  {
    try {
      Method method = ContentStoreEvent.class.getDeclaredMethod("setRepositorySupplier", Supplier.class);
      method.setAccessible(true);
      method.invoke(event, supplier);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to set repository supplier", e);
    }
  }

  private static ComponentData createComponentData(
      final String name,
      final String namespace,
      final String version,
      final String kind)
  {
    ComponentData componentData = new ComponentData();
    componentData.setRepositoryId(CONTENT_REPOSITORY_ID);
    componentData.setName(name);
    componentData.setNamespace(namespace);
    componentData.setVersion(version);
    componentData.setKind(kind);
    return componentData;
  }

  private static Component createMockComponent(
      final String name,
      final String namespace,
      final String version,
      final String kind)
  {
    Component component = mock(Component.class);
    when(component.name()).thenReturn(name);
    when(component.namespace()).thenReturn(namespace);
    when(component.version()).thenReturn(version);
    when(component.kind()).thenReturn(kind);
    return component;
  }
}
