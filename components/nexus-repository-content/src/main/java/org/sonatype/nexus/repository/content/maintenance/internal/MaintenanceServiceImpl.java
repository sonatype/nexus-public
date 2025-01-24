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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.lang.Thread.MIN_PRIORITY;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.sonatype.nexus.security.BreadActions.DELETE;
import static org.sonatype.nexus.thread.NexusExecutorService.forCurrentSubject;

/**
 * @since 3.26
 */
@Named
@Singleton
public class MaintenanceServiceImpl
    extends ComponentSupport
    implements MaintenanceService
{
  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final RepositoryPermissionChecker repositoryPermissionChecker;

  private final DeleteFolderService deleteFolderService;

  private final ExecutorService executorService;

  private final DatabaseCheck databaseCheck;

  @Inject
  public MaintenanceServiceImpl(
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final RepositoryPermissionChecker repositoryPermissionChecker,
      final DeleteFolderService deleteFolderService,
      final ExecutorService executorService,
      final DatabaseCheck databaseCheck)
  {
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.repositoryPermissionChecker = checkNotNull(repositoryPermissionChecker);
    this.deleteFolderService = checkNotNull(deleteFolderService);
    this.databaseCheck = checkNotNull(databaseCheck);

    this.executorService = forCurrentSubject(newSingleThreadExecutor(
        new NexusThreadFactory("delete-path", "Delete path in Tree Browse View", MIN_PRIORITY)));
  }

  @Override
  public Set<String> deleteAsset(final Repository repository, final Asset asset) {
    checkNotNull(repository);
    checkNotNull(asset);

    if (!canDeleteAsset(repository, asset)) {
      throw new AuthorizationException();
    }

    deleteBrowseNode(repository, asset);

    return maintenanceFacet(repository).deleteAsset(asset);
  }

  @Override
  public Set<String> deleteComponent(final Repository repository, final Component component) {
    checkNotNull(repository);
    checkNotNull(component);

    if (!canDeleteComponent(repository, component)) {
      throw new AuthorizationException();
    }

    return maintenanceFacet(repository).deleteComponent(component);
  }

  @Override
  public void deleteFolder(final Repository repository, final String path) {
    checkNotNull(repository);
    checkNotNull(path);

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
    return repositoryPermissionChecker.userCanDeleteInRepository(repository);
  }

  @Override
  public Set<String> deleteAssets(final Repository repository, final List<Integer> assetIds) {
    checkNotNull(repository);
    checkNotNull(assetIds);
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

  private void deleteBrowseNode(final Repository repository, final Asset asset) {
    if (isPostgresql()) {
      Integer internalAssetId = InternalIds.internalAssetId(asset);

      repository.optionalFacet(BrowseFacet.class)
          .ifPresent(facet -> facet.deleteByAssetIdAndPath(internalAssetId, asset.path()));
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
