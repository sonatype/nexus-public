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
package org.sonatype.nexus.audit.internal

import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.audit.AuditDataRecordedEvent
import org.sonatype.nexus.webhooks.GlobalWebhook
import org.sonatype.nexus.webhooks.Webhook
import org.sonatype.nexus.webhooks.WebhookSubscription

import com.google.common.eventbus.AllowConcurrentEvents
import com.google.common.eventbus.Subscribe

/**
 * Global audit {@link Webhook}.
 *
 * @since 3.1
 */
@Named
@Singleton
class GlobalAuditWebhook
    extends GlobalWebhook
{
  public static final String NAME = 'audit'

  @Override
  String getName() {
    return NAME
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final AuditDataRecordedEvent event) {
    // use detached copy to avoid including EntityMetadata
    def body = event.data.detach()

    for (WebhookSubscription subscription in subscriptions) {
      queue(subscription, body)
    }
  }
}

