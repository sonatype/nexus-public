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

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.search.SearchFacet;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.shiro.util.CollectionUtils.isEmpty;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * The {@link SearchFacet} implementation for the SQL Table search.
 */
@Named
public class SqlSearchFacetImpl
    extends FacetSupport
    implements SearchFacet
{
  private final SearchTableStore store;

  private final SqlSearchIndexService sqlSearchIndexService;

  private final int batchSize;

  @Inject
  public SqlSearchFacetImpl(
      final SearchTableStore store,
      final SqlSearchIndexService sqlSearchIndexService,
      @Named("${nexus.rebuild.search.batchSize:-500}") final int batchSize)
  {
    this.store = checkNotNull(store);
    this.sqlSearchIndexService = checkNotNull(sqlSearchIndexService);

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

    populateComponents(repository);
  }

  private void populateComponents(final Repository repository) {
    final FluentComponents fluentComponents = facet(ContentFacet.class).components();
    long processed = 0L;
    int totalComponents = fluentComponents.count();
    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      Stopwatch sw = Stopwatch.createStarted();
      Continuation<FluentComponent> components = fluentComponents.browse(batchSize, null);
      while (!isEmpty(components)) {
        checkCancellation();
        sqlSearchIndexService.indexBatch(components, repository);
        processed += components.size();
        progressLogger
            .info("Indexed {} / {} {} components in {}", processed, totalComponents, repository.getName(), sw);

        components = fluentComponents.browse(batchSize, components.nextContinuationToken());
      }
      progressLogger.flush(); // ensure the final progress message is flushed
    }
  }

  @Guarded(by = STARTED)
  @Override
  public void index(final Collection<EntityId> componentIds) {
    log.debug("Indexing..." + componentIds);
    sqlSearchIndexService.index(componentIds, getRepository());
  }

  @Guarded(by = STARTED)
  @Override
  public void purge(final Collection<EntityId> componentIds) {
    log.debug("Purging..." + componentIds);
    sqlSearchIndexService.purge(componentIds, getRepository());
  }
}
