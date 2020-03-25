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
package org.sonatype.nexus.plugins.defaultrole.internal;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

/**
 * Simple configuration for {@link DefaultRoleCapability} containing a single roleId.
 *
 * @since 3.22
 */
public class DefaultRoleCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  public static final String P_ROLE = "role";

  private String role;

  public DefaultRoleCapabilityConfiguration(final Map<String, String> properties) {
    role = properties.get(P_ROLE);
  }

  public String getRole() {
    return role;
  }

  public void setRole(final String role) {
    this.role = role;
  }
}
