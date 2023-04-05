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
package org.sonatype.nexus.capability.internal;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityReference;

import static com.google.common.base.Preconditions.checkNotNull;

public class CapabilityDTO
{
  private String id;

  private String type;

  private String notes;

  private boolean enabled;

  private Map<String, String> properties;

  protected CapabilityDTO() {
    // deserialization
  }

  public CapabilityDTO(final CapabilityReference reference) {
    checkNotNull(reference);
    CapabilityContext context = checkNotNull(reference.context());

    id = context.id().toString();
    type = context.type().toString();
    enabled = context.isEnabled();
    notes = context.notes();
    properties = CapabilityResource.filterProperties(context.properties(), reference.capability());
  }

  public String getId() {
    return id;
  }

  public String getNotes() {
    return notes;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public String getType() {
    return type;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  public void setProperties(final Map<String, String> properties) {
    this.properties = properties;
  }

  public void setType(final String type) {
    this.type = type;
  }
}
