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
package org.sonatype.nexus.self.hosted.internal.capability.proxy;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

/**
 * Configuration for ForwardedRequestCapability.
 */
public class ForwardedRequestCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  private static final String ENABLED = "enabled";

  private final boolean enabled;

  public ForwardedRequestCapabilityConfiguration(final Map<String, String> properties) {
    this.enabled = parseBoolean(properties.get(ENABLED), true);
  }

  public boolean isEnabled() {
    return enabled;
  }
}
