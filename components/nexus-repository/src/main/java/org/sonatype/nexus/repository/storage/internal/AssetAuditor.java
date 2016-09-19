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
package org.sonatype.nexus.repository.storage.internal;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Repository asset auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class AssetAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "repository.asset";

  public AssetAuditor() {
    registerType(AssetCreatedEvent.class, CREATED_TYPE);
    registerType(AssetDeletedEvent.class, DELETED_TYPE);
    registerType(AssetUpdatedEvent.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    if (isEnabled() && event.isLocal()) {
      Asset asset = event.getAsset();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(asset.name());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("repository.name", event.getRepositoryName());
      attributes.put("format", asset.format());
      attributes.put("name", asset.name());

      record(data);
    }
  }
}
