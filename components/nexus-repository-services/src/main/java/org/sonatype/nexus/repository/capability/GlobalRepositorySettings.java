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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

/**
 * Global repository settings.
 *
 * @since 3.24
 */
@Named
@Singleton
public class GlobalRepositorySettings
    extends ComponentSupport
{
  public static final Duration DEFAULT_LAST_DOWNLOADED_INTERVAL = Duration.ofHours(12);

  private Duration lastDownloadedInterval = DEFAULT_LAST_DOWNLOADED_INTERVAL;

  public void setLastDownloadedInterval(final Duration lastDownloadedInterval) {
    if (lastDownloadedInterval.toHours() < 1) {
      log.warn("A lastDownloaded interval of {} seconds has been configured, a value less than"
          + " 1 hour is not recommended for performance reasons", lastDownloadedInterval.getSeconds());
    }
    this.lastDownloadedInterval = lastDownloadedInterval;
  }

  public Duration getLastDownloadedInterval() {
    return lastDownloadedInterval;
  }
}
