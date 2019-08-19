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
package org.sonatype.nexus.blobstore.group.internal

import org.sonatype.nexus.blobstore.api.BlobStoreMetrics

import spock.lang.Specification

/**
 * {@link BlobStoreGroupMetrics} tests.
 */
class BlobStoreGroupMetricsTest
    extends Specification
{
  def 'Empty metrics is available'() {
    when: 'metrics is empty'
      def metrics = new BlobStoreGroupMetrics([])

    then: 'it is available'
      metrics.unavailable == false
  }

  def 'Metrics with no available member is unavailable'() {
    given:
      BlobStoreMetrics u1 = Mock()
      BlobStoreMetrics u2 = Mock()

    when: 'metrics has two unavailable members'
      def metrics = new BlobStoreGroupMetrics([u1, u2])

    then: 'it is unavailable'
      metrics.unavailable == true
      1 * u1.isUnavailable() >> true
      _ * u1.getAvailableSpaceByFileStore() >> [:]
      1 * u2.isUnavailable() >> true
      _ * u2.getAvailableSpaceByFileStore() >> [:]
  }

  def 'Metrics with one available member is available'() {
    given:
      BlobStoreMetrics u1 = Mock()
      BlobStoreMetrics a1 = Mock()

    when: 'metrics has one available member and one unavailable member'
      def metrics = new BlobStoreGroupMetrics([u1, a1])

    then: 'it is available'
      metrics.unavailable == false
      1 * u1.isUnavailable() >> true
      _ * u1.getAvailableSpaceByFileStore() >> [:]
      1 * a1.isUnavailable() >> false
      _ * a1.getAvailableSpaceByFileStore() >> [:]
  }
}
