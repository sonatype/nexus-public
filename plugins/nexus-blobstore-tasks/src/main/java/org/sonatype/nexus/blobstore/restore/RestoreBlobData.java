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

import java.time.OffsetDateTime;
import java.util.Properties;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;

/**
 * Simple structure for relevant data for a blob during metadata restoration
 *
 * @since 3.6.1
 */
public abstract class RestoreBlobData
{
  private final Blob blob;

  private final Properties blobProperties;

  private final BlobStore blobStore;

  private final Repository repository;

  private OffsetDateTime lastDownloaded;

  protected RestoreBlobData(
      final Blob blob,
      final Properties blobProperties,
      final BlobStore blobStore,
      final RepositoryManager repositoryManager)
  {
    checkNotNull(repositoryManager);
    checkNotNull(blobProperties);

    this.blob = blob;
    this.blobProperties = blobProperties;
    this.blobStore = blobStore;
    this.repository = repositoryManager
        .get(checkNotNull(getProperty(HEADER_PREFIX + REPO_NAME_HEADER), "Blob properties missing repository name"));
  }

  public Blob getBlob() {
    return blob;
  }

  public String getBlobName() {
    return getProperty(HEADER_PREFIX + BLOB_NAME_HEADER);
  }

  public String getBlobType() {
    return getProperty(HEADER_PREFIX + CONTENT_TYPE_HEADER);
  }

  public BlobStore getBlobStore() {
    return blobStore;
  }

  public Repository getRepository() {
    return repository;
  }

  public final String getProperty(final String propertyName) {
    return blobProperties.getProperty(propertyName);
  }

  public void setLastDownloaded(final OffsetDateTime lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
  }

  public OffsetDateTime getLastDownloaded() {
    return lastDownloaded;
  }

  public boolean hasLastDownloaded() {
    return lastDownloaded != null;
  }
}
