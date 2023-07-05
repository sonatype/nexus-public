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
package org.sonatype.nexus.selector;

import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * ContentSelector auditor.
 *
 */
@Named
@Singleton
public class ContentSelectorAuditor
  extends AuditorSupport
  implements EventAware
{
  public static final String DOMAIN = "ContentSelector";

  public ContentSelectorAuditor() {
    registerType(ContentSelectorCreatedEvent.class, CREATED_TYPE);
    registerType(ContentSelectorUpdatedEvent.class, UPDATED_TYPE);
    registerType(ContentSelectorDeletedEvent.class, DELETED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ContentSelectorEvent event) {
    if (isRecording()) {
      SelectorConfiguration selectorConfiguration = event.getSelectorConfiguration();

      AuditData data = getSelectorAuditData(event, selectorConfiguration);
      record(data);
    }
  }

  private AuditData getSelectorAuditData(final ContentSelectorEvent event, final SelectorConfiguration selectorConfiguration) {
    AuditData data = new AuditData();
    data.setDomain(DOMAIN);
    data.setType(type(event.getClass()));
    data.setContext(selectorConfiguration.getName());

    Map<String, Object> attributes = data.getAttributes();
    attributes.put("description", selectorConfiguration.getDescription());
    attributes.put("type", selectorConfiguration.getType());
    attributes.put("expression", selectorConfiguration.getAttributes());
    return data;
  }
}
