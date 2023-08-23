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
package org.sonatype.nexus.cleanup.content;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.event.EventAware;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * {@link CleanupPolicy} auditor.
 *
 */
@Named
@Singleton
public class CleanupPolicyAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "cleanupPolicy";

  public CleanupPolicyAuditor() {
    registerType(CleanupPolicyCreatedEvent.class, CREATED_TYPE);
    registerType(CleanupPolicyUpdatedEvent.class, UPDATED_TYPE);
    registerType(CleanupPolicyDeletedEvent.class, DELETED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final CleanupPolicyEvent event) {
    if (isRecording()) {
      CleanupPolicy cleanupPolicy = event.getCleanupPolicy();

      AuditData data = getPolicyAuditData(event, cleanupPolicy);
      record(data);
    }
  }

  private AuditData getPolicyAuditData(final CleanupPolicyEvent event, final CleanupPolicy cleanupPolicy) {
    AuditData data = new AuditData();
    data.setDomain(DOMAIN);
    data.setType(type(event.getClass()));
    data.setContext(cleanupPolicy.getName());

    Map<String, Object> attributes = data.getAttributes();
    attributes.put("format", cleanupPolicy.getFormat());
    attributes.put("description", cleanupPolicy.getNotes());
    attributes.put("criteria", cleanupPolicy.getCriteria());
    return data;
  }
}
