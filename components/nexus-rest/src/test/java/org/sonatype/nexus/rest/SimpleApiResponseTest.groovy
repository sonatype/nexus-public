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
package org.sonatype.nexus.rest

import javax.ws.rs.core.Response.Status

import spock.lang.Specification

import static javax.ws.rs.core.Response.Status.BAD_REQUEST
import static javax.ws.rs.core.Response.Status.NOT_FOUND
import static javax.ws.rs.core.Response.Status.OK
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED

class SimpleApiResponseTest
    extends Specification
{
  def "OK response without data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.ok('message')

    then: 'response is as expected'
      assertResponse(simpleApiResponse, OK, null)
  }

  def "OK response with data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.ok('message', [foo: 'bar'])

    then: 'response is as expected'
      assertResponse(simpleApiResponse, OK, 'bar')
  }

  def "Not found response without data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.notFound('message')

    then: 'response is as expected'
      assertResponse(simpleApiResponse, NOT_FOUND, null)
  }

  def "Not found response with data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.notFound('message', [foo: 'bar'])

    then: 'response is as expected'
      assertResponse(simpleApiResponse, NOT_FOUND, 'bar')
  }

  def "Bad request response without data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.badRequest('message')

    then: 'response is as expected'
      assertResponse(simpleApiResponse, BAD_REQUEST, null)
  }

  def "Bad request found response with data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.badRequest('message', [foo: 'bar'])

    then: 'response is as expected'
      assertResponse(simpleApiResponse, BAD_REQUEST, 'bar')
  }

  def "Unauthorized response without data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.unauthorized('message')

    then: 'response is as expected'
      assertResponse(simpleApiResponse, UNAUTHORIZED, null)
  }

  def "Unauthorized found response with data"() {
    when: 'the test object'
      def simpleApiResponse = SimpleApiResponse.unauthorized('message', [foo: 'bar'])

    then: 'response is as expected'
      assertResponse(simpleApiResponse, UNAUTHORIZED, 'bar')
  }

  def assertResponse(def simpleApiResponse, Status status, String value) {
    simpleApiResponse.status == status.statusCode
    def entity = simpleApiResponse.entity
    entity.status == status.statusCode
    entity.message == 'message'
    if (value == null) {
      entity.data == null
    }
    else {
      entity.data.foo == 'bar'
    }
  }
}
