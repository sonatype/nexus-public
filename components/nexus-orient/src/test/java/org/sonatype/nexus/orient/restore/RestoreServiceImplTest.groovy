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

import org.sonatype.nexus.common.app.ApplicationVersion
import org.sonatype.nexus.orient.DatabaseManager
import org.sonatype.nexus.orient.DatabaseRestorer

import org.spockframework.lang.Wildcard
import spock.lang.Specification

import static org.sonatype.nexus.orient.DatabaseInstanceNames.DATABASE_NAMES

/**
 * Unit tests for {@link RestoreServiceImpl}.
 */
class RestoreServiceImplTest
  extends Specification
{
  DatabaseRestorer restorer = Mock(DatabaseRestorer.class)

  DatabaseManager manager = Mock(DatabaseManager.class)

  ApplicationVersion applicationVersion = Mock(ApplicationVersion.class)

  RestoreServiceImpl restoreService = new RestoreServiceImpl(restorer, manager, applicationVersion)

  def setup() {
    applicationVersion.getVersion() >> '3.4.1'
  }

  def 'start silently succeeds when no backup files are present'() {
    given: 'no backup files are present'
      restorer.getPendingRestore(_) >> null

    when: 'start is executed'
      restoreService.start()

    then: 'start silently succeeds'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start succeeds when all backup files are present'() {
    given: 'all backup files are present'
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', null)

    when: 'start is executed'
      restoreService.start()

    then: 'start triggers restore successfully'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start succeeds when all backup files are present but have slightly different timestamps'() {
    given: 'all backup files are present'
      Iterator<String> names = DATABASE_NAMES.iterator()
      restorer.getPendingRestore(names.next()) >> mockRestoreFile(_, '2017-07-06-11-16-49', null)
      restorer.getPendingRestore(names.next()) >> mockRestoreFile(_, '2017-07-06-11-16-50', null)
      restorer.getPendingRestore(names.next()) >> mockRestoreFile(_, '2017-07-06-11-16-51', null)
      restorer.getPendingRestore(names.next()) >> mockRestoreFile(_, '2017-07-06-11-16-52', null)

    when: 'start is executed'
      restoreService.start()

    then: 'start triggers restore successfully'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start succeeds when all backup files are present and have same nxrm version'() {
    given: 'all backup files are present'
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', '3.4.1')

    when: 'start is executed'
      restoreService.start()

    then: 'start triggers restore successfully'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start succeeds when all backup files are present and have same historical nxrm version'() {
    given: 'all backup files are present'
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', '3.3.2')

    when: 'start is executed'
      restoreService.start()

    then: 'start triggers restore successfully'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start fails when all backup files are present and have future nxrm version'() {
    given: 'all backup files are present'
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', '3.5.0')

    when: 'start is executed'
      restoreService.start()

    then: 'start throws IllegalStateException'
      thrown IllegalStateException
  }

  def 'start fails if all backup files are present but one has a different nxrm version'() {
    given: 'all backup files are present'
      (DATABASE_NAMES.size() - 1) * restorer.getPendingRestore(_) >> mockRestoreFile(_, "2017-07-06-11-16-49", "3.4.1")
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', '3.4.0')

    when: 'start is executed'
      restoreService.start()

    then: 'start throws IllegalStateException'
      thrown IllegalStateException
  }


  def 'start fails if one backup file is missing'() {
    given: '1 backup file is missing'
      (DATABASE_NAMES.size() - 1) * restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', null)
      restorer.getPendingRestore(_) >> null

    when: 'start is executed'
      restoreService.start()

    then: 'start throws IllegalStateException'
      thrown IllegalStateException
  }

  def 'start fails if IOException occurs checking for backup files'() {
    given: 'hasPendingRestore will throw an IO Exception'
      restorer.getPendingRestore(_) >> { throw new IOException() }

    when: 'start is executed'
      restoreService.start()

    then: 'start throws IOException'
      thrown IOException
  }

  def 'start succeeds when running SNAPSHOT version and all backup files are present with same base version'() {
    given: 'all backup files are present'
      applicationVersion.getVersion() >> '3.4.1-SNAPSHOT'
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', '3.4.1')

    when: 'start is executed'
      restoreService.start()

    then: 'start triggers restore successfully'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start succeeds when running SNAPSHOT version and all backup files are present with previous base version'() {
    given: 'all backup files are present'
      applicationVersion.getVersion() >> '3.5.0-SNAPSHOT'
      restorer.getPendingRestore(_) >> mockRestoreFile(_, '2017-07-06-11-16-49', '3.4.1')

    when: 'start is executed'
      restoreService.start()

    then: 'start triggers restore successfully'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def mockRestoreFile(Wildcard database, String timestamp, String version) {
    RestoreFile file = Mock(RestoreFile)
    file.getDatabaseName() >> database.toString()
    file.getTimestamp() >> timestamp
    file.getVersion() >> version
    return file
  }
}
