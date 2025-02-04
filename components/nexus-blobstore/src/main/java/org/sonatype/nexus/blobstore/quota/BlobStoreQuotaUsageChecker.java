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
package org.sonatype.nexus.blobstore.quota;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonatype.nexus.blobstore.quota.BlobStoreQuotaSupport.createQuotaCheckJob;

/**
 * @since 3.41
 */
@Named
public class BlobStoreQuotaUsageChecker
    extends StateGuardLifecycleSupport
{
  protected final PeriodicJobService jobService;

  protected final int quotaCheckInterval;

  protected final BlobStoreQuotaService quotaService;

  protected BlobStore blobStore;

  protected PeriodicJob quotaCheckingJob;

  @Inject
  public BlobStoreQuotaUsageChecker(
      final PeriodicJobService jobService,
      @Named("${nexus.blobstore.quota.warnIntervalSeconds:-60}") final int quotaCheckInterval,
      final BlobStoreQuotaService quotaService)
  {
    this.jobService = checkNotNull(jobService);
    checkArgument(quotaCheckInterval > 0);
    this.quotaCheckInterval = quotaCheckInterval;
    this.quotaService = checkNotNull(quotaService);
  }

  @Override
  protected void doStart() throws Exception {
    jobService.startUsing();
    quotaCheckingJob = jobService.schedule(createQuotaCheckJob(blobStore, quotaService, log), quotaCheckInterval);
  }

  @Override
  protected void doStop() throws Exception {
    blobStore = null;
    quotaCheckingJob.cancel();
    quotaCheckingJob = null;
    jobService.stopUsing();
  }

  public void setBlobStore(final BlobStore blobStore) {
    checkState(this.blobStore == null, "Do not initialize twice");
    checkNotNull(blobStore);
    this.blobStore = blobStore;
  }
}
