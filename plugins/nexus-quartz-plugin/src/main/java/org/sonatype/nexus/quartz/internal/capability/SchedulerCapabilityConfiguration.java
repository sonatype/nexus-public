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
package org.sonatype.nexus.quartz.internal.capability;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;
import org.sonatype.nexus.capability.UniquePerCapabilityType;
import org.sonatype.nexus.validation.group.Create;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link SchedulerCapability} configuration.
 *
 * @since 3.0
 */
@UniquePerCapabilityType(value = SchedulerCapabilityDescriptor.TYPE_ID, groups = Create.class)
public class SchedulerCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  public static final String ACTIVE = "active";

  public static final boolean DEFAULT_ACTIVE = true;

  private boolean active;

  public SchedulerCapabilityConfiguration(final Map<String, String> properties) {
    checkNotNull(properties);

    this.active = Boolean.valueOf(properties.get(ACTIVE));
  }

  public boolean isActive() { return active; }

  public Map<String, String> asMap() {
    Map<String, String> props = Maps.newHashMap();
    props.put(ACTIVE, String.valueOf(active));
    return props;
  }

  @Override
  public String toString() {
    return "SchedulerCapabilityConfiguration{" +
        "active='" + active + '\'' +
        '}';
  }
}
