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
package org.sonatype.nexus.repository.content.webhooks

import java.util.function.Function

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.audit.InitiatorProvider
import org.sonatype.nexus.common.app.FeatureFlag
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.content.Component
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent
import org.sonatype.nexus.repository.content.event.component.ComponentEvent
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent
import org.sonatype.nexus.repository.content.store.InternalIds
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook
import org.sonatype.nexus.webhooks.WebhookPayload

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe

import static com.google.common.base.Preconditions.checkNotNull
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED

/**
 * Repository {@link Component} {@link Webhook}.
 *
 * @since 3.27
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
class RepositoryComponentWebhook
    extends RepositoryWebhook
{
  public static final String NAME = "component"

  private NodeAccess nodeAccess

  private InitiatorProvider initiatorProvider

  @Override
  String getName() {
    return NAME
  }

  @Inject
  RepositoryComponentWebhook(final NodeAccess nodeAccess, final InitiatorProvider initiatorProvider) {
    this.nodeAccess = checkNotNull(nodeAccess)
    this.initiatorProvider = checkNotNull(initiatorProvider)
  }

  private enum EventAction
  {
    CREATED,
    UPDATED,
    DELETED,
    PURGED
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

  @Subscribe
  @AllowConcurrentEvents
  void on(final ComponentPurgedEvent event) {
    maybeQueue(getPayload(event, EventAction.PURGED))
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final ComponentEvent event, final EventAction eventAction) {
    maybeQueue(getPayload(event, eventAction))
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final RepositoryComponentWebhookPayload payload) {
    subscriptions.each {
      def configuration = it.configuration as RepositoryWebhook.Configuration
      if (configuration.repository == payload.repositoryName) {
        queue(it, payload)
      }
    }
  }

  private RepositoryComponentWebhookPayload getPayload(final String repositoryName, final EventAction eventAction) {
    new RepositoryComponentWebhookPayload(
        nodeId: nodeAccess.getId(),
        timestamp: new Date(),
        initiator: initiatorProvider.get(),
        repositoryName: repositoryName,
        action: eventAction
    )
  }

  private RepositoryComponentWebhookPayload getPayload(final ComponentEvent event, final EventAction eventAction) {
    Optional<Repository> repository = event.repository
    String repositoryName = repository.map({ it.name } as Function).orElse(null)
    String format = repository.map({ it.format } as Function).orElse(null)

    Component component = event.component
    EntityId componentId = InternalIds.toExternalId(InternalIds.internalComponentId(component))
    def payload = getPayload(repositoryName, eventAction)
    payload.component = new RepositoryComponentWebhookPayload.RepositoryComponent(
        id: componentId.value,
        componentId: new RepositoryItemIDXO(repositoryName, componentId.value).value,
        format: format,
        name: component.name(),
        group: component.namespace(),
        version: component.version()
    )
    return payload
  }

  private RepositoryComponentWebhookPayload getPayload(
      final ComponentPurgedEvent event,
      final EventAction eventAction)
  {
    String repositoryName = event.repository.map({ it.name } as Function).orElse(null)

    def payload = getPayload(repositoryName, eventAction)
    payload.components = event.componentIds.collect {InternalIds.toExternalId(it).value }
    return payload
  }

  static class RepositoryComponentWebhookPayload
      extends WebhookPayload
  {
    String repositoryName

    EventAction action

    RepositoryComponent component

    String[] components

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
