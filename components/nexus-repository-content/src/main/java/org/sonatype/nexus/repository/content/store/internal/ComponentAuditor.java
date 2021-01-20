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
package org.sonatype.nexus.repository.content.store.internal;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.component.ComponentAttributesEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentCreatedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Repository component auditor.
 *
 * @since 3.27
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

    registerType(ComponentPurgedEvent.class, PURGE_TYPE);

    registerType(ComponentUpdatedEvent.class, UPDATED_TYPE);
    registerType(ComponentKindEvent.class, UPDATED_TYPE + "-kind");
    registerType(ComponentAttributesEvent.class, UPDATED_TYPE + "-attribute");
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentPurgedEvent event) {
    if (isRecording()) {
      String repositoryName =  event.getRepository().map(Repository::getName).orElse("Unknown");

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(repositoryName);

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("repository.name", repositoryName);
      attributes.put("componentIds", Arrays.toString(event.getComponentIds()));

      record(data);
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentEvent event) {
    if (isRecording()) {
      Component component = event.getComponent();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(component.name());

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("repository.name", event.getRepository().map(Repository::getName).orElse("Unknown"));
      attributes.put("name", component.name());
      attributes.put("kind", component.kind());
      attributes.put("namespace", component.namespace());
      attributes.put("version", component.version());

      if (event instanceof ComponentAttributesEvent){
        ComponentAttributesEvent attributesEvent = (ComponentAttributesEvent) event;
        attributes.put("attribute.change", attributesEvent.getChange());
        attributes.put("attribute.key", attributesEvent.getKey());
        attributes.put("attribute.value", attributesEvent.getValue());
      }

      record(data);
    }
  }
}
