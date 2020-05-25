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
package org.sonatype.nexus.repository.browse.internal.orient;

import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.node.BrowseNodeCrudStore;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentStore;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages format specific behaviour for browse nodes
 *
 * @since 3.7
 */
@Named("orient")
@Priority(Integer.MAX_VALUE)
@Singleton
public class OrientBrowseNodeManager
    extends ComponentSupport
{
  private static final String DEFAULT_PATH_HANDLER = "default";

  private final BrowseNodeCrudStore<Asset, Component> browseNodeStore;

  private final ComponentStore componentStore;

  private final Map<String, BrowseNodeGenerator> pathGenerators;

  private final BrowseNodeGenerator defaultGenerator;

  @Inject
  public OrientBrowseNodeManager(
      final BrowseNodeCrudStore<Asset, Component> browseNodeStore,
      final ComponentStore componentStore,
      final Map<String, BrowseNodeGenerator> pathGenerators)
  {
    this.browseNodeStore = checkNotNull(browseNodeStore);
    this.componentStore = checkNotNull(componentStore);
    this.pathGenerators = checkNotNull(pathGenerators);
    this.defaultGenerator = checkNotNull(pathGenerators.get(DEFAULT_PATH_HANDLER));
  }

  /**
   * Creates the browse nodes used to access an asset and its component (if it has one).
   *
   * @param repositoryName of the repository that the asset is stored in
   * @param asset          that needs to be accessible from the browse nodes
   * @see BrowseNodeGenerator#computeAssetPath(Asset, Component) for details on the default behavior used to compute the asset path
   * @see BrowseNodeGenerator#computeComponentPath(Asset, Component) for details on the default behavior used to compute the component path
   */
  public void createFromAsset(final String repositoryName, final Asset asset) {
    checkNotNull(repositoryName);
    checkNotNull(asset);

    BrowseNodeGenerator generator = pathGenerators.getOrDefault(asset.format(), defaultGenerator);
    createBrowseNodes(repositoryName, asset.format(), generator, asset);
  }

  /**
   * Creates the browse nodes if they don't already exist.  This handles the case where a format creates contentless
   * metadata without browse nodes and later updates the asset with content.
   *
   * @param repositoryName of the repository that the asset is stored in
   * @param assetId        of the existing asset
   * @param asset          that needs to be accessible from the browse nodes
   */
  public void maybeCreateFromUpdatedAsset(final String repositoryName, final EntityId assetId, final Asset asset) {
    checkNotNull(assetId);

    if (asset.blobRef() == null) {
      log.trace("asset {} has no content, not creating browse node", assetId);
    }
    else if (browseNodeStore.assetNodeExists(asset)) {
      log.trace("browse node already exists for {} on update", assetId);
    }
    else {
      log.trace("adding browse node for {} on update", assetId);
      createFromAsset(repositoryName, asset);
    }
  }

  /**
   * Creates the browse nodes used to access a collection of assets and their components (if they have one).
   *
   * @param repository storing the assets
   * @param assets     which need to be accessible from the browse nodes
   * @see BrowseNodeGenerator#computeAssetPath(Asset, Component) for details on the default behavior used to compute the asset path
   * @see BrowseNodeGenerator#computeComponentPath(Asset, Component) for details on the default behavior used to compute the component path
   */
  public void createFromAssets(final Repository repository, final Iterable<Asset> assets) {
    checkNotNull(repository);
    checkNotNull(assets);

    String repositoryName = repository.getName();
    BrowseNodeGenerator generator = pathGenerators.getOrDefault(repository.getFormat().getValue(), defaultGenerator);
    assets.forEach(asset -> createBrowseNodes(repositoryName, repository.getFormat().getValue(), generator, asset));
  }

  /**
   * Creates an asset browse node and optional component browse node if the asset has a component.
   */
  @SuppressWarnings("unchecked")
  private void createBrowseNodes(final String repositoryName,
                                 final String format,
                                 final BrowseNodeGenerator generator,
                                 final Asset asset)
  {
    try {
      Component component = asset.componentId() != null ? componentStore.read(asset.componentId()) : null;

      List<? extends BrowsePath> assetPaths = generator.computeAssetPaths(asset, component);
      if (!assetPaths.isEmpty()) {
        browseNodeStore.createAssetNode(repositoryName, format, (List<BrowsePath>) assetPaths, asset);
      }

      if (component != null) {
        List<? extends BrowsePath> componentPaths = generator.computeComponentPaths(asset, component);
        if (!componentPaths.isEmpty()) {
          browseNodeStore.createComponentNode(repositoryName, format, (List<BrowsePath>) componentPaths, component);
        }
      }
    }
    catch (RuntimeException e) {
      log.warn("Problem generating browse nodes for {}", asset, e);
    }
  }

  /**
   * Deletes the asset's browse node.
   */
  public void deleteAssetNode(final Asset asset) {
    browseNodeStore.deleteAssetNode(asset);
  }

  /**
   * Deletes the component's browse node.
   */
  public void deleteComponentNode(final Component component) {
    browseNodeStore.deleteComponentNode(component);
  }

  /**
   * Deletes all browse nodes belonging to the given repository.
   */
  public void deleteByRepository(final String repositoryName) {
    browseNodeStore.deleteByRepository(repositoryName);
  }
}
