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
package org.sonatype.nexus.repository.tools;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter.P_NAME;

/**
 * Detects orphaned blobs (i.e. non-deleted blobs that exist in the blobstore but not the asset table)
 *
 * @since 3.13
 */
@Named
public class OrphanedBlobFinder
    extends ComponentSupport
{
  private static final String REPOSITORY_NAME_KEY = "Bucket.repo-name";

  private static final String ASSET_NAME_KEY = "BlobStore.blob-name";

  private static final String STORAGE_KEY = "storage";

  private static final String BLOB_STORE_NAME_KEY = "blobStoreName";

  private final RepositoryManager repositoryManager;

  private final BlobStoreManager blobStoreManager;

  @Inject
  public OrphanedBlobFinder(final RepositoryManager repositoryManager,
                            final BlobStoreManager blobStoreManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  /**
   * Delete orphaned blobs for all repositories
   */
  public void delete() {
    log.info("Starting delete of orphaned blobs for all known blob stores");
    
    blobStoreManager.browse().forEach(this::delete);

    log.info("Finished deleting orphaned blobs");
  }

  /**
   * Delete orphaned blobs associated with a given repository
   *
   * @param repository - where to look for orphaned blobs
   */
  public void delete(final Repository repository) {
    log.info("Starting delete of orphaned blobs for {}", repository.getName());

    delete(getBlobStoreForRepository(repository));

    log.info("Finished deleting orphaned blobs for {}", repository.getName());
  }
  
  private void delete(final BlobStore blobStore) {
    detect(blobStore, blobId -> {
      log.info("Deleting orphaned blob {} from blobstore {}", blobId, blobStore.getBlobStoreConfiguration().getName());

      blobStore.deleteHard(new BlobId(blobId));
    });
  }

  /**
   * Look for orphaned blobs in a given repository and callback for each blobId found
   *
   * @param repository - where to look for orphaned blobs
   * @param handler    - callback to handle an orphaned blob
   */
  public void detect(final Repository repository, final Consumer<String> handler) {
    validateRepositoryConfiguration(repository);
    
    detect(getBlobStoreForRepository(repository), handler);
  }
  
  private void detect(final BlobStore blobStore, final Consumer<String> handler) {
    Stream<BlobId> blobIds = blobStore.getBlobIdStream();

    blobIds.forEach(id -> {
      BlobAttributes attributes = blobStore.getBlobAttributes(id);
      if (attributes != null) {
        checkIfOrphaned(handler, id, attributes);
      }
      else{
        log.warn("Skipping cleanup for blob {} because blob properties not found", id);
      }
    });
  }

  private void checkIfOrphaned(final Consumer<String> handler, final BlobId id, final BlobAttributes attributes) {
    String repositoryName = attributes.getHeaders().get(REPOSITORY_NAME_KEY);
    
    if (repositoryName != null) {
      String assetName = attributes.getHeaders().get(ASSET_NAME_KEY);

      Repository repository = repositoryManager.get(repositoryName);
      if (repository == null) {
        log.debug("Blob {} considered orphaned because repository with name {} no longer exists", id.asUniqueString(),
            repositoryName);

        handler.accept(id.asUniqueString());
      }
      else {
        findAssociatedAsset(assetName, repository).ifPresent(asset -> {
          BlobRef blobRef = asset.blobRef();
          if (blobRef != null && !blobRef.getBlobId().asUniqueString().equals(id.asUniqueString())) {
            if (!attributes.isDeleted()) {
              handler.accept(id.asUniqueString());
            }
            else {
              log.debug("Blob {} in repository {} not considered orphaned because it is already marked soft-deleted",
                  id.asUniqueString(), repositoryName);
            }
          }
        });
      }
    }
  }

  private BlobStore getBlobStoreForRepository(final Repository repository) {
    String blobStoreName = (String) repository.getConfiguration().getAttributes().get(STORAGE_KEY)
        .get(BLOB_STORE_NAME_KEY);

    return blobStoreManager.get(blobStoreName);
  }

  private Optional<Asset> findAssociatedAsset(final String assetName, final Repository repository) {
    try (StorageTx tx = repository.facet(StorageFacet.class).txSupplier().get()) {
      tx.begin();

      return ofNullable(tx.findAssetWithProperty(P_NAME, assetName));
    }
  }

  private void validateRepositoryConfiguration(final Repository repository) {
    checkArgument(repository.getConfiguration().getAttributes() != null,
        "Repository configuration not found " + repository.getName());
    checkArgument(repository.getConfiguration().getAttributes().get(STORAGE_KEY) != null,
        "No storage configuration found for the repository " + repository.getName());
    checkArgument(
        isNotBlank((String) repository.getConfiguration().getAttributes().get(STORAGE_KEY).get(BLOB_STORE_NAME_KEY)),
        "Blob store name not set for repository " + repository.getName());
  }
}
