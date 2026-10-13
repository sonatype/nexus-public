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
package org.sonatype.nexus.repository.group.rendered;

import java.time.Duration;

import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Maintenance task that prunes group rendered-view assets whose last_downloaded is older
 * than a configurable horizon (default 14 days), bounding storage.
 * <p>
 * Pruning is not a correctness event — a later request just rebuilds the entry as a miss.
 * Format-neutral naming: iterates group repositories that have a {@link GroupRenderedViewFacet}
 * and calls {@link GroupRenderedViewFacet#pruneRenderedViews(Duration)}.
 */
@Component
@TaskLogging(NEXUS_LOG_ONLY)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupRenderedViewCleanupTask
    extends TaskSupport
    implements Cancelable
{
  private final RepositoryManager repositoryManager;

  @Autowired
  public GroupRenderedViewCleanupTask(final RepositoryManager repositoryManager) {
    this.repositoryManager = requireNonNull(repositoryManager);
  }

  @Override
  protected Void execute() throws Exception {
    int daysHorizon = getConfiguration()
        .getInteger(GroupRenderedViewCleanupTaskDescriptor.DAYS_HORIZON_FIELD_ID, 14);

    Duration unusedFor = Duration.ofDays(daysHorizon);
    int totalPruned = 0;
    int failedRepositories = 0;

    for (Repository repository : repositoryManager.browse()) {
      if (isCanceled()) {
        break;
      }

      var facet = repository.optionalFacet(GroupRenderedViewFacet.class);
      if (facet.isPresent()) {
        // Isolated per repository on purpose. Pruning is independent work per group -- one group's
        // storage being unreadable says nothing about the next group's -- so a single failure must
        // not abort the sweep and leave every repository after it unpruned until the next scheduled
        // run. That would make the storage bound this task exists to enforce depend on iteration
        // order. Cancellation is still a break above, because that is deliberate, not a failure.
        try {
          totalPruned += facet.get().pruneRenderedViews(unusedFor);
        }
        catch (Exception e) {
          failedRepositories++;
          log.warn("Could not prune rendered views for repository {}, continuing with the rest",
              repository.getName(), e);
        }
      }
    }

    if (totalPruned > 0) {
      log.info("Pruned {} total rendered-view assets across all repositories", totalPruned);
    }
    if (failedRepositories > 0) {
      // Surfaced as a task-level summary so a partially-successful run is visible without having to
      // read back through the per-repository warnings.
      log.warn("Rendered-view cleanup failed for {} repository/repositories; see warnings above",
          failedRepositories);
    }

    return null;
  }

  @Override
  public String getMessage() {
    return "Remove unused rendered group views";
  }
}
