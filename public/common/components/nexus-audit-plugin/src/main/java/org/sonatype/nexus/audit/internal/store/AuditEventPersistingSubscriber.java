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
package org.sonatype.nexus.audit.internal.store;

import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditDataRecordedEvent;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = FeatureFlags.PREVIEW_UI_AUDIT_ENABLED, havingValue = "true")
public class AuditEventPersistingSubscriber
    implements EventAware
{
  private static final Logger log = LoggerFactory.getLogger(AuditEventPersistingSubscriber.class);

  private final AuditEventStore auditEventStore;

  @Autowired
  public AuditEventPersistingSubscriber(final AuditEventStore auditEventStore) {
    this.auditEventStore = auditEventStore;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AuditDataRecordedEvent event) {
    if (EventHelper.isReplicating()) {
      return;
    }
    try {
      AuditData data = event.getData();
      AuditEventData entity = new AuditEventData();
      entity.setDomain(data.getDomain());
      entity.setType(data.getType());
      entity.setContext(data.getContext());
      Date ts = data.getTimestamp();
      if (ts != null) {
        entity.setTimestamp(ts.toInstant().atOffset(ZoneOffset.UTC));
      }
      entity.setInitiator(data.getInitiator());
      entity.setNodeId(data.getNodeId());
      entity.setAttributes(data.getAttributes());
      auditEventStore.insert(entity);
    }
    catch (Exception e) {
      log.warn("Failed to persist audit event", e);
    }
  }
}
