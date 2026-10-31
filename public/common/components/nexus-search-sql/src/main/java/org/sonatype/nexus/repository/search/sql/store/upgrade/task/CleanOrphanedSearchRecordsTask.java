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

import java.util.List;
import java.util.function.Predicate;

import org.sonatype.nexus.logging.task.TaskLogType;
import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.sql.store.SearchRepositoryData;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;
import org.sonatype.nexus.scheduling.TaskSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Cleans up orphaned search records.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@TaskLogging(TaskLogType.NEXUS_LOG_ONLY)
public class CleanOrphanedSearchRecordsTask
    extends TaskSupport
{
  public static final String TYPE_ID = "cleanup.orphaned.search.records";

  private final SearchStore searchStore;

  private final RepositoryManager repositoryManager;

  @Autowired
  public CleanOrphanedSearchRecordsTask(
      final SearchStore searchStore,
      final RepositoryManager repositoryManager)
  {
    this.searchStore = requireNonNull(searchStore);
    this.repositoryManager = requireNonNull(repositoryManager);
  }

  @Override
  public String getMessage() {
    return "removing orphaned search records";
  }

  @Override
  protected Object execute() throws Exception {
    log.info("cleaning up orphaned search records...");

    List<SearchRepositoryData> staleRepos = searchStore.getSearchRepositories()
        .stream()
        .filter((Predicate.not(this::repositoryExists)))
        .toList();

    for (SearchRepositoryData staleRepo : staleRepos) {
      log.debug("Removing orphaned search records for stale repository: {} - format {} (id={})",
          staleRepo.repositoryName(),
          staleRepo.repositoryFormat(),
          staleRepo.repositoryId());
      searchStore.deleteAllForRepository(staleRepo.repositoryId(), staleRepo.repositoryFormat());
      searchStore.deleteAllSearchAssets(staleRepo.repositoryId(), staleRepo.repositoryFormat());
    }

    return staleRepos;
  }

  /**
   * Check if repository exists both in repository table and {format}_content_repository table.
   *
   * @param searchRepository the search repository data
   * @return true if repository exists
   */
  private boolean repositoryExists(SearchRepositoryData searchRepository) {
    // check existence with repo manager
    Repository repository = repositoryManager.get(searchRepository.repositoryName());

    if (repository == null) {
      return false;
    }

    // check format, there could be repository with same name but different format
    if (!searchRepository.repositoryFormat().equals(repository.getFormat().getValue())) {
      return false;
    }

    // check id coincides , it's possible repo was deleted and recreated with same name and format but different id
    return searchRepository.repositoryId().equals(repository.facet(ContentFacet.class).contentRepositoryId());
  }
}
