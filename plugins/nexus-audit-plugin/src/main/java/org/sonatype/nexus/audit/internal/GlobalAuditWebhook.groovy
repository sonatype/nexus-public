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
import org.sonatype.nexus.webhooks.WebhookPayload

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
    def auditData = event.data.detach()
    def payload = new AuditWebhookPayload(
        initiator: auditData.initiator,
        nodeId: auditData.nodeId
    )

    payload.audit = new AuditWebhookPayload.Audit(
        domain: auditData.domain,
        context: auditData.context,
        type: auditData.type,
        attributes: auditData.attributes
    )

    subscriptions.each {
      queue(it, payload)
    }
  }

  static class AuditWebhookPayload
      extends WebhookPayload
  {
    Audit audit

    static class Audit
    {
      String domain

      String type

      String context

      Map<String, String> attributes
    }
  }
}

