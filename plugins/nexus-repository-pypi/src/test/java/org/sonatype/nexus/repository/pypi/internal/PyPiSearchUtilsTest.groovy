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
package org.sonatype.nexus.repository.pypi.internal

import groovy.json.JsonSlurper
import groovy.util.slurpersupport.GPathResult
import org.elasticsearch.index.query.QueryBuilder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * {@link PyPiSearchUtils} unit tests.
 */
class PyPiSearchUtilsTest
    extends Specification
{
  def 'Correctly extract name and summary parameters from a search request'() {
    when: 'A valid search request captured from traffic is parsed'
      QueryBuilder query = getClass().getResourceAsStream('single-value-search-request.xml').withCloseable { input ->
        PyPiSearchUtils.parseSearchRequest('my-repo', input)
      }
    then: 'the returned parameter values should be correct'
      new JsonSlurper().parseText(query.toString()) == [
          'bool': [
              'filter'              : [
                  'term': [
                      'repository_name': 'my-repo'
                  ]
              ],
              'should'              : [
                  ['wildcard': [
                      'attributes.pypi.name': '*pyglet*'
                  ]],
                  ['wildcard': [
                      'attributes.pypi.summary': '*pyglet*'
                  ]]
              ],
              'minimum_should_match': '1'
          ]
      ]
  }

  def 'Correctly extract name and summary parameters from a search request with multiple values'() {
    when: 'A valid search request captured from traffic is parsed'
      QueryBuilder query = getClass().getResourceAsStream('multiple-value-search-request.xml').withCloseable { input ->
        PyPiSearchUtils.parseSearchRequest('my-repo', input)
      }
    then: 'the returned parameter values should be correct'
      new JsonSlurper().parseText(query.toString()) == [
          'bool': [
              'filter'              : [
                  'term': [
                      'repository_name': 'my-repo'
                  ]
              ],
              'should'              : [
                  ['wildcard': [
                      'attributes.pypi.name': '*foo*'
                  ]],
                  ['wildcard': [
                      'attributes.pypi.name': '*bar*'
                  ]],
                  ['wildcard': [
                      'attributes.pypi.summary': '*foo*'
                  ]],
                  ['wildcard': [
                      'attributes.pypi.summary': '*bar*'
                  ]]
              ],
              'minimum_should_match': '1'
          ]
      ]
  }

  @Unroll
  def 'Correctly throw exception for invalid search request #source'() {
    when: 'An unsupported or erroneous search request captured from traffic is parsed'
      getClass().getResourceAsStream(source).withCloseable { input ->
        PyPiSearchUtils.parseSearchRequest('my-repo', input)
      }
    then: 'an exception should be thrown'
      thrown expectedException
    where:
      source                                    | expectedException
      'unsupported-field-search-request.xml'    | UnsupportedOperationException
      'unsupported-operator-search-request.xml' | UnsupportedOperationException
      'unsupported-method-search-request.xml'   | UnsupportedOperationException
  }

  def 'Correctly build a search result response from search hits'() {
    when: 'A search result response is generated from search hits'
      String response = PyPiSearchUtils.buildSearchResponse([
          new PyPiSearchResult('foo', '1.0.1', 'Test summary for the search result.'),
          new PyPiSearchResult('foo', '1.0.3a', 'Test summary for the search result.')
      ])
      GPathResult result = new XmlSlurper().parseText(response)
    then: 'the output should contain the expected XML content'
      result.name() == 'methodResponse'

      result.params.param.value.array.data.value.struct[0].member[0].name.text() == '_pypi_ordering'
      result.params.param.value.array.data.value.struct[0].member[0].value.boolean.text() == '0'
      result.params.param.value.array.data.value.struct[0].member[1].name.text() == 'name'
      result.params.param.value.array.data.value.struct[0].member[1].value.string.text() == 'foo'
      result.params.param.value.array.data.value.struct[0].member[2].name.text() == 'version'
      result.params.param.value.array.data.value.struct[0].member[2].value.string.text() == '1.0.1'
      result.params.param.value.array.data.value.struct[0].member[3].name.text() == 'summary'
      result.params.param.value.array.data.value.struct[0].member[3].value.string.text() ==
          'Test summary for the search result.'

      result.params.param.value.array.data.value.struct[1].member[0].name.text() == '_pypi_ordering'
      result.params.param.value.array.data.value.struct[1].member[0].value.boolean.text() == '0'
      result.params.param.value.array.data.value.struct[1].member[1].name.text() == 'name'
      result.params.param.value.array.data.value.struct[1].member[1].value.string.text() == 'foo'
      result.params.param.value.array.data.value.struct[1].member[2].name.text() == 'version'
      result.params.param.value.array.data.value.struct[1].member[2].value.string.text() == '1.0.3a'
      result.params.param.value.array.data.value.struct[1].member[3].name.text() == 'summary'
      result.params.param.value.array.data.value.struct[1].member[3].value.string.text() ==
          'Test summary for the search result.'
  }

  def 'Correctly build an empty search result response'() {
    when: 'No search hits are found'
      String response = PyPiSearchUtils.buildSearchResponse([])
      GPathResult result = new XmlSlurper().parseText(response)
    then: 'the output should contain no values'
      result.name() == 'methodResponse'
      result.params.param.value.array.data.text() == ''
  }

  def 'Correctly parse a valid search response'() {
    when: 'A valid search response is parsed'
      List<PyPiSearchResult> results =
          getClass().getResourceAsStream('valid-search-response.xml').withCloseable { input ->
            PyPiSearchUtils.parseSearchResponse(input)
          }
    then: 'the entries in the response body are returned'
      results.size() == 3

      results[0].version == '0.1.0'
      results[0].name == 'foo'
      results[0].summary == 'Summary for foo.'

      results[1].version == '0.2.0'
      results[1].name == 'bar'
      results[1].summary == 'Summary for bar.'

      results[2].version == '0.3.0'
      results[2].name == 'baz'
      results[2].summary == ''
  }

  def 'Correctly ignore a fault search response'() {
    when: 'A fault search response is parsed'
      List<PyPiSearchResult> results =
          getClass().getResourceAsStream('fault-search-response.xml').withCloseable { input ->
            PyPiSearchUtils.parseSearchResponse(input)
          }
    then: 'no exception is thrown but no results are returned'
      results.isEmpty()
  }
}
