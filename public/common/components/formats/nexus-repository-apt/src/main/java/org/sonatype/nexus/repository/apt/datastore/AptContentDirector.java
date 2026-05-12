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
package org.sonatype.nexus.repository.apt.datastore;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.director.ContentDirector;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allow staging functionality for Apt,
 *
 * @see <a href="https://links.sonatype.com//products/nxrm3/docs/staging">Staging</a> for more details.
 *
 * @since 3.31
 */
@Component
@Qualifier(AptFormat.NAME)
@Singleton
public class AptContentDirector
    implements ContentDirector
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final RepositoryManager repositoryManager;

  // Thread-local tracking of source repositories that need metadata rebuild after staging moves
  // This prevents concurrency issues when multiple staging operations run simultaneously
  private final ThreadLocal<Set<String>> sourceRepositoriesToRebuild = ThreadLocal.withInitial(HashSet::new);

  private static final int LEADING_SLASH_LENGTH = 1;

  private static final String DISTS_PREFIX = "/dists/";

  @Inject
  public AptContentDirector(final RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  @Override
  public boolean allowMoveTo(final Repository destination) {
    return true;
  }

  @Override
  public boolean allowMoveTo(final FluentComponent component, final Repository destination) {
    return true;
  }

  @Override
  public boolean allowMoveFrom(final Repository source) {
    return true;
  }

  @Override
  public org.sonatype.nexus.repository.content.Component beforeMove(
      final org.sonatype.nexus.repository.content.Component component,
      final List<? extends Asset> assets,
      final Repository source,
      final Repository destination)
  {
    log.info("APT beforeMove: {} -> {} component: {}", source.getName(), destination.getName(), component);

    // Track APT source repositories that will need metadata rebuild (thread-safe)
    if (isAptHostedRepository(source)) {
      sourceRepositoriesToRebuild.get().add(source.getName());
      log.info("Tracking APT source repository for metadata rebuild: {}", source.getName());
    }

    return component;
  }

  @Override
  public org.sonatype.nexus.repository.content.Component afterMove(
      final org.sonatype.nexus.repository.content.Component component,
      final Repository destination)
  {
    log.info("APT individual afterMove: {} component: {}", destination.getName(), component);

    // Individual moves are handled by the batch afterMove method during staging operations
    // This method is mainly called for non-staging individual component moves
    if (isAptHostedRepository(destination)) {
      rebuildRepositoryMetadata(destination, "individual move");
    }
    return component;
  }

  @Override
  public void afterMove(final List<Map<String, String>> components, final Repository destination) {
    log.info("APT batch afterMove to DESTINATION: {} : {} components moved", destination.getName(), components.size());

    Set<String> trackedRepos = sourceRepositoriesToRebuild.get();
    try {
      // Rebuild destination metadata
      if (isAptHostedRepository(destination)) {
        rebuildRepositoryMetadata(destination, "staging move destination");
      }

      // Process tracked source repositories - clean up if empty, rebuild if not
      log.info("Processing {} tracked source repositories", trackedRepos.size());
      for (String sourceRepoName : trackedRepos) {
        Repository sourceRepo = repositoryManager.get(sourceRepoName);
        if (sourceRepo != null && isAptHostedRepository(sourceRepo)) {
          cleanupSourceRepositoryMetadata(sourceRepo);
        }
        else {
          log.warn("Tracked source repository not found or not APT hosted: {}", sourceRepoName);
        }
      }
    }
    finally {
      // Clear ThreadLocal to prevent memory leaks and prepare for next operation
      log.debug("Clearing {} tracked source repositories from ThreadLocal", trackedRepos.size());
      sourceRepositoriesToRebuild.remove();
    }
  }

  /**
   * Performs a full metadata rebuild by re-adding all package metadata from .deb assets.
   * This is necessary after move/delete operations because the metadata store may be stale.
   * <p>
   * This method:
   * 1. Re-scans all .deb assets in the repository
   * 2. Re-adds their metadata to the metadata store
   * 3. Rebuilds the metadata files (Packages, Release, InRelease)
   *
   * @param repository the repository to rebuild
   * @param operation description of the operation triggering the rebuild
   */
  private void rebuildRepositoryMetadata(final Repository repository, final String operation) {
    try {
      log.debug("Performing full APT metadata rebuild for repository: {} after {}", repository.getName(), operation);

      AptContentFacet contentFacet = repository.facet(AptContentFacet.class);
      AptHostedMetadataFacet metadataFacet = repository.facet(AptHostedMetadataFacet.class);

      // Re-add metadata for all .deb assets in the repository
      int addedCount = 0;
      for (FluentAsset asset : contentFacet.getAptPackageAssets()) {
        metadataFacet.addPackageMetadata(asset);
        addedCount++;
      }

      log.debug("Added metadata for {} .deb assets in repository: {}", addedCount, repository.getName());

      metadataFacet.rebuildMetadata();

      log.info("Successfully completed full APT metadata rebuild for repository: {}", repository.getName());
    }
    catch (IOException e) {
      if (log.isDebugEnabled()) {
        log.warn("Failed to rebuild APT metadata for repository: {} after {}", repository.getName(), operation, e);
      }
      else {
        log.warn("Failed to rebuild APT metadata for repository: {} after {}. Error: {}", repository.getName(),
            operation, e.getMessage());
      }
    }
  }

  private void cleanupSourceRepositoryMetadata(final Repository sourceRepo) {
    try {
      // Check if repository is empty (no components)
      boolean isEmpty = sourceRepo.facet(ContentFacet.class).components().count() == 0;

      if (isEmpty) {
        log.info("Source repository {} is empty after move - removing all metadata files", sourceRepo.getName());
        deleteMetadataAssets(sourceRepo);
      }
      else {
        log.info("Source repository {} still has components - rebuilding metadata", sourceRepo.getName());
        rebuildRepositoryMetadata(sourceRepo, "staging move source cleanup");
      }
    }
    catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.warn("Failed to cleanup metadata for source repository: {} - {}", sourceRepo.getName(), e.getMessage(), e);
      }
      else {
        log.warn("Failed to cleanup metadata for source repository: {} - {}", sourceRepo.getName(), e.getMessage());
      }
    }
  }

  /**
   * Deletes all metadata assets from an empty APT repository.
   * This method handles pagination through all assets and removes those matching the metadata prefix.
   *
   * @param sourceRepo the repository to clean up
   */
  private void deleteMetadataAssets(final Repository sourceRepo) {
    AptContentFacet aptFacet = sourceRepo.facet(AptContentFacet.class);
    String distribution = aptFacet.getDistribution();
    String prefix = DISTS_PREFIX + distribution + "/";

    log.info("Attempting to delete APT metadata assets with prefix: '{}' from repository: {}", prefix,
        sourceRepo.getName());

    ContentFacet contentFacet = sourceRepo.facet(ContentFacet.class);
    AssetDeletionResult result = deleteAssetsByPrefix(contentFacet, prefix);

    log.info("Found {} total assets in repository {}, deleted {} metadata assets matching prefix '{}'",
        result.totalAssets, sourceRepo.getName(), result.deletedCount, prefix);

    // Also try the original method for non-component assets as fallback
    // Note: original method might expect prefix without leading slash
    aptFacet.deleteAssetsByPrefix(prefix.substring(LEADING_SLASH_LENGTH));
  }

  /**
   * Deletes assets matching the specified prefix using pagination.
   *
   * @param contentFacet the content facet to browse assets
   * @param prefix the prefix to match for deletion
   * @return result containing counts of total and deleted assets
   */
  private AssetDeletionResult deleteAssetsByPrefix(final ContentFacet contentFacet, final String prefix) {
    Continuation<FluentAsset> assetPage = contentFacet.assets().browse(Continuations.BROWSE_LIMIT, null);
    int deletedCount = 0;
    int totalAssets = 0;

    if (assetPage != null) {
      do {
        for (FluentAsset asset : assetPage) {
          totalAssets++;
          log.debug("Found asset: path='{}'", asset.path());
          if (asset.path().startsWith(prefix)) {
            log.info("Deleting metadata asset: {}", asset.path());
            asset.delete();
            deletedCount++;
          }
          else {
            log.debug("Asset '{}' does not match prefix '{}' - skipping", asset.path(), prefix);
          }
        }

        assetPage = assetPage.nextContinuationToken() != null
            ? contentFacet.assets().browse(Continuations.BROWSE_LIMIT, assetPage.nextContinuationToken())
            : null;

      }
      while (assetPage != null);
    }

    return new AssetDeletionResult(totalAssets, deletedCount);
  }

  /**
   * Result of asset deletion operation containing counts.
   */
  private record AssetDeletionResult(int totalAssets, int deletedCount)
  {
  }

  private boolean isAptHostedRepository(final Repository repository) {
    return AptFormat.NAME.equals(repository.getFormat().getValue())
        && HostedType.NAME.equals(repository.getType().getValue());
  }
}
