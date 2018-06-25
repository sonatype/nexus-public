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

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreManager

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import org.apache.commons.io.FileUtils

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Inform on the health of all BlobStores based on available disk space and a given tolerance.
 */
@Named("BlobStores")
@Singleton
class BlobStoreHealthCheck
    extends HealthCheck
{
  // in bytes
  private final long minimumAvailableSpace

  private final Provider<BlobStoreManager> blobStoreManagerProvider

  @Inject
  BlobStoreHealthCheck(
      @Named('${nexus.blobstore.healthcheck.minimumAvailableGB:-10}') final long minimumAvailableGB,
      final Provider<BlobStoreManager> blobStoreManagerProvider)
  {
    this.minimumAvailableSpace = FileUtils.ONE_GB * minimumAvailableGB  //convert GB parameter to bytes to ease check
    this.blobStoreManagerProvider = checkNotNull(blobStoreManagerProvider)
  }

  @Override
  protected Result check() throws Exception {
    Collection<String> unhealthyBlobStores = blobStoreManagerProvider.get().browse().findAll { BlobStore blobStore ->
      !blobStore.metrics.isUnlimited() && (blobStore.metrics.availableSpace < minimumAvailableSpace)
    }.collect { BlobStore blobStore -> blobStore.blobStoreConfiguration.name }
    
    String displaySize = FileUtils.byteCountToDisplaySize(minimumAvailableSpace)
    String stores = unhealthyBlobStores.join(',')
    String message = "There are ${unhealthyBlobStores.size()}/${blobStoreManagerProvider.get().browse().size()} " +
        "blob stores reporting less than $displaySize available. ${stores ? '(' + stores + ')' : ''}"

    Result healthCheckResult = unhealthyBlobStores ? Result.unhealthy(message) : Result.healthy(message)
    return healthCheckResult
  }
}
