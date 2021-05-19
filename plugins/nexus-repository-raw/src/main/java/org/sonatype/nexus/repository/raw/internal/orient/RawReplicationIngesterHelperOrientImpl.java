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
package org.sonatype.nexus.repository.raw.internal.orient;

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
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.raw.RawReplicationIngesterHelper;
import org.sonatype.nexus.repository.replication.ReplicationIngestionException;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.ReplicationFacet;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.Hashes.hash;

@FeatureFlag(name = ORIENT_ENABLED)
@Named("orient")
@Singleton
public class RawReplicationIngesterHelperOrientImpl
    extends ComponentSupport
    implements RawReplicationIngesterHelper
{
  private final BlobStoreManager blobStoreManager;

  private final NodeAccess nodeAccess;

  private final RepositoryManager repositoryManager;

  @Inject
  public RawReplicationIngesterHelperOrientImpl(
      final BlobStoreManager blobStoreManager,
      final NodeAccess nodeAccess,
      final RepositoryManager repositoryManager)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  public void replicate(
      final String blobStoreId, final Blob blob,
      final Map<String, Object> assetAttributesMap,
      final String repositoryName,
      final String blobStoreName) throws IOException
  {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new ReplicationIngestionException(
          String.format("Can't replicate blob %s, the repository %s doesn't exist", blob.getId().toString(),
              repositoryName));
    }

    BlobStore blobStore = blobStoreManager.get(blobStoreId);
    BlobAttributes blobAttributes = blobStore.getBlobAttributes(blob.getId());
    NestedAttributesMap nestedAttributesMap = new NestedAttributesMap("attributes", assetAttributesMap);

    AssetBlob assetBlob = new AssetBlob(nodeAccess,
        blobStore,
        store -> blob,
        blobAttributes.getHeaders().get(CONTENT_TYPE_HEADER),
        hash(ImmutableList.of(SHA1), blob.getInputStream()), true);
    ReplicationFacet replicationFacet = repository.facet(ReplicationFacet.class);
    replicationFacet.replicate(blobAttributes.getHeaders().get(BLOB_NAME_HEADER), assetBlob, nestedAttributesMap);
  }

  @Override
  public void deleteReplication(final String path, final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new ReplicationIngestionException(
          String.format("Can't delete blob in path %s, the repository %s doesn't exist", path, repositoryName));
    }
    ReplicationFacet replicationFacet = repository.facet(ReplicationFacet.class);
    replicationFacet.replicateDelete(path);
  }
}
