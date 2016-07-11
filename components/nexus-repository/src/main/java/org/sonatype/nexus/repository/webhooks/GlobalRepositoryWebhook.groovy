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
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent
import org.sonatype.nexus.webhooks.GlobalWebhook
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookPayload

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe

/**
 * Global repository {@link Webhook}.
 *
 * @since 3.1
 */
@Named
@Singleton
class GlobalRepositoryWebhook
    extends GlobalWebhook
{
  public static final String NAME = 'repository'

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
  void on(final RepositoryCreatedEvent event) {
    queue(event.repository, EventAction.CREATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryUpdatedEvent event) {
    queue(event.repository, EventAction.UPDATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryDeletedEvent event) {
    queue(event.repository, EventAction.DELETED)
  }

  private void queue(final Repository repository, final EventAction eventAction) {
    def payload = new RepositoryWebhookPayload(
        nodeId: nodeAccess.getId(),
        timestamp: new Date(),
        initiator: initiatorProvider.get(),
        action: eventAction
    )

    payload.repository = new RepositoryWebhookPayload.Repository(
        format: repository.format.value,
        name: repository.name,
        type: repository.type.value
    )

    subscriptions.each {
      queue(it, payload)
    }
  }

  static class RepositoryWebhookPayload
      extends WebhookPayload
  {
    EventAction action

    Repository repository

    static class Repository
    {
      String format

      String name

      String type
    }
  }
}
