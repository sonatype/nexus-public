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

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent
import org.sonatype.nexus.security.UserIdHelper
import org.sonatype.nexus.webhooks.GlobalWebhook
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookSubscription

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
  void on(final RepositoryCreatedEvent event) {
    queue(event.repository, EventType.CREATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryUpdatedEvent event) {
    queue(event.repository, EventType.UPDATED)
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryDeletedEvent event) {
    queue(event.repository, EventType.DELETED)
  }

  private void queue(final Repository repository, final EventType eventType) {
    def body = [
        timestamp: new Date(),
        userId: UserIdHelper.get(),
        repositoryName: repository.name,
        repositoryType: repository.type.value,
        repositoryFormat: repository.format.value,
        eventType: eventType
    ]

    for (WebhookSubscription subscription in subscriptions) {
      queue(subscription, body)
    }
  }
}
