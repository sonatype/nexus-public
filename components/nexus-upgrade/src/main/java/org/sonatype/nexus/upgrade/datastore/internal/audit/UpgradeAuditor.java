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
package org.sonatype.nexus.upgrade.datastore.internal.audit;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeCompletedEvent;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeEventSupport;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeFailedEvent;
import org.sonatype.nexus.upgrade.datastore.events.UpgradeStartedEvent;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Captures events for audit logging database migrations.
 */
@Named
@Singleton
public class UpgradeAuditor
    extends AuditorSupport
    implements EventAware, EventAware.Asynchronous
{
  public static final String DOMAIN = "database-migration";

  static final String COMPLETED = "completed";

  static final String FAILED = "failed";

  static final String STARTED = "started";

  static final String MIGRATIONS = "migrations";

  static final String NODE_IDS = "nodeIds";

  static final String MESSAGE = "message";

  static final String SCHEMA_VERSION = "schemaVersion";

  static final String NEXUS_VERSION = "nexusVersion";

  private final String nexusVersion;

  @Inject
  public UpgradeAuditor(final ApplicationVersion applicationVersion) {
    nexusVersion = checkNotNull(applicationVersion).getVersion();

    registerType(UpgradeCompletedEvent.class, COMPLETED);
    registerType(UpgradeFailedEvent.class, FAILED);
    registerType(UpgradeStartedEvent.class, STARTED);
  }

  /*
   * Not concurrent as we want the events to be handled in order
   */
  @Subscribe
  public void on(final UpgradeEventSupport event) {
    if (isRecording() && !EventHelper.isReplicating()) {
      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(SYSTEM_CONTEXT);
      data.setInitiator(event.getUser().orElse(SYSTEM_CONTEXT));

      Map<String, Object> attributes = data.getAttributes();
      attributes.put(NEXUS_VERSION, nexusVersion);
      event.getSchemaVersion()
          .ifPresent(schemaVersion -> attributes.put(SCHEMA_VERSION, schemaVersion));

      if (event instanceof UpgradeFailedEvent) {
        attributes.put(MESSAGE, ((UpgradeFailedEvent) event).getErrorMessage());
      }
      else {
        attributes.put(MIGRATIONS, event.getMigrations());
      }

      if (event instanceof UpgradeCompletedEvent) {
        attributes.put(NODE_IDS, ((UpgradeCompletedEvent) event).getNodeIds());
      }

      record(data);
    }
  }
}
