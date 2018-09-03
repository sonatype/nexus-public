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
package org.sonatype.nexus.repository.internal.blobstore

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Inform on the health of all BlobStores based on their configured soft quota.
 */
@Named("BlobStores")
@Singleton
class BlobStoreHealthCheck
    extends HealthCheck
{
  private final Provider<BlobStoreManager> blobStoreManagerProvider

  private final Provider<BlobStoreQuotaService> quotaServiceProvider

  @Inject
  BlobStoreHealthCheck(final Provider<BlobStoreManager> blobStoreManagerProvider,
                       final Provider<BlobStoreQuotaService> quotaServiceProvider)
  {
    this.blobStoreManagerProvider = checkNotNull(blobStoreManagerProvider)
    this.quotaServiceProvider = checkNotNull(quotaServiceProvider)
  }

  @Override
  protected Result check() throws Exception {
    Collection<String> unhealthyBlobStores = blobStoreManagerProvider.get().browse()
        .findAll { quotaServiceProvider.get().checkQuota(it)?.violation }
        .collect { it.getBlobStoreConfiguration().getName() }

    String stores = unhealthyBlobStores.join(',')
    String message = "There are ${unhealthyBlobStores.size()}/${blobStoreManagerProvider.get().browse().size()} " +
        "blob stores violating their quota. ${stores ? 'Violating blob stores:(' + stores + ')' : ''}"

    Result healthCheckResult = unhealthyBlobStores ? Result.unhealthy(message) : Result.healthy(message)
    return healthCheckResult
  }
}
