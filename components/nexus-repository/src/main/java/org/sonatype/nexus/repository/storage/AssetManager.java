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
package org.sonatype.nexus.repository.storage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.Duration;

import static org.sonatype.nexus.repository.storage.capability.StorageSettingsCapabilityConfiguration.DEFAULT_LAST_DOWNLOADED_INTERVAL;

/**
 * Responsible for altering the runtime behaviour of assets
 *
 * @since 3.16
 */
@Named
@Singleton
public class AssetManager
    extends ComponentSupport
{
  private Duration lastDownloadedInterval;

  @Inject
  public AssetManager() {
    this.lastDownloadedInterval = DEFAULT_LAST_DOWNLOADED_INTERVAL;
  }

  public void setLastDownloadedInterval(final Duration lastDownloadedInterval) {
    if (lastDownloadedInterval.getStandardHours() < 1) {
      log.warn("A lastDownloaded interval of {} seconds has been configured, a value less than"
          + " 1 hour is not recommended for performance reasons", lastDownloadedInterval.getStandardSeconds());
    }
    this.lastDownloadedInterval = lastDownloadedInterval;
  }

  @VisibleForTesting
  public Duration getLastDownloadedInterval() {
    return lastDownloadedInterval;
  }

  public boolean maybeUpdateLastDownloaded(final Asset asset) {
    return asset.markAsDownloaded(lastDownloadedInterval);
  }
}
