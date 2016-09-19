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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryDestroyedEvent;
import org.sonatype.nexus.repository.RepositoryEvent;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.RepositoryStoppedEvent;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryLoadedEvent;
import org.sonatype.nexus.repository.manager.RepositoryRestoredEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Repository auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class RepositoryAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "repository";

  public RepositoryAuditor() {
    registerType(RepositoryCreatedEvent.class, CREATED_TYPE);
    registerType(RepositoryRestoredEvent.class, "restored");
    registerType(RepositoryUpdatedEvent.class, UPDATED_TYPE);
    registerType(RepositoryDestroyedEvent.class, "destroyed");
    registerType(RepositoryDeletedEvent.class, DELETED_TYPE);
    registerType(RepositoryLoadedEvent.class, "loaded");
    registerType(RepositoryStartedEvent.class, "started");
    registerType(RepositoryStoppedEvent.class, "stopped");
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryEvent event) {
    if (isEnabled()) {
      Repository repository = event.getRepository();
      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(repository.getName());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("name", repository.getName());
      attributes.put("type", repository.getType().getValue());
      attributes.put("format", repository.getFormat().getValue());

      record(data);
    }
  }
}
