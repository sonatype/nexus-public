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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple struct for consolidating results of ReleaseRemoval task.
 *
 * @since 2.5
 */
public class ReleaseRemovalResult
{
  private final String repoId;

  private int deletedFileCount;

  private boolean isSuccessful = false;

  public ReleaseRemovalResult(final String repoId) {
    this.repoId = checkNotNull(repoId);
  }

  public String getRepoId() {
    return repoId;
  }

  public int getDeletedFileCount() {
    return deletedFileCount;
  }

  public void setDeletedFileCount(final int deletedFileCount) {
    this.deletedFileCount = deletedFileCount;
  }

  public boolean isSuccessful() {
    return isSuccessful;
  }

  public void setSuccessful(final boolean successful) {
    isSuccessful = successful;
  }

  @Override
  public String toString() {
    return "ReleaseRemovalResult{" +
        "repoId='" + repoId + '\'' +
        ", deletedFileCount=" + deletedFileCount +
        ", isSuccessful=" + isSuccessful +
        '}';
  }
}
