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
package org.sonatype.nexus.capabilities.client;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Capabilities filter.
 *
 * @since capabilities 2.2
 */
public class Filter
{

  private static final String $TYPE = "$type";

  private static final String $PROPERTY = "$p";

  private static final String $ENABLED = "$enabled";

  private static final String $ACTIVE = "$active";

  private static final String $INCLUDE_NOT_EXPOSED = "$includeNotExposed";

  /**
   * Builder method.
   *
   * @return a new filter (never null)
   */
  public static Filter capabilitiesThat() {
    return new Filter();
  }

  private String typeId;

  private Boolean enabled;

  private Boolean active;

  private Boolean includeNotExposed;

  private Map<String, String> properties = new HashMap<String, String>();

  public Filter haveType(final String type) {
    typeId = type;
    return this;
  }

  public Filter areEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public Filter areEnabled() {
    enabled = true;
    return this;
  }

  public Filter areDisabled() {
    enabled = false;
    return this;
  }

  public Filter areActive(boolean active) {
    this.active = active;
    return this;
  }

  public Filter areActive() {
    active = true;
    return this;
  }

  public Filter arePassive() {
    active = false;
    return this;
  }

  public Filter includingNotExposed() {
    includeNotExposed = true;
    return this;
  }

  public Filter haveBoundedProperty(final String key) {
    properties.put(key, null);
    return this;
  }

  public Filter haveProperty(final String key, final String value) {
    properties.put(key, value);
    return this;
  }

  public MultivaluedMap<String, String> toQueryMap() {
    final MultivaluedMap<String, String> queryMap = new MultivaluedMapImpl();
    if (typeId != null) {
      queryMap.putSingle($TYPE, typeId);
    }
    if (enabled != null) {
      queryMap.putSingle($ENABLED, enabled.toString());
    }
    if (active != null) {
      queryMap.putSingle($ACTIVE, active.toString());
    }
    if (includeNotExposed != null) {
      queryMap.putSingle($INCLUDE_NOT_EXPOSED, includeNotExposed.toString());
    }
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      queryMap.add($PROPERTY, entry.getKey() + ":" + (entry.getValue() == null ? "*" : entry.getValue()));
    }
    return queryMap;
  }

}
