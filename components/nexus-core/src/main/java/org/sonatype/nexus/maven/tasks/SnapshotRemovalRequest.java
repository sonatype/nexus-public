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

import java.util.HashSet;
import java.util.Set;

public class SnapshotRemovalRequest
{
  private final String repositoryId;

  private final int minCountOfSnapshotsToKeep;

  private final int removeSnapshotsOlderThanDays;

  private final boolean removeIfReleaseExists;

  private final int graceDaysAfterRelease;

  private final Set<String> processedRepos;

  private final boolean deleteImmediately;

  private final boolean useLastRequestedTimestamp;

  /**
   * Old behavior without changing trash or delete (always trash).
   * <p/>
   * (see NEXUS-4579)
   */
  public SnapshotRemovalRequest(String repositoryId, int minCountOfSnapshotsToKeep,
                                int removeSnapshotsOlderThanDays, boolean removeIfReleaseExists)
  {

    this(repositoryId, minCountOfSnapshotsToKeep, removeSnapshotsOlderThanDays,
        removeIfReleaseExists, 0, false, false);
  }

  public SnapshotRemovalRequest(String repositoryId, int minCountOfSnapshotsToKeep,
                                int removeSnapshotsOlderThanDays, boolean removeIfReleaseExists,
                                int graceDaysAfterRelease, boolean deleteImmediately)
  {
    this(repositoryId, minCountOfSnapshotsToKeep, removeSnapshotsOlderThanDays,
        removeIfReleaseExists, graceDaysAfterRelease, deleteImmediately, false);
  }

  /**
   * @since 2.7.0
   */
  public SnapshotRemovalRequest(String repositoryId, int minCountOfSnapshotsToKeep,
                                int removeSnapshotsOlderThanDays, boolean removeIfReleaseExists,
                                int graceDaysAfterRelease, boolean deleteImmediately,
                                boolean useLastRequestedTimestamp)
  {
    this.repositoryId = repositoryId;

    this.minCountOfSnapshotsToKeep = minCountOfSnapshotsToKeep;

    this.removeSnapshotsOlderThanDays = removeSnapshotsOlderThanDays;

    this.removeIfReleaseExists = removeIfReleaseExists;

    this.graceDaysAfterRelease = graceDaysAfterRelease;

    this.processedRepos = new HashSet<String>();

    this.deleteImmediately = deleteImmediately;

    this.useLastRequestedTimestamp = useLastRequestedTimestamp;

  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public int getMinCountOfSnapshotsToKeep() {
    return minCountOfSnapshotsToKeep;
  }

  public int getRemoveSnapshotsOlderThanDays() {
    return removeSnapshotsOlderThanDays;
  }

  public boolean isRemoveIfReleaseExists() {
    return removeIfReleaseExists;
  }

  public int getGraceDaysAfterRelease() {
    return graceDaysAfterRelease;
  }

  public void addProcessedRepo(String repoId) {
    this.processedRepos.add(repoId);
  }

  public boolean isProcessedRepo(String repoId) {
    return this.processedRepos.contains(repoId);
  }

  public boolean isDeleteImmediately() {
    return deleteImmediately;
  }

  public boolean shouldUseLastRequestedTimestamp() {
    return useLastRequestedTimestamp;
  }

}
