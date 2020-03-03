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
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.RepositoryEvent
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook.RepositoryWebhookPayload
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class GlobalRepositoryWebhookTest
    extends TestSupport
{
  private GlobalRepositoryWebhook globalRepositoryWebhook

  @Mock
  private EventManager eventManager

  @Mock
  private InitiatorProvider initiatorProvider

  @Mock
  private NodeAccess nodeAccess

  @Mock
  private RepositoryCreatedEvent repositoryCreatedEvent

  @Mock
  private RepositoryUpdatedEvent repositoryUpdatedEvent

  @Mock
  private RepositoryDeletedEvent repositoryDeletedEvent

  public class TestFormat
      extends Format
  {
    public TestFormat() {
      super('format')
    }
  }

  @Before
  public void before() {
    globalRepositoryWebhook = new GlobalRepositoryWebhook(
        eventManager: eventManager,
        initiatorProvider: initiatorProvider,
        nodeAccess: nodeAccess
    )

    def configuration = mock(RepositoryWebhook.Configuration.class)
    when(configuration.repository).thenReturn('repoName')
    globalRepositoryWebhook.subscribe(configuration)

    when(initiatorProvider.get()).thenReturn('initiator')
    when(nodeAccess.id).thenReturn('nodeId')

    for (RepositoryEvent repositoryEvent : [repositoryCreatedEvent, repositoryUpdatedEvent, repositoryDeletedEvent]) {
      def repository = mock(Repository.class)

      when(repositoryEvent.getRepository()).thenReturn(repository)
      when(repository.name).thenReturn('name')
      when(repository.format).thenReturn(new TestFormat())
      when(repository.type).thenReturn(new ProxyType())
    }
  }

  @Test
  public void 'has the correct event id'() {
    assert globalRepositoryWebhook.id == "rm:global:repository"
  }

  @Test
  public void 'queues repository created events'() {
    testRepositoryEvent(repositoryCreatedEvent, 'CREATED')
  }

  @Test
  public void 'queues repository updated events'() {
    testRepositoryEvent(repositoryUpdatedEvent, 'UPDATED')
  }

  @Test
  public void 'queues repository deleted events'() {
    testRepositoryEvent(repositoryDeletedEvent, 'DELETED')
  }

  private void testRepositoryEvent(RepositoryEvent repositoryEvent, String type) {
    globalRepositoryWebhook.on(repositoryEvent)

    def assetArgumentCaptor = new ArgumentCaptor<WebhookRequestSendEvent>()
    verify(eventManager).post(assetArgumentCaptor.capture())

    def repositoryPayload = (RepositoryWebhookPayload) assetArgumentCaptor.value.request.payload

    assert repositoryPayload.initiator == 'initiator'
    assert repositoryPayload.nodeId == 'nodeId'
    assert repositoryPayload.action.toString() == type
    assert repositoryPayload.repository.name == 'name'
    assert repositoryPayload.repository.format == 'format'
    assert repositoryPayload.repository.type == 'proxy'
  }
}
