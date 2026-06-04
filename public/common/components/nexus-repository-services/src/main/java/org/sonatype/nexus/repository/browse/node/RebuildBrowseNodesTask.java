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
package org.sonatype.nexus.repository.browse.node;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.spi.TaskResultStateStore;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Browse nodes rebuild task.
 *
 * @since 3.6
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RebuildBrowseNodesTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  private static final String PYPI_FORMAT_NAME = "pypi";

  private final RebuildBrowseNodeService rebuildBrowseNodeService;

  private final TaskResultStateStore taskResultStateStore;

  private final AtomicInteger currentRepositoryIndex = new AtomicInteger(0);

  private final AtomicInteger totalRepositories = new AtomicInteger(0);

  @Autowired
  public RebuildBrowseNodesTask(
      final RebuildBrowseNodeService rebuildBrowseNodeService,
      final TaskResultStateStore taskResultStateStore)
  {
    this.rebuildBrowseNodeService = checkNotNull(rebuildBrowseNodeService);
    this.taskResultStateStore = checkNotNull(taskResultStateStore);
  }

  @Override
  public String getMessage() {
    return "Rebuilding browse tree for " + getRepositoryField();
  }

  @Override
  protected Object execute() throws Exception {
    // Initialize counters for progress tracking (NEXUS-49003)
    currentRepositoryIndex.set(0);
    totalRepositories.set(0);

    // Count total repositories first to provide accurate progress
    String repositoryField = getRepositoryField();
    if (ALL_REPOSITORIES.equals(repositoryField)) {
      // Count all applicable repositories
      for (Repository ignored : getRepositoryManager().browse()) {
        if (appliesTo(ignored)) {
          totalRepositories.incrementAndGet();
        }
      }
    }
    else if (repositoryField.contains(",")) {
      String[] repositoryNames = repositoryField.split(",");
      if (java.util.Arrays.asList(repositoryNames).contains(ALL_REPOSITORIES)) {
        // Count all applicable repositories
        for (Repository ignored : getRepositoryManager().browse()) {
          if (appliesTo(ignored)) {
            totalRepositories.incrementAndGet();
          }
        }
      }
      else {
        for (String repoName : repositoryNames) {
          Repository repo = getRepositoryManager().get(repoName);
          if (appliesTo(repo)) {
            totalRepositories.incrementAndGet();
          }
        }
      }
    }
    else {
      totalRepositories.set(1);
    }

    // Send initial progress
    updateProgress(taskResultStateStore, "0% Complete");

    // Execute the parent's logic which will call execute(Repository) for each repo
    Object result = super.execute();

    // Send completion message only on success
    updateProgress(taskResultStateStore, "100% Complete");

    return result;
  }

  @Override
  protected void execute(final Repository repo) {
    try {
      // Increment the current repository index
      int currentIndex = currentRepositoryIndex.incrementAndGet();

      delayIfPyPi(repo);
      rebuildBrowseNodeService.rebuild(repo, percentageMessage -> {
        // Format: "{repository-name} {percentage} ({num_completed_repo}/{total_repo})"
        String formattedProgress = String.format("%s %s (%d/%d)",
            repo.getName(),
            percentageMessage,
            currentIndex,
            totalRepositories.get());
        updateProgress(taskResultStateStore, formattedProgress);
      });
    }
    catch (RebuildBrowseNodeFailedException e) {
      log.error("Error rebuilding browse nodes for repository: {}", repo, e);
    }
  }

  @SuppressWarnings("java:S2142") // we cannot rethrow exception and we don't want to interrupt the current thread
  private void delayIfPyPi(final Repository repo) {
    if (PYPI_FORMAT_NAME.equals(repo.getFormat().getValue()) && ProxyType.NAME.equals(repo.getType().getValue())) {
      try {
        log.info("Delaying rebuild browse node task for repository {} for 30 seconds", repo);
        TimeUnit.SECONDS.sleep(30);
      }
      catch (InterruptedException e) {
        log.warn("Problem delaying rebuild for PyPI repository: {}", e.getMessage());
      }
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository != null;
  }
}
