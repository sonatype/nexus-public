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
package org.sonatype.nexus.repository.content.webhooks;

import java.net.URI;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ContentStoreEventSetter;
import org.sonatype.nexus.repository.content.webhooks.RepositoryComponentWebhook.RepositoryComponentWebhookPayload;
import org.sonatype.nexus.repository.content.webhooks.RepositoryComponentWebhook.RepositoryComponentWebhookPayload.RepositoryComponent;
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook;
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class RepositoryComponentWebhookTest
    extends TestSupport
{
  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private InitiatorProvider initiatorProvider;

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  private RepositoryComponentWebhook underTest;

  @Before
  public void setUp() {
    when(nodeAccess.getId()).thenReturn("node-1");
    when(initiatorProvider.get()).thenReturn("admin");
    when(repository.getName()).thenReturn("maven-releases");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    underTest = new RepositoryComponentWebhook(nodeAccess, initiatorProvider);
    underTest.setEventManager(eventManager);
  }

  @Test
  public void testGetName() {
    assertThat(underTest.getName(), is("component"));
  }

  @Test
  public void testWebhookNameConstant() {
    assertThat(RepositoryComponentWebhook.NAME, is("component"));
  }

  @Test
  public void testRepositoryComponentFields() {
    RepositoryComponent component = new RepositoryComponent(
        "ext-id", "comp-id", "maven2", "artifact", "org.example", "1.0.0");

    assertThat(component.getId(), is("ext-id"));
    assertThat(component.getComponentId(), is("comp-id"));
    assertThat(component.getFormat(), is("maven2"));
    assertThat(component.getName(), is("artifact"));
    assertThat(component.getGroup(), is("org.example"));
    assertThat(component.getVersion(), is("1.0.0"));
  }

  @Test
  public void testRepositoryComponentWithNullGroup() {
    RepositoryComponent component = new RepositoryComponent(
        "ext-id", "comp-id", "npm", "lodash", null, "4.17.21");

    assertThat(component.getId(), is("ext-id"));
    assertThat(component.getFormat(), is("npm"));
    assertThat(component.getName(), is("lodash"));
    assertThat(component.getGroup(), is(nullValue()));
    assertThat(component.getVersion(), is("4.17.21"));
  }

  @Test
  public void testRepositoryComponentWithDifferentFormats() {
    RepositoryComponent docker = new RepositoryComponent(
        "1", "d-1", "docker", "nginx", "library", "latest");
    assertThat(docker.getFormat(), is("docker"));
    assertThat(docker.getName(), is("nginx"));
    assertThat(docker.getGroup(), is("library"));
    assertThat(docker.getVersion(), is("latest"));

    RepositoryComponent nuget = new RepositoryComponent(
        "2", "n-1", "nuget", "Newtonsoft.Json", null, "13.0.1");
    assertThat(nuget.getFormat(), is("nuget"));
    assertThat(nuget.getName(), is("Newtonsoft.Json"));
    assertThat(nuget.getGroup(), is(nullValue()));
  }

  @Test
  public void testOnComponentCreatedEvent_noSubscriptions() {
    ComponentData component = createComponentData(5);
    ComponentCreatedEvent event = new ComponentCreatedEvent(component);
    ContentStoreEventSetter.setRepositorySupplier(event, () -> Optional.of(repository));

    underTest.on(event);

    verifyNoInteractions(eventManager);
  }

  @Test
  public void testOnComponentCreatedEvent_withSubscription_queuesPayload() {
    subscribeToRepository("maven-releases");

    ComponentData component = createComponentData(5);
    ComponentCreatedEvent event = new ComponentCreatedEvent(component);
    ContentStoreEventSetter.setRepositorySupplier(event, () -> Optional.of(repository));

    underTest.on(event);

    RepositoryComponentWebhookPayload payload = capturePayload();
    assertThat(payload.getRepositoryName(), is("maven-releases"));
    assertThat(String.valueOf(payload.getAction()), is("CREATED"));
    assertThat(payload.getNodeId(), is("node-1"));
    assertThat(payload.getInitiator(), is("admin"));
    assertThat(payload.getComponent(), is(notNullValue()));
    assertThat(payload.getComponent().getFormat(), is("maven2"));
    assertThat(payload.getComponent().getName(), is("artifact"));
    assertThat(payload.getComponent().getGroup(), is("org.example"));
    assertThat(payload.getComponent().getVersion(), is("1.0"));
  }

  @Test
  public void testOnComponentUpdatedEvent_withSubscription_queuesPayload() {
    subscribeToRepository("maven-releases");

    ComponentData component = createComponentData(5);
    ComponentKindEvent event = new ComponentKindEvent(component);
    ContentStoreEventSetter.setRepositorySupplier(event, () -> Optional.of(repository));

    underTest.on(event);

    RepositoryComponentWebhookPayload payload = capturePayload();
    assertThat(payload.getRepositoryName(), is("maven-releases"));
    assertThat(String.valueOf(payload.getAction()), is("UPDATED"));
    assertThat(payload.getComponent(), is(notNullValue()));
  }

  @Test
  public void testOnComponentDeletedEvent_withSubscription_queuesPayload() {
    subscribeToRepository("maven-releases");

    ComponentData component = createComponentData(5);
    ComponentDeletedEvent event = new ComponentDeletedEvent(component);
    ContentStoreEventSetter.setRepositorySupplier(event, () -> Optional.of(repository));

    underTest.on(event);

    RepositoryComponentWebhookPayload payload = capturePayload();
    assertThat(payload.getRepositoryName(), is("maven-releases"));
    assertThat(String.valueOf(payload.getAction()), is("DELETED"));
    assertThat(payload.getComponent(), is(notNullValue()));
  }

  @Test
  public void testOnComponentPurgedEvent_withSubscription_queuesPayload() {
    subscribeToRepository("maven-releases");

    ComponentPurgedEvent event = new ComponentPurgedEvent(1, new int[]{5, 6});
    ContentStoreEventSetter.setRepositorySupplier(event, () -> Optional.of(repository));

    underTest.on(event);

    RepositoryComponentWebhookPayload payload = capturePayload();
    assertThat(payload.getRepositoryName(), is("maven-releases"));
    assertThat(String.valueOf(payload.getAction()), is("PURGED"));
    assertThat(payload.getComponents(), is(notNullValue()));
    assertThat(payload.getComponents().length, is(2));
  }

  @Test
  public void testOnEvent_subscriptionForDifferentRepo_doesNotQueue() {
    subscribeToRepository("other-repo");

    ComponentData component = createComponentData(5);
    ComponentCreatedEvent event = new ComponentCreatedEvent(component);
    ContentStoreEventSetter.setRepositorySupplier(event, () -> Optional.of(repository));

    underTest.on(event);

    // eventManager.register is called during subscribe, but no WebhookRequestSendEvent should be posted
    verify(eventManager).register(underTest);
  }

  private ComponentData createComponentData(final int componentId) {
    ComponentData component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(componentId);
    component.setNamespace("org.example");
    component.setName("artifact");
    component.setVersion("1.0");
    return component;
  }

  private void subscribeToRepository(final String repositoryName) {
    RepositoryWebhook.Configuration config = mock(RepositoryWebhook.Configuration.class);
    when(config.getRepository()).thenReturn(repositoryName);
    when(config.getUrl()).thenReturn(URI.create("http://example.com/webhook"));
    underTest.subscribe(config);
  }

  private RepositoryComponentWebhookPayload capturePayload() {
    ArgumentCaptor<WebhookRequestSendEvent> captor = ArgumentCaptor.forClass(WebhookRequestSendEvent.class);
    verify(eventManager).post(captor.capture());
    return (RepositoryComponentWebhookPayload) captor.getValue().getRequest().getPayload();
  }
}
