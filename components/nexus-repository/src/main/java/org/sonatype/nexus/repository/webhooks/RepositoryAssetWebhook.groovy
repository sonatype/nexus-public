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

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.audit.InitiatorProvider
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.AssetCreatedEvent
import org.sonatype.nexus.repository.storage.AssetDeletedEvent
import org.sonatype.nexus.repository.storage.AssetEvent
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookPayload
import org.sonatype.nexus.webhooks.WebhookRequest

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe

/**
 * Repository {@link Asset} {@link Webhook}.
 *
 * @since 3.1
 */
@Named
@Singleton
class RepositoryAssetWebhook
    extends RepositoryWebhook
{
  public static final String NAME = 'asset'

  @Inject
  NodeAccess nodeAccess

  @Inject
  InitiatorProvider initiatorProvider

  @Override
  String getName() {
    return NAME
  }

  private static enum EventAction
  {
    CREATED,
    UPDATED,
    DELETED
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final AssetCreatedEvent event) {
    maybeQueue(event, EventAction.CREATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final AssetUpdatedEvent event) {
    maybeQueue(event, EventAction.UPDATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final AssetDeletedEvent event) {
    maybeQueue(event, EventAction.DELETED)
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final AssetEvent event, final EventAction eventAction) {
    if (event.local) {

      Asset asset = event.asset
      def payload = new RepositoryAssetWebhookPayload(
          nodeId: nodeAccess.getId(),
          timestamp: new Date(),
          initiator: initiatorProvider.get(),
          repositoryName: event.repositoryName,
          action: eventAction
      )

      payload.asset = new RepositoryAssetWebhookPayload.RepositoryAsset(
          id: asset.entityMetadata.id.value,
          assetId: new RepositoryItemIDXO(event.repositoryName, asset.entityMetadata.id.value).value,
          format: asset.format(),
          name: asset.name()
      )

      subscriptions.each {
        def configuration = it.configuration as RepositoryWebhook.Configuration
        if (configuration.repository == event.repositoryName) {
          // TODO: discriminate on content-selector
          queue(it, payload)
        }
      }
    }
  }

  static class RepositoryAssetWebhookPayload
      extends WebhookPayload
  {
    String repositoryName

    EventAction action

    RepositoryAsset asset

    static class RepositoryAsset
    {
      String id

      String assetId

      String format

      String name
    }
  }
}
