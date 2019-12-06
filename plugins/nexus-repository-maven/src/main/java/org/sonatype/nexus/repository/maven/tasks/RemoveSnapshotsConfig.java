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
package org.sonatype.nexus.repository.maven.tasks;

/**
 * Configuration for {@link org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsTask}.
 *
 * @since 3.0
 */
public class RemoveSnapshotsConfig
{
  private final int minimumRetained;

  private final int snapshotRetentionDays;

  private final boolean removeIfReleased;

  private final int gracePeriod;

  RemoveSnapshotsConfig(final int minimumRetained,
                        final int snapshotRetentionDays,
                        final boolean removeIfReleased,
                        final int gracePeriod)
  {
    this.minimumRetained = minimumRetained;
    this.snapshotRetentionDays = snapshotRetentionDays;
    this.removeIfReleased = removeIfReleased;
    this.gracePeriod = gracePeriod;
  }

  /**
   * The minimum number of snapshots to keep.
   */
  public int getMinimumRetained() {
    return minimumRetained;
  }

  /**
   * Snapshots older than this will be candidates for removal.
   */
  public int getSnapshotRetentionDays() {
    return snapshotRetentionDays;
  }

  /**
   * Whether or not to delete snapshots if a release version of the same artifact is available.
   */
  public boolean getRemoveIfReleased() {
    return removeIfReleased;
  }

  /**
   * An optional period to keep snapshots around, even if a release version of the same artifact is available.
   */
  public int getGracePeriod() {
    return gracePeriod;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "minimumRetained=" + minimumRetained +
        ", snapshotRetentionDays=" + snapshotRetentionDays +
        ", removeIfReleased=" + removeIfReleased +
        ", gracePeriod=" + gracePeriod +
        "}";
  }
}
