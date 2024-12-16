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
package org.sonatype.nexus.testsuite.testsupport.blobstore.restore;

import javax.inject.Inject;

import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.sonatype.nexus.scheduling.TaskState.OK;

public abstract class BlobstoreRestoreTestHelperSupport
    implements BlobstoreRestoreTestHelper
{
  @Inject
  private TaskScheduler taskScheduler;

  @Override
  public void runRestoreMetadataTaskWithTimeout(final String blobstoreName, final long timeout, final boolean dryRun) {
    TaskConfiguration config = taskScheduler.createTaskConfigurationInstance(TYPE_ID);
    config.setEnabled(true);
    config.setName("restore");
    config.setString(BLOB_STORE_NAME_FIELD_ID, blobstoreName);
    config.setBoolean(DRY_RUN, dryRun);
    config.setBoolean(RESTORE_BLOBS, true);
    config.setBoolean(UNDELETE_BLOBS, false);
    config.setBoolean(INTEGRITY_CHECK, false);
    TaskInfo taskInfo = taskScheduler.submit(config);
    await().atMost(timeout, SECONDS)
        .until(() -> taskInfo.getLastRunState() != null && taskInfo.getLastRunState().getEndState().equals(OK));
  }

  @Override
  public void runReconcileTaskWithTimeout(final String blobstoreName, final long timeout) {
    TaskConfiguration planingConfig = taskScheduler.createTaskConfigurationInstance(PLAN_RECONCILE_TYPE_ID);
    planingConfig.setString(BLOB_STORE_NAME_FIELD_ID, blobstoreName);
    TaskInfo planingTaskInfo = taskScheduler.submit(planingConfig);
    await().atMost(timeout, SECONDS)
        .until(() -> planingTaskInfo.getLastRunState() != null
            && planingTaskInfo.getLastRunState().getEndState().equals(OK));

    TaskConfiguration executeConfig = taskScheduler.createTaskConfigurationInstance(EXECUTE_RECONCILE_TYPE_ID);
    executeConfig.setString(BLOB_STORE_NAME_FIELD_ID, blobstoreName);
    TaskInfo executeTaskInfo = taskScheduler.submit(executeConfig);
    await().atMost(timeout, SECONDS)
        .until(() -> executeTaskInfo.getLastRunState() != null
            && executeTaskInfo.getLastRunState().getEndState().equals(OK));
  }
}
