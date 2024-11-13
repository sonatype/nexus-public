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
package org.sonatype.nexus.internal.log;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.freeze.event.FreezeForceReleaseEvent;
import org.sonatype.nexus.freeze.event.FreezeEvent;
import org.sonatype.nexus.freeze.event.FreezeRequestEvent;
import org.sonatype.nexus.freeze.event.FreezeReleaseEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Named
@Singleton
public class FreezeEventAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "freeze";

  public FreezeEventAuditor() {
    registerType(FreezeRequestEvent.class, "freeze");
    registerType(FreezeReleaseEvent.class, "release");
    registerType(FreezeForceReleaseEvent.class, "forceRelease");
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final FreezeEvent event) {
    if (isRecording()) {

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      if (event instanceof FreezeRequestEvent) {
        data.setContext(((FreezeRequestEvent) event).getReason());
      }

      record(data);
    }
  }
}
