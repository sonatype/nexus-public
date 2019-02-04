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

package org.sonatype.nexus.rapture.internal.state

import org.sonatype.nexus.rapture.internal.HealthCheckCacheManager

import com.codahale.metrics.health.HealthCheck.Result
import spock.lang.Specification

import static org.sonatype.nexus.rapture.internal.state.HealthCheckStateContributor.HC_FAILED_KEY

class HealthCheckStateContributorTest
    extends Specification
{
  def "It will return an indicator if any health checks have failed"() {
    given:
      def healthCheckCacheManager = Mock(HealthCheckCacheManager) {
        getResults() >> [
            a: Result.healthy(),
            b: result,
            c: Result.healthy()
        ]
      }
      def subject = new HealthCheckStateContributor(healthCheckCacheManager)

    when:
      def state = subject.state

    then:
      state[HC_FAILED_KEY] == expectedState

    where:
      expectedState | result
      true          | Result.unhealthy('Not healthy')
      false         | Result.healthy()
  }
}
