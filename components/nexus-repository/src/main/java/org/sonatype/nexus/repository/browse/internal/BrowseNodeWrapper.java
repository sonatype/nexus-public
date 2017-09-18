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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeGenerator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentStore;

import org.apache.commons.collections.CollectionUtils;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps format specific behaviour for browse nodes
 *
 * @since 3.6
 */
@Named
@Singleton
public class BrowseNodeWrapper
{
  private static final String DEFAULT_PATH_HANDLER = "default";

  private final BrowseNodeStore browseNodeStore;

  private final ComponentStore componentStore;

  private final Map<String, BrowseNodeGenerator> pathGenerators;

  private final BrowseNodeGenerator defaultGenerator;

  @Inject
  public BrowseNodeWrapper(final BrowseNodeStore browseNodeStore,
                           final ComponentStore componentStore,
                           final Map<String, BrowseNodeGenerator> pathGenerators)
  {
    this.browseNodeStore = checkNotNull(browseNodeStore);
    this.componentStore = checkNotNull(componentStore);
    this.pathGenerators = checkNotNull(pathGenerators);
    this.defaultGenerator = checkNotNull(pathGenerators.get(DEFAULT_PATH_HANDLER));
  }

  /**
   * Creates the browse nodes used to access an asset and it's component (if it has one).
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

    Map<String, RebuildBrowseNode> rootNodes = new HashMap<>();

    computeNodesForAsset(generator, rootNodes, asset);

    writeNodes(repositoryName, rootNodes, true);
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

    BrowseNodeGenerator generator = pathGenerators.getOrDefault(repository.getFormat().getValue(), defaultGenerator);
    Map<String, RebuildBrowseNode> rootNodes = new LinkedHashMap<>();

    assets.forEach(asset -> computeNodesForAsset(generator, rootNodes, asset));

    writeNodes(repository.getName(), rootNodes, false);
  }

  private void computeNodesForAsset(final BrowseNodeGenerator generator, final Map<String, RebuildBrowseNode> rootNodes, final Asset asset) {
    Component component = asset.componentId() != null ? componentStore.read(asset.componentId()) : null;

    List<String> assetPath = generator.computeAssetPath(asset, component);
    List<String> componentPath = generator.computeComponentPath(asset, component);

    String rootName = assetPath.get(0);

    rootNodes.computeIfAbsent(rootName, s -> new RebuildBrowseNode().withName(rootName));
    RebuildBrowseNode root = rootNodes.get(rootName);
    RebuildBrowseNode assetNode = createNodes(root, assetPath.subList(1, assetPath.size()));
    assetNode.withAssetId(EntityHelper.id(asset));

    if (CollectionUtils.isNotEmpty(componentPath)) {
      RebuildBrowseNode componentNode = createNodes(root, componentPath.subList(1, componentPath.size()));
      componentNode.withComponentId(EntityHelper.id(component));
    }
  }

  private RebuildBrowseNode createNodes(final RebuildBrowseNode root, final List<String> path) {
    RebuildBrowseNode lastNode = root;

    for (String name : path) {
      RebuildBrowseNode parentNode = lastNode;
      parentNode.getChildren()
          .computeIfAbsent(name, s -> new RebuildBrowseNode().withParentNode(parentNode).withName(name));
      lastNode = parentNode.getChildren().get(name);
    }

    return lastNode;
  }

  private void writeNodes(final String repositoryName,
                          final Map<String, RebuildBrowseNode> rootNodes,
                          final boolean updateChildLinks)
  {
    Queue<RebuildBrowseNode> nodes = new LinkedList<>(rootNodes.values());

    while (!nodes.isEmpty()) {
      RebuildBrowseNode node = nodes.remove();
      RebuildBrowseNode parent = node.getParentNode();
      nodes.addAll(node.getChildren().values());

      EntityId parentId = parent != null && parent.getBrowseNode() != null ?
          EntityHelper.id(parent.getBrowseNode()) :
          null;

      BrowseNode browseNode = browseNodeStore.save(
          new BrowseNode().withAssetId(node.getAssetId()).withComponentId(node.getComponentId())
              .withPath(node.getName()).withParentId(parentId).withRepositoryName(repositoryName), updateChildLinks);

      node.withBrowseNode(browseNode);
    }
  }

}
