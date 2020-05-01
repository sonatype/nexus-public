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
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.time.DateHelper.toJavaDuration;
import static org.sonatype.nexus.common.time.DateHelper.toJodaDuration;

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
  public static final Duration DEFAULT_LAST_DOWNLOADED_INTERVAL =
      toJodaDuration(GlobalRepositorySettings.DEFAULT_LAST_DOWNLOADED_INTERVAL);

  private final GlobalRepositorySettings globalSettings;

  @Inject
  public AssetManager(final GlobalRepositorySettings globalSettings) {
    this.globalSettings = checkNotNull(globalSettings);
  }

  public void setLastDownloadedInterval(final Duration lastDownloadedInterval) {
    globalSettings.setLastDownloadedInterval(toJavaDuration(lastDownloadedInterval));
  }

  @VisibleForTesting
  public Duration getLastDownloadedInterval() {
    return toJodaDuration(globalSettings.getLastDownloadedInterval());
  }

  public boolean maybeUpdateLastDownloaded(final Asset asset) {
    return asset.markAsDownloaded(getLastDownloadedInterval());
  }
}
