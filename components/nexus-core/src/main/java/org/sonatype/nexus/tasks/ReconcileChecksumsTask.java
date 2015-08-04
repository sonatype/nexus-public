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
package org.sonatype.nexus.tasks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ChecksumReconciler;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.walker.FixedRateWalkerThrottleController;
import org.sonatype.nexus.proxy.walker.WalkerThrottleController;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesPathAwareTask;
import org.sonatype.nexus.tasks.descriptors.ReconcileChecksumsTaskDescriptor;
import org.sonatype.nexus.util.LinearNumberSequence;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reconcile checksums task.
 *
 * @since 2.11.3
 */
@Named(ReconcileChecksumsTaskDescriptor.ID)
public class ReconcileChecksumsTask
    extends AbstractNexusRepositoriesPathAwareTask<Object>
{
  /**
   * System event action: reconcileChecksums
   */
  public static final String ACTION = "RECONCILECHECKSUMS";

  private final ChecksumReconciler checksumReconciler;

  @Inject
  public ReconcileChecksumsTask(final ChecksumReconciler checksumReconciler) {
    this.checksumReconciler = checkNotNull(checksumReconciler);
  }

  @Override
  protected String getRepositoryFieldId() {
    return ReconcileChecksumsTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected String getRepositoryPathFieldId() {
    return ReconcileChecksumsTaskDescriptor.RESOURCE_STORE_PATH_FIELD_ID;
  }

  public long getModifiedSinceMillis() {
    final String value = getParameter(ReconcileChecksumsTaskDescriptor.MODIFIED_SINCE_DATE_ID);
    return StringUtils.isNotBlank(value) ? DateTime.parse(value).getMillis() : -1;
  }

  public void setModifiedSinceDate(final String modifiedSinceDate) {
    addParameter(ReconcileChecksumsTaskDescriptor.MODIFIED_SINCE_DATE_ID, modifiedSinceDate);
  }

  public int getWalkingLimitTps() {
    final String value = getParameter(ReconcileChecksumsTaskDescriptor.WALKING_LIMIT_TPS_FIELD_ID);
    return StringUtils.isNotBlank(value) ? Integer.parseInt(value) : -1;
  }

  public void setWalkingLimitTps(final int walkingLimitTps) {
    addParameter(ReconcileChecksumsTaskDescriptor.WALKING_LIMIT_TPS_FIELD_ID, Integer.toString(walkingLimitTps));
  }

  @Override
  public Object doRun() throws Exception {
    final List<Repository> targetRepositories = new ArrayList<>();

    if (getRepositoryId() != null) {
      // determine if we've been pointed to a group, as then we want to process its non-group members
      final Repository selectedRepository = getRepositoryRegistry().getRepository(getRepositoryId());
      final GroupRepository groupRepository = selectedRepository.adaptToFacet(GroupRepository.class);
      if (groupRepository == null) {
        targetRepositories.add(selectedRepository);
      }
      else {
        targetRepositories.addAll(groupRepository.getTransitiveMemberRepositories());
      }
    }
    else {
      // 'all repos' case, ignore groups as their members are already included
      for (final Repository repo : getRepositoryRegistry().getRepositories()) {
        if (repo.adaptToFacet(GroupRepository.class) == null) {
          targetRepositories.add(repo);
        }
      }
    }

    final ResourceStoreRequest request = new ResourceStoreRequest(getResourceStorePath(), true, false);

    final int limitTps = getWalkingLimitTps();
    if (limitTps > 0) {
      request.getRequestContext().put(WalkerThrottleController.CONTEXT_KEY,
          new FixedRateWalkerThrottleController(limitTps, new LinearNumberSequence(0, 1, 1, 0)));
    }

    for (final Repository repo : targetRepositories) {
      checksumReconciler.reconcileChecksums(repo, request, getModifiedSinceMillis());
    }

    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    String message;
    if (getRepositoryId() != null) {
      message = "Reconciling checksums of repository " + getRepositoryName();
    }
    else {
      message = "Reconciling checksums of all registered repositories";
    }
    return message + " from path " + getResourceStorePath() + " and below.";
  }

}
