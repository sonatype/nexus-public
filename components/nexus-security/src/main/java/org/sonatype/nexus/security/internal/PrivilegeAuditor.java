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
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeCreatedEvent;
import org.sonatype.nexus.security.privilege.PrivilegeDeletedEvent;
import org.sonatype.nexus.security.privilege.PrivilegeEvent;
import org.sonatype.nexus.security.privilege.PrivilegeUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * {@link Privilege} auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class PrivilegeAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "security.privilege";

  public PrivilegeAuditor() {
    registerType(PrivilegeCreatedEvent.class, CREATED_TYPE);
    registerType(PrivilegeDeletedEvent.class, DELETED_TYPE);
    registerType(PrivilegeUpdatedEvent.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final PrivilegeEvent event) {
    if (isEnabled()) {
      Privilege privilege = event.getPrivilege();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(privilege.getId());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("id", privilege.getId());
      attributes.put("name", privilege.getName());
      attributes.put("type", privilege.getType());

      for (Entry<String,String> entry : privilege.getProperties().entrySet()) {
        attributes.put("property." + entry.getKey(), entry.getValue());
      }

      record(data);
    }
  }
}
