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
package org.sonatype.nexus.coreui.internal.orient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.coreui.ComponentXO;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetVariableResolver;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentFinder;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import org.apache.shiro.authz.AuthorizationException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status;
import static org.sonatype.nexus.repository.storage.DefaultComponentFinder.DEFAULT_COMPONENT_FINDER_KEY;

/**
 * Orient {@link ComponentHelper}.
 */
@Priority(Integer.MAX_VALUE) // make sure this implementation appears above the datastore one in mixed-mode
@Named
@Singleton
public class OrientComponentHelper
    extends ComponentSupport
    implements ComponentHelper
{
  @VisibleForTesting
  static ComponentXO COMPONENT_CONVERTER(Component component, String repositoryName) {
    ComponentXO componentXO = new ComponentXO();
    componentXO.setId(EntityHelper.id(component).getValue());
    componentXO.setRepositoryName(repositoryName);
    componentXO.setGroup(component.group());
    componentXO.setName(component.name());
    componentXO.setVersion(component.version());
    componentXO.setFormat(component.format());
    return componentXO;
  }

  private static AssetXO ASSET_CONVERTER(
      Asset asset,
      String repositoryName,
      String privilegedRepositoryName)
  {
    AssetXO assetXO = new AssetXO();
    assetXO.setId(EntityHelper.id(asset).getValue());
    assetXO.setName(asset.name());
    assetXO.setFormat(asset.format());
    assetXO.setContentType(asset.contentType() != null ? asset.contentType() : "unknown");
    assetXO.setSize(asset.size() != null ? asset.size() : 0L);
    assetXO.setRepositoryName(repositoryName);
    assetXO.setContainingRepositoryName(privilegedRepositoryName);
    assetXO.setBlobCreated(asset.blobCreated() != null ? asset.blobCreated().toDate() : null);
    assetXO.setBlobUpdated(asset.blobUpdated() != null ? asset.blobUpdated().toDate() : null);
    assetXO.setLastDownloaded(asset.lastDownloaded() != null ? asset.lastDownloaded().toDate() : null);
    assetXO.setBlobRef(asset.blobRef() != null ? asset.blobRef().toString() : "");
    assetXO.setComponentId(asset.componentId() != null ? asset.componentId().getValue() : "");
    assetXO.setAttributes(asset.attributes().backing());
    assetXO.setCreatedBy(asset.createdBy());
    assetXO.setCreatedByIp(asset.createdByIp());
    return assetXO;
  }

  private final RepositoryManager repositoryManager;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final BrowseService browseService;

  private final MaintenanceService maintenanceService;

  private final Map<String, ComponentFinder> componentFinders;

  private final BucketStore bucketStore;

  @Inject
  public OrientComponentHelper(
      final RepositoryManager repositoryManager,
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final BrowseService browseService,
      final MaintenanceService maintenanceService,
      final Map<String, ComponentFinder> componentFinders,
      final BucketStore bucketStore)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.browseService = checkNotNull(browseService);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.componentFinders = checkNotNull(componentFinders);
    this.bucketStore = checkNotNull(bucketStore);
  }

  public List<AssetXO> readComponentAssets(final Repository repository, final ComponentXO componentXO) {
    ComponentFinder componentFinder = componentFinders.get(componentXO.getFormat());
    if (null == componentFinder) {
      componentFinder = componentFinders.get(DEFAULT_COMPONENT_FINDER_KEY);
    }

    List<Component> components =
        componentFinder.findMatchingComponents(repository, componentXO.getId(), componentXO.getGroup(),
            componentXO.getName(), componentXO.getVersion());

    PageResult<Asset> browseResult = browseService.browseComponentAssets(repository, components.get(0));

    return createAssetXOs(browseResult.getResults(), repository);
  }

  public PageResult<AssetXO> previewAssets(
      final RepositorySelector repositorySelector,
      final List<Repository> selectedRepositories,
      final String jexlExpression,
      final QueryOptions queryOptions)
  {
    PageResult<Asset> page =
        browseService.previewAssets(repositorySelector, selectedRepositories, jexlExpression, queryOptions);

    // buckets not needed for asset preview screen
    return new PageResult<>(page.getTotal(),
        page.getResults().stream().map(asset -> ASSET_CONVERTER(asset, null, null)).collect(toList()));
  }

  public ComponentXO readComponent(final Repository repository, final EntityId componentId) {
    Component component;
    List<Asset> assets;
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      component = storageTx.findComponent(componentId);
      if (component == null) {
        throw new WebApplicationException(Status.NOT_FOUND);
      }

      Iterable<Asset> browsedAssets = storageTx.browseAssets(component);
      if (browsedAssets == null || Iterables.isEmpty(browsedAssets)) {
        throw new WebApplicationException(Status.NOT_FOUND);
      }

      assets = newArrayList(browsedAssets);
    }
    ensurePermissions(repository, assets, BreadActions.BROWSE);
    return COMPONENT_CONVERTER(component, repository.getName());
  }

  public boolean canDeleteComponent(final Repository repository, final ComponentXO componentXO) {
    List<Component> components = getComponents(repository, componentXO);

    for (Component component : components) {
      if (!maintenanceService.canDeleteComponent(repository, component)) {
        return false;
      }
    }

    return true;
  }

  public Set<String> deleteComponent(final Repository repository, final ComponentXO componentXO) {
    List<Component> components = getComponents(repository, componentXO);

    Set<String> deletedAssets = new HashSet<>();
    for (Component component : components) {
      deletedAssets.addAll(maintenanceService.deleteComponent(repository, component));
    }
    return deletedAssets;
  }

  private List<Component> getComponents(final Repository repository, final ComponentXO componentXO) {
    ComponentFinder componentFinder = componentFinders.get(componentXO.getFormat());
    if (null == componentFinder) {
      componentFinder = componentFinders.get(DEFAULT_COMPONENT_FINDER_KEY);
    }

    return componentFinder.findMatchingComponents(repository, componentXO.getId(), componentXO.getGroup(),
        componentXO.getName(), componentXO.getVersion());
  }

  public AssetXO readAsset(final Repository repository, final EntityId assetId) {
    Asset asset = browseService.getAssetById(assetId, repository);

    if (asset == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    Bucket bucket = bucketStore.getById(asset.bucketId());

    if (bucket == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    String permittedRepositoryName =
        ensurePermissions(repositoryManager.get(bucket.getRepositoryName()), singletonList(asset), BreadActions.BROWSE);
    return ASSET_CONVERTER(asset, repository.getName(), permittedRepositoryName);
  }

  public boolean canDeleteAsset(final Repository repository, final EntityId assetId) {
    Asset asset = getAsset(repository, assetId);

    if (asset != null) {
      return maintenanceService.canDeleteAsset(repository, asset);
    }

    return false;
  }

  public Set<String> deleteAsset(final Repository repository, final EntityId assetId) {
    Asset asset = getAsset(repository, assetId);

    if (asset != null) {
      return maintenanceService.deleteAsset(repository, asset);
    }

    return emptySet();
  }

  private Asset getAsset(final Repository repository, final EntityId assetId) {
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      return storageTx.findAsset(assetId, storageTx.findBucket(repository));
    }
  }

  public boolean canDeleteFolder(final Repository repository, final String path) {
    return maintenanceService.canDeleteFolder(repository, path);
  }

  public void deleteFolder(final Repository repository, final String path) {
    maintenanceService.deleteFolder(repository, path);
  }

  /**
   * Ensures that the action is permitted on at least one asset in the collection via any one of the passed in
   * repositories
   *
   * @return the repository that the user has privilege to see the asset(s) through
   * @throws AuthorizationException
   */
  private String ensurePermissions(
      final Repository repository,
      final Iterable<Asset> assets,
      final String action)
  {
    checkNotNull(repository);
    checkNotNull(assets);
    checkNotNull(action);

    AssetVariableResolver assetVariableResolver = variableResolverAdapterManager.get(repository.getFormat().getValue());

    List<String> repositoryNames = repositoryManager.findContainingGroups(repository.getName());
    repositoryNames.add(0, repository.getName());

    for (Asset asset : assets) {
      VariableSource variableSource = assetVariableResolver.fromAsset(asset);
      String repositoryName =
          getPrivilegedRepositoryName(repositoryNames, repository.getFormat().getValue(), action, variableSource);
      if (repositoryName != null) {
        return repositoryName;
      }
    }

    throw new AuthorizationException();
  }

  private List<AssetXO> createAssetXOs(
      List<Asset> assets,
      Repository repository)
  {
    List<AssetXO> assetXOs = new ArrayList<>();
    for (Asset asset : assets) {
      String privilegedRepositoryName = getPrivilegedRepositoryName(repository, asset);
      assetXOs.add(ASSET_CONVERTER(asset, repository.getName(), privilegedRepositoryName));
    }
    return assetXOs;
  }

  private String getPrivilegedRepositoryName(Repository repository, Asset asset) {
    AssetVariableResolver assetVariableResolver = variableResolverAdapterManager.get(repository.getFormat().getValue());
    VariableSource variableSource = assetVariableResolver.fromAsset(asset);
    Bucket bucket = bucketStore.getById(asset.bucketId());
    if (bucket != null) {
      String assetRepositoryName = bucket.getRepositoryName();
      List<String> repositoryNames = repositoryManager.findContainingGroups(assetRepositoryName);
      repositoryNames.add(0, assetRepositoryName);
      return getPrivilegedRepositoryName(repositoryNames, repository.getFormat().getValue(), BreadActions.BROWSE,
          variableSource);
    }
    return null;
  }

  private String getPrivilegedRepositoryName(
      List<String> repositoryNames,
      String format,
      String action,
      VariableSource variableSource)
  {
    for (String repositoryName : repositoryNames) {
      if (contentPermissionChecker.isPermitted(repositoryName, format, action, variableSource)) {
        return repositoryName;
      }
    }

    return null;
  }
}
