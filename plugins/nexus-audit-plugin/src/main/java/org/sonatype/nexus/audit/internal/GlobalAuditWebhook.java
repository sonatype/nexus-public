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
package org.sonatype.nexus.audit.internal;

import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.audit.internal.GlobalAuditWebhook.AuditWebhookPayload.Audit;
import org.sonatype.nexus.webhooks.GlobalWebhook;
import org.sonatype.nexus.webhooks.WebhookPayload;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Global audit {@link Webhook}.
 *
 * @since 3.1
 */
@Named
@Singleton
public class GlobalAuditWebhook
    extends GlobalWebhook
{
  public static final String NAME = "audit";

  @Override
  public String getName() {
    return NAME;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AuditDataRecordedEvent event) {

    AuditData auditData = event.getData();
    AuditWebhookPayload payload = new AuditWebhookPayload();
    payload.setInitiator(auditData.getInitiator());
    payload.setNodeId(auditData.getNodeId());

    Audit audit = new Audit(auditData.getDomain(), auditData.getType(),
        auditData.getContext(), auditData.getAttributes());
    payload.setAudit(audit);

    getSubscriptions().forEach(s -> queue(s, payload));
  }

  public static class AuditWebhookPayload
      extends WebhookPayload
  {
    public Audit getAudit() {
      return audit;
    }

    public void setAudit(Audit audit) {
      this.audit = audit;
    }

    private Audit audit;

    public static class Audit
    {
      private String domain;

      private String type;

      private String context;

      private Map<String, Object> attributes;

      public Audit(final String domain, final String type, final String context, final Map<String, Object> attributes) {
        this.domain = domain;
        this.type = type;
        this.context = context;
        this.attributes = attributes;
      }

      public String getDomain() {
        return domain;
      }

      public void setDomain(String domain) {
        this.domain = domain;
      }

      public String getType() {
        return type;
      }

      public void setType(String type) {
        this.type = type;
      }

      public String getContext() {
        return context;
      }

      public void setContext(String context) {
        this.context = context;
      }

      public Map<String, Object> getAttributes() {
        return attributes;
      }

      public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
      }
    }
  }
}
