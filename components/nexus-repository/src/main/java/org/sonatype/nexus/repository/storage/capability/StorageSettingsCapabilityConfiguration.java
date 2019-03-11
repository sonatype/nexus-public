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
package org.sonatype.nexus.repository.storage.capability;

import java.util.Map;

import org.sonatype.nexus.capability.CapabilityConfigurationSupport;

import org.hibernate.validator.constraints.NotBlank;
import org.joda.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.Duration.standardHours;

/**
 * {@link StorageSettingsCapability} configuration.
 *
 * @since 3.next
 */
public class StorageSettingsCapabilityConfiguration
    extends CapabilityConfigurationSupport
{
  public static final String LAST_DOWNLOADED_INTERVAL = "lastDownloadedInterval";

  public static final Duration DEFAULT_LAST_DOWNLOADED_INTERVAL = standardHours(12);

  @NotBlank
  private String lastDownloadedInterval;

  public StorageSettingsCapabilityConfiguration(final Map<String,String> properties) {
    checkNotNull(properties);
    this.lastDownloadedInterval = properties.get(LAST_DOWNLOADED_INTERVAL);
  }

  public String getLastDownloadedInterval() {
    return lastDownloadedInterval;
  }

  public void setLastDownloadedInterval(final String lastDownloadedInterval) {
    this.lastDownloadedInterval = lastDownloadedInterval;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "lastDownloadedInterval='" + lastDownloadedInterval + '\'' +
        '}';
  }
}
