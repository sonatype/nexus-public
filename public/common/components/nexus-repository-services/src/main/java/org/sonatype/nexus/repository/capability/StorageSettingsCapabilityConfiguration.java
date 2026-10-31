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
package org.sonatype.nexus.repository.capability;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.repository.capability.GlobalRepositorySettings.DEFAULT_LAST_DOWNLOADED_INTERVAL;

/**
 * {@link StorageSettingsCapability} configuration.
 *
 * @since 3.16
 */
public class StorageSettingsCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  public static final String LAST_DOWNLOADED_INTERVAL = "lastDownloadedInterval";

  public static final int MINIMUM_LAST_DOWNLOADED_INTERVAL_HOURS = 1;

  @NotNull
  @Min(MINIMUM_LAST_DOWNLOADED_INTERVAL_HOURS)
  private Integer lastDownloadedInterval;

  public StorageSettingsCapabilityConfiguration(final Map<String, String> properties) {
    requireNonNull(properties);
    this.lastDownloadedInterval = parseInteger(
        properties.get(LAST_DOWNLOADED_INTERVAL),
        (int) DEFAULT_LAST_DOWNLOADED_INTERVAL.toHours());
  }

  public Integer getLastDownloadedInterval() {
    return lastDownloadedInterval;
  }

  public void setLastDownloadedInterval(final Integer lastDownloadedInterval) {
    this.lastDownloadedInterval = lastDownloadedInterval;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "lastDownloadedInterval=" + lastDownloadedInterval +
        '}';
  }
}
