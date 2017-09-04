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
package org.sonatype.nexus.orient.restore

import java.time.LocalDateTime

import spock.lang.Specification

/**
 * Unit tests for {@link RestoreFile}.
 */
class RestoreFileTest
  extends Specification
{

  static File tempDir

  def setupSpec() {
    tempDir = File.createTempDir()
  }

  def cleanupSpec() {
    tempDir.delete()
  }

  def 'newInstance successful on file with full name including version'() {
    given: 'a path pointing to file with full name format'
      File tempFile = new File(tempDir, 'config-2017-07-06-11-16-49-3.4.1.bak')

    when: 'RestoreFile.newInstance'
      RestoreFile restoreFile = RestoreFile.newInstance(tempFile.toPath())

    then: 'valid instance'
      restoreFile.getDatabaseName() == 'config'
      restoreFile.getTimestamp() == '2017-07-06-11-16-49'
      restoreFile.getVersion() == '3.4.1'
  }

  def 'newInstance successful on file with name and timestamp but no version'() {
    given: 'a path pointing to file with name missing version'
      File tempFile = new File(tempDir, 'config-2017-07-06-11-16-49.bak')

    when: 'RestoreFile.newInstance'
      RestoreFile restoreFile = RestoreFile.newInstance(tempFile.toPath())

    then: 'valid instance'
      restoreFile.getDatabaseName() == 'config'
      restoreFile.getTimestamp() == '2017-07-06-11-16-49'
      restoreFile.getVersion() == null
  }

  def 'newInstance throws IllegalArgumentException for invalid filenames' () {
    given: 'a path pointing to file with invalid name'
      File tempFile = new File(tempDir, 'something.bak')

    when: 'RestoreFile.newInstance'
      RestoreFile.newInstance(tempFile.toPath())

    then: 'exception thrown'
      thrown IllegalArgumentException
  }

  def 'formatFilename works with release version' () {
    when:
      String filename = RestoreFile.formatFilename('config',
          LocalDateTime.now(),
          '3.4.0')

    then:
      filename.endsWith('3.4.0.bak')
  }

  def 'formatFilename works with snapshot version' () {
    when:
      String filename = RestoreFile.formatFilename('config',
          LocalDateTime.now(),
          '3.4.0-SNAPSHOT')

    then:
      filename.endsWith('3.4.0.bak')
  }

  def 'toString prints attribute values' () {
    given: 'a path pointing to file for populating attributes'
      File tempFile = new File(tempDir, 'config-2017-07-06-11-16-49-3.4.1.bak')

    when: 'RestoreFile.newInstance'
      RestoreFile restoreFile = RestoreFile.newInstance(tempFile.toPath())

    then:
      restoreFile.toString() == '{databaseName=config, timestamp=2017-07-06-11-16-49, version=3.4.1}'
  }

  def 'toString prints attribute values null with version' () {
    given: 'a path pointing to file for populating attributes'
      File tempFile = new File(tempDir, 'config-2017-07-06-11-16-49.bak')

    when: 'RestoreFile.newInstance'
      RestoreFile restoreFile = RestoreFile.newInstance(tempFile.toPath())

    then:
      restoreFile.toString() == '{databaseName=config, timestamp=2017-07-06-11-16-49, version=null}'
  }

}
