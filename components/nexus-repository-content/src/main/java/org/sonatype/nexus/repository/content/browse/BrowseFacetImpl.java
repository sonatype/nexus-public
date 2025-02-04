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
package org.sonatype.nexus.repository.content.browse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeDAO;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeData;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeManager;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeStore;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.ossindex.PackageUrlService;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.util.Map.Entry;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Default {@link BrowseFacet} implementation.
 *
 * @since 3.26
 */
@Named
public class BrowseFacetImpl
    extends FacetSupport
    implements BrowseFacet
{
  private static final int COMPONENT_ID_CACHE_SIZE = 10_000;

  // 0.75f is the DEFAULT_LOAD_FACTOR for LinkedHashMap
  private static final float CACHE_MAP_DEFAULT_LOAD_FACTOR = 0.75f;

  // Telling LinkedHashMap to retain order an act as an LRU
  private static final boolean CACHE_MAP_RETAIN_ACCESS_ORDER = true;

  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final Map<String, BrowseNodeGenerator> browseNodeGeneratorsByFormat;

  private final PackageUrlService packageUrlService;

  private final int pageSize;

  private String format;

  private BrowseNodeGenerator browseNodeGenerator;

  private BrowseNodeManager browseNodeManager;

  @Inject
  public BrowseFacetImpl(
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      final Map<String, BrowseNodeGenerator> browseNodeGeneratorsByFormat,
      final PackageUrlService packageUrlService,
      @Named("${nexus.browse.rebuild.pageSize:-1000}") final int pageSize)
  {
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);
    this.browseNodeGeneratorsByFormat = checkNotNull(browseNodeGeneratorsByFormat);
    this.packageUrlService = checkNotNull(packageUrlService);
    this.pageSize = max(pageSize, 1);
  }

  @Override
  protected void doStart() throws Exception {
    ContentFacetSupport contentFacet = (ContentFacetSupport) getRepository().facet(ContentFacet.class);

    format = getRepository().getFormat().getValue();
    String storeName = contentFacet.stores().contentStoreName;
    int repositoryId = contentFacet.contentRepositoryId();

    BrowseNodeStore<BrowseNodeDAO> browseNodeStore =
        lookupFormatStoreManager(format).formatStore(storeName, BrowseNodeDAO.class);

    browseNodeGenerator = lookupBrowseNodeGenerator(format);
    browseNodeManager = new BrowseNodeManager(browseNodeStore, repositoryId);
  }

  @Guarded(by = STARTED)
  @Override
  public List<BrowseNode> getByDisplayPath(
      final List<String> displayPath,
      final int limit,
      final String filter,
      final Map<String, Object> filterParams)
  {
    return browseNodeManager.getByDisplayPath(displayPath, limit, filter, filterParams);
  }

  @Guarded(by = STARTED)
  @Override
  public void addPathsToAssets(Collection<EntityId> assetIds) {
    FluentAssets lookup = facet(ContentFacet.class).assets();

    Map<Integer, Integer> componentsProcessed = newComponentCache();

    assetIds.stream()
        .map(lookup::find)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(fluentAsset -> !browseNodeManager.hasAssetNode(fluentAsset))
        .forEach(fluentAsset -> createBrowseNodes(fluentAsset, componentsProcessed));
  }

  @Guarded(by = STARTED)
  @Override
  public void trimBrowseNodes() {
    browseNodeManager.trimBrowseNodes();
  }

  @Guarded(by = STARTED)
  @Override
  public void rebuildBrowseNodes(final Consumer<String> progressUpdater) {
    log.info("Deleting browse nodes for repository {}", getRepository().getName());

    browseNodeManager.deleteBrowseNodes();

    log.info("Rebuilding browse nodes for repository {}", getRepository().getName());

    createAllBrowseNodes(progressUpdater);
  }

  @Guarded(by = STARTED)
  @Override
  public void deleteByAssetIdAndPath(
      final Integer internalAssetId,
      final String path)
  {
    log.debug("Deleting browse nodes for repository = {} and asset path = {}", getRepository().getName(), path);
    Long parentNodeId = browseNodeManager.deleteByAssetIdAndPath(internalAssetId, path);
    log.debug("Deleted browse node for path = '{}' - Returned parent node {}", path, parentNodeId);

    if (parentNodeId != null) {
      List<BrowseNode> parentNodes = browseNodeManager.getNodeParents(parentNodeId);
      for (BrowseNode parentNode : parentNodes) {
        if (parentNode == null) {
          continue;
        }
        long assetCount = parentNode.getAssetCount() == null ? 0L : parentNode.getAssetCount();
        // Once a node has assets, we need to stop the node deletion
        if (assetCount > 0) {
          break;
        }

        BrowseNodeData nodeToDelete = (BrowseNodeData) parentNode;

        // If the node has no assets, check if it has children with assets or components
        boolean hasChildren = browseNodeManager.hasAnyAssetOrComponentChildren(nodeToDelete.getNodeId());
        if (hasChildren) {
          break;
        }

        log.debug("Deleting node with id = {} and path = '{}'", nodeToDelete.getNodeId(), nodeToDelete.getPath());
        deleteByNodeId(nodeToDelete.getNodeId());
      }
    }
  }

  @Override
  public void deleteByNodeId(final Long nodeId) {
    log.debug("Deleting browse node for repository = {} and node id = {}", getRepository().getName(), nodeId);
    browseNodeManager.delete(nodeId);
  }

  @Override
  public Optional<BrowseNode> getByRequestPath(final String requestPath) {
    return Optional.ofNullable(browseNodeManager.getByRequestPath(requestPath));
  }

  /**
   * Create browse nodes for every asset and their components in the repository.
   */
  private void createAllBrowseNodes(final Consumer<String> progressUpdater) {
    String repositoryName = getRepository().getName();
    try {
      FluentAssets assets = getRepository().facet(ContentFacet.class).assets();

      long total = assets.count();
      if (total > 0) {
        // useful for formats that have multiple assets per component
        Map<Integer, Integer> processedComponents = newComponentCache();
        ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
        Stopwatch sw = Stopwatch.createStarted();

        long processed = 0;

        Continuation<FluentAsset> page = assets.browse(pageSize, null);
        while (!page.isEmpty()) {
          page.forEach(fluentAsset -> createBrowseNodes(fluentAsset, processedComponents));
          processed += page.size();

          long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
          progressLogger.info("Processed {} / {} {} assets in {} ms",
              processed, total, repositoryName, elapsed);
          if (progressUpdater != null) {
            long percentageComplete = BigDecimal.valueOf(processed)
                .divide(BigDecimal.valueOf(total),
                    2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
            progressUpdater.accept(
                String.format("%d%% Complete", percentageComplete));
          }

          checkCancellation();

          page = assets.browse(pageSize, page.nextContinuationToken());
        }

        progressLogger.flush(); // ensure the final progress message is flushed
      }
    }
    catch (Exception e) {
      log.error("Unable to rebuild browse nodes for repository {}", repositoryName, e);
    }
  }

  /**
   * Create browse nodes for an asset and it's component. Using a cache of component ids to limit component
   * nodes being recreated
   */
  private void createBrowseNodes(final FluentAsset asset, final Map<Integer, Integer> componentsProcessed) {
    if (browseNodeGenerator.hasMultipleAssetsPerComponent()) {
      createAssetBrowseNodes(asset);
      asset.component().ifPresent(component -> createComponentBrowseNodes(asset, component, componentsProcessed));
    }
    else {
      createCombinedAssetAndComponentBrowseNodes(asset);
    }
  }

  /**
   * Create browse nodes for each segment in an asset's path, assigning the asset to the final node
   */
  private void createAssetBrowseNodes(final FluentAsset asset) {
    List<BrowsePath> assetPaths = browseNodeGenerator.computeAssetPaths(asset);
    if (!assetPaths.isEmpty()) {
      browseNodeManager.createBrowseNodes(assetPaths, node -> node.setAsset(asset));
    }
  }

  /**
   * Create browse nodes for each segment in an asset's component's path, assigning the component to the final node,
   * if the asset has a component
   */
  private void createComponentBrowseNodes(
      final FluentAsset asset,
      final Component component,
      final Map<Integer, Integer> componentsProcessed)
  {
    Integer internalComponentId = internalComponentId(component);
    // null will be returned when adding a key that isn't already in the cache
    if (componentsProcessed.put(internalComponentId, internalComponentId) == null) {
      List<BrowsePath> componentPaths = browseNodeGenerator.computeComponentPaths(asset);
      if (!componentPaths.isEmpty()) {
        browseNodeManager.createBrowseNodes(componentPaths, node -> {
          node.setComponent(component);
          findPackageUrl(component).map(PackageUrl::toString).ifPresent(node::setPackageUrl);
        });
      }
    }
  }

  /**
   * Create browse nodes for each segment in the asset's path, and assign the asset and component to the final node
   */
  private void createCombinedAssetAndComponentBrowseNodes(final FluentAsset asset) {
    List<BrowsePath> assetPaths = browseNodeGenerator.computeAssetPaths(asset);
    if (!assetPaths.isEmpty()) {
      browseNodeManager.createBrowseNodes(assetPaths, node -> {
        node.setAsset(asset);
        asset.component().ifPresent(component -> {
          node.setComponent(component);
          findPackageUrl(component).map(PackageUrl::toString).ifPresent(node::setPackageUrl);
        });
      });
    }
  }

  /**
   * Finds the optional {@link PackageUrl} coordinates for the given component.
   */
  private Optional<PackageUrl> findPackageUrl(final Component component) {
    return packageUrlService.getPackageUrl(format, component.namespace(), component.name(), component.version());
  }

  /**
   * Looks for the {@link FormatStoreManager} to use for the given repository format.
   */
  private FormatStoreManager lookupFormatStoreManager(final String format) {
    FormatStoreManager storeManager = formatStoreManagersByFormat.get(format);
    checkState(storeManager != null, "Could not find a store manager for format: %s", format);
    return storeManager;
  }

  /**
   * Looks for the {@link BrowseNodeGenerator} to use for the given repository format.
   */
  private BrowseNodeGenerator lookupBrowseNodeGenerator(final String format) {
    BrowseNodeGenerator generator = browseNodeGeneratorsByFormat.get(format);
    if (generator == null) {
      generator = browseNodeGeneratorsByFormat.get("default");
    }
    checkState(generator != null, "Could not find a browse node generator for format: %s", format);
    return generator;
  }

  private Map<Integer, Integer> newComponentCache() {
    return new LinkedHashMap<Integer, Integer>(COMPONENT_ID_CACHE_SIZE, CACHE_MAP_DEFAULT_LOAD_FACTOR,
        CACHE_MAP_RETAIN_ACCESS_ORDER)
    {
      @Override
      protected boolean removeEldestEntry(final Entry eldest) {
        return size() > COMPONENT_ID_CACHE_SIZE;
      }
    };
  }
}
