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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeDAO;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeManager;
import org.sonatype.nexus.repository.content.browse.store.BrowseNodeStore;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Default {@link BrowseFacet} implementation.
 *
 * @since 3.next
 */
@Named
public class BrowseFacetImpl
    extends FacetSupport
    implements BrowseFacet
{
  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final Map<String, BrowseNodeGenerator> browseNodeGeneratorsByFormat;

  private final int pageSize;

  private BrowseNodeGenerator browseNodeGenerator;

  private BrowseNodeManager browseNodeManager;

  @Inject
  public BrowseFacetImpl(final Map<String, FormatStoreManager> formatStoreManagersByFormat,
                         final Map<String, BrowseNodeGenerator> browseNodeGeneratorsByFormat,
                         @Named("${nexus.browse.rebuild.pageSize:-1000}") final int pageSize)
  {
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);
    this.browseNodeGeneratorsByFormat = checkNotNull(browseNodeGeneratorsByFormat);
    this.pageSize = max(pageSize, 1);
  }

  @Override
  protected void doStart() throws Exception {
    ContentFacetSupport contentFacet = (ContentFacetSupport) getRepository().facet(ContentFacet.class);

    String format = getRepository().getFormat().getValue();
    String storeName = contentFacet.stores().contentStoreName;
    int repositoryId = contentFacet.contentRepositoryId();

    BrowseNodeStore<BrowseNodeDAO> browseNodeStore =
        lookupFormatStoreManager(format).formatStore(storeName, BrowseNodeDAO.class);

    browseNodeGenerator = lookupBrowseNodeGenerator(format);
    browseNodeManager = new BrowseNodeManager(browseNodeStore, repositoryId);
  }

  @Guarded(by = STARTED)
  public void rebuildBrowseNodes() {
    log.info("Deleting browse nodes for repository {}", getRepository().getName());

    browseNodeManager.deleteBrowseNodes();

    log.info("Rebuilding browse nodes for repository {}", getRepository().getName());

    createBrowseNodes();
  }

  /**
   * Create browse nodes for every asset and their components in the repository.
   */
  private void createBrowseNodes() {
    String repositoryName = getRepository().getName();
    try {
      FluentAssets assets = getRepository().facet(ContentFacet.class).assets();

      long total = assets.count();
      if (total > 0) {
        ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
        Stopwatch sw = Stopwatch.createStarted();

        long processed = 0;

        Continuation<FluentAsset> page = assets.browse(pageSize, null);
        while (!page.isEmpty()) {

          page.forEach(this::createBrowseNodes);
          processed += page.size();

          long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
          progressLogger.info("Processed {} / {} {} assets in {} ms",
              processed, total, repositoryName, elapsed);

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

  private void createBrowseNodes(final FluentAsset asset) {
    List<BrowsePath> assetPaths = browseNodeGenerator.computeAssetPaths(asset);
    if (!assetPaths.isEmpty()) {
      browseNodeManager.createBrowseNodes(assetPaths, node -> node.setAsset(asset));
    }

    if (asset.component().isPresent()) {
      List<BrowsePath> componentPaths = browseNodeGenerator.computeComponentPaths(asset);
      if (!componentPaths.isEmpty()) {
        browseNodeManager.createBrowseNodes(componentPaths, node -> node.setComponent(asset.component().get()));
      }
    }
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
}
