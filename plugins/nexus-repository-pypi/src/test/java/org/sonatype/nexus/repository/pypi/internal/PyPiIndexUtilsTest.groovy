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
import spock.lang.Unroll

/**
 * {@link PyPiIndexUtils} unit tests.
 */
class PyPiIndexUtilsTest
    extends Specification
{
  def 'Correctly extract links and filenames from an index page'() {
    when: 'A valid index page is parsed'
      List<PyPiLink> links = getClass().getResourceAsStream('sample-index.html').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input)
      }
    then: 'the links will be extracted'
      links == [
          new PyPiLink('sample-1.2.0.tar.bz2', '/packages/sample-1.2.0.tar.bz2#md5=00c3db1c8ab5d10a2049fe384c8d53e5'),
          new PyPiLink('sample-1.2.1-py2.py3-none-any.whl',
              '/packages/sample-1.2.1-py2.py3-none-any.whl#md5=5c286195d47014fa0ba6e4e5b0801faf'),
          new PyPiLink('sample-1.2.1.tar.gz', '/packages/sample-1.2.1.tar.gz#md5=8c59858420df240b68e259ceae78a11d',
              '>=3.7')
      ]
  }

  def 'Correctly handle an empty index page'() {
    when: 'An empty index page is parsed'
      List<PyPiLink> links = getClass().getResourceAsStream('sample-empty-index.html').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input)
      }
    then: 'an empty collection is returned'
      links == []
  }

  @Unroll
  def 'Correctly rewrite links on an index page, skipping over unprocessable ones'() {
    when: 'Links are rewritten so that the packages directory is a relative link'
      def links = PyPiIndexUtils.makePackageLinksNexusPaths('sample', [new PyPiLink('sample-1.2.0.tar.bz2', original, '3.7')])
    then: 'the resulting links will be correct (relative to the index)'
      links == [new PyPiLink('sample-1.2.0.tar.bz2', expected, '3.7')]

    where:
     original                                       | expected
      // link without a hash
     '../../packages/sample-1.2.0.tar.bz'            | '../../packages/sample/1.2.0/sample-1.2.0.tar.bz2'
      // link with a hash
     '../../packages/sample-1.2.0.tar.bz2#md5=00e5' | '../../packages/sample/1.2.0/sample-1.2.0.tar.bz2#md5=00e5'
  }

  def 'Correctly extract links from a root index page'() {
    when: 'A valid index page is parsed'
      List<PyPiLink> links = getClass().getResourceAsStream('pypi_index.htm').withCloseable { input ->
        PyPiIndexUtils.extractLinksFromIndex(input)
      }
    then: 'the links will be extracted'
      links == [
          new PyPiLink('1and1', '/simple/1and1/'),
          new PyPiLink('ansible-shell', '/simple/ansible-shell/'),
          new PyPiLink('roleplay', '/simple/roleplay/'),
          new PyPiLink('tibl-cli', '/simple/tibl-cli/'),
          new PyPiLink('zzhfun', '/simple/zzhfun/'),
      ]
  }

  def 'Correctly rewrite links on a root index page'() {
    when: 'A valid index page is parsed'
      List<PyPiLink> links = getClass().getResourceAsStream('pypi_index.htm').withCloseable { input ->
        PyPiIndexUtils.makeRootIndexLinksRelative(PyPiIndexUtils.extractLinksFromIndex(input))
      }
    then: 'the links will be extracted'
      links == [
          new PyPiLink('1and1', '1and1/'),
          new PyPiLink('ansible-shell', 'ansible-shell/'),
          new PyPiLink('roleplay', 'roleplay/'),
          new PyPiLink('tibl-cli', 'tibl-cli/'),
          new PyPiLink('zzhfun', 'zzhfun/'),
      ]
  }
}
