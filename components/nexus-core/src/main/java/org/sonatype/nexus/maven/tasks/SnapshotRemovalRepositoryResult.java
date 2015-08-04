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

public class SnapshotRemovalRepositoryResult
{
  private final String repoId;

  private int deletedSnapshots;

  private int deletedFiles;

  private boolean isSuccessful;

  private boolean skipped;

  private int skippedCount;

  public SnapshotRemovalRepositoryResult(String repoId, boolean skipped) {
    this.repoId = repoId;
    this.skipped = skipped;
    this.isSuccessful = true;
  }

  public SnapshotRemovalRepositoryResult(String repoId, int deletedSnapshots, int deletedFiles, boolean isSucceful) {
    this.repoId = repoId;

    this.deletedSnapshots = deletedSnapshots;

    this.deletedFiles = deletedFiles;

    this.isSuccessful = isSucceful;
  }

  public String getRepositoryId() {
    return repoId;
  }

  public int getDeletedSnapshots() {
    return deletedSnapshots;
  }

  public void setDeletedSnapshots(int deletedSnapshots) {
    this.deletedSnapshots = deletedSnapshots;
  }

  public int getDeletedFiles() {
    return deletedFiles;
  }

  public void setDeletedFiles(int deletedFiles) {
    this.deletedFiles = deletedFiles;
  }

  public boolean isSuccessful() {
    return isSuccessful;
  }

  public void setSuccessful(boolean isSuccessful) {
    this.isSuccessful = isSuccessful;
  }

  public boolean isSkipped() {
    return skipped;
  }

  public void setSkipped(boolean skipped) {
    this.skipped = skipped;
  }

  public int getSkippedCount() {
    return skippedCount;
  }

  public void setSkippedCount(int skippedCount) {
    this.skippedCount = skippedCount;
  }
}
