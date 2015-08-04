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
package org.sonatype.nexus.rest;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to build {@link PathProtectionDescriptor} instances.
 *
 * @since 2.2
 */
public class PathProtectionDescriptorBuilder
{

  public static final String ANON = "anon";

  public static final String AUTHC = "authc";

  public static final String AUTHC_BASIC = "authcBasic";

  public static final String PERMS = "perms";

  public static final String ROLES = "roles";

  private String path;

  private Map<String, String> filters;

  public PathProtectionDescriptorBuilder path(final String value) {
    this.path = checkNotNull(value);
    return this;
  }

  public PathProtectionDescriptorBuilder path(final PlexusResource resource) {
    checkNotNull(resource);
    return path(resource.getResourceUri());
  }

  public PathProtectionDescriptorBuilder filter(final String name, final String config) {
    checkNotNull(name);
    if (filters == null) {
      filters = Maps.newLinkedHashMap();
    }
    filters.put(name, config);
    return this;
  }

  public PathProtectionDescriptorBuilder filter(final String name) {
    return filter(name, null);
  }

  public PathProtectionDescriptorBuilder anon() {
    return filter(ANON);
  }

  public PathProtectionDescriptorBuilder authc() {
    return filter(AUTHC);
  }

  public PathProtectionDescriptorBuilder authcBasic() {
    return filter(AUTHC_BASIC);
  }

  public PathProtectionDescriptorBuilder perms(final String... names) {
    return filter(PERMS, join(names));
  }

  public PathProtectionDescriptorBuilder roles(final String... names) {
    return filter(ROLES, join(names));
  }

  private String join(final String[] values) {
    checkNotNull(values);
    checkArgument(values.length != 0);
    StringBuilder buff = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      buff.append(values[i]);
      if (i + 1 < values.length) {
        buff.append(",");
      }
    }
    return buff.toString();
  }

  public PathProtectionDescriptor build() {
    return new PathProtectionDescriptor(buildPathExpression(), buildFilterExpression());
  }

  private String buildPathExpression() {
    checkState(path != null, "Missing path");
    return path;
  }

  private String buildFilterExpression() {
    if (filters == null) {
      return null;
    }

    StringBuilder buff = new StringBuilder();
    Iterator<Entry<String, String>> iter = filters.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, String> entry = iter.next();
      String name = entry.getKey();
      String config = entry.getValue();
      buff.append(name);
      if (config != null) {
        buff.append("[").append(config).append("]");
      }
      if (iter.hasNext()) {
        buff.append(",");
      }
    }
    return buff.toString();
  }

}
