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
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentCreatedEvent
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent
import org.sonatype.nexus.repository.storage.ComponentEvent
import org.sonatype.nexus.repository.storage.ComponentUpdatedEvent
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookPayload
import org.sonatype.nexus.webhooks.WebhookRequest

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe

/**
 * Repository {@link Component} {@link Webhook}.
 *
 * @since 3.1
 */
@Named
@Singleton
class RepositoryComponentWebhook
    extends RepositoryWebhook
{
  public static final String NAME = 'component'

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
  void on(final ComponentCreatedEvent event) {
    maybeQueue(event, EventAction.CREATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final ComponentUpdatedEvent event) {
    maybeQueue(event, EventAction.UPDATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final ComponentDeletedEvent event) {
    maybeQueue(event, EventAction.DELETED)
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final ComponentEvent event, final EventAction eventAction) {
    if (event.local) {

      Component component = event.component
      def payload = new RepositoryComponentWebhookPayload(
          nodeId: nodeAccess.getId(),
          timestamp: new Date(),
          initiator: initiatorProvider.get(),
          repositoryName: event.repositoryName,
          action: eventAction
      )

      payload.component = new RepositoryComponentWebhookPayload.RepositoryComponent(
          id: component.entityMetadata.id.value,
          componentId: new RepositoryItemIDXO(event.repositoryName, component.entityMetadata.id.value).value,
          format: component.format(),
          name: component.name(),
          group: component.group(),
          version: component.version()
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

  static class RepositoryComponentWebhookPayload
      extends WebhookPayload
  {
    String repositoryName

    EventAction action

    RepositoryComponent component

    static class RepositoryComponent
    {
      String id

      String componentId

      String format

      String name

      String group

      String version
    }
  }
}
