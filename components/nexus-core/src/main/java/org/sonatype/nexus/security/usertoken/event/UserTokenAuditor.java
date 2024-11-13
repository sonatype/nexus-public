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
package org.sonatype.nexus.security.usertoken.event;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * UserToken auditor.
 *
 */
@Named
@Singleton
public class UserTokenAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "userToken";

  @Inject
  public UserTokenAuditor()
  {
    registerType(UserTokenEvent.class, CREATED_TYPE);
    registerType(UserTokenDeletedEvent.class, DELETED_TYPE);
    registerType(UserTokenPurgedEvent.class, PURGE_TYPE);
    registerType(UserTokenConfigChangedEvent.class, "configChanged");
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final UserTokenEvent event) {
    if (isRecording()) {
      AuditData data = getAuditData(event.getEventType().toString());
      if (event instanceof UserTokenConfigChangedEvent) {
        Map<String, Object> attributes = data.getAttributes();
        UserTokenConfigChangedEvent configChangedEvent = (UserTokenConfigChangedEvent) event;
        attributes.put("Enabled", configChangedEvent.isEnabled());
        attributes.put("Protect", configChangedEvent.isProtectContent());
        if (configChangedEvent.isExpirationEnabled()) {
          attributes.put("Expiration Days", configChangedEvent.getExpirationDays());
        }
      }
      else if (event instanceof UserTokenDeletedEvent) {
        UserTokenDeletedEvent deletedEvent = (UserTokenDeletedEvent) event;
        int deleted = deletedEvent.getDeleted();
        String username = deletedEvent.getUsername();
        Map<String, Object> attributes = data.getAttributes();
        attributes.put("Deleted", deleted);
        if (null != username) {
          attributes.put("username", username);
        }
      }
      data.setType(type(event.getClass()));

      record(data);
    }
  }

  private AuditData getAuditData(final String eventType) {
    AuditData data = new AuditData();
    data.setDomain(DOMAIN);
    data.setContext(eventType);
    return data;
  }

}
