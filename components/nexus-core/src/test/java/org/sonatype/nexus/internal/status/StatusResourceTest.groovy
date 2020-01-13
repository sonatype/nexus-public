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
package org.sonatype.nexus.internal.status

import javax.ws.rs.core.Response

import org.sonatype.nexus.common.app.FreezeService
import org.sonatype.nexus.common.app.NotReadableException
import org.sonatype.nexus.common.app.NotWritableException

import com.codahale.metrics.health.HealthCheckRegistry
import com.codahale.metrics.health.HealthCheck.Result
import spock.lang.Specification

class StatusResourceTest
    extends Specification
{
  final FreezeService freezeService = Mock()

  final HealthCheckRegistry registry = Mock()

  final StatusResource statusResource = new StatusResource(freezeService, registry)

  def 'is available if server can execute read check'() {
    when:
      Response response = statusResource.isAvailable()
    then:
      1 * freezeService.checkReadable(_)
      response.status == 200
  }

  def 'is not available if server can not execute read check'() {
    given:
      freezeService.checkReadable(_) >> { throw new NotReadableException('mock test error') }
    when:
      Response response = statusResource.isAvailable()
    then:
      response.status == 503
  }

  def 'is writable if server can execute write check'() {
    when:
      Response response = statusResource.isWritable()
    then:
      1 * freezeService.checkWritable(_)
      response.status == 200
  }

  def 'is not writable if server can not execute write check'() {
    given:
      freezeService.checkWritable(_) >> { throw new NotWritableException('mock test error') }
    when:
      Response response = statusResource.isWritable()
    then:
      response.status == 503
  }

  def 'is not writable and no write check is made if database is frozen'() {
    given:
      freezeService.isFrozen() >> true
    when:
      Response response = statusResource.isWritable()
    then:
      0 * freezeService.checkWritable()
      response.status == 503

  }

  def 'returns the system status checks'() {
    given:
      def expectedStatusChecks = [:] as SortedMap<String, Result>
      registry.runHealthChecks() >> expectedStatusChecks

    when:
      SortedMap<String, Result> systemStatusChecks = statusResource.systemStatusChecks

    then:
      systemStatusChecks == expectedStatusChecks
  }
}
