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
package org.sonatype.nexus.cleanup.internal.content.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.internal.content.search.CleanupComponentBrowse;
import org.sonatype.nexus.cleanup.internal.method.CleanupMethod;
import org.sonatype.nexus.cleanup.service.CleanupService;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.task.DeletionProgress;
import org.sonatype.nexus.repository.types.GroupType;

import com.google.common.base.Predicates;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.elasticsearch.search.SearchContextMissingException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * @since 3.29
 */
@Named
@Singleton
public class CleanupServiceImpl
    extends ComponentSupport
    implements CleanupService
{
  public static final String CLEANUP_ATTRIBUTES_KEY = "cleanup";

  public static final String CLEANUP_NAME_KEY = "policyName";

  private final RepositoryManager repositoryManager;

  private final CleanupComponentBrowse browseService;

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final CleanupMethod cleanupMethod;

  private final GroupType groupType;

  private int cleanupRetryLimit;

  @Inject
  public CleanupServiceImpl(final RepositoryManager repositoryManager,
                            final CleanupComponentBrowse browseService,
                            final CleanupPolicyStorage cleanupPolicyStorage,
                            final CleanupMethod cleanupMethod,
                            final GroupType groupType,
                            @Named("${nexus.cleanup.retries:-3}") final int cleanupRetryLimit)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.browseService = checkNotNull(browseService);
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.cleanupMethod = checkNotNull(cleanupMethod);
    this.groupType = checkNotNull(groupType);
    this.cleanupRetryLimit = cleanupRetryLimit;
  }

  @Override
  public void cleanup(final BooleanSupplier cancelledCheck) {
    AtomicLong totalDeletedCount = new AtomicLong(0L);
    repositoryManager.browse().forEach(repository -> {
      if (!cancelledCheck.getAsBoolean() && !repository.getType().equals(groupType)) {
        totalDeletedCount.addAndGet(this.cleanup(repository, cancelledCheck));
      }
    });
    log.info("{} components cleaned up across all repositories", totalDeletedCount.get());
  }

  private Long cleanup(final Repository repository, final BooleanSupplier cancelledCheck) {
    AtomicLong deleted = new AtomicLong(0L);
    findPolicies(repository).forEach(p -> {
      deleted.addAndGet(deleteByPolicy(repository, p, cancelledCheck));
      log.info("{} components cleaned up for repository {} in total", deleted, repository.getName());
    });
    return deleted.get();
  }

  protected Long deleteByPolicy(final Repository repository,
                                final CleanupPolicy policy,
                                final BooleanSupplier cancelledCheck)
  {
    log.info("Deleting components in repository {} using policy {}", repository.getName(), policy.getName());

    DeletionProgress deletionProgress = new DeletionProgress(cleanupRetryLimit);

    if (!policy.getCriteria().isEmpty()) {
      do {
        try {
          Stream<FluentComponent> componentsToDelete = browseService.browse(policy, repository);
          DeletionProgress currentProgress = cleanupMethod.run(repository, componentsToDelete, cancelledCheck);
          deletionProgress.update(currentProgress);
        }
        catch (Exception e) {
          deletionProgress.setAttempts(deletionProgress.getAttempts() + 1);
          deletionProgress.setFailed(true);
          if (ExceptionUtils.getRootCause(e) instanceof SearchContextMissingException) {
            log.warn("Search scroll timed out, continuing with new scrollId.", log.isDebugEnabled() ? e : null);
          }
          else {
            log.error("Failed to delete components.", e);
          }
        }
      } while (!deletionProgress.isFinished());

      if (deletionProgress.isFailed()) {
        log.warn("Deletion attempts exceeded for repository {}", repository.getName());
      }
      return deletionProgress.getComponentCount();
    }
    else {
      log.info("Policy {} has no criteria and will therefore be ignored (i.e. no components will be deleted)",
          policy.getName());
      return 0L;
    }
  }

  @SuppressWarnings("unchecked")
  private List<CleanupPolicy> findPolicies(final Repository repository) {
    List<CleanupPolicy> cleanupPolicies = new ArrayList<>();

    Collection<String> policyNames = Optional.ofNullable(repository.getConfiguration().getAttributes())
        .map(attributes -> attributes.get(CLEANUP_ATTRIBUTES_KEY))
        .map(cleanupAttr -> (Collection<String>) cleanupAttr.get(CLEANUP_NAME_KEY)).orElseGet(Collections::emptySet);

    policyNames.stream().filter(Predicates.notNull()).forEach(policyName -> {
      CleanupPolicy cleanupPolicy = cleanupPolicyStorage.get(policyName);

      if (nonNull(cleanupPolicy)) {
        log.debug("Cleanup policy '{}' found for repository {}", policyName, repository.getName());

        cleanupPolicies.add(cleanupPolicy);
      }
      else {
        log.debug("Cleanup policy '{}' was associated to repository {} but did not exist in storage", policyName,
            repository.getName());
      }
    });

    if (cleanupPolicies.isEmpty()) {
      log.debug("No cleanup policies found for repository {}", repository.getName());
    }

    return cleanupPolicies;
  }
}
