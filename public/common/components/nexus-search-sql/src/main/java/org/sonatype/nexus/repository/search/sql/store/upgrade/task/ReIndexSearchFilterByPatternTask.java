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
package org.sonatype.nexus.repository.search.sql.store.upgrade.task;

import java.util.Collections;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.sql.index.SqlSearchIndexService;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.base.Stopwatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Task that re-indexes search records for components matching a configurable SQL filter pattern.
 * This task is generic and can be used for various re-indexing scenarios by providing different filter conditions.
 *
 * Example filter conditions:
 * - "namespace LIKE '%_%' OR version LIKE '%_%'" for components with underscores
 * - "normalized_version IS NULL" for components without normalized version
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReIndexSearchFilterByPatternTask
    extends TaskSupport
    implements Cancelable
{
  public static final String TYPE_ID = "reindex.search.records.filtered.by.pattern";

  public static final String FILTER_CONDITION_FIELD_ID = "filterCondition";

  private static final int DEFAULT_BATCH_SIZE = 500;

  private final RepositoryManager repositoryManager;

  private final SqlSearchIndexService sqlSearchIndexService;

  private final int batchSize;

  @Autowired
  public ReIndexSearchFilterByPatternTask(
      final RepositoryManager repositoryManager,
      final SqlSearchIndexService sqlSearchIndexService,
      @Value("${nexus.reindex.search.by.pattern.batchSize:500}") final int batchSize)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.sqlSearchIndexService = checkNotNull(sqlSearchIndexService);
    this.batchSize = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
  }

  @Override
  protected Object execute() throws Exception {
    String filterCondition = getFilterCondition();
    if (filterCondition == null || filterCondition.isBlank()) {
      log.warn("No filter condition provided, skipping re-index task");
      return null;
    }

    log.info("Starting re-index task with filter condition: {}", filterCondition);

    long totalProcessed = 0;
    Stopwatch totalStopwatch = Stopwatch.createStarted();

    for (Repository repository : repositoryManager.browse()) {
      checkCancellation();

      long processed = processRepository(repository, filterCondition);
      totalProcessed += processed;
    }

    log.info("Completed re-index task. Total components processed: {} in {}", totalProcessed, totalStopwatch);
    return totalProcessed;
  }

  private long processRepository(final Repository repository, final String filterCondition) {
    try {
      FluentComponents fluentComponents = repository.facet(ContentFacet.class).components();
      FluentQuery<FluentComponent> filteredQuery = fluentComponents.byFilter(filterCondition, Collections.emptyMap());

      return reindexFilteredComponents(repository, filteredQuery);
    }
    catch (Exception e) {
      log.error("Error processing repository {}: {}", repository.getName(), e.getMessage(), e);
      return 0;
    }
  }

  private long reindexFilteredComponents(
      final Repository repository,
      final FluentQuery<FluentComponent> filteredQuery)
  {
    long processed = 0;

    try (ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60)) {
      Stopwatch sw = Stopwatch.createStarted();
      Continuation<FluentComponent> components = filteredQuery.browse(batchSize, null);

      if (components.isEmpty()) {
        log.info("No components matching filter in repository {}", repository.getName());
        return 0;
      }

      while (!components.isEmpty()) {
        checkCancellation();

        sqlSearchIndexService.indexBatch(components, repository);
        processed += components.size();

        progressLogger.info("Re-indexed {} components in {} ({})",
            processed, repository.getName(), sw);

        components = filteredQuery.browse(batchSize, components.nextContinuationToken());
      }

      log.info("Completed re-indexing {} components for repository {} ({})", processed, repository.getName(), sw);
    }

    return processed;
  }

  private String getFilterCondition() {
    return getConfiguration().getString(FILTER_CONDITION_FIELD_ID);
  }

  @Override
  public String getMessage() {
    String filterCondition = getFilterCondition();
    if (filterCondition != null && !filterCondition.isBlank()) {
      return "Re-indexing search records matching filter: " + filterCondition;
    }
    return "Re-indexing search records by pattern";
  }
}
