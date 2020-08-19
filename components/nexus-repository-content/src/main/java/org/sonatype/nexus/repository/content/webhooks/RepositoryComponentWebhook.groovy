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
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent
import org.sonatype.nexus.repository.content.store.InternalIds
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookPayload
import org.sonatype.nexus.webhooks.WebhookRequest

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe
import org.apache.commons.lang.StringUtils

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Repository {@link Component} {@link Webhook}.
 *
 * @since 3.next
 */
@FeatureFlag(name = "nexus.datastore.enabled")
@Named
@Singleton
class RepositoryComponentWebhook
    extends RepositoryWebhook
{
  public static final String NAME = "component";

  private NodeAccess nodeAccess;

  private InitiatorProvider initiatorProvider;

  @Override
  public String getName() {
    return NAME;
  }

  @Inject
  public RepositoryComponentWebhook(final NodeAccess nodeAccess, final InitiatorProvider initiatorProvider) {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.initiatorProvider = checkNotNull(initiatorProvider);
  }

  private enum EventAction
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
    Repository repository = event.repository
    Component component = event.component
    EntityId componentId = InternalIds.toExternalId(InternalIds.internalComponentId(component))

    def payload = new RepositoryComponentWebhookPayload(
        nodeId: nodeAccess.getId(),
        timestamp: new Date(),
        initiator: initiatorProvider.get(),
        repositoryName: repository.name,
        action: eventAction
    )

    payload.component = new RepositoryComponentWebhookPayload.RepositoryComponent(
        id: componentId.value,
        componentId: new RepositoryItemIDXO(repository.name, componentId.value).value,
        format: repository.format,
        name: StringUtils.stripStart(component.name(), "/"),
        group: component.namespace(),
        version: component.version()
    )

    subscriptions.each {
      def configuration = it.configuration as RepositoryWebhook.Configuration
      if (configuration.repository == repository.name) {
        queue(it, payload)
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
