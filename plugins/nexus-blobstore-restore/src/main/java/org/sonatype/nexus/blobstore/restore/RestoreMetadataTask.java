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
package org.sonatype.nexus.blobstore.restore;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.*;

/**
 * @since 3.4
 */
@Named
public class RestoreMetadataTask
    extends TaskSupport
{
  private final RestoreMetadataService restoreMetadataService;

  @Inject
  public RestoreMetadataTask(final RestoreMetadataService restoreMetadataService) {
    this.restoreMetadataService = checkNotNull(restoreMetadataService);
  }

  @Override
  public String getMessage() {
    return null;
  }

  @Override
  protected Object execute() throws Exception {
    String blobStoreId =
        checkNotNull(getConfiguration().getString(BLOB_STORE_NAME_FIELD_ID));

    restoreMetadataService.restore(blobStoreId);

    return null;
  }
}
