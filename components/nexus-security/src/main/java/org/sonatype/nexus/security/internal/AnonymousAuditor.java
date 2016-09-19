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
package org.sonatype.nexus.security.internal;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousConfigurationChangedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Anonymous auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class AnonymousAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "security.anonymous";

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AnonymousConfigurationChangedEvent event) {
    if (isEnabled()) {
      AnonymousConfiguration configuration = event.getConfiguration();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(CHANGED_TYPE);
      data.setContext(SYSTEM_CONTEXT);

      Map<String, String> attributes = data.getAttributes();
      attributes.put("enabled", string(configuration.isEnabled()));
      attributes.put("userId", configuration.getUserId());
      attributes.put("realm", configuration.getRealmName());

      record(data);
    }
  }
}
