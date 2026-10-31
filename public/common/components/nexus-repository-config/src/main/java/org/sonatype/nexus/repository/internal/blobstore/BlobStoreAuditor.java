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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.Map;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreCreatedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreDeletedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreStartedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreStoppedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreUpdatedEvent;
import org.sonatype.nexus.common.event.EventAware;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

/**
 * {@link BlobStore} auditor.
 *
 * @since 3.1
 */
@Component
public class BlobStoreAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "blobstore";

  public BlobStoreAuditor() {
    registerType(BlobStoreCreatedEvent.class, CREATED_TYPE);
    registerType(BlobStoreUpdatedEvent.class, UPDATED_TYPE);
    registerType(BlobStoreDeletedEvent.class, DELETED_TYPE);
    registerType(BlobStoreStartedEvent.class, "started");
    registerType(BlobStoreStoppedEvent.class, "stopped");
  }

  private static final String PRE_SIGNED_URL_ENABLED = "preSignedUrlEnabled";

  @Subscribe
  @AllowConcurrentEvents
  public void on(final BlobStoreEvent event) {
    if (isRecording()) {
      BlobStore blobStore = event.getBlobStore();
      BlobStoreConfiguration configuration = blobStore.getBlobStoreConfiguration();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(configuration.getName());

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("name", configuration.getName());
      attributes.put("type", configuration.getType());

      Boolean preSignedUrlEnabled = detectPreSignedUrlEnabled(configuration);
      if (preSignedUrlEnabled != null) {
        attributes.put(PRE_SIGNED_URL_ENABLED, preSignedUrlEnabled);
      }

      record(data);
    }
  }

  private static Boolean detectPreSignedUrlEnabled(final BlobStoreConfiguration configuration) {
    Map<String, Map<String, Object>> outer = configuration.getAttributes();
    if (outer == null) {
      return null;
    }
    for (Map<String, Object> sub : outer.values()) {
      if (sub != null && sub.containsKey(PRE_SIGNED_URL_ENABLED)) {
        Object value = sub.get(PRE_SIGNED_URL_ENABLED);
        if (value instanceof Boolean bool) {
          return bool;
        }
        if (value != null) {
          return Boolean.parseBoolean(value.toString());
        }
      }
    }
    return null;
  }
}
