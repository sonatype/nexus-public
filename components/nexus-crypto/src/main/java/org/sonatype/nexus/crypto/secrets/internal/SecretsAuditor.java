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
package org.sonatype.nexus.crypto.secrets.internal;

import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.crypto.secrets.ActiveKeyChangeEvent;

import com.google.common.eventbus.Subscribe;

@Named
@Singleton
public class SecretsAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "security.secrets";

  @Subscribe
  public void on(final ActiveKeyChangeEvent event) {
    if (isRecording() && !EventHelper.isReplicating()) {
      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(CHANGED_TYPE);
      data.setContext(SYSTEM_CONTEXT);

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("newKeyId", event.getNewKeyId());
      attributes.put("previousKeyId", event.getPreviousKeyId());
      attributes.put("userId", event.getUserId());
      record(data);
    }
  }
}
