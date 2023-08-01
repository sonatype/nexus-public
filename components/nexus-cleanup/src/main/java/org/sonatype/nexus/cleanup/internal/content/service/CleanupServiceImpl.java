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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.internal.CleanupFeatureCheck;
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

  public static final String DEFAULT_CLEANUP_BROWSE_NAME = "DataStoreCleanupComponentBrowse";

  public static final String COMPONENT_SET_CLEANUP_BROWSE_NAME = "ComponentSetCleanupComponentBrowse";

  private final RepositoryManager repositoryManager;

  private final Map<String, CleanupComponentBrowse> browseServices;

  private final CleanupPolicyStorage cleanupPolicyStorage;

  private final CleanupMethod cleanupMethod;

  private final GroupType groupType;

  private int cleanupRetryLimit;

  private CleanupFeatureCheck featureCheck;

  @Inject
  public CleanupServiceImpl(final RepositoryManager repositoryManager,
                            final Map<String, CleanupComponentBrowse> browseServices,
                            final CleanupPolicyStorage cleanupPolicyStorage,
                            final CleanupMethod cleanupMethod,
                            final GroupType groupType,
                            @Named("${nexus.cleanup.retries:-3}") final int cleanupRetryLimit,
                            final CleanupFeatureCheck featureChecks)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.browseServices = checkNotNull(browseServices);
    this.cleanupMethod = checkNotNull(cleanupMethod);
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
    this.groupType = checkNotNull(groupType);
    this.cleanupRetryLimit = cleanupRetryLimit;
    this.featureCheck = checkNotNull(featureChecks);
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
    findPolicies(repository).forEach(policy -> {
      CleanupComponentBrowse browseService = selectBrowseService(repository, policy.getCriteria());
      deleted.addAndGet(deleteByPolicy(repository, policy, cancelledCheck, browseService));
      log.info("{} components cleaned up for repository {} in total", deleted, repository.getName());
    });
    return deleted.get();
  }

  private CleanupComponentBrowse selectBrowseService(final Repository repository, Map<String, String> criteria) {
    String serviceName = DEFAULT_CLEANUP_BROWSE_NAME;
    if (this.featureCheck.isRetainSupported(repository.getFormat().getValue(), criteria)) {
      serviceName = COMPONENT_SET_CLEANUP_BROWSE_NAME;
    }
    CleanupComponentBrowse browseService = browseServices.get(serviceName);
    if (browseService==null) {
      log.error("Missing Cleanup Component Browse service: {}", serviceName);
      throw new IllegalStateException("Missing Cleanup Component Browse service");
    }
    return browseService;
  }

  protected Long deleteByPolicy(final Repository repository,
                                final CleanupPolicy policy,
                                final BooleanSupplier cancelledCheck,
                                CleanupComponentBrowse browseService)
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
