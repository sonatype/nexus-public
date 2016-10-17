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
package org.sonatype.nexus.repository.internal.blobstore;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.internal.blobstore.CompactBlobStoreTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;

/**
 * Task to compact a given blob store.
 *
 * @since 3.0
 */
@Named
public class CompactBlobStoreTask
    extends TaskSupport
{
  private final BlobStoreManager blobStoreManager;

  @Inject
  public CompactBlobStoreTask(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  protected Object execute() throws Exception {
    blobStoreManager.get(getBlobStoreField()).compact();
    return null;
  }

  @Override
  public String getMessage() {
    return "Compacting " + getBlobStoreField() + " blob store";
  }

  protected String getBlobStoreField() {
    return getConfiguration().getString(BLOB_STORE_NAME_FIELD_ID);
  }
}
