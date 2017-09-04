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
package org.sonatype.nexus.orient.internal.freeze

import org.joda.time.DateTime
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService
import org.sonatype.nexus.orient.freeze.FreezeRequest
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType

import spock.lang.Specification

class ReadOnlyMetricSetTest
    extends Specification {

  DatabaseFreezeService databaseFreezeService = Mock()

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
      ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet({ -> databaseFreezeService})
      def metrics = readOnlyMetricSet.metrics.collectEntries { name, metric ->
        [name, metric.value]
      }

    then:
      1 * databaseFreezeService.isFrozen() >> false
      2 * databaseFreezeService.getState() >> []
      metrics.enabled == false 
      metrics.pending == 0
      metrics.freezeTime == 0L
  }

  def 'expected metrics generated when in read only mode'() {
    given:
      def freezeRequests = [
        new FreezeRequest(InitiatorType.SYSTEM, "system initiator", new DateTime(1504111817165)),
        new FreezeRequest(InitiatorType.USER_INITIATED, "user initiator", new DateTime(1504111817166))
      ]

    when:
      ReadOnlyMetricSet readOnlyMetricSet = new ReadOnlyMetricSet({ -> databaseFreezeService})
      def metrics = readOnlyMetricSet.metrics.collectEntries { name, metric ->
        [name, metric.value]
      }

    then:
      1 * databaseFreezeService.isFrozen() >> true
      2 * databaseFreezeService.getState() >> freezeRequests
      metrics.enabled == true 
      metrics.pending == 2
      metrics.freezeTime == 1504111817165
  }
}
