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
package org.sonatype.nexus.repository.content.search.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.shiro.util.CollectionUtils.isEmpty;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;

/**
 * The {@link SearchFacet} implementation for the SQL Table search.
 */
@Named
public class SqlSearchIndexFacet
    extends FacetSupport
    implements SearchFacet
{
  private final SearchTableStore store;

  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final int batchSize;

  @Inject
  public SqlSearchIndexFacet(
      final SearchTableStore store,
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      @Named("${nexus.rebuild.search.batchSize:-1000}") final int batchSize)
  {
    this.store = checkNotNull(store);
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);

    checkState(batchSize >= 1, "batchSize should be greater than 1");
    this.batchSize = batchSize;
  }

  @Guarded(by = STARTED)
  @Override
  public void rebuildIndex() {
    Repository repository = getRepository();
    String repositoryFormat = repository.getFormat().getValue();
    String repositoryName = repository.getName();
    log.info("Starting regenerate the search data for the {} repository: {}", repositoryFormat, repositoryName);

    process(repository);

    log.info("Finish regenerating the search data for the {} repository: {}", repositoryFormat, repositoryName);
  }

  private void process(final Repository repository) {
    String repositoryFormat = repository.getFormat().getValue();
    Integer repositoryId = repository.facet(ContentFacet.class).contentRepositoryId();
    log.info("Processing the {} repository: {}", repositoryFormat, repository.getName());

    // delete the old search data
    store.deleteAllForRepository(repositoryId, repositoryFormat);

    AssetStore<?> assetStore = getAssetStore(repositoryFormat);
    long processed = 0L;
    int totalAssets = assetStore.countAssets(repositoryId, null, "component_id is not null", null);
    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      Stopwatch sw = Stopwatch.createStarted();
      Continuation<Asset> assets = assetStore.browseEagerAssets(repositoryId, null, batchSize);
      while (!isEmpty(assets)) {
        List<SearchTableData> searchData = new ArrayList<>(assets.size());
        assets.stream()
            .map(a -> SearchTableDataUtils.convert(a, repository))
            .filter(Optional::isPresent)
            .forEach(searchTableData -> searchData.add(searchTableData.get()));

        store.saveBatch(searchData);
        processed += searchData.size();
        progressLogger.info("Indexed {} / {} {} assets in {}", processed, totalAssets, repository.getName(), sw);

        assets = assetStore.browseEagerAssets(repositoryId, assets.nextContinuationToken(), batchSize);
      }
      progressLogger.flush(); // ensure the final progress message is flushed
    }
  }

  private AssetStore<?> getAssetStore(final String format) {
    FormatStoreManager formatStoreManager = formatStoreManagersByFormat.get(format);
    return formatStoreManager.assetStore(DEFAULT_DATASTORE_NAME);
  }

  @Override
  public void index(final Collection<EntityId> componentIds) {
    // no op (is used for ES only)
  }

  @Override
  public void purge(final Collection<EntityId> componentIds) {
    // no op (is used for ES only)
  }
}
