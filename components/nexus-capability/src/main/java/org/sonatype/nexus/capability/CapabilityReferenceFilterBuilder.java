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
package org.sonatype.nexus.capability;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Predicate;

public class CapabilityReferenceFilterBuilder
{

  public static CapabilityReferenceFilter capabilities() {
    return new CapabilityReferenceFilter();
  }

  public static class CapabilityReferenceFilter
      implements Predicate<CapabilityReference>
  {

    private String typeId;

    private Boolean enabled;

    private Boolean active;

    private Boolean includeNotExposed = false;

    private Map<String, String> properties = new HashMap<String, String>();

    public CapabilityReferenceFilter withType(final CapabilityType type) {
      typeId = type.toString();
      return this;
    }

    public CapabilityReferenceFilter enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public CapabilityReferenceFilter enabled() {
      enabled = true;
      return this;
    }

    public CapabilityReferenceFilter disabled() {
      enabled = false;
      return this;
    }

    public CapabilityReferenceFilter active(boolean active) {
      this.active = active;
      return this;
    }

    public CapabilityReferenceFilter active() {
      active = true;
      return this;
    }

    public CapabilityReferenceFilter passive() {
      active = false;
      return this;
    }

    public CapabilityReferenceFilter withBoundedProperty(final String key) {
      properties.put(key, null);
      return this;
    }

    public CapabilityReferenceFilter withProperty(final String key, final String value) {
      properties.put(key, value);
      return this;
    }

    public CapabilityReferenceFilter onRepository(final String key, final String repositoryId) {
      return withProperty(key, repositoryId);
    }

    public CapabilityReferenceFilter includeNotExposed() {
      includeNotExposed = true;
      return this;
    }

    @Override
    public boolean apply(final CapabilityReference input) {
      if (input == null) {
        return false;
      }
      if (input.context().descriptor() != null
          && !input.context().descriptor().isExposed() && !includeNotExposed) {
        return false;
      }
      if (typeId != null && !typeId.equals(input.context().type().toString())) {
        return false;
      }
      if (enabled != null && !enabled.equals(input.context().isEnabled())) {
        return false;
      }
      if (active != null && !active.equals(input.context().isActive())) {
        return false;
      }
      if (properties != null && !properties.isEmpty()) {
        final Map<String, String> inputPropertiesMap = input.context().properties();
        if (inputPropertiesMap == null || inputPropertiesMap.isEmpty()) {
          return false;
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
          final String key = entry.getKey();
          if (key == null) {
            return false;
          }
          final String value = entry.getValue();
          if (value == null) {
            if (!inputPropertiesMap.containsKey(key)) {
              return false;
            }
          }
          else {
            final String inputValue = inputPropertiesMap.get(key);
            if (inputValue == null || !value.equals(inputValue)) {
              return false;
            }
          }
        }
      }
      return true;
    }

  }

}
