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
package org.sonatype.nexus.repository.content.replication;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.replication.ReplicationIngesterHelper;
import org.sonatype.nexus.repository.replication.ReplicationIngestionException;

import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.31
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class ReplicationIngesterHelperImpl
    extends ComponentSupport
    implements ReplicationIngesterHelper
{
  protected final RepositoryManager repositoryManager;

  protected final BlobStoreManager blobStoreManager;

  @Inject
  public ReplicationIngesterHelperImpl(final RepositoryManager repositoryManager,
                                       final BlobStoreManager blobStoreManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  public void replicate(final String blobStoreId,
                        final Blob blob,
                        final Map<String, Object> assetAttributes,
                        final Map<String, Object> componentAttributes,
                        final String repositoryName,
                        final String blobStoreName)
      throws IOException
  {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new ReplicationIngestionException(
          String.format("Can't replicate blob %s as the repository %s doesn't exist", blob.getId().toString(),
              repositoryName));
    }

    BlobStore blobStore = blobStoreManager.get(blobStoreName);
    BlobAttributes blobAttributes = blobStore.getBlobAttributes(blob.getId());
    blobAttributes.getHeaders().put(REPO_NAME_HEADER, repositoryName);
    blobAttributes.store();
    String path = normalizePath(blobAttributes.getHeaders().get(BLOB_NAME_HEADER));

    ReplicationFacet replicationFacet = repository.facet(ReplicationFacet.class);
    replicationFacet.replicate(path, blob, assetAttributes, componentAttributes);
  }

  @Override
  public void deleteReplication(final String path, final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new ReplicationIngestionException(
          String.format("Can't delete blob in path %s, the repository %s doesn't exist", path, repositoryName));
    }
    ReplicationFacet replicationFacet = repository.facet(ReplicationFacet.class);
    replicationFacet.replicateDelete(normalizePath(path));
  }

  private String normalizePath(final String path) {
    if (path.startsWith("/")) {
      return path;
    }
    return "/" + path;
  }
}
