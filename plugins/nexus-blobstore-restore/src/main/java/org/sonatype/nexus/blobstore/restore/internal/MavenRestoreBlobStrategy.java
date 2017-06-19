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
package org.sonatype.nexus.blobstore.restore.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.hash.MultiHashingInputStream;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

/**
 * @since 3.4
 */
@Named("maven2")
@Singleton
public class MavenRestoreBlobStrategy
    extends ComponentSupport
    implements RestoreBlobStrategy
{
  private final MavenPathParser mavenPathParser;

  private final NodeAccess nodeAccess;

  private final RepositoryManager repositoryManager;

  private final BlobStoreManager blobStoreManager;

  @Inject
  public MavenRestoreBlobStrategy(final MavenPathParser mavenPathParser,
                                  final NodeAccess nodeAccess,
                                  final RepositoryManager repositoryManager,
                                  final BlobStoreManager blobStoreManager)
  {
    this.mavenPathParser = checkNotNull(mavenPathParser);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  public void restore(final Properties properties, final Blob blob, final String blobStoreName) {
    String name = properties.getProperty(HEADER_PREFIX + BLOB_NAME_HEADER);
    String repoName = properties.getProperty(HEADER_PREFIX + REPO_NAME_HEADER);

    MavenPath mavenPath = mavenPathParser.parsePath(name);

    if (mavenPath.getCoordinates() == null && !mavenPathParser.isRepositoryMetadata(mavenPath)) {
      log.warn("Skipping as no maven coordinates found and is not maven metadata, blob store: {}, repository: {}, blob name: {}, blob id: {}",
          blobStoreName, repoName, name, blob.getId());
      return;
    }

    try (MultiHashingInputStream hashingStream = new MultiHashingInputStream(
        Arrays.asList(HashAlgorithm.MD5, HashAlgorithm.SHA1), blob.getInputStream()))
    {
      Optional<StorageFacet> storageFacet = getFacet(repoName, StorageFacet.class);
      Optional<MavenFacet> mavenFacet = getFacet(repoName, MavenFacet.class);
      if (storageFacet.isPresent() && mavenFacet.isPresent()) {
        Content content = TransactionalTouchMetadata.operation
            .withDb(storageFacet.get().txSupplier())
            .throwing(IOException.class)
            .call(() -> mavenFacet.get().get(mavenPath));

        if (content != null) {
          log.info("Skipping as asset already exists, blob store: {}, repository: {}, maven path: {}, blob name: {}, blob id: {}",
              blobStoreName, repoName, mavenPath.getPath(), name, blob.getId());
          return;
        }

        TransactionalStoreMetadata.operation
            .withDb(storageFacet.get().txSupplier())
            .throwing(IOException.class)
            .call(() -> mavenFacet.get().put(mavenPath,
                new AssetBlob(nodeAccess, blobStoreManager.get(blobStoreName), store -> blob,
                    properties.getProperty(HEADER_PREFIX + CONTENT_TYPE_HEADER), hashingStream.hashes(), true
                ),
                null));

        log.info("Restored asset, blob store: {}, repository: {}, maven path: {}, blob name: {}, blob id: {}",
            blobStoreName, repoName, mavenPath.getPath(), name, blob.getId());
      }
      else {
        log.debug("Skipping asset, blob store: {}, repository: {}, maven path: {}, blob name: {}, blob id: {}",
            blobStoreName, repoName, mavenPath.getPath(), name, blob.getId());
      }
    }
    catch (IOException e) {
      log.error("Error while restoring asset: blob store: {}, repository: {}, maven path: {}, blob name: {}, blob id: {}",
          blobStoreName, repoName, mavenPath.getPath(), name, blob.getId(), e);
    }
  }

  private <T extends Facet> Optional<T> getFacet(final String repositoryName, final Class<T> facetClass) {
    Optional<T> facet = Optional.ofNullable(repositoryName)
        .map(repositoryManager::get)
        .map(r -> r.optionalFacet(facetClass).orElse(null));
    if (!facet.isPresent()) {
      log.debug("Facet not found, repository: {}, facet type: {}", repositoryName, facetClass);
    }
    return facet;
  }
}
