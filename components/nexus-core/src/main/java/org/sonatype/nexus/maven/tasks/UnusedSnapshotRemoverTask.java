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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.maven.tasks.descriptors.UnusedSnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.maven.tasks.descriptors.UnusedSnapshotRemovalTaskDescriptor.DAYS_SINCE_LAST_REQUESTED_FIELD_ID;
import static org.sonatype.nexus.maven.tasks.descriptors.UnusedSnapshotRemovalTaskDescriptor.REPO_OR_GROUP_FIELD_ID;

/**
 * Unused Snapshot Remover Task.
 *
 * @since 2.7.0
 */
@Named(UnusedSnapshotRemovalTaskDescriptor.ID)
public class UnusedSnapshotRemoverTask
    extends AbstractNexusRepositoriesTask<SnapshotRemovalResult>
{

  public static final String SYSTEM_REMOVE_SNAPSHOTS_ACTION = "REMOVESNAPSHOTS";

  private final SnapshotRemover snapshotRemover;

  @Inject
  public UnusedSnapshotRemoverTask(final SnapshotRemover snapshotRemover) {
    this.snapshotRemover = checkNotNull(snapshotRemover);
  }

  @Override
  protected String getRepositoryFieldId() {
    return REPO_OR_GROUP_FIELD_ID;
  }

  public int getDaysSinceLastRequested() {
    final String param = getParameters().get(DAYS_SINCE_LAST_REQUESTED_FIELD_ID);
    return Integer.parseInt(checkNotNull(param, DAYS_SINCE_LAST_REQUESTED_FIELD_ID));
  }

  @Override
  public SnapshotRemovalResult doRun()
      throws Exception
  {
    final SnapshotRemovalRequest req = new SnapshotRemovalRequest(
        getRepositoryId(),
        -1,                             // not applicable (minCountOfSnapshotsToKeep)
        getDaysSinceLastRequested(),
        false,                          // do not remove if release available
        -1,                             // not applicable (graceDaysAfterRelease)
        false,                          // do not delete immediately (will move to trash),
        true                            // calculate number of days based on last time snapshot was requested
    );

    return snapshotRemover.removeSnapshots(req);
  }

  @Override
  protected String getAction() {
    return SYSTEM_REMOVE_SNAPSHOTS_ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Removing unused snapshots from repository " + getRepositoryName();
    }
    else {
      return "Removing unused snapshots from all registered repositories";
    }
  }

}
