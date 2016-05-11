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

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentCreatedEvent
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent
import org.sonatype.nexus.repository.storage.ComponentEvent
import org.sonatype.nexus.repository.storage.ComponentUpdatedEvent
import org.sonatype.nexus.security.UserIdHelper
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookRequest
import org.sonatype.nexus.webhooks.WebhookSubscription

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

  @Override
  String getName() {
    return NAME
  }

  private static enum EventType
  {
    CREATED,
    UPDATED,
    DELETED
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final ComponentCreatedEvent event) {
    maybeQueue(event, EventType.CREATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final ComponentUpdatedEvent event) {
    maybeQueue(event, EventType.UPDATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final ComponentDeletedEvent event) {
    maybeQueue(event, EventType.DELETED)
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final ComponentEvent event, final EventType eventType) {
    Component component = event.component
    for (WebhookSubscription subscription in subscriptions) {
      def configuration = subscription.configuration as RepositoryWebhook.Configuration
      if (configuration.repository == event.repositoryName) {
        // TODO: discriminate on content-selector
        queue(subscription, [
            timestamp: new Date(),
            userId: UserIdHelper.get(),
            repositoryName: event.repositoryName,
            componentFormat: component.format(),
            componentName: component.name(),
            componentGroup: component.group(),
            componentVersion: component.version(),
            eventType: eventType
        ])
      }
    }
  }
}
