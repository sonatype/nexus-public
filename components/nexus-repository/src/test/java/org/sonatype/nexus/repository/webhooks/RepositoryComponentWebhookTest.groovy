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
package org.sonatype.nexus.repository.webhooks

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.audit.InitiatorProvider
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.common.entity.EntityMetadata
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentCreatedEvent
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent
import org.sonatype.nexus.repository.storage.ComponentEvent
import org.sonatype.nexus.repository.storage.ComponentUpdatedEvent
import org.sonatype.nexus.repository.webhooks.RepositoryComponentWebhook.RepositoryComponentWebhookPayload
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class RepositoryComponentWebhookTest
    extends TestSupport
{
  private RepositoryComponentWebhook repositoryComponentWebhook

  @Mock
  private EventManager eventManager

  @Mock
  private InitiatorProvider initiatorProvider

  @Mock
  private NodeAccess nodeAccess

  @Mock
  private ComponentCreatedEvent componentCreatedEvent

  @Mock
  private ComponentUpdatedEvent componentUpdatedEvent

  @Mock
  private ComponentDeletedEvent componentDeletedEvent

  @Before
  void before() {
    repositoryComponentWebhook = new RepositoryComponentWebhook(
        eventManager: eventManager,
        initiatorProvider: initiatorProvider,
        nodeAccess: nodeAccess
    )

    def configuration = mock(RepositoryWebhook.Configuration.class)
    when(configuration.repository).thenReturn('repoName')
    repositoryComponentWebhook.subscribe(configuration)

    when(initiatorProvider.get()).thenReturn('initiator')
    when(nodeAccess.id).thenReturn('nodeId')

    for (ComponentEvent componentEvent : [componentCreatedEvent, componentUpdatedEvent, componentDeletedEvent]) {
      def component = mock(Component.class)
      def entityMetadata = mock(EntityMetadata.class)
      def entityId = mock(EntityId.class)

      when(componentEvent.getRepositoryName()).thenReturn('repoName')
      when(componentEvent.getComponent()).thenReturn(component)
      when(component.name()).thenReturn('name')
      when(component.format()).thenReturn('format')
      when(component.group()).thenReturn('group')
      when(component.version()).thenReturn('version')
      when(component.entityMetadata).thenReturn(entityMetadata)
      when(entityMetadata.id).thenReturn(entityId)
      when(entityId.value).thenReturn('id')
    }
  }

  @Test
  void 'has the correct event id'() {
    assert repositoryComponentWebhook.id == "rm:repository:component"
  }

  @Test
  void 'queues local component created events'() {
    testLocalComponentEvent(componentCreatedEvent, 'CREATED')
  }

  @Test
  void 'queues local component updated events'() {
    testLocalComponentEvent(componentUpdatedEvent, 'UPDATED')
  }

  @Test
  void 'queues local component deleted events'() {
    testLocalComponentEvent(componentDeletedEvent, 'DELETED')
  }

  @Test
  void 'ignores non local component created events'() {
    testNonLocalAssetEvent(componentCreatedEvent)
  }

  @Test
  void 'ignores non local component updated events'() {
    testNonLocalAssetEvent(componentUpdatedEvent)
  }

  @Test
  void 'ignores non local component deleted events'() {
    testNonLocalAssetEvent(componentDeletedEvent)
  }

  private void testLocalComponentEvent(ComponentEvent componentEvent, String type) {
    when(componentEvent.isLocal()).thenReturn(true)

    repositoryComponentWebhook.on(componentEvent)

    def argumentCaptor = new ArgumentCaptor<WebhookRequestSendEvent>()
    verify(eventManager).post(argumentCaptor.capture())

    def componentPayload = (RepositoryComponentWebhookPayload) argumentCaptor.value.request.payload

    assert componentPayload.initiator == 'initiator'
    assert componentPayload.nodeId == 'nodeId'
    assert componentPayload.action.toString() == type
    assert componentPayload.repositoryName == 'repoName'
    assert componentPayload.component.name == 'name'
    assert componentPayload.component.format == 'format'
    assert componentPayload.component.group == 'group'
    assert componentPayload.component.version == 'version'
    assert componentPayload.component.id == 'id'
    assert componentPayload.component.componentId == 'cmVwb05hbWU6aWQ'
  }

  private void testNonLocalAssetEvent(ComponentEvent componentEvent) {
    when(componentEvent.isLocal()).thenReturn(false)

    repositoryComponentWebhook.on(componentEvent)

    verify(eventManager, times(0)).post(any(WebhookRequestSendEvent.class))
  }
}
