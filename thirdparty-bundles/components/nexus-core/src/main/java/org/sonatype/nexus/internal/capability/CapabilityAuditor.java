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
package org.sonatype.nexus.internal.capability;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityDescriptor;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.formfields.Encrypted;
import org.sonatype.nexus.formfields.FormField;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * {@link Capability} auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class CapabilityAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "capability";

  public CapabilityAuditor() {
    registerType(CapabilityEvent.Created.class, CREATED_TYPE);
    registerType(CapabilityEvent.AfterActivated.class, "activated");
    registerType(CapabilityEvent.BeforePassivated.class, "passivated");
    registerType(CapabilityEvent.AfterRemove.class, DELETED_TYPE);
    registerType(CapabilityEvent.AfterUpdate.class, UPDATED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final CapabilityEvent event) {
    if (isRecording()) {
      CapabilityReference reference = event.getReference();
      CapabilityContext context = reference.context();
      CapabilityDescriptor descriptor = context.descriptor();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(context.type().toString());

      Map<String, String> attributes = data.getAttributes();
      attributes.put("id", context.id().toString());
      attributes.put("type", context.type().toString());
      attributes.put("enabled", string(context.isEnabled()));
      attributes.put("active", string(context.isActive()));
      attributes.put("failed", string(context.hasFailure()));

      // include all non-secure properties
      Map<String,FormField> fields = fields(descriptor);
      for (Entry<String,String> entry : context.properties().entrySet()) {
        FormField field = fields.get(entry.getKey());
        // skip secure fields
        if (field instanceof Encrypted) {
          continue;
        }
        attributes.put("property." + entry.getKey(), entry.getValue());
      }

      record(data);
    }
  }

  private static Map<String, FormField> fields(final CapabilityDescriptor descriptor) {
    Map<String,FormField> result = new HashMap<>();
    for (FormField field : descriptor.formFields()) {
      result.put(field.getId(), field);
    }
    return result;
  }
}
