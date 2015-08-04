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
package org.sonatype.timeline;

import java.io.File;

public class TimelineConfiguration
{
  /**
   * Count of days to restore on repairing timeline index from persistor. The default is 30 days.
   */
  public static final int DEFAULT_REPAIR_DAYS_TO_RESTORE = 30;

  /**
   * Interval for how long to roll records into one file. Default rolling interval is one day. If negative, each
   * record gets into it's own file!
   */
  public static final int DEFAULT_ROLLING_INTERVAL_MILLIS = 60 * 60 * 24;

  // ==

  private final File persistDirectory;

  private final File indexDirectory;

  private final int persistRollingIntervalMillis;

  private final int repairDaysCountRestored;

  public TimelineConfiguration(final File persistDirectory, final File indexDirectory,
                               final int persistRollingIntervalMillis, final int repairDaysCountRestored)
  {
    this.persistDirectory = persistDirectory;
    this.indexDirectory = indexDirectory;
    this.persistRollingIntervalMillis = persistRollingIntervalMillis;
    this.repairDaysCountRestored = repairDaysCountRestored;
  }

  public TimelineConfiguration(final File persistDirectory, final File indexDirectory) {
    this(persistDirectory, indexDirectory, DEFAULT_ROLLING_INTERVAL_MILLIS, DEFAULT_REPAIR_DAYS_TO_RESTORE);
  }

  public TimelineConfiguration(final File baseDir) {
    this(new File(baseDir, "persist"), new File(baseDir, "index"), DEFAULT_ROLLING_INTERVAL_MILLIS,
        DEFAULT_REPAIR_DAYS_TO_RESTORE);
  }

  public File getPersistDirectory() {
    return persistDirectory;
  }

  public File getIndexDirectory() {
    return indexDirectory;
  }

  public int getPersistRollingIntervalMillis() {
    return persistRollingIntervalMillis;
  }

  public int getRepairDaysCountRestored() {
    return repairDaysCountRestored;
  }
}
