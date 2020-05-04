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

import java.time.Duration;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilitySupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Long.parseLong;
import static java.time.Duration.ofHours;
import static org.sonatype.nexus.repository.capability.GlobalRepositorySettings.DEFAULT_LAST_DOWNLOADED_INTERVAL;

/**
 * Storage settings capability.
 *
 * @since 3.16
 */
@Named(StorageSettingsCapabilityDescriptor.TYPE_ID)
public class StorageSettingsCapability
    extends CapabilitySupport<StorageSettingsCapabilityConfiguration>
{
  private final GlobalRepositorySettings globalSettings;

  @Inject
  public StorageSettingsCapability(final GlobalRepositorySettings globalSettings) {
    this.globalSettings = checkNotNull(globalSettings);
  }

  @Override
  protected StorageSettingsCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new StorageSettingsCapabilityConfiguration(properties);
  }

  @Override
  protected void onUpdate(final StorageSettingsCapabilityConfiguration config) throws Exception {
    if (context().isActive()) {
      configureDownloadedInterval(config);
    }
  }

  @Override
  protected void onActivate(final StorageSettingsCapabilityConfiguration config) throws Exception {
    configureDownloadedInterval(config);
  }

  @Override
  protected void onPassivate(final StorageSettingsCapabilityConfiguration config) throws Exception {
    resetDownloadedInterval();
  }

  private void configureDownloadedInterval(final StorageSettingsCapabilityConfiguration config) {
    log.info("Using configured value of {} hours for LastDownloaded interval", config.getLastDownloadedInterval());
    globalSettings.setLastDownloadedInterval(parseAsHours(config.getLastDownloadedInterval()));
  }

  private void resetDownloadedInterval() {
    log.info("Reverting back to {} hours for LastDownloaded interval",
        DEFAULT_LAST_DOWNLOADED_INTERVAL.toHours());
    globalSettings.setLastDownloadedInterval(DEFAULT_LAST_DOWNLOADED_INTERVAL);
  }

  private Duration parseAsHours(final String hours) {
    return ofHours(parseLong(hours));
  }
}
