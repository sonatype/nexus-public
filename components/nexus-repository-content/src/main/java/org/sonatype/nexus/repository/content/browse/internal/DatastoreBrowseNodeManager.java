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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNodeCrudStore;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.DatastoreBrowseNodeGenerator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages format specific behaviour for browse nodes
 *
 * @since 3.24
 */
@Named("mybatis")
@Singleton
public class DatastoreBrowseNodeManager
    extends ComponentSupport
{
  private final BrowseNodeCrudStore<Asset, Component> browseNodeStore;

  private final Map<String, DatastoreBrowseNodeGenerator> pathGenerators;

  private final DatastoreBrowseNodeGenerator defaultGenerator;

  @Inject
  public DatastoreBrowseNodeManager(
      final BrowseNodeCrudStore<Asset, Component> browseNodeStore,
      final Map<String, DatastoreBrowseNodeGenerator> pathGenerators)
  {
    this.browseNodeStore = checkNotNull(browseNodeStore);
    this.pathGenerators = checkNotNull(pathGenerators);
    this.defaultGenerator = checkNotNull(pathGenerators.get(DefaultDatastoreBrowseNodeGenerator.NAME));
  }

  public void createFromAsset(final Repository repository, final Asset asset) {
    checkNotNull(repository);
    checkNotNull(asset);

    final DatastoreBrowseNodeGenerator generator = pathGenerators.getOrDefault(formatOf(repository), defaultGenerator);
    createBrowseNodes(repository, generator, asset);
  }

  public void createFromAssets(final Repository repository, final Iterable<? extends Asset> assets) {
    checkNotNull(repository);
    checkNotNull(assets);

    final DatastoreBrowseNodeGenerator generator = pathGenerators.getOrDefault(formatOf(repository), defaultGenerator);
    assets.forEach(asset -> createBrowseNodes(repository, generator, asset));
  }

  /**
   * Creates an asset browse node and optional component browse node if the asset has a component.
   */
  private void createBrowseNodes(
      final Repository repository,
      final DatastoreBrowseNodeGenerator generator,
      final Asset asset)
  {
    try {
      List<BrowsePath> assetPaths = generator.computeAssetPaths(asset, asset.component());
      if (!assetPaths.isEmpty()) {
        browseNodeStore.createAssetNode(repository.getName(), formatOf(repository), assetPaths, asset);
      }

      asset.component().ifPresent(component -> {
        List<BrowsePath> componentPaths = generator.computeComponentPaths(asset, component);
        if (!componentPaths.isEmpty()) {
          browseNodeStore.createComponentNode(repository.getName(), formatOf(repository), componentPaths, component);
        }
      });
    }
    catch (RuntimeException e) {
      log.warn("Problem generating browse nodes for {}", asset, e);
    }
  }

  public void maybeCreateFromUpdatedAsset(final Repository repository, final Asset asset)
  {
    checkNotNull(repository);
    checkNotNull(asset);

    if (!asset.blob().isPresent()) {
      log.trace("asset {} has no content, not creating browse node", asset.path());
    }
    else if (browseNodeStore.assetNodeExists(asset)) {
      log.trace("browse node already exists for {} on update", asset.path());
    }
    else {
      log.trace("adding browse node for {} on update", asset.path());
      createFromAsset(repository, asset);
    }
  }

  public void deleteAssetNode(final Asset asset) {
    browseNodeStore.deleteAssetNode(asset);
  }

  public void deleteComponentNode(final Component component) {
    browseNodeStore.deleteComponentNode(component);
  }

  public void deleteByRepository(final Repository repository) {
    log.info("Deleting browse nodes for repository {}", repository.getName());
    browseNodeStore.deleteByRepository(repository.getName());
  }

  private String formatOf(final Repository repository) {
    return repository.getFormat().getValue();
  }
}
