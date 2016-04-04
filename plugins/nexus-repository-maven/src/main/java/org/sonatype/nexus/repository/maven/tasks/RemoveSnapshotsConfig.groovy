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
package org.sonatype.nexus.repository.maven.tasks

import groovy.transform.Immutable
import groovy.transform.ToString;

/**
 * Configuration for {@link org.sonatype.nexus.repository.maven.tasks.RemoveSnapshotsTask}
 * @since 3.0
 */
@Immutable
@ToString
class RemoveSnapshotsConfig
{
  /**
   * The minimum number of snapshots to keep.
   */
  int minimumRetained;

  /**
   * Snapshots older than this will be candidates for removal.
   */
  int snapshotRetentionDays;

  /**
   * Whether or not to delete snapshots if a release version of the same artifact is available.
   */
  boolean removeIfReleased;

  /**
   * An optional period to keep snapshots around, even if a release version of the same artifact is available.
   */
  int gracePeriod;
}
