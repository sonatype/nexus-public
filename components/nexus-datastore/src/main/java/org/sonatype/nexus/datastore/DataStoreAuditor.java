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
package org.sonatype.nexus.datastore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.distributed.event.service.api.common.DataStoreConfigurationEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Named
@Singleton
public class DataStoreAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "DataStore";

  public DataStoreAuditor() {
    registerType(DataStoreConfigurationEvent.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final DataStoreConfigurationEvent event) {
    if (isRecording()) {
      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(event.getConfigurationName());

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("type", event.getType());
      attributes.put("source", event.getSource());

      Map<String, String> eventAttributes = new HashMap<>(event.getAttributes());
      eventAttributes.replace("password", DataStoreConfiguration.REDACTED);
      attributes.put("attributes", eventAttributes);

      record(data);
    }
  }
}
