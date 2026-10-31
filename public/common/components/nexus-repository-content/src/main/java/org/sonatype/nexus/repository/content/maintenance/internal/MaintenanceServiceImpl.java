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
package org.sonatype.nexus.repository.content.maintenance.internal;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import jakarta.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static java.lang.String.format;
import static java.lang.Thread.MIN_PRIORITY;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.thread.NexusExecutorService.forCurrentSubject;

/**
 * @since 3.26
 */
@org.springframework.stereotype.Component
public class MaintenanceServiceImpl
    implements MaintenanceService
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final DeleteFolderService deleteFolderService;

  private final ExecutorService executorService;

  private final DatabaseCheck databaseCheck;

  @Autowired
  public MaintenanceServiceImpl(
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final DeleteFolderService deleteFolderService,
      @Named("org.sonatype.nexus.thread.DatabaseStatusDelayedExecutor") final ExecutorService executorService,
      final DatabaseCheck databaseCheck)
  {
    this.contentPermissionChecker = requireNonNull(contentPermissionChecker);
    this.variableResolverAdapterManager = requireNonNull(variableResolverAdapterManager);
    this.repositoryPermissionChecker = requireNonNull(repositoryPermissionChecker);
    this.deleteFolderService = requireNonNull(deleteFolderService);
    this.databaseCheck = requireNonNull(databaseCheck);

    this.executorService = forCurrentSubject(newSingleThreadExecutor(
        new NexusThreadFactory("delete-path", "Delete path in Tree Browse View", MIN_PRIORITY)));
  }

  @Override
  public Set<String> deleteAsset(final Repository repository, final Asset asset) {
    requireNonNull(repository);
    requireNonNull(asset);

    if (!canDeleteAsset(repository, asset)) {
      throw new AuthorizationException();
    }

    ContentMaintenanceFacet facet = maintenanceFacet(repository);

    // Pre-capture the browse node id before deleting the asset. The DB has an ON DELETE SET NULL
    // constraint on browse_node.asset_id, so once the asset row is deleted, the browse node's
    // asset_id becomes NULL and cannot be used to find the node. We look it up now while it is
    // still set.
    Long browseNodeId = prefetchBrowseNodeId(repository, asset);

    // Delete the asset first; if the format facet rejects the deletion (e.g. 409 referential-
    // integrity guard), the browse node must not be removed — otherwise it becomes orphaned.
    Set<String> deleted = facet.deleteAsset(asset);

    // Asset deletion succeeded; delete the browse node using the pre-captured node id.
    deleteBrowseNodeById(repository, browseNodeId);

    return deleted;
  }

  @Nullable
  private Long prefetchBrowseNodeId(final Repository repository, final Asset asset) {
    if (!isPostgresql()) {
      return null;
    }
    if (isNpmRepository(repository) && shouldPreserveBrowseNode(asset)) {
      return null;
    }
    Integer internalAssetId = InternalIds.internalAssetId(asset);
    return repository.optionalFacet(BrowseFacet.class)
        .map(facet -> facet.getNodeIdForAsset(internalAssetId))
        .orElse(null);
  }

  private void deleteBrowseNodeById(final Repository repository, @Nullable final Long nodeId) {
    if (nodeId != null) {
      repository.optionalFacet(BrowseFacet.class)
          .ifPresent(facet -> facet.deleteByNodeIdAndCleanParents(nodeId));
    }
  }

  @Override
  public Set<String> deleteComponent(final Repository repository, final Component component) {
    requireNonNull(repository);
    requireNonNull(component);

    if (!canDeleteComponent(repository, component)) {
      throw new AuthorizationException();
    }

    ContentMaintenanceFacet facet = maintenanceFacet(repository);
    Set<String> deleted = facet.deleteComponent(component);
    deleteBrowseNode(repository, component);
    return deleted;
  }

  @Override
  public void deleteFolder(final Repository repository, final String path) {
    requireNonNull(repository);
    requireNonNull(path);

    if (!canDeleteFolder(repository, path)) {
      throw new AuthorizationException();
    }

    executorService.submit(() -> deleteFolderService.deleteFolder(repository, path, now()));
  }

  @Override
  public boolean canDeleteComponent(final Repository repository, final Component component) {
    boolean canDeleteComponent = true;

    ContentFacet contentFacet = repository.facet(ContentFacet.class);

    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);

    for (Asset asset : contentFacet.components().with(component).assets()) {
      if (!canDeleteAssetInRepository(repositoryName, format, variableResolverAdapter, asset)) {
        canDeleteComponent = false;
        break;
      }
    }

    return canDeleteComponent;
  }

  @Override
  public boolean canDeleteAsset(final Repository repository, final Asset asset) {

    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    return canDeleteAssetInRepository(repositoryName, format, variableResolverAdapterManager.get(format), asset);
  }

  @Override
  public boolean canDeleteFolder(final Repository repository, final String folder) {
    // checking for repository-level permission
    if (!repositoryPermissionChecker.userCanDeleteInRepository(repository)) {
      return false;
    }
    // Evaluate Content Selector pattern against the folder path
    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);
    String folderPath = URLDecoder.decode(folder, StandardCharsets.UTF_8);
    if (!folderPath.startsWith("/")) {
      folderPath = "/" + folderPath;
    }
    VariableSource source = variableResolverAdapter.fromPath(folderPath, format);
    return contentPermissionChecker.isPermitted(repositoryName, format, DELETE, source);
  }

  @Override
  public Set<String> deleteAssets(final Repository repository, final List<Integer> assetIds) {
    requireNonNull(repository);
    requireNonNull(assetIds);
    if (assetIds.isEmpty()) {
      return Set.of();
    }

    AssetStore<?> assetStore = contentFacetSupport(repository).stores().assetStore;
    Set<String> deletedAssets = new HashSet<>();
    String repositoryName = repository.getName();
    for (Integer assetId : assetIds) {
      assetStore.readAsset(assetId).ifPresent(asset -> {
        Set<String> currentDeletedAssets = deleteAsset(repository, asset);
        log.trace("Current deleted assets {} from {} repository", currentDeletedAssets, repositoryName);
        deletedAssets.addAll(currentDeletedAssets);
      });
    }
    log.debug("Total deleted assets {} from {} repository", deletedAssets, repositoryName);
    return deletedAssets;
  }

  private boolean canDeleteAssetInRepository(
      final String repositoryName,
      final String format,
      final VariableResolverAdapter variableResolverAdapter,
      final Asset asset)
  {
    VariableSource source = variableResolverAdapter.fromPath(asset.path(), format);
    return contentPermissionChecker.isPermitted(repositoryName, format, DELETE, source);
  }

  private ContentMaintenanceFacet maintenanceFacet(final Repository repository) {
    try {
      return repository.facet(ContentMaintenanceFacet.class);
    }
    catch (MissingFacetException e) {
      throw new IllegalOperationException(format("Deleting from repository %s of type %s is not supported",
          repository.getName(), repository.getFormat()), e);
    }
  }

  protected void deleteBrowseNode(final Repository repository, final Asset asset) {
    if (isPostgresql()) {
      // For NPM repositories, only delete browse nodes for PACKAGE_ROOT assets during asset deletion
      // This preserves the UI tree structure when only metadata is being deleted
      if (isNpmRepository(repository) && shouldPreserveBrowseNode(asset)) {
        log.debug("Preserving browse node for NPM asset: {} (kind: {})", asset.path(), asset.kind());
        return;
      }

      Integer internalAssetId = InternalIds.internalAssetId(asset);

      repository.optionalFacet(BrowseFacet.class)
          .ifPresent(facet -> facet.deleteByAssetIdAndPath(internalAssetId, asset.path()));
    }
  }

  private boolean isNpmRepository(final Repository repository) {
    return "npm".equals(repository.getFormat().getValue());
  }

  private boolean shouldPreserveBrowseNode(final Asset asset) {
    // For NPM, preserve browse nodes for TARBALL assets (only delete for PACKAGE_ROOT)
    String assetKind = asset.kind();
    return "PACKAGE_ROOT".equals(assetKind);
  }

  private void deleteBrowseNode(final Repository repository, final Component component) {
    if (isPostgresql()) {
      repository.optionalFacet(ContentFacet.class)
          .ifPresent(contentFacet -> contentFacet.components()
              .with(component)
              .assets()
              .forEach(asset -> deleteBrowseNode(repository, asset)));
    }
  }

  private boolean isPostgresql() {
    return this.databaseCheck.isPostgresql();
  }

  @VisibleForTesting
  protected ContentFacetSupport contentFacetSupport(final Repository repository) {
    try {
      return (ContentFacetSupport) repository.facet(ContentFacet.class);
    }
    catch (Exception e) {
      throw new MissingFacetException(repository, ContentFacetSupport.class);
    }
  }
}
