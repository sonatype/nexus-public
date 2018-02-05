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
package org.sonatype.nexus.internal.email;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailConfigurationChangedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Email auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class EmailAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "email";

  @Subscribe
  @AllowConcurrentEvents
  public void on(final EmailConfigurationChangedEvent event) {
    if (isRecording()) {
      EmailConfiguration configuration = event.getConfiguration();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(CHANGED_TYPE);
      data.setContext(SYSTEM_CONTEXT);

      Map<String, String> attributes = data.getAttributes();
      attributes.put("enabled", string(configuration.isEnabled()));
      attributes.put("host", configuration.getHost());
      attributes.put("port", string(configuration.getPort()));
      attributes.put("username", configuration.getUsername());
      attributes.put("fromAddress", configuration.getFromAddress());
      attributes.put("subjectPrefix", configuration.getSubjectPrefix());

      // TODO: various ssl/tls/trust-store shit

      record(data);
    }
  }
}
