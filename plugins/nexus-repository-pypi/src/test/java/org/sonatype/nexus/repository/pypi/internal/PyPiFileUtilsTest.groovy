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
 * {@link PyPiFileUtils} unit tests.
 */
class PyPiFileUtilsTest
    extends Specification
{
  @Unroll
  def 'Extract version number from #filename'() {
    expect:
      PyPiFileUtils.extractVersionFromFilename(filename) == version
    where:
      filename                                  | version
      'aldryn-addons-0.1.tar.gz'                | '0.1'
      'SQLObject-3.0.0a2dev_20151224-py2.6.egg' | '3.0.0a2dev_20151224'
      'pyglet-1.2.4-py2-none-any.whl'           | '1.2.4'
      'anyit.djattributes-0.2.5-py2.6.egg'      | '0.2.5'
      'Twisted-16.1.1.tar.bz2'                  | '16.1.1'
      'Twisted-16.1.1-cp27-none-win_amd64.whl'  | '16.1.1'
      'Twisted-11.1.0.win32-py2.7.msi'          | '11.1.0.win32'
      'Twisted-15.4.0.win-amd64-py2.7.exe'      | '15.4.0.win'
      'name-version.foo'                        | 'version'
      'name-version'                            | 'version'
  }

  @Unroll
  def 'Extract name from #filename'() {
    expect:
      PyPiFileUtils.extractNameFromFilename(filename) == name
    where:
      filename                                  | name
      'aldryn-addons-0.1.tar.gz'                | 'aldryn-addons'
      'SQLObject-3.0.0a2dev_20151224-py2.6.egg' | 'SQLObject'
      'pyglet-1.2.4-py2-none-any.whl'           | 'pyglet'
      'anyit.djattributes-0.2.5-py2.6.egg'      | 'anyit.djattributes'
      'Twisted-16.1.1.tar.bz2'                  | 'Twisted'
      'Twisted-16.1.1-cp27-none-win_amd64.whl'  | 'Twisted'
      'Twisted-11.1.0.win32-py2.7.msi'          | 'Twisted'
      'Twisted-15.4.0.win-amd64-py2.7.exe'      | 'Twisted'
      'name-version.foo'                        | 'name'
      'name-version'                            | 'name'
  }

  @Unroll
  def 'Extract extension from #filename'() {
    expect:
      PyPiFileUtils.extractExtension(filename) == extension
    where:
      filename                                  | extension
      'aldryn-addons-0.1.tar.gz'                | '.tar.gz'
      'SQLObject-3.0.0a2dev_20151224-py2.6.egg' | '.egg'
      'pyglet-1.2.4-py2-none-any.whl'           | '.whl'
      'anyit.djattributes-0.2.5-py2.6.egg'      | '.egg'
      'Twisted-16.1.1.tar.bz2'                  | '.tar.bz2'
      'Twisted-16.1.1-cp27-none-win_amd64.whl'  | '.whl'
      'Twisted-11.1.0.win32-py2.7.msi'          | '.msi'
      'Twisted-15.4.0.win-amd64-py2.7.exe'      | '.exe'
      'name-1.2.3.tar.bz2'                      | '.tar.bz2'
      'name-1.2.3.tbz'                          | '.tbz'
      'name-1.2.3.tar.gz'                       | '.tar.gz'
      'name-1.2.3.tgz'                          | '.tgz'
      'name-1.2.3.tlz'                          | '.tlz'
      'name-1.2.3.tar.lz'                       | '.tar.lz'
      'name-1.2.3.tar.lzma'                     | '.tar.lzma'
      'name-1.2.3.tar.xz'                       | '.tar.xz'
      'name-1.2.3.txz'                          | '.txz'
      'name-1.2.3.tar.Z'                        | '.tar.z'
      'name-1.2.3.tz'                           | '.tz'
      'name-1.2.3.taz'                          | '.taz'
      'name-1.2.3.tar'                          | '.tar'
      'name-1.2.3.TAR'                          | '.tar'
      'name-version.foo'                        | '.foo'
      'NAME-VERSION.FOO'                        | '.foo'
      'name-version'                            | ''
  }

  @Unroll
  def 'Remove extension from #filename'() {
    expect:
      PyPiFileUtils.removeExtension(filename) == filenameWithoutExtension
    where:
      filename                                  | filenameWithoutExtension
      'aldryn-addons-0.1.tar.gz'                | 'aldryn-addons-0.1'
      'SQLObject-3.0.0a2dev_20151224-py2.6.egg' | 'SQLObject-3.0.0a2dev_20151224-py2.6'
      'pyglet-1.2.4-py2-none-any.whl'           | 'pyglet-1.2.4-py2-none-any'
      'anyit.djattributes-0.2.5-py2.6.egg'      | 'anyit.djattributes-0.2.5-py2.6'
      'Twisted-16.1.1.tar.bz2'                  | 'Twisted-16.1.1'
      'Twisted-16.1.1-cp27-none-win_amd64.whl'  | 'Twisted-16.1.1-cp27-none-win_amd64'
      'Twisted-11.1.0.win32-py2.7.msi'          | 'Twisted-11.1.0.win32-py2.7'
      'Twisted-15.4.0.win-amd64-py2.7.exe'      | 'Twisted-15.4.0.win-amd64-py2.7'
      'name-1.2.3.tar.bz2'                      | 'name-1.2.3'
      'name-1.2.3.tbz'                          | 'name-1.2.3'
      'name-1.2.3.tar.gz'                       | 'name-1.2.3'
      'name-1.2.3.tgz'                          | 'name-1.2.3'
      'name-1.2.3.tlz'                          | 'name-1.2.3'
      'name-1.2.3.tar.lz'                       | 'name-1.2.3'
      'name-1.2.3.tar.lzma'                     | 'name-1.2.3'
      'name-1.2.3.tar.xz'                       | 'name-1.2.3'
      'name-1.2.3.txz'                          | 'name-1.2.3'
      'name-1.2.3.tar.Z'                        | 'name-1.2.3'
      'name-1.2.3.tz'                           | 'name-1.2.3'
      'name-1.2.3.taz'                          | 'name-1.2.3'
      'name-1.2.3.tar'                          | 'name-1.2.3'
      'name-1.2.3.TAR'                          | 'name-1.2.3'
      'name-version.foo'                        | 'name-version'
      'NAME-VERSION.FOO'                        | 'NAME-VERSION'
      'name-version'                            | 'name-version'
  }
}
