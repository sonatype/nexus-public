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
import org.sonatype.nexus.blobstore.file.FileBlobStoreConfigurationBuilder
import org.sonatype.nexus.common.atlas.SystemInformationGenerator

import com.codahale.metrics.health.HealthCheck.Result
import org.apache.commons.io.FileUtils
import spock.lang.Specification
import spock.lang.Subject


class BlobStoreHealthCheckTest
    extends Specification
{
  static final long MINIMUM_SIZE = 1

  BlobStoreManager blobStoreManager = Mock()

  BlobStore blobStore = Mock()

  BlobStoreMetrics blobStoreMetrics = Mock()

  BlobStoreConfiguration blobStoreConfiguration = Mock()

  @Subject
  BlobStoreHealthCheck blobStoreHealthCheck = new BlobStoreHealthCheck(MINIMUM_SIZE, blobStoreManager)

  def "Healthy response with no BlobStores configured"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      result.healthy
      blobStoreManager.browse() >> []
  }

  def "Healthy response with BlobStore reporting enough available space"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      result.healthy
      blobStoreManager.browse() >> [blobStore]
      blobStore.metrics >> blobStoreMetrics
      blobStoreMetrics.isUnlimited() >> false
      blobStoreMetrics.availableSpace >> FileUtils.ONE_GB
      blobStore.blobStoreConfiguration >> blobStoreConfiguration
      blobStoreConfiguration.name >> 'test'
  }

  def "Healthy response with BlobStore reporting unlimited space"() {
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      result.healthy
      blobStoreManager.browse() >> [blobStore]
      blobStore.metrics >> blobStoreMetrics
      blobStoreMetrics.isUnlimited() >> true
  }

  def "Unhealthy response with BlobStore reporting not enough available space"() {
    given: 'An unhealthy repository'
      String unhealthyRepositoryName = 'foo'
    when:
      Result result = blobStoreHealthCheck.check()

    then:
      !result.healthy
      result.message.contains(unhealthyRepositoryName)
      blobStoreManager.browse() >> [blobStore]
      blobStore.metrics >> blobStoreMetrics
      blobStoreMetrics.isUnlimited() >> false
      blobStoreMetrics.availableSpace >> (MINIMUM_SIZE - 1)
      blobStore.blobStoreConfiguration >> blobStoreConfiguration
      blobStoreConfiguration.name >> unhealthyRepositoryName
  }
}
