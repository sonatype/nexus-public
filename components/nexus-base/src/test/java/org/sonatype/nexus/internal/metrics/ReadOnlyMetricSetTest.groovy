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
package org.sonatype.nexus.internal.metrics

import org.sonatype.nexus.common.app.FreezeRequest
import org.sonatype.nexus.common.app.FreezeService

import org.joda.time.DateTime
import spock.lang.Specification

class ReadOnlyMetricSetTest
    extends Specification {

  FreezeService freezeService = Mock()

  def 'default metrics generated when no freeze service available'() {
    when:
      ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet({ -> null})
      def metrics = readOnlyMetricSet.metrics.collectEntries { name, metric ->
          [name, metric.value]
      }

    then:
      metrics.enabled == false 
      metrics.pending == 0
      metrics.freezeTime == 0L
  }

  def 'false metrics generated when not in read only mode'() {
    when:
      ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet({ -> freezeService})
      def metrics = readOnlyMetricSet.metrics.collectEntries { name, metric ->
        [name, metric.value]
      }

    then:
      1 * freezeService.isFrozen() >> false
      2 * freezeService.currentFreezeRequests() >> []
      metrics.enabled == false 
      metrics.pending == 0
      metrics.freezeTime == 0L
  }

  def 'expected metrics generated when in read only mode'() {
    given:
      def freezeRequests = [
        new FreezeRequest('SYSTEM', "system initiator", new DateTime(1504111817165), null, null),
        new FreezeRequest('USER', "user initiator", new DateTime(1504111817166), null, null)
      ]

    when:
      ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet({ -> freezeService})
      def metrics = readOnlyMetricSet.metrics.collectEntries { name, metric ->
        [name, metric.value]
      }

    then:
      1 * freezeService.isFrozen() >> true
      2 * freezeService.currentFreezeRequests() >> freezeRequests
      metrics.enabled == true 
      metrics.pending == 2
      metrics.freezeTime == 1504111817165
  }
}
