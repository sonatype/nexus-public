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
package org.sonatype.nexus.maven.tasks;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;

public interface SnapshotRemover
{
  /**
   * A flag to mark that -- even if we are doing something (eg. deleting, see NEXUS-814) within this GAV -- this GAV
   * is contained multiple times in Repository and this is not the last one, there are more instances (eg. more
   * snapshot builds) for this GAV still left in Repository.
   */
  String MORE_TS_SNAPSHOTS_EXISTS_FOR_GAV = "moreTsSnapshotsExistsForGav";

  SnapshotRemovalResult removeSnapshots(SnapshotRemovalRequest request)
      throws NoSuchRepositoryException, IllegalArgumentException;
}
