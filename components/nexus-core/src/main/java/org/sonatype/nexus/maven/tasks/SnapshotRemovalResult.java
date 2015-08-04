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

import java.util.HashMap;
import java.util.Map;

public class SnapshotRemovalResult
{
  private Map<String, SnapshotRemovalRepositoryResult> processedRepositories;

  private boolean isSuccessful;

  public SnapshotRemovalResult() {
    super();

    this.processedRepositories = new HashMap<String, SnapshotRemovalRepositoryResult>();

    this.isSuccessful = true;
  }

  public Map<String, SnapshotRemovalRepositoryResult> getProcessedRepositories() {
    return processedRepositories;
  }

  public void addResult(SnapshotRemovalRepositoryResult res) {
    if (res != null) {
      if (processedRepositories.containsKey(res.getRepositoryId())) {
        SnapshotRemovalRepositoryResult ex = processedRepositories.get(res.getRepositoryId());

        ex.setDeletedFiles(ex.getDeletedFiles() + res.getDeletedFiles());

        ex.setDeletedSnapshots(ex.getDeletedSnapshots() + res.getDeletedSnapshots());

        if (res.isSkipped()) {
          ex.setSkippedCount(ex.getSkippedCount() + 1);
        }
      }
      else {
        processedRepositories.put(res.getRepositoryId(), res);
      }

      if (!res.isSuccessful()) {
        isSuccessful = false;
      }
    }
  }

  public boolean isSuccessful() {
    return isSuccessful;
  }

}
