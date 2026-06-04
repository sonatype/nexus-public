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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.util.Properties;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.view.payloads.DetachedBlobPayload;

/**
 * Simple structure for relevant data for a blob during metadata restoration
 *
 * @since 3.38
 */
public class DataStoreRestoreBlobData
    extends RestoreBlobData
{
  public DataStoreRestoreBlobData(
      final Blob blob,
      final Properties blobProperties,
      final BlobStore blobStore,
      final RepositoryManager repositoryManager)
  {
    super(blob, blobProperties, blobStore, repositoryManager);
  }

  @Override
  public String getBlobName() {
    String blobName = super.getBlobName();
    if (blobName.startsWith("/")) {
      return blobName;
    }
    return "/" + blobName;
  }

  /**
   * Creates a {@link DetachedBlobPayload} that includes the source blobstore reference.
   * This allows the ingest process to detect when a blob needs to be re-ingested into a different blobstore.
   */
  public DetachedBlobPayload createDetachedPayload() {
    return new DetachedBlobPayload(getBlob(), getBlobStore());
  }
}
