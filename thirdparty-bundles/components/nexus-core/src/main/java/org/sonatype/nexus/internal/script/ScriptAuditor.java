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
package org.sonatype.nexus.internal.script;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptCreatedEvent;
import org.sonatype.nexus.script.ScriptDeletedEvent;
import org.sonatype.nexus.script.ScriptEvent;
import org.sonatype.nexus.script.ScriptUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * {@link Script} auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class ScriptAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "script";

  public ScriptAuditor() {
    registerType(ScriptCreatedEvent.class, CREATED_TYPE);
    registerType(ScriptDeletedEvent.class, DELETED_TYPE);
    registerType(ScriptUpdatedEvent.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ScriptEvent event) {
    if (isRecording()) {
      Script script = event.getScript();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(script.getName());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("name", script.getName());
      attributes.put("type", script.getType());

      record(data);
    }
  }
}
