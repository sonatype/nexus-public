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

import org.sonatype.nexus.orient.DatabaseManager
import org.sonatype.nexus.orient.DatabaseRestorer

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

  RestoreServiceImpl restoreService = new RestoreServiceImpl(restorer, manager)

  def 'start silently succeeds when no backup files are present'() {
    given: 'no backup files are present'
      restorer.hasPendingRestore(_) >> false

    when: 'start is executed'
      restoreService.start()

    then: 'start silently succeeds'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start succeeds when all backup files are present'() {
    given: 'all backup files are present'
      restorer.hasPendingRestore(_) >> true

    when: 'start is executed'
      restoreService.start()

    then: 'start silently succeeds'
      DATABASE_NAMES.size() * manager.instance(_)
  }

  def 'start fails if one backup file is missing'() {
    given: '1 backup file is missing'
      (DATABASE_NAMES.size() - 1) * restorer.hasPendingRestore(_) >> true
      restorer.hasPendingRestore(_) >> false

    when: 'start is executed'
      restoreService.start()

    then: 'start throws IllegalStateException'
      thrown IllegalStateException
  }

  def 'start fails if IOException occurs checking for backup files'() {
    given: 'hasPendingRestore will throw an IO Exception'
      restorer.hasPendingRestore(_) >> { throw new IOException() }

    when: 'start is executed'
      restoreService.start()

    then: 'start throws IOException'
      thrown IOException
  }
}
