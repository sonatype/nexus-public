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

import org.sonatype.nexus.blobstore.api.BlobStore
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService

import com.codahale.metrics.health.HealthCheck.Result
import spock.lang.Specification
import spock.lang.Subject


class BlobStoreHealthCheckTest
    extends Specification
{
  BlobStoreManager blobStoreManager = Mock()

  BlobStore blobStore = Mock()

  BlobStoreMetrics blobStoreMetrics = Mock()

  BlobStoreConfiguration blobStoreConfiguration = Mock()

  BlobStoreQuotaService quotaService = Mock()

  BlobStoreQuotaResult quotaResult = Mock()

  @Subject
  BlobStoreHealthCheck blobStoreHealthCheck = new BlobStoreHealthCheck({ -> blobStoreManager}, { -> quotaService })

  def "Healthy response with no BlobStores configured"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      result.healthy
      blobStoreManager.browse() >> []
  }

  def "Healthy response with BlobStore not violating its quota"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      result.healthy
      blobStoreManager.browse() >> [blobStore]
      quotaService.checkQuota(blobStore) >> quotaResult
      quotaResult.violation >> false
      blobStore.blobStoreConfiguration >> blobStoreConfiguration
      blobStoreConfiguration.name >> 'test'
  }

  def "Unhealthy response with BlobStore violating its quota"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      !result.healthy
      blobStoreManager.browse() >> [blobStore]
      quotaService.checkQuota(blobStore) >> quotaResult
      quotaResult.violation >> true
      blobStore.blobStoreConfiguration >> blobStoreConfiguration
      blobStoreConfiguration.name >> 'test'
  }

  def "Healthy response with BlobStore with no quota"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      result.healthy
      blobStoreManager.browse() >> [blobStore]
      quotaService.checkQuota(blobStore) >> null
      blobStore.blobStoreConfiguration >> blobStoreConfiguration
      blobStoreConfiguration.name >> 'test'
  }
}
