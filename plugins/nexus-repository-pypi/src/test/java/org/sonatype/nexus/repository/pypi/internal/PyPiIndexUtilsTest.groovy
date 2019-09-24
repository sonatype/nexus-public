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

import spock.lang.Specification

import java.util.stream.Collector
import java.util.stream.Collectors

/**
 * {@link PyPiIndexUtils} unit tests.
 */
class PyPiIndexUtilsTest
    extends Specification
{
  def 'Correctly extract links and filenames from an index page'() {
    when: 'A valid index page is parsed'
      List<Map<String, String>> links = getClass().getResourceAsStream('sample-index.html').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input).stream().map({ link ->
          PyPiIndexUtils.indexLinkToMap(link)
        }).collect(Collectors.toList())
      }
    then: 'the links will be extracted'
      links == [
        ['file': 'sample-1.2.0.tar.bz2', 'link': '../../packages/sample-1.2.0.tar.bz2#md5=00c3db1c8ab5d10a2049fe384c8d53e5', 'data-requires-python': ''],
        ['file': 'sample-1.2.1-py2.py3-none-any.whl', 'link': '../../packages/sample-1.2.1-py2.py3-none-any.whl#md5=5c286195d47014fa0ba6e4e5b0801faf', 'data-requires-python': ''],
        ['file': 'sample-1.2.1.tar.gz', 'link': '../../packages/sample-1.2.1.tar.gz#md5=8c59858420df240b68e259ceae78a11d', 'data-requires-python': '>=3.7']
      ]
  }

  def 'Correctly handle an empty index page'() {
    when: 'An empty index page is parsed'
    List<Map<String, String>> links = getClass().getResourceAsStream('sample-empty-index.html').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input).stream().map({ link ->
          PyPiIndexUtils.indexLinkToMap(link)
        }).collect(Collectors.toList())
    }
    then: 'an empty collection is returned'
      links == []
  }

  def 'Correctly rewrite links on an index page, skipping over unprocessable ones'() {
    when: 'Links are rewritten so that the packages directory is a relative link'
        List<Map<String, String>> links = getClass().getResourceAsStream('sample-index-invalid.html').withCloseable { input ->
            PyPiIndexUtils.extractLinksFromIndex(input).stream().map({ link ->
                PyPiIndexUtils.indexLinkToMap(link)
            }).collect(Collectors.toList())
        }
    then: 'the resulting links will be correct (relative to the index)'
      links == [
          ['file': 'sample-1.2.0.tar.bz2', 'link':'../../packages/sample-1.2.0.tar.bz2#md5=00c3db1c8ab5d10a2049fe384c8d53e5', 'data-requires-python': ''],
          ['file': 'sample-1.2.1-py2.py3-none-any.whl', 'link': '../../packages/sample-1.2.1-py2.py3-none-any.whl#md5=5c286195d47014fa0ba6e4e5b0801faf', 'data-requires-python': ''],
          ['file': 'sample-1.2.2-py2.py3-none-any.whl', 'link': '../../packages/sample-1.2.1-py2.py3-none-any.whl#md5=5c286195d47014fa0ba6e4e5b0801faf', 'data-requires-python': ''],
          ['file': 'test', 'link': 'http://example.com/test', 'data-requires-python': '']
      ]
  }

  def 'Correctly extract links from a root index page'() {
    when: 'A valid index page is parsed'
      Map<String, String> links = getClass().getResourceAsStream('pypi_index.htm').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input).stream().collect(
          Collectors.toMap({ link -> link.getFile() }, { link -> link.getLink() })
        )
      }
    then: 'the links will be extracted'
      links == [
          '1and1' : '/simple/1and1/',
          'ansible-shell' : '/simple/ansible-shell/',
          'roleplay' : '/simple/roleplay/',
          'tibl-cli' : '/simple/tibl-cli/',
          'zzhfun' : '/simple/zzhfun/'
      ]
  }

  def 'Correctly rewrite links on a root index page'() {
    when: 'A valid index page is parsed'
      List<Map<String, String>> links = getClass().getResourceAsStream('pypi_index.htm').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input).stream().map({ link ->
          PyPiIndexUtils.rootIndexLinkToMap(link)
        }).collect(Collectors.toList())
      }
    then: 'the links will be extracted'
      links == [
          ['name': '1and1', 'link': '1and1/'],
          ['name': 'ansible-shell', 'link': 'ansible-shell/'],
          ['name': 'roleplay', 'link': 'roleplay/'],
          ['name': 'tibl-cli', 'link': 'tibl-cli/'],
          ['name': 'zzhfun', 'link': 'zzhfun/']
      ]
  }
}
