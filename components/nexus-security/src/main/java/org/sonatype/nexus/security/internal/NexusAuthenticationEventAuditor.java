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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.authc.AuthenticationFailureReason;
import org.sonatype.nexus.security.authc.NexusAuthenticationEvent;

import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Writes to the audit log for fired {@link NexusAuthenticationEvent}
 *
 * @since 3.22
 */
@Named
@Singleton
public class NexusAuthenticationEventAuditor
    extends AuditorSupport
    implements EventAware
{
  private static final String DOMAIN = "security.user";

  /* for now only log a subset of failure reasons */
  private static Set<AuthenticationFailureReason> AUDITABLE_FAILURE_REASONS = new HashSet<>();

  static {
    AUDITABLE_FAILURE_REASONS.add(AuthenticationFailureReason.INCORRECT_CREDENTIALS);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final NexusAuthenticationEvent event) {
    Set<AuthenticationFailureReason> failureReasonsToLog = getFailureReasonsToLog(event);

    if (isRecording() && !failureReasonsToLog.isEmpty()) {
      AuditData auditData = new AuditData();

      auditData.setType("authentication");
      auditData.setDomain(DOMAIN);
      auditData.setTimestamp(event.getEventDate());

      Map<String, Object> attributes = auditData.getAttributes();
      attributes.put("failureReasons", failureReasonsToLog);
      attributes.put("wasSuccessful", event.isSuccessful());

      if (event.getClientInfo() != null) {
        ClientInfo clientInfo = event.getClientInfo();
        attributes.put("userId", clientInfo.getUserid());
        attributes.put("remoteIp", clientInfo.getRemoteIP());
        attributes.put("userAgent", clientInfo.getUserAgent());
        attributes.put("path", clientInfo.getPath());
      }

      record(auditData);
    }
  }

  private Set<AuthenticationFailureReason> getFailureReasonsToLog(NexusAuthenticationEvent event) {
    return Sets.intersection(event.getAuthenticationFailureReasons(), AUDITABLE_FAILURE_REASONS);
  }
}
