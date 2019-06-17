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
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.AssetCreatedEvent
import org.sonatype.nexus.repository.storage.AssetDeletedEvent
import org.sonatype.nexus.repository.storage.AssetEvent
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent
import org.sonatype.nexus.repository.webhooks.RepositoryAssetWebhook.RepositoryAssetWebhookPayload
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

class RepositoryAssetWebhookTest
    extends TestSupport
{
  private RepositoryAssetWebhook repositoryAssetWebhook

  @Mock
  private EventManager eventManager

  @Mock
  private InitiatorProvider initiatorProvider

  @Mock
  private NodeAccess nodeAccess

  @Mock
  private AssetCreatedEvent assetCreatedEvent

  @Mock
  private AssetUpdatedEvent assetUpdatedEvent

  @Mock
  private AssetDeletedEvent assetDeletedEvent

  @Before
  void before() {
    repositoryAssetWebhook = new RepositoryAssetWebhook(
        eventManager: eventManager,
        initiatorProvider: initiatorProvider,
        nodeAccess: nodeAccess
    )

    def configuration = mock(RepositoryWebhook.Configuration.class)
    when(configuration.repository).thenReturn('repoName')
    repositoryAssetWebhook.subscribe(configuration)

    when(initiatorProvider.get()).thenReturn('initiator')
    when(nodeAccess.id).thenReturn('nodeId')

    for (AssetEvent assetEvent : [assetCreatedEvent, assetUpdatedEvent, assetDeletedEvent]) {
      def asset = mock(Asset.class)
      def entityMetadata = mock(EntityMetadata.class)
      def entityId = mock(EntityId.class)

      when(assetEvent.getRepositoryName()).thenReturn('repoName')
      when(assetEvent.getAsset()).thenReturn(asset)
      when(asset.name()).thenReturn('name')
      when(asset.format()).thenReturn('format')
      when(asset.entityMetadata).thenReturn(entityMetadata)
      when(entityMetadata.id).thenReturn(entityId)
      when(entityId.value).thenReturn('id')
    }
  }

  @Test
  void 'has the correct event id'() {
    assert repositoryAssetWebhook.id == "rm:repository:asset"
  }

  @Test
  void 'queues local asset created events'() {
    testLocalAssetEvent(assetCreatedEvent, 'CREATED')
  }

  @Test
  void 'queues local asset updated events'() {
    testLocalAssetEvent(assetUpdatedEvent, 'UPDATED')
  }

  @Test
  void 'queues local asset deleted events'() {
    testLocalAssetEvent(assetDeletedEvent, 'DELETED')
  }

  @Test
  void 'ignores non local asset created events'() {
    testNonLocalAssetEvent(assetCreatedEvent)
  }

  @Test
  void 'ignores non local asset updated events'() {
    testNonLocalAssetEvent(assetUpdatedEvent)
  }

  @Test
  void 'ignores non local asset deleted events'() {
    testNonLocalAssetEvent(assetDeletedEvent)
  }

  private void testLocalAssetEvent(AssetEvent assetEvent, String type) {
    when(assetEvent.isLocal()).thenReturn(true)

    repositoryAssetWebhook.on(assetEvent)

    def assetArgumentCaptor = new ArgumentCaptor<WebhookRequestSendEvent>()
    verify(eventManager).post(assetArgumentCaptor.capture())

    def assetPayload = (RepositoryAssetWebhookPayload) assetArgumentCaptor.value.request.payload

    assert assetPayload.initiator == 'initiator'
    assert assetPayload.nodeId == 'nodeId'
    assert assetPayload.action.toString() == type
    assert assetPayload.repositoryName == 'repoName'
    assert assetPayload.asset.name == 'name'
    assert assetPayload.asset.format == 'format'
    assert assetPayload.asset.id == 'id'
    assert assetPayload.asset.assetId == 'cmVwb05hbWU6aWQ'
  }

  private void testNonLocalAssetEvent(AssetEvent assetEvent) {
    when(assetEvent.isLocal()).thenReturn(false)

    repositoryAssetWebhook.on(assetEvent)

    verify(eventManager, times(0)).post(any(WebhookRequestSendEvent.class))
  }
}
