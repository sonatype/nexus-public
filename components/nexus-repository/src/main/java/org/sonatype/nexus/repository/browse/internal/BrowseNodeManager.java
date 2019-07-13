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
package org.sonatype.nexus.repository.browse.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentStore;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages format specific behaviour for browse nodes
 *
 * @since 3.7
 */
@Named
@Singleton
public class BrowseNodeManager
    extends ComponentSupport
{
  private static final String DEFAULT_PATH_HANDLER = "default";

  private final BrowseNodeStore browseNodeStore;

  private final ComponentStore componentStore;

  private final Map<String, BrowseNodeGenerator> pathGenerators;

  private final BrowseNodeGenerator defaultGenerator;

  @Inject
  public BrowseNodeManager(final BrowseNodeStore browseNodeStore,
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
  private void createBrowseNodes(final String repositoryName,
                                 final String format,
                                 final BrowseNodeGenerator generator,
                                 final Asset asset)
  {
    try {
      Component component = asset.componentId() != null ? componentStore.read(asset.componentId()) : null;

      List<BrowsePaths> assetPaths = generator.computeAssetPaths(asset, component);
      if (!assetPaths.isEmpty()) {
        browseNodeStore.createAssetNode(repositoryName, format, assetPaths, asset);
      }

      if (component != null) {
        List<BrowsePaths> componentPaths = generator.computeComponentPaths(asset, component);
        if (!componentPaths.isEmpty()) {
          browseNodeStore.createComponentNode(repositoryName, format, componentPaths, component);
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
  public void deleteAssetNode(final EntityId assetId) {
    browseNodeStore.deleteAssetNode(assetId);
  }

  /**
   * Deletes the component's browse node.
   */
  public void deleteComponentNode(final EntityId componentId) {
    browseNodeStore.deleteComponentNode(componentId);
  }

  /**
   * Deletes all browse nodes belonging to the given repository.
   */
  public void deleteByRepository(final String repositoryName) {
    browseNodeStore.deleteByRepository(repositoryName);
  }
}
