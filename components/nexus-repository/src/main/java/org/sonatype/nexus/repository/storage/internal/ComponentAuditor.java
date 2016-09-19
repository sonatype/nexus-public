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
package org.sonatype.nexus.repository.storage.internal;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentCreatedEvent;
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent;
import org.sonatype.nexus.repository.storage.ComponentEvent;
import org.sonatype.nexus.repository.storage.ComponentUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Repository component auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class ComponentAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "repository.component";

  public ComponentAuditor() {
    registerType(ComponentCreatedEvent.class, CREATED_TYPE);
    registerType(ComponentDeletedEvent.class, DELETED_TYPE);
    registerType(ComponentUpdatedEvent.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentEvent event) {
    if (isEnabled() && event.isLocal()) {
      Component component = event.getComponent();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(component.name());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("repository.name", event.getRepositoryName());
      attributes.put("format", component.format());
      attributes.put("name", component.name());
      attributes.put("group", component.group());
      attributes.put("version", component.version());

      record(data);
    }
  }
}
