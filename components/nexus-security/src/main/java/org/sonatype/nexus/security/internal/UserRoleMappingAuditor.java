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
import org.sonatype.nexus.security.user.UserRoleMappingCreatedEvent;
import org.sonatype.nexus.security.user.UserRoleMappingDeletedEvent;
import org.sonatype.nexus.security.user.UserRoleMappingEvent;
import org.sonatype.nexus.security.user.UserRoleMappingUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * User role-mapping auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class UserRoleMappingAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "security.user-role-mapping";

  public UserRoleMappingAuditor() {
    registerType(UserRoleMappingCreatedEvent.class, CREATED_TYPE);
    registerType(UserRoleMappingDeletedEvent.class, DELETED_TYPE);
    registerType(UserRoleMappingUpdatedEvent.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final UserRoleMappingEvent event) {
    if (isEnabled()) {
      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(event.getUserId());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("id", event.getUserId());
      attributes.put("source", event.getUserSource());
      attributes.put("roles", string(event.getRoles()));

      record(data);
    }
  }
}
