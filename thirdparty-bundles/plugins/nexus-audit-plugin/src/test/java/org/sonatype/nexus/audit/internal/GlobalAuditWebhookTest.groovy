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

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.audit.AuditData
import org.sonatype.nexus.audit.AuditDataRecordedEvent
import org.sonatype.nexus.audit.internal.GlobalAuditWebhook.AuditWebhookPayload
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.webhooks.WebhookConfiguration
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent

import org.junit.Test
import org.mockito.ArgumentCaptor

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

class GlobalAuditWebhookTest
    extends TestSupport
{
  @Test
  public void 'has the correct event id'() {
    def eventManager = mock(EventManager.class)

    def globalAuditWebhook = new GlobalAuditWebhook(
        eventManager: eventManager
    )

    assert globalAuditWebhook.id == "rm:global:audit"
  }

  @Test
  public void 'queues audit webhook'() {
    def eventManager = mock(EventManager.class)

    def globalAuditWebhook = new GlobalAuditWebhook(
        eventManager: eventManager
    )

    def configuration = mock(WebhookConfiguration.class)
    globalAuditWebhook.subscribe(configuration)

    def auditData = new AuditData(
        attributes: ['key': 'value']
    )
    auditData.initiator = 'initiator'
    auditData.nodeId = 'nodeId'
    auditData.domain = 'domain'
    auditData.context = 'context'
    auditData.type = 'type'

    def auditRecordedEvent = new AuditDataRecordedEvent(auditData)

    globalAuditWebhook.on(auditRecordedEvent)

    def argumentCaptor = new ArgumentCaptor<WebhookRequestSendEvent>()
    verify(eventManager).post(argumentCaptor.capture())

    def auditPayload = (AuditWebhookPayload) argumentCaptor.value.request.payload

    assert auditPayload.initiator == 'initiator'
    assert auditPayload.nodeId == 'nodeId'
    assert auditPayload.audit.domain == 'domain'
    assert auditPayload.audit.type == 'type'
    assert auditPayload.audit.context == 'context'
    assert auditPayload.audit.attributes.size() == 1
    assert auditPayload.audit.attributes['key'] == 'value'
  }
}
